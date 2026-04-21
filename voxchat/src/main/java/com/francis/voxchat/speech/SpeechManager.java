package com.francis.voxchat.speech;

import com.francis.voxchat.VoxChatMod;
import com.francis.voxchat.config.VoxChatConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Manages Vosk speech recognition.
 *
 * Responsibilities:
 *  - Download / verify Vosk model files into &lt;game-dir&gt;/voxchat-models/
 *  - Capture microphone audio on a dedicated background thread
 *  - Feed audio to Vosk Recognizer
 *  - Return the final transcription text via a callback
 *
 * Thread safety:
 *  - startListening() and stopListening() may be called from the MC client thread.
 *  - All Vosk work runs on the internal executor thread.
 *  - The result callback is invoked on the speech thread; callers must dispatch
 *    to the MC thread themselves (see KeybindHandler).
 */
public class SpeechManager {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** 16 kHz mono 16-bit PCM — required by Vosk */
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16_000, 16, 1, true, false);

    /** Buffer size: 16 kHz × 16 bit × 0.1 s = 3200 bytes → round to 4096 */
    private static final int BUFFER_SIZE = 4096;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final VoxChatConfig config;

    /** The loaded Vosk model — loaded lazily, reloaded on language change. */
    private volatile Model voskModel;

    /** Flag that controls the recording loop. */
    private final AtomicBoolean recording = new AtomicBoolean(false);

    /** Single-thread executor for all Vosk work (model load + recognition). */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "VoxChat-Speech");
                t.setDaemon(true);
                return t;
            });

    /** Callback to deliver the final text to when stopListening() is called. */
    private volatile Consumer<String> pendingCallback;

    /** Accumulated partial text while recording. */
    private final StringBuilder partialBuilder = new StringBuilder();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public SpeechManager(VoxChatConfig config) {
        this.config = config;
        // Pre-load the model in the background so first keypress is fast
        executor.submit(this::ensureModelLoaded);
    }

    // -------------------------------------------------------------------------
    // Public API — called from MC client thread
    // -------------------------------------------------------------------------

    /** Begin microphone capture and recognition. */
    public void startListening() {
        if (recording.get()) {
            VoxChatMod.LOGGER.warn("[VoxChat] startListening() called while already recording.");
            return;
        }
        recording.set(true);
        partialBuilder.setLength(0);

        executor.submit(this::runRecognitionLoop);
    }

    /**
     * Stop microphone capture. The provided callback is invoked (on the speech
     * thread) with the recognised text once the Vosk finalises the utterance.
     *
     * @param callback receives the recognised string; may be empty if nothing heard.
     */
    public void stopListening(Consumer<String> callback) {
        this.pendingCallback = callback;
        recording.set(false); // signals the recognition loop to exit
    }

    /** Reload the Vosk model (e.g. after a language switch). */
    public void reloadModel() {
        executor.submit(() -> {
            closeModel();
            ensureModelLoaded();
        });
    }

    /** Release all Vosk resources. Called on JVM shutdown. */
    public void shutdown() {
        recording.set(false);
        executor.shutdownNow();
        closeModel();
    }

    // -------------------------------------------------------------------------
    // Recognition loop — runs entirely on the executor thread
    // -------------------------------------------------------------------------

    private void runRecognitionLoop() {
        VoxChatMod.LOGGER.debug("[VoxChat] Recognition loop starting…");

        if (!ensureModelLoaded()) {
            VoxChatMod.LOGGER.error("[VoxChat] Cannot start recording: model not loaded.");
            deliverResult("");
            return;
        }

        TargetDataLine microphone = openMicrophone();
        if (microphone == null) {
            deliverResult("");
            return;
        }

        try (microphone; Recognizer recognizer = new Recognizer(voskModel, 16_000)) {
            microphone.start();
            byte[] buf = new byte[BUFFER_SIZE];

            while (recording.get()) {
                int bytesRead = microphone.read(buf, 0, buf.length);
                if (bytesRead <= 0) continue;

                if (recognizer.acceptWaveForm(buf, bytesRead)) {
                    // A complete utterance was detected mid-session; accumulate it
                    String partial = extractText(recognizer.getResult());
                    if (!partial.isBlank()) {
                        partialBuilder.append(partial).append(" ");
                    }
                }
            }

            // Flush whatever is left in the recognizer buffer
            String finalText = extractText(recognizer.getFinalResult());
            if (!finalText.isBlank()) {
                partialBuilder.append(finalText);
            }

            deliverResult(partialBuilder.toString().trim());

        } catch (Exception e) {
            VoxChatMod.LOGGER.error("[VoxChat] Recognition loop error: {}", e.getMessage(), e);
            deliverResult(partialBuilder.toString().trim());
        }

        VoxChatMod.LOGGER.debug("[VoxChat] Recognition loop finished.");
    }

    private void deliverResult(String text) {
        Consumer<String> cb = pendingCallback;
        pendingCallback = null;
        if (cb != null) cb.accept(text);
    }

    // -------------------------------------------------------------------------
    // Microphone setup
    // -------------------------------------------------------------------------

    private TargetDataLine openMicrophone() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                VoxChatMod.LOGGER.error("[VoxChat] Microphone line not supported by this system.");
                return null;
            }
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(AUDIO_FORMAT, BUFFER_SIZE * 4);
            return line;
        } catch (LineUnavailableException e) {
            VoxChatMod.LOGGER.error("[VoxChat] Could not open microphone: {}", e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Vosk model management
    // -------------------------------------------------------------------------

    /**
     * Ensures the Vosk model for the configured language is loaded.
     * Models live in: &lt;game-dir&gt;/voxchat-models/&lt;lang-code&gt;/
     *
     * If the model directory does not exist, a helpful error is logged with
     * a download URL.
     *
     * @return true if the model is ready, false on failure.
     */
    private synchronized boolean ensureModelLoaded() {
        if (voskModel != null) return true;

        String langCode = config.getLanguage().getCode();
        Path modelDir   = getModelDirectory(langCode);

        if (!Files.isDirectory(modelDir)) {
            logModelMissingHelp(langCode, modelDir);
            return false;
        }

        try {
            VoxChatMod.LOGGER.info("[VoxChat] Loading Vosk model from {} …", modelDir);
            voskModel = new Model(modelDir.toString());
            VoxChatMod.LOGGER.info("[VoxChat] Vosk model loaded for language '{}'.", langCode);
            return true;
        } catch (Exception e) {
            VoxChatMod.LOGGER.error("[VoxChat] Failed to load Vosk model: {}", e.getMessage(), e);
            return false;
        }
    }

    private synchronized void closeModel() {
        if (voskModel != null) {
            try { voskModel.close(); } catch (Exception ignored) {}
            voskModel = null;
        }
    }

    private Path getModelDirectory(String langCode) {
        return FabricLoader.getInstance()
                .getGameDir()
                .resolve("voxchat-models")
                .resolve(langCode);
    }

    private void logModelMissingHelp(String langCode, Path expectedPath) {
        VoxChatMod.LOGGER.error("""
                [VoxChat] ──────────────────────────────────────────────────────
                [VoxChat] Vosk model for language '{}' not found!
                [VoxChat] Expected directory: {}
                [VoxChat]
                [VoxChat] HOW TO FIX:
                [VoxChat]   1. Download the small model for your language:
                [VoxChat]      English : https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
                [VoxChat]      German  : https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip
                [VoxChat]   2. Unzip it.
                [VoxChat]   3. Rename the unzipped folder to the language code (e.g. 'en' or 'de').
                [VoxChat]   4. Place it in: <minecraft-dir>/voxchat-models/<lang-code>/
                [VoxChat]      So the directory should be: {}
                [VoxChat] ──────────────────────────────────────────────────────""",
                langCode, expectedPath, expectedPath);
    }

    // -------------------------------------------------------------------------
    // Vosk JSON parsing — minimal, avoids a full JSON library dependency
    // -------------------------------------------------------------------------

    /**
     * Vosk returns JSON like: {"text": "hello world"} or {"partial": "hel"}
     * We extract the value of the "text" key only.
     */
    static String extractText(String json) {
        if (json == null || json.isBlank()) return "";
        // Find "text" : "..."
        int keyIdx = json.indexOf("\"text\"");
        if (keyIdx < 0) return "";
        int colonIdx = json.indexOf(':', keyIdx);
        if (colonIdx < 0) return "";
        int openQuote = json.indexOf('"', colonIdx + 1);
        if (openQuote < 0) return "";
        int closeQuote = json.indexOf('"', openQuote + 1);
        if (closeQuote < 0) return "";
        return json.substring(openQuote + 1, closeQuote).trim();
    }
}

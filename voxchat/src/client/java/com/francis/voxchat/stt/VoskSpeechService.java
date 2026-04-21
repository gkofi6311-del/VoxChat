package com.francis.voxchat.stt;

import com.francis.voxchat.VoxChatMod;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wraps the Vosk offline speech-recognition library.
 *
 * Setup (required – models are NOT bundled in the mod jar because they are
 * several hundred MB each):
 *
 *   .minecraft/voxchat-models/
 *       en/    ← vosk-model-small-en-us-0.15   (or full model)
 *       de/    ← vosk-model-small-de-0.15
 *
 * Download models from: https://alphacephei.com/vosk/models
 */
public class VoskSpeechService {

    public enum Language { ENGLISH, GERMAN }

    private Model modelEn;
    private Model modelDe;
    private boolean initialized;

    /**
     * Loads both language models from the given base directory.
     * Called once at client start-up.
     */
    public void initialize(Path modelsDir) {
        VoxChatMod.LOGGER.info("[VoxChat] Loading Vosk models from: {}", modelsDir);
        initialized = false;

        Path enPath = modelsDir.resolve("en");
        Path dePath = modelsDir.resolve("de");

        boolean enOk = loadModel(enPath, Language.ENGLISH);
        boolean deOk = loadModel(dePath, Language.GERMAN);

        if (!enOk && !deOk) {
            VoxChatMod.LOGGER.error("[VoxChat] ❌ No Vosk models found in {}.", modelsDir);
            VoxChatMod.LOGGER.error("[VoxChat]    Please download models and place them at:");
            VoxChatMod.LOGGER.error("[VoxChat]      {}/en/   (English)", modelsDir);
            VoxChatMod.LOGGER.error("[VoxChat]      {}/de/   (German)", modelsDir);
            VoxChatMod.LOGGER.error("[VoxChat]    Download from: https://alphacephei.com/vosk/models");
            return;
        }

        initialized = true;
        VoxChatMod.LOGGER.info("[VoxChat] ✅ Vosk ready. EN={} DE={}", enOk, deOk);
    }

    private boolean loadModel(Path path, Language lang) {
        if (!Files.isDirectory(path)) {
            VoxChatMod.LOGGER.warn("[VoxChat] Model directory missing for {}: {}", lang, path);
            return false;
        }
        try {
            Model m = new Model(path.toString());
            if (lang == Language.ENGLISH) modelEn = m;
            else modelDe = m;
            VoxChatMod.LOGGER.info("[VoxChat] Loaded {} model from {}", lang, path);
            return true;
        } catch (Exception e) {
            VoxChatMod.LOGGER.error("[VoxChat] Failed to load {} model: {}", lang, e.getMessage());
            return false;
        }
    }

    /**
     * Transcribes raw PCM bytes (16 kHz, 16-bit, mono) into a String.
     *
     * @param pcm      PCM audio bytes
     * @param language target language
     * @return recognized text, or null if recognition failed / no model loaded
     */
    public String recognize(byte[] pcm, Language language) {
        if (!initialized) {
            VoxChatMod.LOGGER.warn("[VoxChat] Vosk not initialized – cannot recognize speech.");
            return null;
        }
        if (pcm == null || pcm.length == 0) {
            VoxChatMod.LOGGER.warn("[VoxChat] Empty audio – nothing to recognize.");
            return null;
        }

        Model model = (language == Language.GERMAN) ? modelDe : modelEn;
        if (model == null) {
            VoxChatMod.LOGGER.warn("[VoxChat] No model available for language: {}", language);
            // Fall back to the other language if available
            model = (language == Language.GERMAN) ? modelEn : modelDe;
            if (model == null) return null;
        }

        try (Recognizer rec = new Recognizer(model, 16_000f)) {
            rec.acceptWaveForm(pcm, pcm.length);
            String json = rec.getFinalResult();
            // Vosk returns JSON like: {"text": "hello world"}
            return extractText(json);
        } catch (IOException e) {
            VoxChatMod.LOGGER.error("[VoxChat] Recognizer error", e);
            return null;
        }
    }

    /** Parses the "text" field from Vosk JSON output without pulling in a JSON lib. */
    private static String extractText(String json) {
        if (json == null || json.isEmpty()) return null;
        int start = json.indexOf("\"text\"");
        if (start < 0) return null;
        int colon = json.indexOf(':', start);
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        String text = json.substring(q1 + 1, q2).trim();
        return text.isEmpty() ? null : text;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean hasModel(Language language) {
        return language == Language.ENGLISH ? modelEn != null : modelDe != null;
    }

    /** Frees native resources held by Vosk models. */
    public void shutdown() {
        if (modelEn != null) { modelEn.close(); modelEn = null; }
        if (modelDe != null) { modelDe.close(); modelDe = null; }
        initialized = false;
    }
}

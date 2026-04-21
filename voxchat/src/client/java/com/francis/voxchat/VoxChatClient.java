package com.francis.voxchat;

import com.francis.voxchat.audio.VoiceRecorder;
import com.francis.voxchat.hud.RecordingOverlay;
import com.francis.voxchat.stt.VoskSpeechService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Environment(EnvType.CLIENT)
public class VoxChatClient implements ClientModInitializer {

    // ── Language selection ─────────────────────────────────────────────────
    // Change to GERMAN if you primarily use German.
    private static final VoskSpeechService.Language DEFAULT_LANGUAGE =
            VoskSpeechService.Language.ENGLISH;

    // ── Internal state ─────────────────────────────────────────────────────
    private final VoiceRecorder      recorder   = new VoiceRecorder();
    private final VoskSpeechService  sttService = new VoskSpeechService();
    private final ExecutorService    executor   = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "voxchat-stt");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void onInitializeClient() {
        VoxChatMod.LOGGER.info("[VoxChat] Client initialising…");

        // 1. Register keybind (V)
        KeyBindings.register();

        // 2. Register HUD overlay
        RecordingOverlay.register();

        // 3. Load Vosk models asynchronously so the game doesn't freeze
        executor.submit(this::loadModels);

        // 4. Poll keybind each game tick
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        // 5. Graceful shutdown
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());

        VoxChatMod.LOGGER.info("[VoxChat] Client initialised. Press V to start/stop voice recording.");
    }

    // ── Keybind tick ──────────────────────────────────────────────────────

    private void onTick(MinecraftClient client) {
        if (KeyBindings.VOICE_RECORD == null) return;

        // wasPressed() consumes the press event — fires once per key-down
        if (KeyBindings.VOICE_RECORD.wasPressed()) {
            toggleRecording(client);
        }
    }

    private void toggleRecording(MinecraftClient client) {
        if (!VoxChatState.isRecording()) {
            // ── START recording ──────────────────────────────────────────
            if (!sttService.isInitialized()) {
                sendMessage(client, "§c[VoxChat] Vosk models not loaded. Check logs for setup instructions.");
                return;
            }
            boolean started = recorder.start();
            if (started) {
                VoxChatState.setRecording(true);
                sendMessage(client, "§a[VoxChat] 🎤 Recording… Press V again to stop.");
            } else {
                sendMessage(client, "§c[VoxChat] Could not open microphone.");
            }
        } else {
            // ── STOP recording + run STT ─────────────────────────────────
            VoxChatState.setRecording(false);
            byte[] pcm = recorder.stop();

            if (pcm.length == 0) {
                sendMessage(client, "§e[VoxChat] No audio captured.");
                return;
            }

            sendMessage(client, "§7[VoxChat] Processing speech…");

            // Run recognition off the render thread
            executor.submit(() -> {
                String text = sttService.recognize(pcm, DEFAULT_LANGUAGE);

                client.execute(() -> {   // back on main thread
                    if (text == null || text.isEmpty()) {
                        sendMessage(client, "§e[VoxChat] Could not understand speech. Try again.");
                        return;
                    }

                    VoxChatMod.LOGGER.info("[VoxChat] Recognized: \"{}\"", text);

                    if (client.currentScreen instanceof ChatScreen) {
                        // Chat is already open → store text; mixin will inject on next init,
                        // or we can directly set via the stored pending state and force-reopen.
                        VoxChatState.setPendingText(text);
                        // Close and reopen chat to trigger mixin injection
                        client.setScreen(null);
                        client.execute(() -> client.setScreen(new ChatScreen("")));
                    } else {
                        // Store text and open chat
                        VoxChatState.setPendingText(text);
                        client.setScreen(new ChatScreen(""));
                    }
                });
            });
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Shows a chat message visible only to the local player (no network). */
    private void sendMessage(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(
                    net.minecraft.text.Text.literal(msg), true /* action bar */);
        }
    }

    private void loadModels() {
        // Models live at: .minecraft/voxchat-models/en/  and  .../de/
        Path gameDir   = MinecraftClient.getInstance().runDirectory.toPath();
        Path modelsDir = gameDir.resolve("voxchat-models");
        sttService.initialize(modelsDir);
    }

    private void shutdown() {
        VoxChatMod.LOGGER.info("[VoxChat] Shutting down…");
        if (VoxChatState.isRecording()) recorder.stop();
        sttService.shutdown();
        executor.shutdownNow();
    }
}

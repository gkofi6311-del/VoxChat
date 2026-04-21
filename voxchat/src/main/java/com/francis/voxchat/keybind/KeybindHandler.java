package com.francis.voxchat.keybind;

import com.francis.voxchat.VoxChatMod;
import com.francis.voxchat.chat.ChatInjector;
import com.francis.voxchat.config.VoxChatConfig;
import com.francis.voxchat.speech.SpeechManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and handles all VoxChat keybindings.
 *
 * Keybinds:
 *  - VOICE KEY (default: V)  — hold to record (push-to-talk) or press to toggle
 *  - LANG  KEY (default: L)  — cycle through supported languages
 *
 * Both appear in Minecraft's Controls screen under the "VoxChat" category.
 */
public class KeybindHandler {

    // Keybind category shown in Controls menu
    private static final String CATEGORY = "key.category.voxchat";

    // Translation keys (registered in lang JSON)
    private static final String KEY_VOICE_ID = "key.voxchat.voice";
    private static final String KEY_LANG_ID  = "key.voxchat.language";

    private final VoxChatConfig config;
    private final SpeechManager speechManager;

    private KeyBinding voiceKey;
    private KeyBinding langKey;

    // Push-to-talk state tracking
    private boolean voiceKeyHeldLastTick = false;
    // Toggle state tracking (used when push_to_talk = false)
    private boolean isToggleActive = false;
    // Debounce for lang key
    private boolean langKeyHeldLastTick = false;

    public KeybindHandler(VoxChatConfig config, SpeechManager speechManager) {
        this.config = config;
        this.speechManager = speechManager;
    }

    /** Call once during mod initialisation to register both keybinds. */
    public void register() {
        voiceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_VOICE_ID,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        langKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_LANG_ID,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                CATEGORY
        ));

        VoxChatMod.LOGGER.info("[VoxChat] Keybinds registered (Voice=V, Language=L)");
    }

    /**
     * Called every client tick via {@code ClientTickEvents.END_CLIENT_TICK}.
     * Handles both push-to-talk and toggle modes.
     */
    public void tick(MinecraftClient client) {
        if (client.player == null) return; // not in a world

        handleVoiceKey(client);
        handleLangKey(client);
    }

    // -------------------------------------------------------------------------
    // Voice key logic
    // -------------------------------------------------------------------------

    private void handleVoiceKey(MinecraftClient client) {
        boolean isPressed = voiceKey.isPressed();

        if (config.isPushToTalk()) {
            // Push-to-talk: start on press, stop on release
            if (isPressed && !voiceKeyHeldLastTick) {
                startRecording(client);
            } else if (!isPressed && voiceKeyHeldLastTick) {
                stopRecordingAndInject(client);
            }
        } else {
            // Toggle mode: flip on each fresh press
            if (isPressed && !voiceKeyHeldLastTick) {
                if (isToggleActive) {
                    stopRecordingAndInject(client);
                    isToggleActive = false;
                } else {
                    startRecording(client);
                    isToggleActive = true;
                }
            }
        }

        voiceKeyHeldLastTick = isPressed;
    }

    private void startRecording(MinecraftClient client) {
        VoxChatMod.LOGGER.debug("[VoxChat] Recording started");
        showActionBar(client, "§a[VoxChat] 🎤 Recording…");
        speechManager.startListening();
    }

    private void stopRecordingAndInject(MinecraftClient client) {
        VoxChatMod.LOGGER.debug("[VoxChat] Recording stopped, awaiting transcription…");
        showActionBar(client, "§e[VoxChat] ⏳ Processing…");
        speechManager.stopListening(recognizedText -> {
            // This callback arrives from the speech thread; dispatch to MC thread
            client.execute(() -> {
                if (recognizedText == null || recognizedText.isBlank()) {
                    showActionBar(client, "§c[VoxChat] Nothing recognised.");
                    return;
                }
                VoxChatMod.LOGGER.info("[VoxChat] Recognised: \"{}\"", recognizedText);
                showActionBar(client, "§b[VoxChat] ✔ " + recognizedText);
                ChatInjector.injectText(client, recognizedText);
            });
        });
    }

    // -------------------------------------------------------------------------
    // Language key logic
    // -------------------------------------------------------------------------

    private void handleLangKey(MinecraftClient client) {
        boolean isPressed = langKey.isPressed();

        if (isPressed && !langKeyHeldLastTick) {
            config.cycleLanguage();
            speechManager.reloadModel(); // reload Vosk model for new language
            String msg = "§d[VoxChat] Language → " + config.getLanguage().getDisplayName();
            showActionBar(client, msg);
        }

        langKeyHeldLastTick = isPressed;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void showActionBar(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true); // true = action bar
        }
    }
}

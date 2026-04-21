package com.francis.voxchat;

import com.francis.voxchat.config.VoxChatConfig;
import com.francis.voxchat.keybind.KeybindHandler;
import com.francis.voxchat.speech.SpeechManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VoxChat - Voice-to-text chat mod for Minecraft Fabric 1.21.1
 *
 * Main client-side entry point. Wires together:
 *  - Config loading/saving
 *  - Keybind registration
 *  - Speech recognition lifecycle
 */
@Environment(EnvType.CLIENT)
public class VoxChatMod implements ClientModInitializer {

    public static final String MOD_ID = "voxchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Singleton accessors — set during init, never null after onInitializeClient()
    private static VoxChatConfig config;
    private static SpeechManager speechManager;
    private static KeybindHandler keybindHandler;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[VoxChat] Initialising VoxChat v{}", getVersion());

        // 1. Load (or create) config
        config = VoxChatConfig.loadOrCreate();
        LOGGER.info("[VoxChat] Language: {}, Push-to-talk mode: {}",
                config.getLanguage(), config.isPushToTalk());

        // 2. Initialise speech manager (loads Vosk model lazily on first use)
        speechManager = new SpeechManager(config);

        // 3. Register keybinds
        keybindHandler = new KeybindHandler(config, speechManager);
        keybindHandler.register();

        // 4. Tick event — processes keybind state each client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> keybindHandler.tick(client));

        // 5. Shutdown hook — release Vosk resources when the game closes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[VoxChat] Shutting down speech manager …");
            speechManager.shutdown();
        }, "VoxChat-Shutdown"));

        LOGGER.info("[VoxChat] Initialisation complete.");
    }

    // -------------------------------------------------------------------------
    // Static accessors used by other classes
    // -------------------------------------------------------------------------

    public static VoxChatConfig getConfig() {
        return config;
    }

    public static SpeechManager getSpeechManager() {
        return speechManager;
    }

    /** Returns the mod version from the fabric.mod.json at runtime. */
    private static String getVersion() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer(MOD_ID)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}

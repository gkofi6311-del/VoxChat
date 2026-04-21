package com.francis.voxchat.speech;

import com.francis.voxchat.VoxChatMod;
import com.francis.voxchat.config.VoxChatConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Centralises language-related logic: model paths, download URLs, display names.
 *
 * This is intentionally a thin utility class — it does not hold state.
 * Call it via its static methods.
 */
public final class LanguageManager {

    // -------------------------------------------------------------------------
    // Model metadata — extend this map to add more languages in future
    // -------------------------------------------------------------------------

    private record LangMeta(String downloadUrl, String zipFileName) {}

    private static final Map<VoxChatConfig.Language, LangMeta> LANG_META =
            new EnumMap<>(VoxChatConfig.Language.class);

    static {
        LANG_META.put(VoxChatConfig.Language.EN, new LangMeta(
                "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
                "vosk-model-small-en-us-0.15"
        ));
        LANG_META.put(VoxChatConfig.Language.DE, new LangMeta(
                "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip",
                "vosk-model-small-de-0.15"
        ));
    }

    private LanguageManager() {} // utility class

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the expected model directory path for the given language.
     * Format: &lt;game-dir&gt;/voxchat-models/&lt;lang-code&gt;/
     */
    public static Path modelDirectory(VoxChatConfig.Language lang) {
        return FabricLoader.getInstance()
                .getGameDir()
                .resolve("voxchat-models")
                .resolve(lang.getCode());
    }

    /**
     * Returns {@code true} if the model directory exists and appears to be a
     * valid Vosk model (contains at least an "am" or "conf" subdirectory).
     */
    public static boolean isModelAvailable(VoxChatConfig.Language lang) {
        Path dir = modelDirectory(lang);
        if (!Files.isDirectory(dir)) return false;
        // Basic sanity check: Vosk models contain "am/" and "conf/" folders
        return Files.isDirectory(dir.resolve("am")) || Files.isDirectory(dir.resolve("conf"));
    }

    /**
     * Returns the download URL for the small Vosk model of the given language.
     */
    public static String getDownloadUrl(VoxChatConfig.Language lang) {
        LangMeta meta = LANG_META.get(lang);
        return meta != null ? meta.downloadUrl() : "(unknown)";
    }

    /**
     * Logs a formatted help message explaining how to install the model for a
     * language whose directory is missing or incomplete.
     */
    public static void logInstallHelp(VoxChatConfig.Language lang) {
        Path dir  = modelDirectory(lang);
        String url = getDownloadUrl(lang);

        VoxChatMod.LOGGER.error("""
                [VoxChat] ════════════════════════════════════════════════
                [VoxChat]  Missing Vosk model for language: {} ({})
                [VoxChat]  Expected path : {}
                [VoxChat]
                [VoxChat]  ► INSTALLATION STEPS:
                [VoxChat]    1. Download the model:
                [VoxChat]       {}
                [VoxChat]    2. Extract the ZIP.
                [VoxChat]    3. Rename the extracted folder to "{}" (just the code).
                [VoxChat]    4. Place it at:
                [VoxChat]       {}
                [VoxChat]    5. Restart Minecraft.
                [VoxChat] ════════════════════════════════════════════════""",
                lang.getDisplayName(), lang.getCode(),
                dir,
                url,
                lang.getCode(),
                dir);
    }

    /**
     * Checks all supported languages and logs a summary of which models are
     * available. Called at startup for user convenience.
     */
    public static void logAvailabilityReport() {
        VoxChatMod.LOGGER.info("[VoxChat] ── Vosk model availability ──");
        for (VoxChatConfig.Language lang : VoxChatConfig.Language.values()) {
            boolean available = isModelAvailable(lang);
            VoxChatMod.LOGGER.info("[VoxChat]   {} ({}): {}",
                    lang.getDisplayName(),
                    lang.getCode(),
                    available ? "✔ found" : "✘ missing — see log above for install instructions");
            if (!available) {
                logInstallHelp(lang);
            }
        }
    }
}

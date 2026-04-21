package com.francis.voxchat.config;

import com.francis.voxchat.VoxChatMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persistent configuration for VoxChat.
 *
 * Config file location: &lt;game-dir&gt;/config/voxchat.properties
 *
 * Keys:
 *   language      = en | de          (default: en)
 *   push_to_talk  = true | false      (default: true)
 *   key_vox       = GLFW key name     (informational; actual binding stored by MC)
 *   key_lang      = GLFW key name     (informational)
 */
public class VoxChatConfig {

    // -------------------------------------------------------------------------
    // Supported languages
    // -------------------------------------------------------------------------
    public enum Language {
        EN("en", "English"),
        DE("de", "Deutsch");

        private final String code;
        private final String displayName;

        Language(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }

        public static Language fromCode(String code) {
            for (Language l : values()) {
                if (l.code.equalsIgnoreCase(code)) return l;
            }
            return EN; // fallback
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private Language language = Language.EN;
    private boolean pushToTalk = true; // true = hold key; false = toggle

    private static final String CONFIG_FILENAME = "voxchat.properties";

    // -------------------------------------------------------------------------
    // Load / Save
    // -------------------------------------------------------------------------

    public static VoxChatConfig loadOrCreate() {
        VoxChatConfig cfg = new VoxChatConfig();
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                Properties props = new Properties();
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                cfg.language    = Language.fromCode(props.getProperty("language", "en"));
                cfg.pushToTalk  = Boolean.parseBoolean(props.getProperty("push_to_talk", "true"));
                VoxChatMod.LOGGER.info("[VoxChat] Config loaded from {}", configPath);
            } catch (IOException e) {
                VoxChatMod.LOGGER.warn("[VoxChat] Failed to read config, using defaults: {}", e.getMessage());
            }
        } else {
            cfg.save(); // write defaults on first run
        }

        return cfg;
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            Properties props = new Properties();
            props.setProperty("language",    language.getCode());
            props.setProperty("push_to_talk", String.valueOf(pushToTalk));
            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                        "VoxChat configuration — edit and restart, or use in-game keybinds");
            }
            VoxChatMod.LOGGER.info("[VoxChat] Config saved to {}", configPath);
        } catch (IOException e) {
            VoxChatMod.LOGGER.error("[VoxChat] Failed to save config: {}", e.getMessage());
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILENAME);
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public Language getLanguage() { return language; }

    public void setLanguage(Language language) {
        this.language = language;
        save();
    }

    /** Cycle through available languages in order. */
    public void cycleLanguage() {
        Language[] values = Language.values();
        int next = (language.ordinal() + 1) % values.length;
        setLanguage(values[next]);
        VoxChatMod.LOGGER.info("[VoxChat] Language switched to {}", values[next].getDisplayName());
    }

    public boolean isPushToTalk() { return pushToTalk; }

    public void setPushToTalk(boolean pushToTalk) {
        this.pushToTalk = pushToTalk;
        save();
    }
}

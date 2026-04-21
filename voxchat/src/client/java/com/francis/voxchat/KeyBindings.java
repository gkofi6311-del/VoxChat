package com.francis.voxchat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyBinding VOICE_RECORD;

    public static void register() {
        VOICE_RECORD = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.voxchat.voice_record",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        "category.voxchat"
                )
        );
    }
}

package com.francis.voxchat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyBinding VOICE_KEY;

    public static void register() {
        VOICE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.voxchat.push_to_talk",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.voxchat"
            )
        );
    }
}

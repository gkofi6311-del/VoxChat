package com.francis.voxchat.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    public static KeyBinding TOGGLE_VOICE;

    public static void register() {
        TOGGLE_VOICE = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.voxchat.toggle",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_V,
                        "category.voxchat"
                )
        );
    }
}

package com.francis.voxchat;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class KeyBindings {

    public static KeyBinding PUSH_TO_TALK;

    public static void register() {

        PUSH_TO_TALK = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.voxchat.push_to_talk",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_V,
                        "key.categories.voxchat"
                )
        );
    }
}

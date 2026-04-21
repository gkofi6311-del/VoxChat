package com.francis.voxchat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyBinding PUSH_TO_TALK;

    public static void register() {
        PUSH_TO_TALK = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.voxchat.push_to_talk",
                GLFW.GLFW_KEY_V,
                "category.voxchat"
            )
        );
    }
}

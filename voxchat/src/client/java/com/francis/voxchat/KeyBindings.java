package com.francis.voxchat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyBinding VOXCHAT_PUSH_TO_TALK;

    public static void register() {

        VOXCHAT_PUSH_TO_TALK = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.voxchat.push_to_talk",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_V,
                        "category.voxchat"
                )
        );
    }
}

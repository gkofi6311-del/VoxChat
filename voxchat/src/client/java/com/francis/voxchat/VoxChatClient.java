package com.francis.voxchat;

import net.fabricmc.api.ClientModInitializer;

public class VoxChatClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // register keybinds
        KeyBindings.register();

        // example tick check (safe usage pattern)
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (KeyBindings.VOICE_RECORD != null && KeyBindings.VOICE_RECORD.wasPressed()) {
                System.out.println("VOICE RECORD PRESSED");
                // TODO: your voice logic here
            }

        });
    }
}

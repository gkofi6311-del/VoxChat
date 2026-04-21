package com.francis.voxchat;

import net.fabricmc.api.ClientModInitializer;

public class VoxChatClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindings.register();
    }
}

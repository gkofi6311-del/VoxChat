package com.francis.voxchat;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoxChatMod implements ModInitializer {

    public static final String MOD_ID = "voxchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[VoxChat] Mod initialized.");
    }
}

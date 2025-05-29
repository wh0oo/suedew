package com.example.suedew;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuedewMod implements ModInitializer {
    public static final String MOD_ID = "suedew";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Suedew mod");
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SudoCommand2.register(dispatcher);
            LOGGER.info("Registered /sudo command");
        });
    }
}

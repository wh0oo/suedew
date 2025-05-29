package com.example.suedew;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class SuedewMod implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("[Suedew] Mod initialized.");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SudoCommand2.register(dispatcher);
        });
    }
}

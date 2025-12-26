package com.example.suedew;

import carpet.CarpetExtension;
import carpet.script.CarpetExpression;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.value.Value;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class SuedewCarpetExtension implements CarpetExtension {

    @Override
    public void onGameStarted() {
        CarpetExpression.registerFunction(
            "suedew_chat",
            2,
            this::suedewChat
        );
    }

    private Value suedewChat(Context ctx, List<Value> args) {
        MinecraftServer server = ctx.getServer();

        if (server == null) {
            return Value.NULL;
        }

        String playerName = args.get(0).getString();
        String message = args.get(1).getString();

        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            return Value.NULL;
        }

        PlayerChatMessage chat = PlayerChatMessage.system(message);

        server.getPlayerList().broadcastChatMessage(
            chat,
            player,
            ChatType.bind(ChatType.CHAT, player)
        );

        return Value.TRUE;
    }
}

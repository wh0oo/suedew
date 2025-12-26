package com.example.suedew;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.Expression;
import carpet.script.ExpressionAPI;
import carpet.script.context.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class SuedewCarpetExtension implements CarpetExtension {

    @Override
    public void onGameStarted() {
        ExpressionAPI.registerFunction(
            "suedew_chat",
            2,
            (Context ctx, List<Value> args) -> {

                String playerName = args.get(0).getString();
                String message = args.get(1).getString();

                MinecraftServer server = CarpetServer.minecraft_server;
                if (server == null) {
                    throw new InternalExpressionException("Server not available");
                }

                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player == null) {
                    throw new InternalExpressionException("Player not found: " + playerName);
                }

                // Emit REAL signed player chat
                player.getChatSession().sendChatMessage(
                    Component.literal(message),
                    ChatType.CHAT,
                    player
                );

                return Value.TRUE;
            }
        );
    }

    @Override
    public String name() {
        return "suedew";
    }
}

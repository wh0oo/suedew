package com.example.suedew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SudoCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Component.literal("Targeted player could not be found"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("sudo")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

            // /sudo <player> chat <message>
            .then(Commands.argument("player", StringArgumentType.word())
                .then(Commands.literal("chat")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();
                            PlayerList playerList = server.getPlayerList();

                            String targetName = StringArgumentType.getString(ctx, "player");
                            ServerPlayer target = playerList.getPlayerByName(targetName);
                            if (target == null) {
                                throw PLAYER_NOT_FOUND.create();
                            }

                            String message = StringArgumentType.getString(ctx, "message");
                            String displayName = target.getName().getString();

                            LOGGER.info("[suedew][chat] {} as {}: \"{}\"",
                                src.getTextName(), displayName, message);

                            UUID uuid = target.getUUID();
                            PlayerChatMessage chatMessage = PlayerChatMessage.unsigned(uuid, message);

                            CommandSourceStack asSource = target.createCommandSourceStack();
                            ChatType.Bound bound = ChatType.bind(ChatType.CHAT, asSource);

                            // Go through the real chat pipeline so listeners (Discord, etc.) see it
                            playerList.broadcastChatMessage(chatMessage, asSource, bound);

                            return 1;
                        })
                    )
                )

                // /sudo <player> command <...>
                .then(Commands.literal("command")
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();
                            PlayerList playerList = server.getPlayerList();

                            String targetName = StringArgumentType.getString(ctx, "player");
                            ServerPlayer target = playerList.getPlayerByName(targetName);
                            if (target == null) {
                                throw PLAYER_NOT_FOUND.create();
                            }

                            String cmd = StringArgumentType.getString(ctx, "command");
                            String displayName = target.getName().getString();

                            LOGGER.info("[suedew][command] {} as {}: \"{}\"",
                                src.getTextName(), displayName, cmd);

                            server.getCommands()
                                .performPrefixedCommand(target.createCommandSourceStack(), cmd);

                            return 1;
                        })
                    )
                );

        dispatcher.register(root);
    }
}

package com.example.suedew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SudoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("sudo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("player", StringArgumentType.word())

                        // /sudo <player> chat <message>
                        .then(
                            Commands.literal("chat")
                                .then(
                                    Commands.argument(
                                        "message",
                                        StringArgumentType.greedyString()
                                    )
                                    .executes(ctx -> {
                                        CommandSourceStack source = ctx.getSource();
                                        MinecraftServer server = source.getServer();

                                        String targetName =
                                            StringArgumentType.getString(ctx, "player");
                                        String message =
                                            StringArgumentType.getString(ctx, "message");

                                        ServerPlayer target =
                                            server.getPlayerList()
                                                .getPlayerByName(targetName);

                                        if (target == null) {
                                            source.sendFailure(
                                                Component.literal(
                                                    "Player not found: " + targetName
                                                )
                                            );
                                            return 0;
                                        }

                                        target.sendChatMessage(
                                            OutgoingChatMessage.create(
                                                Component.literal(message)
                                            ),
                                            ChatType.bind(ChatType.CHAT, target),
                                            target.getUUID()
                                        );

                                        return 1;
                                    })
                                )
                        )

                        // /sudo <player> command <anything>
                        .then(
                            Commands.literal("command")
                                .then(
                                    Commands.argument(
                                        "command",
                                        StringArgumentType.greedyString()
                                    )
                                    .executes(ctx -> {
                                        CommandSourceStack source = ctx.getSource();
                                        MinecraftServer server = source.getServer();

                                        String targetName =
                                            StringArgumentType.getString(ctx, "player");
                                        String command =
                                            StringArgumentType.getString(ctx, "command");

                                        ServerPlayer target =
                                            server.getPlayerList()
                                                .getPlayerByName(targetName);

                                        if (target == null) {
                                            source.sendFailure(
                                                Component.literal(
                                                    "Player not found: " + targetName
                                                )
                                            );
                                            return 0;
                                        }

                                        server.getCommands().performPrefixedCommand(
                                            target.createCommandSourceStack(),
                                            command
                                        );

                                        return 1;
                                    })
                                )
                        )
                )
        );
    }
}

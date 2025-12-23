package com.example.suedew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SudoCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
            new SimpleCommandExceptionType(
                    Component.literal("Targeted player could not be found")
            );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("suedew")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.argument("player", StringArgumentType.word())
                        .then(
                            Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    MinecraftServer server = source.getServer();

                                    String targetName =
                                            StringArgumentType.getString(ctx, "player");
                                    String command =
                                            StringArgumentType.getString(ctx, "command");

                                    ServerPlayer target =
                                            server.getPlayerList().getPlayerByName(targetName);

                                    if (target == null) {
                                        throw PLAYER_NOT_FOUND.create();
                                    }

                                    LOGGER.info(
                                        "[suedew] {} executed as {}: /{}",
                                        source.getTextName(),
                                        target.getName().getString(),
                                        command
                                    );

                                    server.getCommands().performPrefixedCommand(
                                        target.createCommandSourceStack(),
                                        command
                                    );

                                    return 1;
                                })
                        )
                )
        );
    }
}

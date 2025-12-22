package com.example.suedew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SudoCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
            new SimpleCommandExceptionType(Text.literal("Targeted player could not be found"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("suedew")
                .requires(source -> source.hasPermissionLevel(2))
                .then(
                    CommandManager.argument("player", StringArgumentType.word())
                        .then(
                            CommandManager.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    MinecraftServer server = source.getServer();

                                    String targetName = StringArgumentType.getString(ctx, "player");
                                    String command = StringArgumentType.getString(ctx, "command");

                                    ServerPlayerEntity target =
                                            server.getPlayerManager().getPlayer(targetName);

                                    if (target == null) {
                                        throw PLAYER_NOT_FOUND.create();
                                    }

                                    String fullCommand = "/" + command;

                                    LOGGER.info(
                                        "[suedew] {} executed as {}: {}",
                                        source.getName(),
                                        target.getName().getString(),
                                        fullCommand
                                    );

                                    // Execute the command as the target player
                                    server.getCommandManager().executeWithPrefix(
                                        target.getCommandSource(),
                                        command
                                    );

                                    return 1;
                                })
                        )
                )
        );
    }
}

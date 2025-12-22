package com.example.suedew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SudoCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("suedew")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.argument("player", StringArgumentType.word())
                        .then(
                            Commands.argument("command", StringArgumentType.greedyString())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    MinecraftServer server = source.getServer();

                                    String playerName =
                                        StringArgumentType.getString(context, "player");
                                    String rawCommand =
                                        StringArgumentType.getString(context, "command");

                                    ServerPlayer target =
                                        server.getPlayerList().getPlayerByName(playerName);

                                    if (target == null) {
                                        source.sendFailure(
                                            Component.literal("Player not found: " + playerName)
                                        );
                                        return 0;
                                    }

                                    // Strip leading slash if present
                                    String commandToRun = rawCommand.startsWith("/")
                                        ? rawCommand.substring(1)
                                        : rawCommand;

                                    LOGGER.info(
                                        "[suedew] {} executing as {}: /{}",
                                        source.getTextName(),
                                        target.getGameProfile().getName(),
                                        commandToRun
                                    );

                                    // Execute as the target player
                                    server.getCommands().performPrefixedCommand(
                                        target.createCommandSourceStack(),
                                        commandToRun
                                    );

                                    return 1;
                                })
                        )
                )
        );
    }
}

package com.example.suedew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class SudoCommand {

    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Component.literal("Targeted player could not be found"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("suedew")
                // ops only, hidden from non-ops (Fortune pattern)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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

                                    // Execute as the target player
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

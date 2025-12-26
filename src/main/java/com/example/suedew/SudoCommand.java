package com.example.suedew;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SudoCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Component.literal("Targeted player could not be found"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root =
            Commands.literal("sudo")
                // Ops / gamemasters only, same pattern as fortune
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                // /sudo <player> chat <message>
                .then(
                    Commands.argument("player", StringArgumentType.word())
                        .then(
                            Commands.literal("chat")
                                .then(
                                    Commands.argument("message", StringArgumentType.greedyString())
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
                                            GameProfile profile = target.getGameProfile();
                                            String displayName = profile.getName(); // Mojang username

                                            // Make it look like normal player chat: <name> message
                                            Component display =
                                                Component.literal("<" + displayName + "> " + message);

                                            LOGGER.info(
                                                "[suedew][chat] {} as {}: \"{}\"",
                                                src.getTextName(),
                                                displayName,
                                                message
                                            );

                                            // Go through the normal broadcast path (Discord bridge will see this)
                                            playerList.broadcastSystemMessage(display, false);

                                            return 1;
                                        })
                                )
                        )

                        // /sudo <player> command <...>
                        .then(
                            Commands.literal("command")
                                .then(
                                    Commands.argument("command", StringArgumentType.greedyString())
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

                                            LOGGER.info(
                                                "[suedew][command] {} as {}: \"{}\"",
                                                src.getTextName(),
                                                target.getGameProfile().getName(),
                                                cmd
                                            );

                                            // Run the command as the target player
                                            return server.getCommands()
                                                .performPrefixedCommand(
                                                    target.createCommandSourceStack(),
                                                    cmd
                                                );
                                        })
                                )
                        )
                );

        dispatcher.register(root);
    }

    // Kept for potential future tab-complete integration
    private static Collection<String> getPlayers(CommandSourceStack src) {
        return src.getServer()
            .getPlayerList()
            .getPlayers()
            .stream()
            .map(p -> p.getGameProfile().getName())
            .toList();
    }
}

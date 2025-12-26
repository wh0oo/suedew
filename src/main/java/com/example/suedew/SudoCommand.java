package com.example.suedew;

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

public class SudoCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Component.literal("Targeted player could not be found"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("sudo")
            // Same pattern you used in /fortune reload
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
                                            src.sendFailure(Component.literal("Targeted player could not be found"));
                                            return 0;
                                        }

                                        String message = StringArgumentType.getString(ctx, "message");

                                        LOGGER.info(
                                            "[sudo][chat] {} as {}: \"{}\"",
                                            src.getTextName(),
                                            targetName,
                                            message
                                        );

                                        // Simple fake "<name> msg" line broadcast to all players.
                                        // This avoids the fragile chat pipeline types.
                                        Component line = Component.literal("<" + targetName + "> " + message);
                                        for (ServerPlayer viewer : playerList.getPlayers()) {
                                            viewer.sendSystemMessage(line);
                                        }

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

                                        String cmdPart = StringArgumentType.getString(ctx, "command");
                                        String withSlash = cmdPart.startsWith("/") ? cmdPart : "/" + cmdPart;

                                        LOGGER.info(
                                            "[sudo][cmd] {} as {}: \"{}\"",
                                            src.getTextName(),
                                            targetName,
                                            withSlash
                                        );

                                        // Run the command as the target player
                                        server.getCommands().performPrefixedCommand(
                                            target.createCommandSourceStack(),
                                            withSlash
                                        );

                                        // Brigadier wants an int result; just return success
                                        return 1;
                                    })
                            )
                    )
            );

        dispatcher.register(root);
    }
}

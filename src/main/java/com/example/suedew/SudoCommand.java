package com.example.suedew;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SudoCommand {
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Component.literal("Targeted player could not be found"));
    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = literal("sudo")
            // Use the same permission helper pattern as in fortune
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

            // /sudo <player> chat <message>
            .then(argument("player", StringArgumentType.word())
                .then(literal("chat")
                    .then(argument("message", MessageArgument.message())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();
                            String targetName = StringArgumentType.getString(ctx, "player");
                            ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);

                            if (target == null) {
                                throw PLAYER_NOT_FOUND.create();
                            }

                            PlayerList playerList = server.getPlayerList();

                            LOGGER.info(
                                "[sudo][chat] {} as {}: \"{}\"",
                                src.getTextName(),
                                target.getGameProfile().getName(),
                                ctx.getInput()
                            );

                            // Use Mojang's signed chat pipeline
                            MessageArgument.resolveChatMessage(ctx, "message", (chatMessage) -> {
                                ChatType.Bound bound = ChatType.bind(
                                    ChatType.CHAT,
                                    src
                                );
                                playerList.broadcastChatMessage(chatMessage, bound, src.isSilent());
                            });

                            return 1;
                        })
                    )
                )

                // /sudo <player> command <...>
                .then(literal("command")
                    .then(argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            MinecraftServer server = src.getServer();
                            String targetName = StringArgumentType.getString(ctx, "player");
                            ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);

                            if (target == null) {
                                throw PLAYER_NOT_FOUND.create();
                            }

                            String cmdPart = StringArgumentType.getString(ctx, "command");
                            String withSlash = "/" + cmdPart;

                            LOGGER.info(
                                "[sudo][cmd] {} as {}: \"{}\"",
                                src.getTextName(),
                                target.getGameProfile().getName(),
                                withSlash
                            );

                            // Execute the command as the target player
                            return server.getCommands().performPrefixedCommand(
                                target.createCommandSourceStack(),
                                cmdPart
                            );
                        })
                    )
                )
            );

        dispatcher.register(cmd);
    }

    private static Collection<String> getPlayers(CommandSourceStack src) {
        return src.getPlayerNames();
    }
}

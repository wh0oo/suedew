package com.example.suedew;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SudoCommand2 {

    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Component.literal("Targeted player could not be found"));

    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        LiteralArgumentBuilder<CommandSourceStack> cmd = literal("sudo")
            .requires(src -> src.hasPermissionLevel(2))

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
                                src.sendFailure(Component.literal("Targeted player could not be found"));
                                return 0;
                            }

                            String raw = MessageArgument.getMessage(ctx, "message").getString();
                            PlayerList pm = server.getPlayerList();

                            LOGGER.info("[sudo][chat] {} as {}: \"{}\"", src.getTextName(), targetName, raw);

                            MessageArgument.resolveChatMessage(ctx, "message", signedMsg -> {
                                pm.broadcastChatMessage(
                                    signedMsg,
                                    target,
                                    ChatType.bind(ChatType.CHAT, target)
                                );
                            });

                            return 1;
                        })
                    )
                )

                // /sudo <player> command <...>
                .then(literal("command")
                    .redirect(dispatcher.getRoot(), ctx -> {
                        CommandSourceStack src = ctx.getSource();
                        MinecraftServer server = src.getServer();
                        String targetName = StringArgumentType.getString(ctx, "player");
                        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);

                        if (target == null) throw PLAYER_NOT_FOUND.create();

                        String fullInput = ctx.getInput();
                        String afterTarget = fullInput.substring(
                            fullInput.indexOf(targetName) + targetName.length()
                        ).trim();
                        String cmdPart = afterTarget.startsWith("command")
                            ? afterTarget.substring("command".length()).trim()
                            : afterTarget;

                        String withSlash = "/" + cmdPart;

                        LOGGER.info("[sudo][cmd] {} as {}: \"{}\"", src.getTextName(), targetName, withSlash);

                        return target.createCommandSourceStack();
                    })
                )
            );

        dispatcher.register(cmd);
    }

    private static Collection<String> getPlayers(CommandSourceStack src) {
        return src.getServer().getPlayerList().getPlayerNames();
    }
}

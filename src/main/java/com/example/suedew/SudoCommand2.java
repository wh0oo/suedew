package com.example.suedew;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SudoCommand2 {
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Text.literal("Targeted player could not be found"));
    private static final Logger LOGGER = LoggerFactory.getLogger("Suedew");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> cmd = literal("sudo")
            .requires(src -> src.hasPermissionLevel(2))

            // /sudo <player> chat <message>
            .then(argument("player", StringArgumentType.word())
                .then(literal("chat")
                    .then(argument("message", MessageArgumentType.message())
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            MinecraftServer server = src.getServer();
                            String targetName = StringArgumentType.getString(ctx, "player");
                            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

                            if (target == null) {
                                src.sendError(Text.literal("Targeted player could not be found"));
                                return 0;
                            }

                            String raw = MessageArgumentType.getMessage(ctx, "message").getString();
                            PlayerManager pm = server.getPlayerManager();

                            // Log to server console via Logger
                            LOGGER.info("[sudo][chat] {} as {}: \"{}\"", src.getName(), targetName, raw);

                            // Broadcast signed chat as <target>
                            MessageArgumentType.getSignedMessage(ctx, "message", signedMsg -> {
                                pm.broadcast(
                                    signedMsg,
                                    /* except= */ target,
                                    MessageType.params(MessageType.CHAT, target.getCommandSource())
                                );
                            });
                            return 1;
                        })
                    )
                )

                // /sudo <player> command <...>
                .then(literal("command")
                    .redirect(dispatcher.getRoot(), ctx -> {
                        ServerCommandSource src = ctx.getSource();
                        MinecraftServer server = src.getServer();
                        String targetName = StringArgumentType.getString(ctx, "player");
                        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

                        if (target == null) throw PLAYER_NOT_FOUND.create();

                        // Extract everything after "command <player>"
                        String fullInput = ctx.getInput();
                        String afterTarget = fullInput.substring(
                            fullInput.indexOf(targetName) + targetName.length()
                        ).trim(); // e.g., "command say Hello"
                        String cmdPart = afterTarget.startsWith("command")
                            ? afterTarget.substring("command".length()).trim()
                            : afterTarget;     // e.g., "say Hello"
                        String withSlash = "/" + cmdPart; // e.g., "/say Hello"

                        // Log to server console via Logger
                        LOGGER.info("[sudo][cmd] {} as {}: \"{}\"", src.getName(), targetName, withSlash);

                        return target.getCommandSource(); // run with target’s permissions
                    })
                )
            );

        dispatcher.register(cmd);
    }

    private static Collection<String> getPlayers(ServerCommandSource src) {
        return src.getPlayerNames();
    }
}

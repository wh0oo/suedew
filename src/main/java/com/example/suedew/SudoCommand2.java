package com.example.suedew;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class SudoCommand2 {
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Text.literal("Targeted player could not be found"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> cmd = literal("sudo")
            .requires(src -> src.hasPermissionLevel(2))
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

                            PlayerManager pm = server.getPlayerManager();
                            MessageArgumentType.getSignedMessage(ctx, "message", signedMsg -> {
                                pm.broadcast(
                                    signedMsg,
                                    /* except= */ null,
                                    MessageType.params(MessageType.CHAT, target.getCommandSource())
                                );
                            });
                            return 1;
                        })
                    )
                )
                .then(literal("command")
                    .redirect(dispatcher.getRoot(), ctx -> {
                        String targetName = StringArgumentType.getString(ctx, "player");
                        MinecraftServer server = ctx.getSource().getServer();
                        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
                        if (target == null) throw PLAYER_NOT_FOUND.create();
                        return target.getCommandSource();
                    })
                )
            );

        dispatcher.register(cmd);
    }

    private static Collection<String> getPlayers(ServerCommandSource src) {
        return src.getPlayerNames();
    }
}

package com.example.suedew;

import static net.minecraft.command.CommandSource.suggestMatching;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;

import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SudoCommand2 {
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(
            Text.literal("Targeted player could not be found")
        );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // build the literal tree
        LiteralArgumentBuilder<ServerCommandSource> cmd = literal("sudo")
            .requires(src -> src.hasPermissionLevel(2))
            .then(argument("player", StringArgumentType.word())
                .suggests((ctx, builder) ->
                    suggestMatching(getPlayers(ctx.getSource()), builder))
                .then(literal("chat")
                    .then(argument("message", MessageArgumentType.message())
                        .executes(ctx -> {
                            String targetName = StringArgumentType.getString(ctx, "player");
                            MinecraftServer server = ctx.getSource().getServer();
                            PlayerManager pm = server.getPlayerManager();
                            ServerPlayerEntity target = pm.getPlayer(targetName);
                            if (target == null) {
                                ctx.getSource().sendError(
                                    Text.literal("Targeted player could not be found"));
                                return 0;
                            }
                            // this is Rug’s approach to chat impersonation
                            MessageArgumentType.getSignedMessage(ctx, "message", signed -> {
                                pm.broadcast(
                                    signed,
                                    target,
                                    MessageType.params(MessageType.CHAT, ctx.getSource())
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
                        ServerPlayerEntity target = server
                            .getPlayerManager()
                            .getPlayer(targetName);
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

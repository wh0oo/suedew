package com.example.suedew;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.MinecraftServer;
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

                            // Send through the normal player-chat pipeline so StyledChat picks it up
                            String raw = MessageArgumentType.getMessage(ctx, "message").getString();
                            target.networkHandler.chat(raw, null);
                            return 1;
                        })
                    )
                )

                // /sudo <player> command <...>
                .then(literal("command")
                    .redirect(dispatcher.getRoot(), ctx -> {
                        String targetName = StringArgumentType.getString(ctx, "player");
                        MinecraftServer server = ctx.getSource().getServer();
                        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

                        if (target == null) throw PLAYER_NOT_FOUND.create();
                        return target.getCommandSource();   // run with target’s permissions
                    })
                )
            );

        dispatcher.register(cmd);
    }

    private static Collection<String> getPlayers(ServerCommandSource src) {
        return src.getPlayerNames();
    }
}

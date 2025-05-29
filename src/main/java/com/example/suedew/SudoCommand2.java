package com.example.suedew;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SudoCommand2 {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("sudo")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("player", StringArgumentType.word())
                    .then(literal("chat")
                        .then(argument("message", MessageArgumentType.message())
                            .executes(context -> {
                                String targetName = StringArgumentType.getString(context, "player");
                                MinecraftServer server = context.getSource().getServer();
                                ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

                                if (target == null) {
                                    context.getSource().sendError(Text.of("Targeted player not found."));
                                    return 0;
                                }

                                String message = MessageArgumentType.getMessage(context, "message").getContent();
                                target.sendMessage(Text.of(message));
                                return 1;
                            })
                        )
                    )
                    .then(literal("command")
                        .redirect(dispatcher.getRoot(), context -> {
                            String targetName = StringArgumentType.getString(context, "player");
                            MinecraftServer server = context.getSource().getServer();
                            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

                            if (target == null) {
                                Text error = Text.of("Targeted player not found.");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(error), error);
                            }

                            return target.getCommandSource();
                        })
                    )
                )
        );
    }
}

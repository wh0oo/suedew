package com.example.suedew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SudoCommand2 {
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND = new SimpleCommandExceptionType(
        Text.literal("Player not found").formatted(Formatting.RED)
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("sudo")
                .requires(source -> source.hasPermissionLevel(4))
                .then(
                    argument("target", EntityArgumentType.player())
                    .then(
                        argument("command", StringArgumentType.greedyString())
                        .executes(context -> executeSudo(
                            context,
                            EntityArgumentType.getPlayer(context, "target"),
                            StringArgumentType.getString(context, "command")
                        ))
                )
        );
    }

    private static int executeSudo(CommandContext<ServerCommandSource> context, ServerPlayerEntity target, String command) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();

        if (target == null) {
            throw PLAYER_NOT_FOUND.create();
        }

        // Format and send the command
        String formattedCommand = command.startsWith("/") ? command.substring(1) : command;
        server.getCommandManager().executeWithPrefix(
            target.getCommandSource().withLevel(4),
            formattedCommand
        );

        // Feedback to sender
        source.sendFeedback(() -> Text.literal("Forced ")
            .append(target.getDisplayName())
            .append(" to execute: ")
            .append(Text.literal(formattedCommand).formatted(Formatting.ITALIC))
            .formatted(Formatting.GREEN), true);

        return 1;
    }
}

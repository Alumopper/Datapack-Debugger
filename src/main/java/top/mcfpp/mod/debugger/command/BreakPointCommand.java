package top.mcfpp.mod.debugger.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import top.mcfpp.mod.debugger.utils.Debugger;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BreakPointCommand {
    public static void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("breakpoint")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        Debugger.breakPoint(context.getSource());
                        return 1;
                    })
                    .then(literal("step")
                            .executes(context -> {
                                Debugger.step(1, context.getSource());
                                return 1;
                            })
                            .then(argument("lines", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        final int lines = IntegerArgumentType.getInteger(context, "lines");
                                        Debugger.step(lines, context.getSource());
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("move")
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.translatable("commands.breakpoint.move"), false);
                                Debugger.moveOn(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("get")
                            .then(argument("key", StringArgumentType.string())
                                    .suggests(BreakpointSuggestionProvider.INSTANCE)
                                    .executes(context -> {
                                        final String key = StringArgumentType.getString(context, "key");
                                        var nbt = Debugger.getNBT(key, context.getSource());
                                        if (nbt != null) {
                                            if (nbt.getRight()) {
                                                context.getSource().sendFeedback(() -> Text.translatable("commands.breakpoint.get", key, NbtHelper.toPrettyPrintedText(nbt.getLeft())), false);
                                            } else {
                                                context.getSource().sendError(Text.translatable("commands.breakpoint.get.fail.not_macro"));
                                            }
                                        }
                                        return 1;
                                    })
                            )
                            .executes(context -> {
                                final var args = Debugger.getAllNBT(context.getSource());
                                if (args == null) {
                                    context.getSource().sendError(Text.translatable("commands.breakpoint.get.fail.not_macro"));
                                } else {
                                    context.getSource().sendFeedback(() -> (NbtHelper.toPrettyPrintedText(args)), false);
                                }
                                return 1;
                            })
                    )
                    .then(literal("stack")
                            .executes(context -> {
                                Debugger.printStack(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("run")
                            .redirect(dispatcher.getRoot(), context -> (ServerCommandSource) FunctionStackManager.source.peek())
                    )
                    .then(literal("clear")
                            .executes(context -> {
                                FunctionStackManager.functionStack.clear();
                                FunctionStackManager.source.clear();
                                return 1;
                            })
                    )
            );
        });
    }


}

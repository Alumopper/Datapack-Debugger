package net.gunivers.sniffer.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.gunivers.sniffer.DatapackDebugger;
import net.gunivers.sniffer.command.FunctionTextLoader;
import net.gunivers.sniffer.util.Extension;
import net.gunivers.sniffer.util.ReflectUtil;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.FunctionBuilder;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static net.minecraft.server.function.CommandFunction.parse;
import static net.minecraft.server.function.CommandFunction.validateCommandLength;

/**
 * @author theogiraudet
 */
@Mixin(CommandFunction.class)
public interface CommandFunctionMixin {

    @Shadow
    private static boolean continuesToNextLine(CharSequence string) {
        return false;
    }

    /**
     * @author theogiraudet
     * @reason to save in each command the file and source line
     */
    @Overwrite
    static <T extends AbstractServerCommandSource<T>> CommandFunction<T> create(Identifier id, CommandDispatcher<T> dispatcher, T source, List<String> lines) {
        FunctionTextLoader.put(id, lines);
        //noinspection unchecked
        FunctionBuilder<T> functionBuilder = ReflectUtil.newInstance(FunctionBuilder.class).getData();
        for (int i = 0; i < lines.size(); ++i) {
            int j = i + 1;
            String string = lines.get(i).trim();
            String string3;
            if (continuesToNextLine(string)) {
                StringBuilder stringBuilder = new StringBuilder(string);

                do {
                    ++i;
                    if (i == lines.size()) {
                        throw new IllegalArgumentException("Line continuation at end of file");
                    }

                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    String string2 = lines.get(i).trim();
                    stringBuilder.append(string2);
                    validateCommandLength(stringBuilder);
                } while (continuesToNextLine(stringBuilder));

                string3 = stringBuilder.toString();
            } else {
                string3 = string;
            }

            validateCommandLength(string3);
            StringReader stringReader = new StringReader(string3);
            if (stringReader.canRead() && stringReader.peek() != '#') {
                if (stringReader.peek() == '/') {
                    stringReader.skip();
                    if (stringReader.peek() == '/') {
                        throw new IllegalArgumentException("Unknown or invalid command '" + string3 + "' on line " + j + " (if you intended to make a comment, use '#' not '//')");
                    }

                    String string2 = stringReader.readUnquotedString();
                    throw new IllegalArgumentException("Unknown or invalid command '" + string3 + "' on line " + j + " (did you mean '" + string2 + "'? Do not use a preceding forwards slash.)");
                }

                if (stringReader.peek() == '$') {
                    functionBuilder.addMacroCommand(string3.substring(1), j, source);
                } else {
                    try {
                        SourcedCommandAction<T> action = parse(dispatcher, source, stringReader);
                        ReflectUtil.invoke(action, "setSourceFunction", id.toString())
                                .onFailure(msg -> {
                                    throw new IllegalArgumentException(msg);
                                });
                        ReflectUtil.invoke(action, "setSourceLine", j - 1)
                                .onFailure(msg -> {
                                    throw new IllegalArgumentException(msg);
                                });
                        functionBuilder.addAction(action);
                    } catch (CommandSyntaxException commandSyntaxException) {
                        throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + commandSyntaxException.getMessage());
                    }
                }
            }else if(stringReader.canRead() && Extension.test(stringReader, "#!")){
                stringReader.skip();
                stringReader.skip();
                try {
                    SourcedCommandAction<T> action = parse(dispatcher, source, stringReader);
                    ReflectUtil.invoke(action, "setSourceFunction", id.toString())
                            .onFailure(msg -> {
                                throw new IllegalArgumentException(msg);
                            });
                    ReflectUtil.invoke(action, "setSourceLine", j - 1)
                            .onFailure(msg -> {
                                throw new IllegalArgumentException(msg);
                            });
                    functionBuilder.addAction(action);
                } catch (CommandSyntaxException commandSyntaxException) {
                     DatapackDebugger.getLogger().warn("Whilst parsing debug command on line " + j + ": " + commandSyntaxException.getMessage());
                }
            }
        }
        return functionBuilder.toCommandFunction(id);
    }

    
}

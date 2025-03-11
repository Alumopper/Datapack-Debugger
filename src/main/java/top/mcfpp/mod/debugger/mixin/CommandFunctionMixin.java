package top.mcfpp.mod.debugger.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.FunctionBuilder;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.mcfpp.mod.debugger.EncapsulationBreaker;
import top.mcfpp.mod.debugger.command.FunctionTextLoader;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.function.CommandFunction.validateCommandLength;
import static net.minecraft.server.function.CommandFunction.parse;

@Mixin(CommandFunction.class)
public interface CommandFunctionMixin {

    @Shadow
    private static boolean continuesToNextLine(CharSequence string) {
        return false;
    }

    @ModifyVariable(method = "create", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static List<String> create(List<String> value) {
        ArrayList<String> list = new ArrayList<>();
        // Iteration over the lines of the function
        for (String str : value){
            // If the current line is a breakpoint comment, we replace it by the breakpoint command
            if(str.equals("#breakpoint")){
                list.add("breakpoint");
                // We add the line without modification otherwise
            }else {
                list.add(str);
            }
        }
        return list;
    }

//    @Inject(method = "create",at = @At("HEAD"))
//    private static <T extends AbstractServerCommandSource<T>> void create(Identifier id, CommandDispatcher<T> dispatcher, T source, List<String> lines, CallbackInfoReturnable<CommandFunction<T>> cir) {
//        FunctionTextLoader.put(id,lines);
//    }

//    @Inject(method = "create",at = @At("HEAD"))
/**
 * @author
 * @reason
 */
@Overwrite
    static <T extends AbstractServerCommandSource<T>> CommandFunction<T> create(Identifier id, CommandDispatcher<T> dispatcher, T source, List<String> lines) {
        FunctionTextLoader.put(id, lines);
        FunctionBuilder<T> functionBuilder;
        try {
            functionBuilder = FunctionBuilder.class.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < lines.size(); ++i) {
            int j = i + 1;
            String string = ((String) lines.get(i)).trim();
            String string3;
            if (continuesToNextLine(string)) {
                StringBuilder stringBuilder = new StringBuilder(string);

                do {
                    ++i;
                    if (i == lines.size()) {
                        throw new IllegalArgumentException("Line continuation at end of file");
                    }

                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    String string2 = ((String) lines.get(i)).trim();
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
                        EncapsulationBreaker.callFunction(action, "setSourceFunction", id.toString());
                        EncapsulationBreaker.callFunction(action, "setSourceLine", j - 1);
                        functionBuilder.addAction(action);
                    } catch (CommandSyntaxException commandSyntaxException) {
                        throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + commandSyntaxException.getMessage());
                    }
                }
            }
        }
        return functionBuilder.toCommandFunction(id);
    }
}

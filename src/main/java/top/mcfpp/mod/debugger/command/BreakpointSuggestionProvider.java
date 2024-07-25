package top.mcfpp.mod.debugger.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import top.mcfpp.mod.debugger.utils.Debugger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BreakpointSuggestionProvider implements SuggestionProvider<ServerCommandSource>{

    public static final BreakpointSuggestionProvider INSTANCE = new BreakpointSuggestionProvider();

    private BreakpointSuggestionProvider() {}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> c, SuggestionsBuilder builder) {
        var context = Debugger.storedCommandExecutionContext.peekFirst();
        if(context == null){
            return builder.buildFuture();
        }
        try {
            var cls = context.getClass();
            var method = cls.getDeclaredMethod("getKeys");
            method.setAccessible(true);
            var keys = (List<String>) method.invoke(context);
            for (var key : keys) {
                builder.suggest(key);
            }
            return builder.buildFuture();
        }catch (Exception e){
            return builder.buildFuture();
        }
    }
}

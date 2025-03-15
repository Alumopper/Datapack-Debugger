package net.gunivers.sniffer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides command suggestions for breakpoint-related commands.
 * This class helps users by suggesting available variable names and keys
 * when using the breakpoint get command.
 */
public class BreakpointSuggestionProvider implements SuggestionProvider<ServerCommandSource>{

    /** Singleton instance of the suggestion provider */
    public static final BreakpointSuggestionProvider INSTANCE = new BreakpointSuggestionProvider();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private BreakpointSuggestionProvider() {}

    /**
     * Generates suggestions for available keys in the current debug context.
     * @param c The command context
     * @param builder The suggestions builder
     * @return A future containing the generated suggestions
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> c, SuggestionsBuilder builder) {
        var context = BreakPointCommand.storedCommandExecutionContext.peekFirst();
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

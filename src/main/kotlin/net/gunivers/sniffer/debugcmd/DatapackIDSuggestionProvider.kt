package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.WorldSavePath
import java.io.File
import java.util.concurrent.CompletableFuture

object DatapackIDSuggestionProvider: SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val datapackPath = context.source.server.getSavePath(WorldSavePath.DATAPACKS)
        val datapackIDs = datapackPath.toFile().listFiles(File::isDirectory)?.map { it.name } ?: emptyList()
        for (datapackID in datapackIDs) {
            builder.suggest(datapackID)
        }
        return builder.buildFuture()
    }
}
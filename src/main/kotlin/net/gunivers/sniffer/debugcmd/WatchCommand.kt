package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.logging.LogUtils
import io.methvin.watcher.DirectoryChangeEvent
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.gunivers.sniffer.mixin.CommandFunctionManagerAccessors
import net.gunivers.sniffer.mixin.FunctionLoaderAccessors
import net.gunivers.sniffer.util.ReflectUtil
import net.gunivers.sniffer.watcher.WatcherManager
import net.minecraft.screen.ScreenTexts
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.function.CommandFunction
import net.minecraft.server.function.FunctionLoader
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object WatchCommand {

    private enum class State { CREATED, MODIFIED, DELETED }

    private data class Entry(val state: State, val datapack: Path)


    private val LOGGER = LogUtils.getLogger()

    private var createdFunction = emptyList<Pair<Path, Path>>()
    private var deletedFunction = emptyList<Pair<Path, Path>>()
    private var modifiedFunction = emptyList<Pair<Path, Path>>()

    private val map = ConcurrentHashMap<Path, Entry>()

    private var isAutoReload = false

    @JvmStatic
    fun onInitialize(){
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal<ServerCommandSource?>("watch")
                    .requires{it.hasPermissionLevel(2)}
                    .then(literal<ServerCommandSource?>("start")
                        .then(argument("id", StringArgumentType.string())
                            .suggests(DatapackIDSuggestionProvider)
                            .executes {
                                val id = StringArgumentType.getString(it ,"id")
                                val src = it.source
                                val server = src.server
                                return@executes startWatch(server, src, id)
                            }
                        )
                    ).then(literal<ServerCommandSource?>("stop")
                        .then(argument("id", StringArgumentType.string())
                            .suggests(DatapackIDSuggestionProvider)
                            .executes {
                                val id = StringArgumentType.getString(it ,"id")
                                val src = it.source
                                return@executes stopWatch(src, id)
                            }
                        )
                    ).then(literal<ServerCommandSource?>("auto")
                        .then(argument("bool", BoolArgumentType.bool())
                            .executes {
                                //set if watcher will auto reload function when changed
                                val bool = BoolArgumentType.getBool(it, "bool")
                                isAutoReload = bool
                                if(isAutoReload){
                                    it.source.sendFeedback({ Text.translatable("sniffer.commands.watcher.auto.enable")}, false)
                                }else{
                                    it.source.sendFeedback({ Text.translatable("sniffer.commands.watcher.auto.disable")}, false)
                                }
                                return@executes 1
                            }
                        ).executes {
                            //return if auto reload is enabled
                            it.source.sendFeedback({ Text.translatable("sniffer.commands.watcher.auto", isAutoReload) }, false)
                            return@executes 1
                        }
                    ).then(literal<ServerCommandSource?>("reload")
                        .executes {
                            it.source.sendFeedback({Text.translatable("sniffer.commands.watcher.hot_reload")}, false)
                            hotReload(it.source.server)
                            return@executes 1
                        }
                    )

            )
        }
    }

    private fun startWatch(server: MinecraftServer, src: ServerCommandSource, id: String): Int{
        try{
            val datapackPath = server.getSavePath(WorldSavePath.DATAPACKS)
            val packPath = datapackPath.resolve(id)
            if(Files.notExists(packPath)){
                src.sendError(Text.translatable("sniffer.commands.watcher.failed.datapack_not_found", id))
                return 0
            }
            val functionsRoot = packPath.resolve("data")
            val ok = WatcherManager.start(id, functionsRoot, server){
                server.execute {
//                    val msg = java.lang.String.format("[watch:%s] %s %s", id, it.eventType(), it.path())
//                    server.playerManager.broadcast(Text.of(msg), false)
                    processFunctionChange(it, packPath)
                    if(isAutoReload){
                        hotReload(server)
                    }
                }
            }
            if(ok){
                src.sendFeedback({ Text.translatable("sniffer.commands.watcher.start", id) }, false)
                return 1
            }else{
                src.sendError(Text.translatable("sniffer.commands.watcher.start.failed", id))
                return 0
            }
        }catch (ex: Exception){
            src.sendError(Text.translatable("sniffer.commands.watcher.start.failed", id))
            LOGGER.error("Failed to start watching: $id", ex)
            return 0
        }
    }

    private fun stopWatch(src: ServerCommandSource, id: String): Int{
        try{
            val ok = WatcherManager.stop(id)
            if(ok){
                src.sendFeedback({ Text.translatable("sniffer.commands.watcher.stop", id) }, false)
                return 1
            }else{
                src.sendError(Text.translatable("sniffer.commands.watcher.stop.failed", id))
                return 0
            }
        }catch (ex: Exception){
            src.sendError(Text.translatable("sniffer.commands.watcher.stop.failed", id))
            LOGGER.error("Failed to stop watching: $id", ex)
            return 0
        }
    }

    private fun processFunctionChange(event: DirectoryChangeEvent, datapackPath: Path) {
        val p = event.path()
        when (event.eventType()) {
            DirectoryChangeEvent.EventType.CREATE -> map.compute(p) { _, old ->
                when (old?.state) {
                    null -> Entry(State.CREATED, datapackPath)
                    State.CREATED -> Entry(State.CREATED, datapackPath)         // keep created
                    State.MODIFIED -> Entry(State.MODIFIED, datapackPath)     // keep modified
                    State.DELETED -> Entry(State.MODIFIED, datapackPath)      // deleted -> recreated => treated as modification
                }
            }

            DirectoryChangeEvent.EventType.MODIFY -> map.compute(p) { _, old ->
                when (old?.state) {
                    null -> Entry(State.MODIFIED, datapackPath)
                    State.CREATED -> Entry(State.CREATED, datapackPath)        // created stays created
                    State.MODIFIED -> Entry(State.MODIFIED, datapackPath)
                    State.DELETED -> Entry(State.MODIFIED, datapackPath)      // deleted -> re-appeared => modification
                }
            }

            DirectoryChangeEvent.EventType.DELETE -> map.compute(p) { _, old ->
                when (old?.state) {
                    null -> Entry(State.DELETED, datapackPath)
                    State.CREATED -> null                                     // created then deleted => remove (no-op)
                    State.MODIFIED -> Entry(State.DELETED, datapackPath)
                    State.DELETED -> Entry(State.DELETED, datapackPath)      // keep deleted
                }
            }

            else -> {
                LOGGER.error("Unknown event type: ${event.eventType()}")
            }
        }
    }

    // Record function change
    private fun created(): List<Pair<Path, Path>> =
        map.entries.filter { it.value.state == State.CREATED }.map { it.key to it.value.datapack }

    private fun modified(): List<Pair<Path, Path>> =
        map.entries.filter { it.value.state == State.MODIFIED }.map { it.key to it.value.datapack }

    private fun deleted(): List<Pair<Path, Path>> =
        map.entries.filter { it.value.state == State.DELETED }.map { it.key to it.value.datapack }

    private fun snapshotAndClear(){
        createdFunction = created()
        modifiedFunction = modified()
        deletedFunction = deleted()
        map.clear()
    }

    private fun hotReload(server: MinecraftServer){
        snapshotAndClear()
        val loader = (server.commandFunctionManager as CommandFunctionManagerAccessors).loader
        createFunction(server, loader, createdFunction)
        modifyFunction(server, loader, modifiedFunction)
        deleteFunction(server, loader, deletedFunction)
    }

    private fun modifyFunction(server: MinecraftServer, loader: FunctionLoader, path: List<Pair<Path, Path>>){
        val la = loader as FunctionLoaderAccessors
        val dispatcher = la.commandDispatcher
        val functions = la.functions
        val level = la.level
        val serverCommandSource = ServerCommandSource(
            CommandOutput.DUMMY, Vec3d.ZERO, Vec2f.ZERO, null, level, "", ScreenTexts.EMPTY, null, null
        )
        CompletableFuture.supplyAsync {
            path.map { (functionPath, datapackPath) ->
                val identifier = getIdentifier(functionPath, datapackPath)
                //read function contents
                val lines = Files.readAllLines(functionPath)
                try{
                    return@map CommandFunction.create(identifier, dispatcher, serverCommandSource, lines)
                }catch (ex: Exception){
                    val text = Text.translatable("sniffer.commands.watcher.modify.failed", identifier).withColor(Colors.RED)
                    server.playerManager.broadcast(text, false)
                    LOGGER.error("Failed to modify function: $identifier", ex)
                    return@map null
                }
            }.filterNotNull()
        }.handle {modified, ex ->
            if(ex != null){
                val text = Text.translatable("sniffer.commands.watcher.modify.failed.ex", ex.message).withColor(Colors.RED)
                server.playerManager.broadcast(text, false)
                LOGGER.error("Failed to modify functions", ex)
            }
            val qwq = HashMap<Identifier, CommandFunction<ServerCommandSource>>()
            qwq.putAll(functions)
            modified.forEach {
                qwq[it.id()] = it
                val text = Text.literal("• ${it.id()}").withColor(TextColor.parse("#D1A21E").getOrThrow().rgb)
                server.playerManager.broadcast(text, false)
            }
            la.functions = qwq
        }
    }

    private fun createFunction(server: MinecraftServer, loader: FunctionLoader, path: List<Pair<Path, Path>>){
        val la = loader as FunctionLoaderAccessors
        val dispatcher = la.commandDispatcher
        val functions = la.functions
        val level = la.level
        val serverCommandSource = ServerCommandSource(
            CommandOutput.DUMMY, Vec3d.ZERO, Vec2f.ZERO, null, level, "", ScreenTexts.EMPTY, null, null
        )
        CompletableFuture.supplyAsync {
            path.map { (functionPath, datapackPath) ->
                val identifier = getIdentifier(functionPath, datapackPath)
                //read function contents
                val lines = Files.readAllLines(functionPath)
                try{
                    return@map CommandFunction.create(identifier, dispatcher, serverCommandSource, lines)
                }catch (ex: Exception){
                    val text = Text.translatable("sniffer.commands.watcher.create.failed", identifier).withColor(Colors.RED)
                    server.playerManager.broadcast(text, false)
                    LOGGER.error("Failed to create function: $identifier", ex)
                    return@map null
                }
            }.filterNotNull()
        }.handle {created, ex ->
            if(ex != null){
                val text = Text.translatable("sniffer.commands.watcher.create.failed.ex", ex.message).withColor(Colors.RED)
                server.playerManager.broadcast(text, false)
                LOGGER.error("Failed to create functions", ex)
            }
            val qwq = HashMap<Identifier, CommandFunction<ServerCommandSource>>()
            qwq.putAll(functions)
            created.forEach {
                qwq[it.id()] = it
                val text = Text.literal("+ ${it.id()}").withColor(TextColor.parse("#12B617").getOrThrow().rgb)
                server.playerManager.broadcast(text, false)
            }
            la.functions = qwq
        }
    }

    private fun deleteFunction(server: MinecraftServer, loader:FunctionLoader, path: List<Pair<Path, Path>>){
        val la = loader as FunctionLoaderAccessors
        val functions = la.functions
        CompletableFuture.supplyAsync {
            path.map { (functionPath, datapackPath) ->
                getIdentifier(functionPath, datapackPath)
            }
        }.handle {deleted, ex ->
            if(ex != null){
                val text = Text.translatable("sniffer.commands.watcher.delete.failed.ex", ex.message).withColor(Colors.RED)
                server.playerManager.broadcast(text, false)
                LOGGER.error("Failed to delete functions", ex)
            }
            val qwq = HashMap<Identifier, CommandFunction<ServerCommandSource>>()
            qwq.putAll(functions)
            deleted.forEach {
                qwq.remove(it)
                val text = Text.literal("- $it").withColor(TextColor.parse("#B61212").getOrThrow().rgb)
                server.playerManager.broadcast(text, false)
            }
            la.functions = qwq
        }
    }

    private fun getIdentifier(functionPath: Path, datapackPath: Path): Identifier {
        val func = functionPath.toAbsolutePath().normalize()
        val dp = datapackPath.toAbsolutePath().normalize()
        val rel = dp.relativize(func) // 假设输入合法，rel 格式为 data/<namespace>/functions/...

        val namespace = rel.getName(1).toString()
        val funcPathPart = rel.subpath(3, rel.nameCount).toString().replace(File.separatorChar, '/')
        val pathWithoutExt = funcPathPart.removeSuffix(".mcfunction")

        return Identifier.of(namespace, pathWithoutExt)
    }
}
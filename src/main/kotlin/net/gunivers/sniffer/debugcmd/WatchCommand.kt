package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.logging.LogUtils
import io.methvin.watcher.DirectoryChangeEvent
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.gunivers.sniffer.util.ReflectUtil
import net.gunivers.sniffer.watcher.WatcherManager
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.function.FunctionLoader
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
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
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
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
                                return@executes 1
                            }
                        ).executes {
                            //return if auto reload is enabled
                            it.source.sendFeedback({ Text.literal("Auto reload: $isAutoReload") }, false)
                            return@executes 1
                        }
                    ).then(literal<ServerCommandSource?>("reload")
                        .executes {
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
                src.sendError(Text.literal("Datapack $id not found"))
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
                src.sendFeedback({ Text.literal("Started watching: $id") }, false)
                return 1
            }else{
                src.sendError(Text.literal("Failed to start watching: $id"))
                return 0
            }
        }catch (ex: Exception){
            src.sendError(Text.literal("Failed to start watching: $id"))
            LOGGER.error("Failed to start watching: $id", ex)
            return 0
        }
    }

    private fun stopWatch(src: ServerCommandSource, id: String): Int{
        try{
            val ok = WatcherManager.stop(id)
            if(ok){
                src.sendFeedback({ Text.literal("Stopped watching: $id") }, false)
                return 1
            }else{
                src.sendError(Text.literal("Failed to stop watching: $id"))
                return 0
            }
        }catch (ex: Exception){
            src.sendError(Text.literal("Failed to stop watching: $id"))
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
        val loader = ReflectUtil.getT(server.commandFunctionManager, "loader", FunctionLoader::class.java).data
        for ((path, datapackPath) in createdFunction){
            createFunction(server, loader, path, datapackPath)
        }
        for ((path, datapackPath) in modifiedFunction){
            modifyFunction(server, loader, path, datapackPath)
        }
        for ((path, datapackPath) in deletedFunction){
            deleteFunction(server, loader, path, datapackPath)
        }
    }

    private fun modifyFunction(server: MinecraftServer, loader: FunctionLoader, path: Path, datapackPath: Path){
        val id = getIdentifier(path, datapackPath)
        val text = Text.literal("• $id").withColor(TextColor.parse("#D1A21E").getOrThrow().rgb)
        server.playerManager.broadcast(text, false)
    }

    private fun createFunction(server: MinecraftServer, loader: FunctionLoader, path: Path, datapackPath: Path){
        val id = getIdentifier(path, datapackPath)
        val text = Text.literal("+ $id").withColor(TextColor.parse("#12B617").getOrThrow().rgb)
        server.playerManager.broadcast(text, false)
    }

    private fun deleteFunction(server: MinecraftServer, loader:FunctionLoader, path: Path, datapackPath: Path){
        val id = getIdentifier(path, datapackPath)
        val text = Text.literal("- $id").withColor(TextColor.parse("#B62712").getOrThrow().rgb)
        server.playerManager.broadcast(text, false)
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
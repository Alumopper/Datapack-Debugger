package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.Message
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.gunivers.sniffer.util.ReflectUtil
import net.gunivers.sniffer.util.Extension.expect
import net.gunivers.sniffer.util.Extension.readUntil
import net.gunivers.sniffer.util.Extension.readWord
import net.gunivers.sniffer.util.Extension.test
import net.minecraft.command.BlockDataObject
import net.minecraft.command.EntityDataObject
import net.minecraft.command.EntitySelector
import net.minecraft.command.StorageDataObject
import net.minecraft.command.argument.*
import net.minecraft.command.argument.NbtPathArgumentType.NbtPath
import net.minecraft.command.argument.NbtPathArgumentType.nbtPath
import net.minecraft.command.argument.ScoreHolderArgumentType.ScoreHolders
import net.minecraft.nbt.*
import net.minecraft.server.command.DataCommand
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class LogArgumentType: ArgumentType<LogArgumentType.Companion.Log> {
    @Suppress("unused", "PrivatePropertyName")
    private val EXAMPLES = mutableListOf("qwq {(some thing) == 1}")

    override fun getExamples(): MutableCollection<String> = EXAMPLES

    override fun parse(reader: StringReader): Log {
        val log = Log()
        while (reader.canRead()){
            if(reader.test('{')){
                //an argument
                log.logs.add(ExprArgumentType().parse(reader))
            }else {
                //plain text
                log.logs.add(PlainData(reader.readUntil('{')))
            }
        }
        return log
    }

    companion object {
        class Log(val logs: ArrayList<DebugData> = arrayListOf())

        @JvmStatic
        fun getLog(context: CommandContext<*>, name: String?): Log {
            return context.getArgument(name, Log::class.java)
        }

        @JvmStatic
        fun log() = LogArgumentType()
    }
}

class ExprArgumentType: ArgumentType<ExprArgumentType.Companion.Experiment> {

    override fun parse(reader: StringReader): Experiment {
        val startIndex = reader.cursor
        reader.expect('{')
        reader.skipWhitespace()
        var first: DebugData? = null
        var op: String? = null
        val ops = ArrayList<Pair<String, DebugData?>>()
        while(reader.canRead() && !reader.test('}')){
            //Testing whether the next argument is a value or a variable.
            if(reader.test('(')){
                //is a parameter
                val arg = parseArgument(reader)
                if(first == null && ops.isEmpty()){
                    first = arg
                }else{
                    if(op == null){
                        throw MISSING_OP_ERROR.createWithContext(reader)
                    }
                    ops.add(op to arg)
                }
            }else if(reader.test('{')){
                //is another experiment
                if(op == null){
                    throw MISSING_OP_ERROR.createWithContext(reader)
                }
                ops.add(op to parse(reader))
            }else {
                //is an operation or a value
                val isOp = reader.test { supportedOps.contains(reader.readWord()) }
                if(isOp){
                    op = reader.readWord()
                }else{
                    //is a value
                    val arg = PlainData(NbtElementArgumentType.nbtElement().parse(reader))
                    if(first == null && ops.isEmpty()){
                        first = arg
                    }else{
                        if(op == null){
                            throw MISSING_OP_ERROR.createWithContext(reader)
                        }
                        ops.add(op to arg)
                    }
                }
            }
            reader.skipWhitespace()
        }
        if(ops.isEmpty() && first == null){
            throw EMPTY_EXPR_ERROR.createWithContext(reader)
        }
        if(ops.isNotEmpty() && ops.last().second == null){
            throw MISSING_OP_ERROR.createWithContext(reader)
        }
        reader.expect('}')
        val endIndex = reader.cursor
        return Experiment(first!!, ops.map { (op, arg) -> op to arg!! }, reader.string.substring(startIndex, endIndex))
    }

    fun parseArgument(reader: StringReader): DebugData {
        return if(reader.test().expect('(').skipWhitespace().expect("data").result()){
            //is data
            DataArgumentType().parse(reader)
        }else if(reader.test().expect('(').skipWhitespace().expect("score").result()){
            //is score
            ScoreArgumentType().parse(reader)
        }else if(reader.test().expect('(').skipWhitespace().expect("name").result()){
            //is a name
            EntityNameType().parse(reader)
        }else {
            PlainData("")
        }
    }

    companion object {

        @JvmStatic
        fun expr() = ExprArgumentType()

        @JvmStatic
        fun getExpr(context: CommandContext<*>, name: String?): Experiment {
            return context.getArgument(name, Experiment::class.java)
        }

        class Experiment(val first: DebugData?, val ops: List<Pair<String, DebugData>>, val content: String): DebugData {
            override fun get(ctx: CommandContext<ServerCommandSource>): Any {
                var qwq = first?.get(ctx)
                for ((op, arg) in ops){
                    val argValue = arg.get(ctx)
                    if(op == "||" && qwq is NbtByte && qwq.value == 1.toByte()) continue
                    if(op == "&&" && qwq is NbtByte && qwq.value == 0.toByte()) continue
                    qwq = supportedOps[op]!!.apply(qwq, argValue)
                }
                return qwq!!
            }
        }

        private val supportedOps = mapOf(
            "+" to object: Operation("+"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return if(left is NbtDouble || right is NbtDouble){
                            NbtDouble.of(left.doubleValue() + right.doubleValue())
                        }else if(left is NbtFloat || right is NbtFloat){
                            NbtFloat.of(left.floatValue() + right.floatValue())
                        }else if(left is NbtLong || right is NbtLong){
                            NbtLong.of(left.longValue() + right.longValue())
                        }else {
                            NbtInt.of(left.intValue() + right.intValue())
                        }
                    }else if(left is Text && right is Text){
                        return Text.empty().append(left).append(right)
                    }else if(left is NbtCompound && right is NbtCompound){
                        return left.copyFrom(right)
                    }else if(left is NbtList && right is NbtList) {
                        return left.addAll(right)
                    }else if(left is NbtString && right is NbtString){
                        return NbtString.of(left.value + right.value)
                    }else if(left is Text && right is NbtString){
                        return NbtString.of(left.literalString + right.value)
                    }else if(left is NbtString && right is Text){
                        return NbtString.of(left.value + right.literalString)
                    }
                    else {
                        throw OPERATION_TYPE_ERROR.create(name, left?.javaClass, right.javaClass)
                    }
                }
            },
            "-" to object: Operation("-"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return if(left is NbtDouble || right is NbtDouble){
                            NbtDouble.of(left.doubleValue() - right.doubleValue())
                        }else if(left is NbtFloat || right is NbtFloat){
                            NbtFloat.of(left.floatValue() - right.floatValue())
                        }else if(left is NbtLong || right is NbtLong){
                            NbtLong.of(left.longValue() - right.longValue())
                        }else {
                            NbtInt.of(left.intValue() - right.intValue())
                        }
                    }else {
                        throw buildOperationTypeError(left, right)
                    }
                }
            },
            "*" to object: Operation("*"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return if(left is NbtDouble || right is NbtDouble){
                            NbtDouble.of(left.doubleValue() * right.doubleValue())
                        }else if(left is NbtFloat || right is NbtFloat){
                            NbtFloat.of(left.floatValue() * right.floatValue())
                        }else if(left is NbtLong || right is NbtLong){
                            NbtLong.of(left.longValue() * right.longValue())
                        }else {
                            NbtInt.of(left.intValue() * right.intValue())
                        }
                    }else {
                        throw buildOperationTypeError(left, right)
                    }
                }
            },
            "/" to object: Operation("/"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return if(left is NbtDouble || right is NbtDouble){
                            NbtDouble.of(left.doubleValue() / right.doubleValue())
                        }else if(left is NbtFloat || right is NbtFloat){
                            NbtFloat.of(left.floatValue() / right.floatValue())
                        }else if(left is NbtLong || right is NbtLong){
                            NbtLong.of(left.longValue() / right.longValue())
                        }else {
                            NbtInt.of(left.intValue() / right.intValue())
                        }
                    }else {
                        throw buildOperationTypeError(left, right)
                    }
                }
            },
            "<" to object: Operation("<"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return NbtByte.of(left.doubleValue() < right.doubleValue())
                    }else {
                        throw buildOperationTypeError(left, right)
                    }
                }
            },
            ">" to object: Operation(">"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return NbtByte.of(left.doubleValue() > right.doubleValue())
                    }else {
                        throw buildOperationTypeError(left, right)
                    }
                }
            },
            "<=" to object: Operation("<="){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return NbtByte.of(left.doubleValue() <= right.doubleValue())
                    }else {
                        throw buildOperationTypeError(left, right)
                    }
                }
            },
            ">=" to object: Operation(">="){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is AbstractNbtNumber && right is AbstractNbtNumber){
                        return NbtByte.of(left.doubleValue() >= right.doubleValue())
                    }else {
                        throw buildOperationTypeError(left, right)
                    }
                }
            },
            "==" to object: Operation("=="){
                override fun apply(left: Any?, right: Any): Any {
                    if(left is NbtElement && right is NbtElement) return NbtByte.of(left == right)
                    if(left is Text && right is NbtString) return NbtByte.of(left.literalString == right.value)
                    if(left is NbtString && right is Text) return NbtByte.of(left.value == right.literalString)
                    return false
                }
            },
            "!=" to object: Operation("!=") {
                override fun apply(left: Any?, right: Any): Any {
                    if (left is NbtElement && right is NbtElement) return NbtByte.of(left != right)
                    if (left is Text && right is NbtString) return NbtByte.of(left.literalString != right.value)
                    if (left is NbtString && right is Text) return NbtByte.of(left.value != right.literalString)
                    return true
                }
            },
            "is" to object: Operation("is") {
                override fun apply(left: Any?, right: Any): Any {
                    if(right !is NbtString) throw buildOperationTypeError(left, right)
                    val qwq = when(right.value){
                        "nbt" -> left is NbtElement
                        "text" -> left is Text
                        "string" -> left is NbtString
                        "number" -> left is AbstractNbtNumber
                        "byte" -> left is NbtByte
                        "short" -> left is NbtShort
                        "int" -> left is NbtInt
                        "long" -> left is NbtLong
                        "float" -> left is NbtFloat
                        "double" -> left is NbtDouble
                        "int_array" -> left is NbtIntArray
                        "long_array" -> left is NbtLongArray
                        "byte_array" -> left is NbtByteArray
                        "list" -> left is NbtList
                        "compound" -> left is NbtCompound
                        else -> false
                    }
                    return NbtByte.of(qwq)
                }
            },
            "!" to object: Operation("!"){
                override fun apply(left: Any?, right: Any): Any {
                    if(right !is NbtByte) throw buildOperationTypeError(left, right)
                    return NbtByte.of(!right.asBoolean().get())
                }
            },
            "||" to object: Operation("||"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left !is NbtByte || right !is NbtByte) throw buildOperationTypeError(left, right)
                    return NbtByte.of(left.asBoolean().get() || right.asBoolean().get())
                }
            },
            "&&" to object: Operation("&&"){
                override fun apply(left: Any?, right: Any): Any {
                    if(left !is NbtByte || right !is NbtByte) throw buildOperationTypeError(left, right)
                    return NbtByte.of(left.asBoolean().get() && right.asBoolean().get())
                }
            }
        )

        abstract class Operation(val name: String){
            abstract fun apply(left: Any?, right: Any): Any
            override fun toString(): String {
                return name
            }
            fun buildOperationTypeError(left: Any?, right: Any) = 
                OPERATION_TYPE_ERROR.create(name, left?.javaClass?.simpleName, right.javaClass.simpleName)
            
        }

        private val EMPTY_EXPR_ERROR = SimpleCommandExceptionType { "Empty expression" }
        private val MISSING_OP_ERROR = SimpleCommandExceptionType { "Missing operation between arguments" }
        private val MISSING_ARG_ERROR = SimpleCommandExceptionType { "Missing arguments after operation" }
        private val OPERATION_TYPE_ERROR = Dynamic3CommandExceptionType { operation, left, right -> Message { "Operation $operation is not applicable to $left and $right" }}

    }

}

interface DebugData {
    fun get(ctx: CommandContext<ServerCommandSource>): Any

    companion object {
        fun toText(any: Any): Text{
            return when(any){
                is NbtElement -> NbtHelper.toPrettyPrintedText(any)
                is Text -> any
                else -> Text.literal(any.toString())
            }
        }
    }
}

class PlainData(private val value: Any): DebugData {
    override fun get(ctx: CommandContext<ServerCommandSource>): Any {
        return value
    }
}

class EntityNameType: ArgumentType<EntityNameType.Companion.Name>{
    override fun parse(reader: StringReader): Name {
        reader.expect('(')
        reader.skipWhitespace()
        reader.expect("name")
        reader.skipWhitespace()
        val name = MessageArgumentType.message().parse(reader)
        reader.expect(')')
        return Name(name)
    }

    companion object {
        class Name(val msg: MessageArgumentType.MessageFormat): DebugData {
            override fun get(ctx: CommandContext<ServerCommandSource>): Text {
                return msg.format(ctx.source, true)
            }

        }
    }
}

class DataArgumentType: ArgumentType<DataArgumentType.Companion.Data> {

    override fun parse(reader: StringReader): Data {
        reader.expect('(')
        reader.skipWhitespace()
        reader.expect("data")
        reader.skipWhitespace()
        val qwq = when(reader.readUnquotedString()){
            "block" -> {
                reader.skipWhitespace()
                val pos = BlockPosArgumentType.blockPos().parse(reader)
                reader.skipWhitespace()
                val path = nbtPath().parse(reader)
                BlockDataSource(pos, path)
            }
            "entity" -> {
                reader.skipWhitespace()
                val selector = EntityArgumentType.entity().parse(reader)
                reader.skipWhitespace()
                val path = nbtPath().parse(reader)
                EntityDataSource(selector, path)
            }
            "storage" -> {
                reader.skipWhitespace()
                val id = IdentifierArgumentType.identifier().parse(reader)
                reader.skipWhitespace()
                val path = nbtPath().parse(reader)
                StorageDataSource(id, path)
            }
            else -> throw INVALID_OBJECT_ERROR.createWithContext(reader)
        }
        reader.skipWhitespace()
        reader.expect(')')
        return Data(qwq)
    }

    companion object {

        private val INVALID_OBJECT_ERROR = SimpleCommandExceptionType { "Invalid object type for data argument" }
        val INVALID_BLOCK_EXCEPTION = SimpleCommandExceptionType(Text.translatable("commands.data.block.invalid"))

        class Data(val source: DataSource): DebugData {
            override fun get(ctx: CommandContext<ServerCommandSource>): NbtElement {
                return source.getNbtElement(ctx)
            }
        }

        interface DataSource {
                fun getNbtElement(ctx: CommandContext<ServerCommandSource>): NbtElement
        }

        private class EntityDataSource(val selector: EntitySelector, val path: NbtPath) : DataSource {
            override fun getNbtElement(ctx: CommandContext<ServerCommandSource>): NbtElement {
                return DataCommand.getNbt(path, EntityDataObject(selector.getEntity(ctx.source)))
            }
        }

        private class BlockDataSource(val pos: PosArgument, val path: NbtPath): DataSource {
            @Suppress("DEPRECATION")
            override fun getNbtElement(ctx: CommandContext<ServerCommandSource>): NbtElement {
                val blockPos = pos.toAbsoluteBlockPos(ctx.source)
                val world = ctx.source.world
                if (!world.isChunkLoaded(blockPos)) {
                    throw BlockPosArgumentType.UNLOADED_EXCEPTION.create()
                } else if (!world.isInBuildLimit(blockPos)) {
                    throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create()
                }
                val blockEntity = ctx.source.world.getBlockEntity(blockPos) ?: throw INVALID_BLOCK_EXCEPTION.create()
                return DataCommand.getNbt(path, BlockDataObject(blockEntity, blockPos))
            }
        }

        private class StorageDataSource(val id: Identifier, val path: NbtPath): DataSource {
            override fun getNbtElement(ctx: CommandContext<ServerCommandSource>): NbtElement {
                return DataCommand.getNbt(path, ReflectUtil.newInstance(StorageDataObject::class.java, ctx.source.server.dataCommandStorage, id).data)
            }
        }
    }
}

class ScoreArgumentType: ArgumentType<ScoreArgumentType.Companion.Score> {

    @Suppress("unused", "PrivatePropertyName")
    private val EXAMPLES = listOf("{score @s test}", "{score qwq uwu}")
    @Suppress("PrivatePropertyName")
    private val ERROR = SimpleCommandExceptionType { "Invalid score argument" }

    override fun parse(reader: StringReader): Score {
        reader.expect('(')
        skipWhitespace(reader)
        val keyword = reader.readUnquotedString()
        //check keyword
        if("score" != keyword){
            throw ERROR.createWithContext(reader)
        }
        skipWhitespace(reader)
        //read selector
        val scoreHolder = ScoreHolderArgumentType.scoreHolder().parse(reader)
        skipWhitespace(reader)
        val objective = ScoreboardObjectiveArgumentType.scoreboardObjective().parse(reader)
        reader.expect(')')
        return Score(scoreHolder, objective)
    }

    companion object {
        class Score (
            val scoreHolder: ScoreHolders,
            val objective: String
        ): DebugData {
            override fun get(ctx: CommandContext<ServerCommandSource>): NbtInt {
                val scoreboard = ctx.source.server.scoreboard
                val holder = scoreHolder.getNames(ctx.source) { ArrayList() }.last()
                val obj = ScoreboardObjectiveArgumentType.getObjective(ctx, objective)
                val readableScoreboardScore = scoreboard.getScore(holder, obj)
                if (readableScoreboardScore == null) {
                    throw PLAYERS_GET_NULL_EXCEPTION.create(
                        obj.name,
                        holder.styledDisplayName
                    )
                } else {
                    return NbtInt.of(readableScoreboardScore.score)
                }
            }

        }

         fun score(): ScoreArgumentType = ScoreArgumentType()

        private fun skipWhitespace(reader: StringReader) {
            while (reader.canRead() && Character.isWhitespace(reader.peek())) reader.skip()
        }
        private val PLAYERS_GET_NULL_EXCEPTION = Dynamic2CommandExceptionType { objective: Any?, target: Any? ->
            Text.stringifiedTranslatable(
                "commands.scoreboard.players.get.null",
                objective,
                target
            )
        }
    }
}
package top.mcfpp.mod.debugger.mixin;

import com.google.common.collect.Queues;
import net.minecraft.command.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.server.function.Procedure;
import net.minecraft.server.function.Tracer;
import net.minecraft.util.Pair;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.List;

import static top.mcfpp.mod.debugger.command.BreakPointCommand.*;

@Mixin(CommandExecutionContext.class)
abstract public class CommandExecutionContextMixin<T> {

    @Unique
    private final Deque<CommandQueueEntry<T>> storedCommandQueue = Queues.newArrayDeque();

    @Shadow @Final private static int MAX_COMMAND_QUEUE_LENGTH;
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private int maxCommandChainLength;
    @Shadow @Final private int forkLimit;
    @Shadow @Final private Profiler profiler;
    @Shadow private Tracer tracer;
    @Shadow private int commandsRemaining;
    @Shadow private boolean queueOverflowed;
    @Shadow @Final private Deque<CommandQueueEntry<T>> commandQueue;
    @Shadow @Final private List<CommandQueueEntry<T>> pendingCommands;
    @Shadow private int currentDepth;
    @Shadow protected abstract void queuePendingCommands();

    @Shadow private static <T extends AbstractServerCommandSource<T>> Frame frame(CommandExecutionContext<T> context, ReturnValueConsumer returnValueConsumer){return null;}

    @Inject(method = "enqueueProcedureCall", at = @At("HEAD"), cancellable = true)
    private static <T extends AbstractServerCommandSource<T>> void enqueueProcedureCall(
            CommandExecutionContext<T> context, Procedure<T> procedure, T source, ReturnValueConsumer returnValueConsumer, CallbackInfo ci
    ) {
        Frame frame = frame(context, returnValueConsumer);
        try {
            Field field = frame.getClass().getDeclaredField("function");
            field.setAccessible(true);
            field.set(frame, procedure);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        context.enqueueCommand(
                new CommandQueueEntry<>(frame, new CommandFunctionAction<>(procedure, source.getReturnValueConsumer(), false).bind(source))
        );
        ci.cancel();
    }

    @Inject(method = "run()V", at = @At("HEAD"), cancellable = true)
    private void onRun(CallbackInfo ci){

        final var THIS = (CommandExecutionContext<T>) (Object) this;

        this.queuePendingCommands();

        while (true) {
            if (this.commandsRemaining <= 0) {
                LOGGER.info("Command execution stopped due to limit (executed {} commands)", this.maxCommandChainLength);
                break;
            }

            CommandQueueEntry<T> commandQueueEntry = this.commandQueue.pollFirst();

            if (commandQueueEntry == null) {
                ci.cancel();
                return;
            }

            if(isDebugging && commandQueueEntry.frame().depth() != 0 && moveSteps == 0) {
                //在函数中执行的，把命令暂存
                commandQueue.addFirst(commandQueueEntry);
                if(storedCommandExecutionContext.peekFirst() != THIS) {
                    storedCommandExecutionContext.addFirst(THIS);
                }
                ci.cancel();
                return;
            }

            this.currentDepth = commandQueueEntry.frame().depth();
            commandQueueEntry.execute(THIS);
            if (this.queueOverflowed) {
                LOGGER.error("Command execution stopped due to command queue overflow (max {})", 10000000);
                break;
            }

            this.queuePendingCommands();
        }

        this.currentDepth = 0;

        //取消原方法的执行
        ci.cancel();
    }

    @Unique
    private Pair<NbtElement, Boolean> getKey(String key){
        CommandQueueEntry<T> commandQueueEntry = this.commandQueue.peekFirst();

        if (commandQueueEntry == null) {
            return null;
        }

        var frame = commandQueueEntry.frame();
        try {
            Field field = frame.getClass().getDeclaredField("function");
            field.setAccessible(true);
            var function = (ExpandedMacro<T>) field.get(frame);
            Field field1 = function.getClass().getDeclaredField("arguments");
            field1.setAccessible(true);
            var args = (NbtCompound)field1.get(function);
            if(args == null){
                return new Pair<>(null, false);
            }
            return new Pair<>(args.get(key), true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private NbtElement getAllNBT(){
        CommandQueueEntry<T> commandQueueEntry = this.commandQueue.peekFirst();

        if (commandQueueEntry == null) {
            return null;
        }

        var frame = commandQueueEntry.frame();
        try {
            Field field = frame.getClass().getDeclaredField("function");
            field.setAccessible(true);
            var function = (ExpandedMacro<T>) field.get(frame);
            Field field1 = function.getClass().getDeclaredField("arguments");
            field1.setAccessible(true);
            return (NbtCompound)field1.get(function);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private List<String> getKeys(){
        CommandQueueEntry<T> commandQueueEntry = this.commandQueue.peekFirst();

        if (commandQueueEntry == null) {
            return null;
        }

        var frame = commandQueueEntry.frame();
        try {
            Field field = frame.getClass().getDeclaredField("function");
            field.setAccessible(true);
            var function = (ExpandedMacro<T>) field.get(frame);
            Field field1 = function.getClass().getDeclaredField("arguments");
            field1.setAccessible(true);
            var args = (NbtCompound)field1.get(function);
            if(args != null){
                return args.getKeys().stream().toList();
            }else {
                return null;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private void onStep(){

        final var THIS = (CommandExecutionContext<T>) (Object) this;

        this.queuePendingCommands();

        while (true) {
            if (this.commandsRemaining <= 0) {
                LOGGER.info("Command execution stopped due to limit (executed {} commands)", this.maxCommandChainLength);
                break;
            }

            CommandQueueEntry<T> commandQueueEntry = this.commandQueue.pollFirst();

            if (commandQueueEntry == null) {
                return;
            }

            if(isDebugging && commandQueueEntry.frame().depth() != 0 && moveSteps == 0) {
                //在函数中执行的，把所有命令暂存
                commandQueue.addFirst(commandQueueEntry);
                if(storedCommandExecutionContext.peekFirst() != THIS) {
                    storedCommandExecutionContext.addFirst(THIS);
                }
                return;
            }

            this.currentDepth = commandQueueEntry.frame().depth();
            commandQueueEntry.execute(THIS);
            if (this.queueOverflowed) {
                LOGGER.error("Command execution stopped due to command queue overflow (max {})", 10000000);
                break;
            }

            this.queuePendingCommands();
        }

        this.currentDepth = 0;
    }

    @Unique
    private boolean isFixCommandAction(@NotNull CommandAction<T> action){
        if(!(action instanceof SourcedCommandAction)) return false;
        try {
            Class<?> actionClass = action.getClass();
            Field[] fields = actionClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(action);
                if (fieldValue instanceof FixedCommandAction) {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Unique
    private boolean ifContainsCommandAction(){
        return this.commandQueue.peekFirst() != null;
    }

}
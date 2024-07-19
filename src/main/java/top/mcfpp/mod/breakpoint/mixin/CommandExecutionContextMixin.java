package top.mcfpp.mod.breakpoint.mixin;

import com.google.common.collect.Queues;
import net.minecraft.command.*;
import net.minecraft.server.function.Tracer;
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
import top.mcfpp.mod.breakpoint.DatapackBreakpoint;
import top.mcfpp.mod.breakpoint.command.BreakPointCommand;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import static top.mcfpp.mod.breakpoint.command.BreakPointCommand.*;

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

    @Shadow public abstract void enqueueCommand(CommandQueueEntry<T> entry);

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

            LOGGER.info("Method run()V is injected!");

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

            LOGGER.info("Method run()V is injected!");

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

}
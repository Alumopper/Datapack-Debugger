package top.mcfpp.mod.breakpoint.mixin;

import com.google.common.collect.Queues;
import net.minecraft.command.*;
import net.minecraft.server.function.Tracer;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.mcfpp.mod.breakpoint.DatapackBreakpoint;

import java.util.Deque;
import java.util.List;

import static top.mcfpp.mod.breakpoint.command.BreakPointCommand.*;

@Mixin(CommandExecutionContext.class)
abstract public class CommandExecutionContextMixin<T> {

    @Unique
    private final Deque<CommandQueueEntry<T>> storedCommandQueue = Queues.newArrayDeque();

    @Shadow private static int MAX_COMMAND_QUEUE_LENGTH;
    @Shadow private static Logger LOGGER;
    @Shadow private int maxCommandChainLength;
    @Shadow private int forkLimit;
    @Shadow private Profiler profiler;
    @Shadow private Tracer tracer;
    @Shadow private int commandsRemaining;
    @Shadow private boolean queueOverflowed;
    @Shadow private Deque<CommandQueueEntry<T>> commandQueue;
    @Shadow private List<CommandQueueEntry<T>> pendingCommands;
    @Shadow private int currentDepth;
    @Shadow abstract void queuePendingCommands();

    @Shadow public abstract void enqueueCommand(CommandQueueEntry<T> entry);

    @Inject(method = "run()V", at = @At("HEAD"), cancellable = true)
    private void onRun(CallbackInfo ci){

        final var THIS = (CommandExecutionContext<?>) (Object) this;

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

            if(INSTANCE.isDebugging() && commandQueueEntry.frame().depth() != 0 && INSTANCE.getMoveSteps() == 0) {
                //在函数中执行的，把所有命令暂存
                if(INSTANCE.getStoredCommandExecutionContext().peekFirst() != THIS) {
                    INSTANCE.getStoredCommandExecutionContext().addFirst(THIS);
                }
                ci.cancel();
                return;
            }

            if(commandQueueEntry.action() instanceof FixedCommandAction<?>){
                INSTANCE.setMoveSteps(INSTANCE.getMoveSteps() - 1);
            }

            this.currentDepth = commandQueueEntry.frame().depth();
            commandQueueEntry.execute((CommandExecutionContext)(Object)this);
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

}
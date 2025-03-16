package net.gunivers.sniffer.command;

/**
 * Represents the different types of debugging steps supported by the Datapack Debugger.
 * This enum defines the behavior of the debugger when stepping through code.
 */
public enum StepType {
    /**
     * Step into mode - executes commands one by one, following function calls.
     * When a function call is encountered, the debugger will step into that function
     * and continue debugging inside it.
     */
    STEP_IN,
    
    /**
     * Step over mode - executes commands one by one, but treats function calls as single steps.
     * When a function call is encountered, the debugger will execute the entire function
     * and then stop at the next command after the function call.
     */
    STEP_OVER,
    
    /**
     * Step out mode - continues execution until the current function returns.
     * This is useful when you want to quickly exit the current function and
     * return to its caller.
     */
    STEP_OUT;

    /**
     * Checks if the current step type is STEP_IN.
     * 
     * @return true if the current step type is STEP_IN, false otherwise
     */
    public static boolean isStepIn() {
        return BreakPointCommand.stepType == StepType.STEP_IN;
    }

    /**
     * Checks if the current step type is STEP_OVER.
     * 
     * @return true if the current step type is STEP_OVER, false otherwise
     */
    public static boolean isStepOver() {
        return BreakPointCommand.stepType == StepType.STEP_OVER;
    }

    /**
     * Checks if the current step type is STEP_OUT.
     * 
     * @return true if the current step type is STEP_OUT, false otherwise
     */
    public static boolean isStepOut() {
        return BreakPointCommand.stepType == StepType.STEP_OUT;
    }
}

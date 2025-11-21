> [!NOTE]
> This project is now being developed by Bookshelf team and has been renamed to Sniffer. Bookshelf has added more features to Sniffer and developed a VSCode plugin! So head over to their repository to stay updated~
>
> It's worth mentioning that the original author is still actively contributing to Sniffer's development~
>
> Sniffer: <https://github.com/mcbookshelf/sniffer>


# Datapack Breakpoint

English | [简体中文](README_zh.md)

## Introduce

This is a fabric mod for Minecraft 1.21, which allows you to set breakpoints in the game and "freeze" the game when 
the breakpoint is reached.

## Usage

* Set a breakpoint

In datapack, you can insert `#breakpoint` into .mcfunction file to set a breakpoint. For example:

```mcfunction
#test:test

say 1
say 2
#breakpoint
say 3
say 4
```

In this case, after the game executes `say 2`, the game will be "frozen" because it meets the breakpoint. 

When the game is "frozen", you can still move around, do whatever you want, just like execute the command `tick freeze`.
So you can check the game state, or do some debugging.

* Step

When the game is "frozen", you can use the command `/breakpoint step` to execute the next command. In above example, 
after the game meets the breakpoint, you can use `/breakpoint step` to execute `say 3`, and then use `/breakpoint step`
to execute `say 4`. When all commands are executed, the game will be unfrozen and continue running.

* Continue

When the game is "frozen", you can use the command `/breakpoint move` to unfreeze the game and continue running.

* Get Macro Arguments

By using `/breakpoint get <key>`, you can get the value of the macro argument if the game is executing a macro function.
For example:

```mcfunction
#test:test_macro

say start
#breakpoint
$say $(msg)
say end
```

After executing `function test:test_macro {"msg":"test"}`, we passed the value `test` to the macro argument `msg` and 
then the game will pause before `$say $(msg)`. At this time, you can use `/breakpoint get msg` to get the value `test`.

* Get Function Stack

By using `/breakpoint stack`, you can get the function stack of the current game. For example, if we have following two
functions:

```mcfunction
#test:test1

say 1
function test:test2
say 2

#test: test2
say A
#breakpoint
say B
```

When the game pauses at the breakpoint, you can use `/breakpoint stack` and the function stack will be printed in the
chat screen:

```
test:test2
test:test

```

* Run command in current context

By using `/breakpoint run <command>`, you can run any command in the current context, just like `execute ... run ...`.

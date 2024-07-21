# 断点调试

[English](README.md) | 简体中文

## 介绍

这是一个适用于Minecraft 1.21的fabric模组，为数据包的开发引入了重要的断点调试功能

## 使用

* 设置断点

在数据包中，你可以在.mcfunction文件中插入`#breakpoint`注释来设置断点。例如：

```mcfunction
#test:test

say 1
say 2
#breakpoint
say 3
say 4
```

在这个例子中，当游戏执行完`say 2`后，游戏将会遇到断点，从而被“冻结"，或者说进入“断点调试模式”。这个时候，你仍然可以四处移动，或者执行命令之类的，任何
你想做的事情。这个“冻结”的效果和执行了`tick freeze`命令后的效果类似。

你可以在游戏被冻结的时候，执行一些命令来观察你数据包的过程量，或者进行一些调试。不过需要注意的是，只有聊天栏里面执行的命令才会被正常运行，即使是通过
聊天栏执行了函数，那么被执行的函数中的语句也会被冻结。

* 步进

当游戏被冻结的时候，你可以使用命令`/breakpoint step`来执行下一条命令。在上面的例子中，当游戏遇到断点后，你可以使用`/breakpoint step`来执行`say 3`，
然后再使用`/breakpoint step`来执行`say 4`。当所有的命令都被执行完后，游戏将会被解冻，继续运行。

当游戏被冻结的时候，你可以使用命令`/breakpoint move`来解冻游戏，继续运行。

* 宏参数的获取

使用`/breakpoint get <key>`可以获取到宏函数中的宏参数的值。例如：

```mcfunction
#test:test_macro

say start
#breakpoint
$say $(msg)
say end
```

在执行`function test:test_macro {"msg":"test"}`后，我们将值`test`传递给了宏参数`msg`。同时，在执行函数的过程中，游戏会在`$say $(msg)`
之前触发断点暂停。这个时候，就可以使用`/breakpoint get msg`来获取到值`test`。


* 获取函数栈
* Get Function Stack

使用`/breakpoint stack`可以获取到当前游戏的函数栈。例如，如果我们有以下两个函数：

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

当游戏暂停在断点位置的时候，在聊天栏执行`/breakpoint stack`即可将函数调用栈打印在聊天栏中。

```
test:test2
test:test

```

* 在当前上下文中运行命令

通过`/breakpoint run <command>`，你可以在当前上下文中运行任何命令，就像`execute ... run ...`一样。
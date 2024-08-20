package top.mcfpp.mod.debugger.gui;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import top.mcfpp.mod.debugger.command.FunctionTextLoader;

import java.util.ArrayList;
import java.util.List;

public class EditScreen extends Screen {
    private static Identifier function = Identifier.of("datapack-debugger","hello");
    public static void setFunction(Identifier identifier){function=identifier;}
    private static boolean open = false;
    public static boolean isOpen(){ return open; }
    public static void setOpen(boolean open){EditScreen.open = open;}

    public CommandLineTextList commandLineTextList;
    final List<String> lines = new ArrayList<>();

    @Override
    public boolean shouldPause() {
        return true;
    }

    public EditScreen() {
        this(function);
    }

    public EditScreen(Identifier function){
        super(Text.literal("Function "+function.toString()));
        EditScreen.function = function;
        this.lines.clear();
        this.lines.addAll(FunctionTextLoader.get(function));
    }

    @Override
    protected void init() {
        this.commandLineTextList = new CommandLineTextList(client,this,lines,textRenderer,this.width,this.height,0,0,10);
        this.addDrawableChild(commandLineTextList);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandLineTextList.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != 257 && keyCode != 335) {
            return false;
        } else {
            this.commitAndClose();
            return true;
        }
    }

    protected void commitAndClose() {
        this.client.setScreen(null);
    }





    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

}

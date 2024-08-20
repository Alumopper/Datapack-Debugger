package top.mcfpp.mod.debugger.gui;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static top.mcfpp.mod.debugger.gui.CommandHighLineUtils.highlightOneLine;

public class CommandLineTextList extends AlwaysSelectedEntryListWidget<CommandLineTextList.CommandLineTextField> {

    private final TextRenderer textRenderer;
    private final Screen screen;
    private final List<String> lines;
    public CommandLineTextList(MinecraftClient minecraftClient, Screen owner, List<String> lines, TextRenderer textRenderer,int width, int height,int x,  int y, int itemHeight) {
        super(minecraftClient, width, height, y, itemHeight);
        this.textRenderer = textRenderer;
        this.screen = owner;
        this.lines = lines;
        this.setX(x);
        this.setRenderHeader(false,0);
        this.updateRenderList();
        this.setFocused(this.children().getLast());
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        if (this.getFocused() != focused) {
            super.setFocused(focused);
            if (focused == null) {
                this.setSelected(null);
            }
        }
    }

    public void updateRenderList(){
        this.clearEntries();
        for(int i=0;i<this.lines.size();i++){
            this.addEntry(new CommandLineTextField(lines.get(i),i+1));
        }
        this.notifyListUpdated();
    }
    private void notifyListUpdated() {
    }

    @Override
    public void setSelected(@Nullable CommandLineTextList.CommandLineTextField entry) {
        super.setSelected(entry);
        if (entry != null) {
            entry.setSelected(true);
            for(CommandLineTextList.CommandLineTextField textField:this.children()){
                if(textField!=entry){
                    textField.setSelected(false);
                }
            }
        }
    }

    public class CommandLineTextField extends AlwaysSelectedEntryListWidget.Entry<CommandLineTextField>  implements ParentElement {

        protected final TextFieldWidget consoleCommandTextField;
        protected final ChatInputSuggestor commandSuggestor;

        protected final CommandDispatcher<CommandSource> commandDispatcher;
        protected final CommandSource commandSource;
        protected boolean selected = false;
        protected final int rowNumber;
        public void setSelected(boolean selected) {
            this.selected = selected;
            this.consoleCommandTextField.visible = selected;
            this.commandSuggestor.setWindowActive(selected);
            this.consoleCommandTextField.setFocused(selected);
        }

        public CommandLineTextField(String rawText, int rowNumber){
            this.consoleCommandTextField = new TextFieldWidget(CommandLineTextList.this.textRenderer, 0, 50, CommandLineTextList.this.width, CommandLineTextList.this.itemHeight, Text.translatable("advMode.command")) {
                protected MutableText getNarrationMessage() {
                    return super.getNarrationMessage().append(CommandLineTextField.this.commandSuggestor.getNarration());
                }
            };
            this.consoleCommandTextField.setMaxLength(32500);
            this.consoleCommandTextField.setDrawsBackground(false);
            this.consoleCommandTextField.setChangedListener(this::onCommandChanged);
            this.consoleCommandTextField.visible = false;

            this.commandSuggestor = new ChatInputSuggestor(CommandLineTextList.this.client, CommandLineTextList.this.screen, this.consoleCommandTextField, CommandLineTextList.this.textRenderer, true, true, 0, 7, true, Integer.MIN_VALUE);
            this.commandSuggestor.setWindowActive(false);
            this.consoleCommandTextField.setText(rawText);
            //this.commandSuggestor.refresh();

            this.rowNumber = rowNumber;

            this.commandDispatcher = CommandLineTextList.this.client.player.networkHandler.getCommandDispatcher();
            this.commandSource = CommandLineTextList.this.client.player.networkHandler.getCommandSource();
        }

        private void onCommandChanged(String text) {
            this.commandSuggestor.refresh();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.commandSuggestor.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            } else if (ParentElement.super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            } else if (keyCode != 257 && keyCode != 335) {
                return false;
            } else {

                return true;
            }
        }
        @Nullable
        private Element focused;
        @Override
        public void setFocused(@Nullable Element focused) {
            if (this.focused != null) {
                this.focused.setFocused(false);
            }

            if (focused != null) {
                focused.setFocused(true);
            }

            this.focused = focused;
        }

        @Nullable
        @Override
        public Element getFocused() {
            return this.focused;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            return this.commandSuggestor.mouseScrolled(verticalAmount) || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        @Override
        public Text getNarration() {
            return this.commandSuggestor.isOpen() ? this.commandSuggestor.getSuggestionUsageNarrationText() : Text.empty();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.commandSuggestor.mouseClicked(mouseX, mouseY, button) || ParentElement.super.mouseClicked(mouseX,mouseY,button) || super.mouseClicked(mouseX,mouseY,button);
        }

        private boolean dragging;

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        @Override
        public List<? extends Element> children() {
            return List.of(this.consoleCommandTextField);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawText(CommandLineTextList.this.textRenderer,Text.literal(String.valueOf(this.rowNumber)),x,y, Formatting.DARK_GRAY.getColorValue(),false);
            if(!selected){
                context.drawText(CommandLineTextList.this.textRenderer,highlightOneLine(this.commandDispatcher,this.commandSource,this.consoleCommandTextField.getText()),x+20,y,0xFFFFFF,false);
            }
            this.consoleCommandTextField.setX(x+20);
            this.consoleCommandTextField.setY(y);
            if(selected){
                this.consoleCommandTextField.render(context, mouseX, mouseY, tickDelta);
                this.commandSuggestor.render(context, mouseX, mouseY);
            }
        }
    }
}

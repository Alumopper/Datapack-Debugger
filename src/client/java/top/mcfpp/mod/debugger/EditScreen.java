package top.mcfpp.mod.debugger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EditScreen extends Screen {

    protected TextFieldWidget consoleCommandTextField;
    ChatInputSuggestor commandSuggestor;

    CommandDispatcher<CommandSource> commandDispatcher;
    CommandSource commandSource;

    protected EditScreen() {
        super(Text.literal("test"));
    }

    @Override
    protected void init() {
        this.consoleCommandTextField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 50, 300, 20, Text.translatable("advMode.command")) {
            protected MutableText getNarrationMessage() {
                return super.getNarrationMessage().append(EditScreen.this.commandSuggestor.getNarration());
            }
        };
        this.consoleCommandTextField.setMaxLength(32500);
        this.consoleCommandTextField.setChangedListener(this::onCommandChanged);
        this.addSelectableChild(this.consoleCommandTextField);
        this.commandSuggestor = new ChatInputSuggestor(this.client, this, this.consoleCommandTextField, this.textRenderer, true, true, 0, 7, false, Integer.MIN_VALUE);
        this.commandSuggestor.setWindowActive(true);
        this.commandSuggestor.refresh();

        commandDispatcher = this.client.player.networkHandler.getCommandDispatcher();
        commandSource = this.client.player.networkHandler.getCommandSource();
    }

    @Override
    protected Text getUsageNarrationText() {
        return this.commandSuggestor.isOpen() ? this.commandSuggestor.getSuggestionUsageNarrationText() : super.getUsageNarrationText();
    }

    private void onCommandChanged(String text) {
        this.commandSuggestor.refresh();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandSuggestor.keyPressed(keyCode, scanCode, modifiers)) {
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

    public OrderedText highlightOneLine(String original){
        StringReader stringReader = new StringReader(original);
        boolean bl = stringReader.canRead() && stringReader.peek() == '/';
        if (bl) {
            stringReader.skip();
        }
        ParseResults<CommandSource> parse = commandDispatcher.parse(stringReader, commandSource);
        return highlight(parse,original,bl?1:0);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.commandSuggestor.mouseScrolled(verticalAmount) ? true : super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.commandSuggestor.mouseClicked(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(this.textRenderer,highlightOneLine("say hello"),this.width / 2 - 150,10,0xFFFFFF,false);
        context.drawText(this.textRenderer,highlightOneLine("execute run tellraw @s {\"text\":\"hello world\"}"),this.width / 2 - 150,10+this.textRenderer.fontHeight,0xFFFFFF,false);

        this.consoleCommandTextField.render(context, mouseX, mouseY, delta);
        this.commandSuggestor.render(context, mouseX, mouseY);
    }



    private static final Style ERROR_STYLE;
    private static final Style INFO_STYLE;
    private static final List<Style> HIGHLIGHT_STYLES;
    static {
        ERROR_STYLE = Style.EMPTY.withColor(Formatting.RED);
        INFO_STYLE = Style.EMPTY.withColor(Formatting.GRAY);
        Stream<Formatting> formattings = Stream.of(Formatting.AQUA, Formatting.YELLOW, Formatting.GREEN, Formatting.LIGHT_PURPLE, Formatting.GOLD);
        Style emptyStyle = Style.EMPTY;
        Objects.requireNonNull(emptyStyle);
        HIGHLIGHT_STYLES = formattings.map(emptyStyle::withColor).collect(ImmutableList.toImmutableList());
    }
    private static OrderedText highlight(ParseResults<CommandSource> parse, String original, int firstCharacterIndex) {
        List<OrderedText> list = Lists.newArrayList();
        int i = 0;
        int j = -1;
        CommandContextBuilder<CommandSource> commandContextBuilder = parse.getContext().getLastChild();
        Iterator var7 = commandContextBuilder.getArguments().values().iterator();

        while(var7.hasNext()) {
            ParsedArgument<CommandSource, ?> parsedArgument = (ParsedArgument)var7.next();
            ++j;
            if (j >= HIGHLIGHT_STYLES.size()) {
                j = 0;
            }

            int k = Math.max(parsedArgument.getRange().getStart() - firstCharacterIndex, 0);
            if (k >= original.length()) {
                break;
            }

            int l = Math.min(parsedArgument.getRange().getEnd() - firstCharacterIndex, original.length());
            if (l > 0) {
                list.add(OrderedText.styledForwardsVisitedString(original.substring(i, k), INFO_STYLE));
                list.add(OrderedText.styledForwardsVisitedString(original.substring(k, l), (Style)HIGHLIGHT_STYLES.get(j)));
                i = l;
            }
        }

        if (parse.getReader().canRead()) {
            int m = Math.max(parse.getReader().getCursor() - firstCharacterIndex, 0);
            if (m < original.length()) {
                int n = Math.min(m + parse.getReader().getRemainingLength(), original.length());
                list.add(OrderedText.styledForwardsVisitedString(original.substring(i, m), INFO_STYLE));
                list.add(OrderedText.styledForwardsVisitedString(original.substring(m, n), ERROR_STYLE));
                i = n;
            }
        }

        list.add(OrderedText.styledForwardsVisitedString(original.substring(i), INFO_STYLE));
        return OrderedText.concat(list);
    }

}

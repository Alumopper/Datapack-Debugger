package top.mcfpp.mod.debugger.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import net.minecraft.command.CommandSource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CommandHighLineUtils {

    public static OrderedText highlightOneLine(CommandDispatcher<CommandSource> commandDispatcher, CommandSource commandSource, String original){
        StringReader stringReader = new StringReader(original);
        boolean bl = stringReader.canRead() && stringReader.peek() == '/';
        if (bl) {
            stringReader.skip();
        }
        ParseResults<CommandSource> parse = commandDispatcher.parse(stringReader, commandSource);
        return highlight(parse,original,bl?1:0);
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

        for (ParsedArgument<CommandSource, ?> parsedArgument : commandContextBuilder.getArguments().values()) {
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
                list.add(OrderedText.styledForwardsVisitedString(original.substring(k, l), (Style) HIGHLIGHT_STYLES.get(j)));
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

package tf.bug.fishutils.xivapi;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.util.*;
import java.util.function.ToDoubleBiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public final class DiscordColorFormatter {

    private DiscordColorFormatter() {}

    private static String setColor(final int r, final int g, final int b) {
        return "\u001b[38;2;%d;%d;%dm".formatted(r, g, b);
    }

    public static final Pattern COLOR_MATCHER =
            Pattern.compile("color:rgba\\((\\d+),(\\d+),(\\d+),1\\);");

    private static void renderString(StringBuilder sb, Node target, String resetColor) {
        switch(target) {
            case Element e when e.nameIs("span") -> {
                String style = target.attr("style");
                Matcher m = COLOR_MATCHER.matcher(style);
                // TODO don't assume spans will always have color
                if(m.find()) {
                    int r = Integer.parseInt(m.group(1), 10);
                    int g = Integer.parseInt(m.group(2), 10);
                    int b = Integer.parseInt(m.group(3), 10);

                    String setColor = setColor(r, g, b);
                    sb.append(setColor);
                    for(Node child : e.childNodes()) renderString(sb, child, setColor);
                    sb.append(resetColor);

                    if(m.find()) throw new IllegalArgumentException("More than one color found in span!");
                }
            }
            case Element e when e.nameIs("br") -> {
                sb.append("\n");
            }
            case Element e when e.nameIs("body") -> {
                for(Node child : e.childNodes()) renderString(sb, child, resetColor);
            }
            case TextNode textNode -> {
                sb.append(textNode.text());
            }
            default -> throw new IllegalArgumentException("Unknown node: " + target);
        }
    }

    public static String toAnsi(String target) {
        Document doc = Jsoup.parseBodyFragment(target);
        Element root = doc.body();

        StringBuilder result = new StringBuilder();
        renderString(result, root, "\u001b[0m");
        return result.toString();
    }

}

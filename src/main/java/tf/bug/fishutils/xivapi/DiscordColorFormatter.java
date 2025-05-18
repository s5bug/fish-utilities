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

    public static final List<Color> DISCORD_DEFAULT_COLORS;
    public static final List<Color> SIX_BIT_COLORS;

    static {
        DISCORD_DEFAULT_COLORS = List.of(
                new Color(0xDC322F),
                new Color(0x859900),
                new Color(0xB58900),
                new Color(0x268BD2),
                new Color(0xD33682),
                new Color(0x2AA198)
        );
        ArrayList<Color> sixBitAccum = new ArrayList<>(216);
        for(int r = 0; r < 6; r++) {
            int rc = (r * 255 / 5);
            for(int g = 0; g < 6; g++) {
                int gc = (g * 255 / 5);
                for(int b = 0; b < 6; b++) {
                    int bc = (b * 255 / 5);
                    sixBitAccum.add(new Color(rc, gc, bc));
                }
            }
        }
        SIX_BIT_COLORS = List.copyOf(sixBitAccum);
    }

    public static <W, J> int[] hungarian(
            final List<W> workersIndexed,
            final List<J> jobsLengthed,
            final ToDoubleBiFunction<W, J> cost) {
        int numberOfWorkers = workersIndexed.size();
        int numberOfJobs = jobsLengthed.size();

        if(numberOfJobs > numberOfWorkers) throw new IllegalArgumentException("More jobs than workers!");

        // given a worker, has it been assigned a job?
        int[] jobAssignment = new int[numberOfWorkers + 1];
        Arrays.fill(jobAssignment, -1);

        double[] ys = new double[numberOfJobs];
        double[] yt = new double[numberOfWorkers + 1];

        double[] costs = new double[numberOfWorkers * numberOfJobs];

        for(int w = 0; w < numberOfWorkers; w++) {
            for(int j = 0; j < numberOfJobs; j++) {
                costs[(numberOfJobs * w) + j] = cost.applyAsDouble(workersIndexed.get(w), jobsLengthed.get(j));
            }
        }

        double[] minTo = new double[numberOfWorkers + 1];
        int[] prev = new int[numberOfWorkers + 1];
        boolean[] inZ = new boolean[numberOfWorkers + 1];

        for(int currentJob = 0; currentJob < numberOfJobs; currentJob++) {
            int currentWorker = numberOfWorkers;
            jobAssignment[currentWorker] = currentJob;

            Arrays.fill(minTo, Double.POSITIVE_INFINITY);
            Arrays.fill(prev, -1);

            while (jobAssignment[currentWorker] != -1) {
                inZ[currentWorker] = true;
                final int j = jobAssignment[currentWorker];

                double delta = Double.POSITIVE_INFINITY;

                int nextWorker = -1;
                for(int w = 0; w < numberOfWorkers; w++) {
                    if (inZ[w]) continue;

                    double newMin = costs[(numberOfJobs * w) + j] - ys[j] - yt[w];
                    if(newMin < minTo[w]) {
                        minTo[w] = newMin;
                        prev[w] = currentWorker;
                    }
                    if(minTo[w] < delta) {
                        delta = minTo[w];
                        nextWorker = w;
                    }
                }

                for (int w = 0; w <= numberOfWorkers; w++) {
                    if (inZ[w]) {
                        ys[jobAssignment[w]] += delta;
                        yt[w] -= delta;
                    } else {
                        minTo[w] -= delta;
                    }
                }
                currentWorker = nextWorker;
            }

            for (int w = 0; currentWorker < numberOfWorkers; currentWorker = w) {
                w = prev[currentWorker];
                jobAssignment[currentWorker] = jobAssignment[w];
            }
        }

        int[] result = new int[numberOfJobs];

        for(int w = 0; w < numberOfWorkers; w++) {
            if(jobAssignment[w] != -1) {
                result[jobAssignment[w]] = w;
            }
        }

        return result;
    }

    private static double distSqInLab(final Color a, final Color b) {
        float[] ac = a.getColorComponents(CieLabSpace.INSTANCE, null);
        float[] bc = b.getColorComponents(CieLabSpace.INSTANCE, null);

        double dr = ac[0] - bc[0];
        double dg = ac[1] - bc[1];
        double db = ac[2] - bc[2];

        return (dr * dr) + (dg * dg) + (db * db);
    }

    private static String setColor(final int discordIdx, final int extendedIdx) {
        return "\u001b[38;5;%d;48;5;%d;49m".formatted(16 + extendedIdx, 31 + discordIdx);
    }

    public static final Pattern COLOR_MATCHER =
            Pattern.compile("color:rgba\\((\\d+),(\\d+),(\\d+),1\\);");

    private static void detectColors(Node target, HashSet<Color> detections) {
        if(target.nameIs("span")) {
            String style = target.attr("style");
            Matcher m = COLOR_MATCHER.matcher(style);
            if(m.find()) {
                int r = Integer.parseInt(m.group(1), 10);
                int g = Integer.parseInt(m.group(2), 10);
                int b = Integer.parseInt(m.group(3), 10);
                detections.add(new Color(r, g, b));
                if(m.find()) throw new IllegalArgumentException("More than one color found in span!");
            }
        }
        target.childNodes().forEach(childNode -> detectColors(childNode, detections));
    }

    private static void renderString(StringBuilder sb, Node target, String resetColor, Map<Color, Integer> discordIndices, Map<Color, Integer> sixBitIndices) {
        switch(target) {
            case Element e when e.nameIs("span") -> {
                String style = target.attr("style");
                Matcher m = COLOR_MATCHER.matcher(style);
                // TODO don't assume spans will always have color
                if(m.find()) {
                    int r = Integer.parseInt(m.group(1), 10);
                    int g = Integer.parseInt(m.group(2), 10);
                    int b = Integer.parseInt(m.group(3), 10);

                    Color targetColor = new Color(r, g, b);

                    int closestDiscord = discordIndices.get(targetColor);
                    int closestExtended = sixBitIndices.get(targetColor);
                    String setColor = setColor(closestDiscord, closestExtended);
                    sb.append(setColor);
                    for(Node child : e.childNodes()) renderString(sb, child, setColor, discordIndices, sixBitIndices);
                    sb.append(resetColor);

                    if(m.find()) throw new IllegalArgumentException("More than one color found in span!");
                }
            }
            case Element e when e.nameIs("br") -> {
                sb.append("\n");
            }
            case Element e when e.nameIs("body") -> {
                for(Node child : e.childNodes()) renderString(sb, child, resetColor, discordIndices, sixBitIndices);
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

        HashSet<Color> colorsDetectedSet = new HashSet<>();
        detectColors(root, colorsDetectedSet);
        ArrayList<Color> colorsDetectedList = new ArrayList<>(colorsDetectedSet);

        Map<Color, Integer> discordIndices = new HashMap<>();
        int[] colorToDiscordIdx = hungarian(DISCORD_DEFAULT_COLORS, colorsDetectedList, DiscordColorFormatter::distSqInLab);
        for(int i = 0; i < colorsDetectedList.size(); i++) {
            discordIndices.put(colorsDetectedList.get(i), colorToDiscordIdx[i]);
        }

        Map<Color, Integer> sixBitIndices = new HashMap<>();
        int[] colorToSixBitIdx = hungarian(SIX_BIT_COLORS, colorsDetectedList, DiscordColorFormatter::distSqInLab);
        for(int i = 0; i < colorsDetectedList.size(); i++) {
            sixBitIndices.put(colorsDetectedList.get(i), colorToSixBitIdx[i]);
        }

        StringBuilder result = new StringBuilder();
        renderString(result, root, "\u001b[0m", discordIndices, sixBitIndices);
        return result.toString();
    }

}

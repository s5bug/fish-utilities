package tf.bug.fishutils.xivapi;

import discord4j.core.object.emoji.CustomEmoji;
import discord4j.core.object.entity.ApplicationEmoji;
import discord4j.rest.util.Image;
import gg.xp.xivapi.XivApiClient;
import gg.xp.xivapi.assets.ImageFormat;
import gg.xp.xivapi.clienttypes.XivApiAsset;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import reactor.core.publisher.Mono;
import tf.bug.fishutils.FishUtilities;
import tf.bug.fishutils.xivapi.model.ClassJob;

public final class XivApiUtil {
    private XivApiUtil() {}

    public static URI getBorderlessJobIcon(XivApiClient client, ClassJob classJob, ImageFormat format) {
        return client.getAssetUri("ui/icon/062000/0620%02d_hr1.tex".formatted(classJob.getRowId()), format);
    }

    public static Mono<CustomEmoji> getOrAddJobIconEmoji(FishUtilities client, XivApiClient xivApiClient, ClassJob classJob) {
        return client.gateway.getApplicationInfo().flatMap(ai -> {
            Mono<ApplicationEmoji> getFirstMatching = ai.getEmojis()
                    .filter(ae -> ae.getName().toUpperCase(Locale.US).equals(classJob.getAbbreviation()))
                    .next();
            return getFirstMatching.switchIfEmpty(Image.ofUrl(
                    getBorderlessJobIcon(xivApiClient, classJob, ImageFormat.PNG).toString()
            ).flatMap(img -> ai.createEmoji(
                    classJob.getAbbreviation().toLowerCase(Locale.US),
                    img
            )));
        });
    }

    public static String discordLocaleToXiv(String discord) {
        switch(discord) {
            case "en", "en-US", "en-GB" -> {
                return "en";
            }
            case "ja" -> {
                return "ja";
            }
            case "fr" -> {
                return "fr";
            }
            case "de" -> {
                return "de";
            }
            default -> {
                return "en";
            }
        }
    }
}

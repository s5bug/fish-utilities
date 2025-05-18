package tf.bug.fishutils.xivapi;

import gg.xp.xivapi.XivApiClient;
import gg.xp.xivapi.assets.ImageFormat;
import gg.xp.xivapi.clienttypes.XivApiAsset;
import java.net.URI;
import tf.bug.fishutils.xivapi.model.ClassJob;

public final class XivApiUtil {
    private XivApiUtil() {}

    public static URI getBorderlessJobIcon(XivApiClient client, ClassJob classJob, ImageFormat format) {
        return client.getAssetUri("ui/icon/062000/0620%02d_hr1.tex".formatted(classJob.getRowId()), format);
    }
}

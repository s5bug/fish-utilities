package tf.bug.fishutils.xivapi.model;

import gg.xp.xivapi.annotations.XivApiField;
import gg.xp.xivapi.assets.ImageFormat;
import gg.xp.xivapi.clienttypes.XivApiAsset;
import gg.xp.xivapi.clienttypes.XivApiStruct;

public interface Icon extends XivApiStruct {

    @XivApiField("path_hr1")
    XivApiAsset<ImageFormat> getAssetPathHd();

}

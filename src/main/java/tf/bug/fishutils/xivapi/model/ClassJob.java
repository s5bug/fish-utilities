package tf.bug.fishutils.xivapi.model;

import gg.xp.xivapi.annotations.XivApiSheet;
import gg.xp.xivapi.clienttypes.XivApiObject;

@XivApiSheet("ClassJob")
public interface ClassJob extends XivApiObject {
    String getAbbreviation();
}

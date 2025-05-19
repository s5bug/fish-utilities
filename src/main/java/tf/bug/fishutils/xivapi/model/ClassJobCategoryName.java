package tf.bug.fishutils.xivapi.model;

import gg.xp.xivapi.annotations.XivApiMapKeys;
import gg.xp.xivapi.annotations.XivApiSheet;
import gg.xp.xivapi.annotations.XivApiThis;
import gg.xp.xivapi.clienttypes.XivApiLangValue;
import gg.xp.xivapi.clienttypes.XivApiObject;
import java.util.Map;

@XivApiSheet("ClassJobCategory")
public interface ClassJobCategoryName extends XivApiObject {
    XivApiLangValue<String> getName();
}

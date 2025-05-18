package tf.bug.fishutils.xivapi.model;

import gg.xp.xivapi.annotations.XivApiSheet;
import gg.xp.xivapi.clienttypes.XivApiLangValue;
import gg.xp.xivapi.clienttypes.XivApiObject;

@XivApiSheet("Action")
public interface SearchAction extends XivApiObject {
    XivApiLangValue<String> getName();
}

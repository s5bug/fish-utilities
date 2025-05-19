package tf.bug.fishutils.xivapi.model;

import gg.xp.xivapi.annotations.*;
import gg.xp.xivapi.clienttypes.XivApiLangValue;
import gg.xp.xivapi.clienttypes.XivApiObject;

@XivApiSheet("Action")
public interface Action extends XivApiObject {
    XivApiLangValue<String> getName();

    @XivApiTransientField("Description")
    @XivApiAs("html")
    XivApiLangValue<String> getDescriptionHtml();

    Icon getIcon();

    ClassJobCategoryJobs getClassJobCategory();

    int getCastType();
    int getRange();
    int getEffectRange();
}

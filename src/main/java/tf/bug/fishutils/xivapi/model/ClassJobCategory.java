package tf.bug.fishutils.xivapi.model;

import gg.xp.xivapi.annotations.XivApiMapKeys;
import gg.xp.xivapi.annotations.XivApiSheet;
import gg.xp.xivapi.annotations.XivApiThis;
import gg.xp.xivapi.clienttypes.XivApiObject;
import java.util.Map;

@XivApiSheet("ClassJobCategory")
public interface ClassJobCategory extends XivApiObject {
    String getName();

    // Should filter out "Name" field
    @XivApiThis
    @XivApiMapKeys("[A-Z]{3}")
    Map<String, Boolean> getJobs();
}

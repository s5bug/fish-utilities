package tf.bug.fishutils;

import java.net.URI;
import java.util.function.Function;
import tf.bug.fishutils.properties.PropertyAccess;

import static tf.bug.fishutils.properties.PropertyAccess.*;

public final record FishUtilitiesProperties(
        String discordToken,
        URI jmdictEgz,
        URI freqCc100Json,
        URI xivapiBase
) {

    public static PropertyAccess<FishUtilitiesProperties> ACCESS = aggregate(
            FishUtilitiesProperties::new,
            namespace("discord", get("token", Function.identity())),
            namespace("jmdict", get("jmdict_e_gz_uri", URI::create)),
            namespace("freq_cc100", get("freq_cc100_json_uri", URI::create)),
            namespace("xivapi", get("base_uri", URI::create))
    );

}

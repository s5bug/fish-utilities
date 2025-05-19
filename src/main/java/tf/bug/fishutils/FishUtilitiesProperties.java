package tf.bug.fishutils;

import discord4j.common.util.Snowflake;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import tf.bug.fishutils.properties.PropertyAccess;
import tf.bug.fishutils.xivapi.FfXivProperties;

import static tf.bug.fishutils.properties.PropertyAccess.*;

public final record FishUtilitiesProperties(
        String discordToken,
        URI jmdictEgz,
        URI freqCc100Json,
        FfXivProperties ffXivProperties
) {

    public static final PropertyAccess<FishUtilitiesProperties> ACCESS = aggregate(
            FishUtilitiesProperties::new,
            namespace("discord", get("token", Function.identity())),
            namespace("jmdict", get("jmdict_e_gz_uri", URI::create)),
            namespace("freq_cc100", get("freq_cc100_json_uri", URI::create)),
            namespace("ffxiv", FfXivProperties.ACCESS)
    );

}

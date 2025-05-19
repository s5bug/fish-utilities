package tf.bug.fishutils.xivapi;

import discord4j.common.util.Snowflake;
import discord4j.core.object.emoji.CustomEmoji;
import discord4j.core.object.emoji.Emoji;
import java.net.URI;
import java.util.Map;
import java.util.function.BiFunction;
import tf.bug.fishutils.properties.PropertyAccess;

import static tf.bug.fishutils.properties.PropertyAccess.*;

public final record FfXivProperties(
        URI xivapiBase
) {

    public static final PropertyAccess<FfXivProperties> ACCESS = aggregate(
            FfXivProperties::new,
            get("xivapi_base", URI::create)
    );

}

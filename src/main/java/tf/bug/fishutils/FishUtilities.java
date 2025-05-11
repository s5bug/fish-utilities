package tf.bug.fishutils;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.store.Directory;

public final class FishUtilities {

    public GatewayDiscordClient gateway;
    public Directory directory;
    public final Map<String, Snowflake> commandIds;

    public FishUtilities() {
        this.gateway = null;
        this.directory = null;
        this.commandIds = new HashMap<>();
    }

}

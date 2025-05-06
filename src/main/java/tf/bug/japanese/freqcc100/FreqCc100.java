package tf.bug.japanese.freqcc100;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.*;
import org.jetbrains.annotations.Nullable;

public final record FreqCc100(List<FreqCc100.Entry> entries) {
    public static final record Entry(String keb, @Nullable String reb, int frequency) {
        public static final class Deserializer extends StdDeserializer<Entry> {
            public Deserializer() { this(null); }

            public Deserializer(final Class<Entry> t) { super(t); }

            @Override
            public Entry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                ObjectCodec codec = p.getCodec();
                JsonNode node = codec.readTree(p);

                String keb = node.get(0).asText();
                JsonNode details = node.get(2);

                String reb;
                int frequency;
                if(details.isObject()) {
                    reb = details.get("reading").asText();
                    frequency = details.get("frequency").asInt();
                } else {
                    reb = null;
                    frequency = details.asInt();
                }

                return new Entry(keb, reb, frequency);
            }
        }
    }

    public Map<String, Set<FreqCc100.Entry>> kebLookupMap() {
        HashMap<String, Set<FreqCc100.Entry>> kebLookupMap = new HashMap<>();

        for(FreqCc100.Entry entry : entries) {
            kebLookupMap.compute(entry.keb(), (_, set) -> {
                if(set == null) {
                    set = new HashSet<>();
                }
                set.add(entry);
                return set;
            });
        }

        return kebLookupMap;
    }
}

package cn.charlotte.pit.data.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class LongStringMapDeserializer extends JsonDeserializer<Map<Long, String>> {

    @Override
    public Map<Long, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonNode node = p.getCodec().readTree(p);

            if (node == null || node.isNull()) {
                return new HashMap<>();
            }

            if (!node.isObject()) {
                return new HashMap<>();
            }

            Map<Long, String> result = new HashMap<>();
            Iterator<Entry<String, JsonNode>> fields = node.fields();

            while (fields.hasNext()) {
                Entry<String, JsonNode> entry = fields.next();
                try {
                    Long key = Long.parseLong(entry.getKey());
                    String value = entry.getValue().asText();
                    result.put(key, value);
                } catch (NumberFormatException ignored) {
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}
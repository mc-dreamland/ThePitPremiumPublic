package cn.charlotte.pit.data.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class StringBooleanMapDeserializer extends JsonDeserializer<Map<String, Boolean>> {

    @Override
    public Map<String, Boolean> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || node.isNull()) return new HashMap<>();
            if (!node.isObject()) return new HashMap<>();

            Map<String, Boolean> result = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode valueNode = entry.getValue();
                result.put(key, valueNode.isNull() ? null : valueNode.asBoolean());
            }
            return result;
        } catch (Exception e) {
            log.error("", e);
            return new HashMap<>();
        }
    }
}
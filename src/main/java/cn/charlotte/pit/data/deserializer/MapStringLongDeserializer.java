package cn.charlotte.pit.data.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MapStringLongDeserializer extends JsonDeserializer<Map<String, Long>> {

    @Override
    public Map<String, Long> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }

            if (p.currentToken() != JsonToken.START_OBJECT) {
                ctxt.reportWrongTokenException(this, JsonToken.START_OBJECT,
                        String.format("Expected START_OBJECT, got %s", p.currentToken()));
            }

            Map<String, Long> result = new HashMap<>();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                p.nextToken();
                result.put(p.getCurrentName(), p.getValueAsLong());
            }
            return result;
        } catch (Exception e) {
            log.error("", e);
            return new HashMap<>();
        }
    }
}
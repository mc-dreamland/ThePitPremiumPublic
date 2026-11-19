package cn.charlotte.pit.data.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class UUIDListDeserializer extends JsonDeserializer<List<UUID>> {

    @Override
    public List<UUID> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || !node.isArray()) return new ArrayList<>();

            List<UUID> list = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                if (item.isNull())
                    list.add(null);
                else
                    list.add(UUID.fromString(item.asText()));
            }
            return list;
        } catch (Exception e) {
            log.error("", e);
            return new ArrayList<>();
        }
    }
}
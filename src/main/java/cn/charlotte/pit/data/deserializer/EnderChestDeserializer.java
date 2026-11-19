package cn.charlotte.pit.data.deserializer;

import cn.charlotte.pit.data.sub.PlayerEnderChest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/8 18:35
 */
public class EnderChestDeserializer extends JsonDeserializer<PlayerEnderChest> {
    @Override
    public PlayerEnderChest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null) {
                return null;
            }
            String content = node.get("content").asText();
            int rows = node.get("rows").asInt(3);
            return PlayerEnderChest.deserialization(content, rows);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}

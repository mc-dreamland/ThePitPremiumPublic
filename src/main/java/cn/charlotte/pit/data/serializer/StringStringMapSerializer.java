package cn.charlotte.pit.data.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class StringStringMapSerializer extends JsonSerializer<Map<String, String>> {

    @Override
    public void serialize(Map<String, String> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            if (value == null || value.isEmpty()) {
                gen.writeStartObject();
                gen.writeEndObject();
                return;
            }
            gen.writeStartObject();
            for (Map.Entry<String, String> entry : value.entrySet()) {
                if (entry.getValue() == null)
                    gen.writeNullField(entry.getKey());
                else
                    gen.writeStringField(entry.getKey(), entry.getValue());
            }
            gen.writeEndObject();
        } catch (Exception e) {
            log.error("", e);
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }
}
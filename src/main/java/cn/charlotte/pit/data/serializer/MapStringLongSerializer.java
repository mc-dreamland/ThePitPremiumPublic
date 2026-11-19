package cn.charlotte.pit.data.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class MapStringLongSerializer extends JsonSerializer<Map<String, Long>> {

    @Override
    public void serialize(Map<String, Long> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            gen.writeStartObject();
            if (value != null) {
                value.forEach((key, val) -> {
                    try {
                        gen.writeNumberField(key, val);
                    } catch (IOException e) {
                        throw new RuntimeException(String.format("Serialize field error: %s", key), e);
                    }
                });
            }
            gen.writeEndObject();
        } catch (RuntimeException e) {
            log.error("", e.getCause());
            gen.writeStartObject();
            gen.writeEndObject();
        } catch (Exception e) {
            log.error("", e);
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }
}
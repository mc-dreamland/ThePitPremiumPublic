package cn.charlotte.pit.data.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class StringBooleanMapSerializer extends JsonSerializer<Map<String, Boolean>> {

    @Override
    public void serialize(Map<String, Boolean> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            if (value == null || value.isEmpty()) {
                gen.writeStartObject();
                gen.writeEndObject();
                return;
            }
            gen.writeStartObject();
            for (Map.Entry<String, Boolean> entry : value.entrySet()) {
                String key = entry.getKey();
                Boolean val = entry.getValue();
                if (val == null)
                    gen.writeNullField(key);
                else
                    gen.writeBooleanField(key, val);

            }
            gen.writeEndObject();
        } catch (Exception e) {
            log.error("", e);
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }
}
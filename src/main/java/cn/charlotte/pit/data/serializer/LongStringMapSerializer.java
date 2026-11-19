package cn.charlotte.pit.data.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class LongStringMapSerializer extends JsonSerializer<Map<Long, String>> {

    @Override
    public void serialize(Map<Long, String> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            if (value == null || value.isEmpty()) {
                gen.writeStartObject();
                gen.writeEndObject();
                return;
            }

            gen.writeStartObject();
            for (Map.Entry<Long, String> entry : value.entrySet()) {
                gen.writeStringField(entry.getKey().toString(), entry.getValue());
            }
            gen.writeEndObject();
        } catch (Exception e) {
            e.printStackTrace();
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }
}
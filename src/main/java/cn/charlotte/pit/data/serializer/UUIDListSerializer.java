package cn.charlotte.pit.data.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
public class UUIDListSerializer extends JsonSerializer<List<UUID>> {

    @Override
    public void serialize(List<UUID> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            if (value == null || value.isEmpty()) {
                gen.writeStartArray();
                gen.writeEndArray();
                return;
            }
            gen.writeStartArray();
            for (UUID uuid : value) {
                if (uuid == null)
                    gen.writeNull();
                else
                    gen.writeString(uuid.toString());
            }
            gen.writeEndArray();
        } catch (Exception e) {
            log.error("", e);
            gen.writeStartArray();
            gen.writeEndArray();
        }
    }
}
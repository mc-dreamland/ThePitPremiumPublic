package cn.charlotte.pit.util.configuration.serializer;

import cn.charlotte.pit.util.configuration.AbstractSerializer;

public class StringValueSerializer extends AbstractSerializer<String> {

    @Override
    public Object serializer(String value) {
        return value;
    }

    @Override
    public String deserializer(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

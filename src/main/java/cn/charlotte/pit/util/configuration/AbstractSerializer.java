package cn.charlotte.pit.util.configuration;

import com.google.gson.Gson;

public abstract class AbstractSerializer<T> {
    protected static Gson GSON = new Gson();

    public AbstractSerializer() {
    }

    public abstract Object serializer(T var1);

    public abstract T deserializer(Object var1);
}

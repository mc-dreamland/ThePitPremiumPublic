package cn.charlotte.pit.util.configuration;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.util.configuration.annotations.ConfigData;
import cn.charlotte.pit.util.configuration.annotations.ConfigSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
    private final YamlConfiguration config;
    private final JavaPlugin plugin;
    private final File file;
    private final File directory;

    public Configuration(JavaPlugin plugin) {
        this(plugin, "config.yml");
    }

    public Configuration(JavaPlugin plugin, String filename) {
        this(plugin, filename, plugin.getDataFolder().getPath());
    }

    public Configuration(JavaPlugin plugin, String filename, String directory) {
        this.plugin = plugin;
        this.directory = new File(directory);
        this.file = new File(directory, filename);
        this.config = new YamlConfiguration();
        this.createFile();
    }

    public void createFile() {
        if (!this.directory.exists()) {
            this.directory.mkdirs();
        }

        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

        try {
            this.config.load(this.file);
        } catch (InvalidConfigurationException | IOException var2) {
            var2.printStackTrace();
        }

    }

    public void save() {
        Field[] toSave = this.getClass().getDeclaredFields();

        for (Field f : toSave) {
            if (f.isAnnotationPresent(ConfigData.class)) {
                ConfigData configData = f.getAnnotation(ConfigData.class);

                try {
                    f.setAccessible(true);
                    Object saveValue = f.get(this);
                    Object configValue = null;
                    if (f.isAnnotationPresent(ConfigSerializer.class)) {
                        ConfigSerializer serializer = f.getAnnotation(ConfigSerializer.class);
                        if (saveValue instanceof List) {
                            configValue = new ArrayList<>();

                            for (Object o : (List) saveValue) {
                                AbstractSerializer as = (AbstractSerializer) serializer.serializer().newInstance();
                                ((List) configValue).add(as.serializer(o));
                            }
                        } else {
                            AbstractSerializer as = (AbstractSerializer) serializer.serializer().newInstance();
                            configValue = as.serializer(saveValue);
                        }
                    } else if (saveValue instanceof List) {
                        configValue = new ArrayList<>();

                        for (Object o : (List) saveValue) {
                            ((List) configValue).add(o);
                        }
                    }

                    if (configValue == null) {
                        configValue = saveValue;
                    }

                    this.config.addDefault(configData.path(), configValue);
                    this.config.set(configData.path(), configValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            this.config.save(this.file);
        } catch (IOException var13) {
            var13.printStackTrace();
        }

    }

    public void load() {
        Field[] toLoad = this.getClass().getDeclaredFields();

        for (Field f : toLoad) {
            try {
                if (f.isAnnotationPresent(ConfigData.class)) {
                    ConfigData configData = f.getAnnotation(ConfigData.class);
                    f.setAccessible(true);
                    Object defaultValue = f.get(this);
                    if (this.config.contains(configData.path())) {
                        if (!f.isAnnotationPresent(ConfigSerializer.class)) {
                            try {
                                if (this.config.isList(configData.path())) {
                                    f.set(this, this.config.getList(configData.path(), new ArrayList<>()));
                                } else {
                                    f.set(this, this.config.get(configData.path()));
                                }
                            } catch (IllegalAccessException var11) {
                                var11.printStackTrace();
                            }
                        } else if (this.config.isList(configData.path())) {
                            try {
                                List<Object> list = (List<Object>) this.config.getList(configData.path());
                                List<Object> deserializedList = new ArrayList<>();

                                if (list != null) {
                                    for (Object s : list) {
                                        deserializedList.add(this.deserializeValue(f, s));
                                    }
                                }

                                f.set(this, deserializedList);
                            } catch (InstantiationException | IllegalAccessException var13) {
                                var13.printStackTrace();
                            }
                        } else {
                            if (List.class.isAssignableFrom(f.getType()) && this.config.get(configData.path()) == null) {
                                f.set(this, new ArrayList<>());
                            } else {
                                try {
                                    Object object = this.config.get(configData.path());
                                    f.set(this, this.deserializeValue(f, object));
                                } catch (InstantiationException | IllegalAccessException var12) {
                                    var12.printStackTrace();
                                }
                            }
                        }
                    } else {
                        if (defaultValue != null) {
                            this.config.set(configData.path(), defaultValue);
                        } else {
                            if (f.isAnnotationPresent(ConfigSerializer.class)) {
                                try {
                                    f.set(this, this.deserializeValue(f, null));
                                } catch (InstantiationException | IllegalAccessException |
                                         IllegalArgumentException var12) {
                                    var12.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
            ThePit.getInstance().getLogger().severe("保存配置文件失败: " + e.getMessage());
        }
    }

    public File getFile() {
        return this.file;
    }

    public Object deserializeValue(Field f, Object value) throws IllegalAccessException, InstantiationException {
        if (value == null) {
            return null;
        }
        AbstractSerializer serializer = (AbstractSerializer) f.getAnnotation(ConfigSerializer.class).serializer().newInstance();
        return serializer.deserializer(value);
    }
}
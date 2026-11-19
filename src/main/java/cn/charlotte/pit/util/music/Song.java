package cn.charlotte.pit.util.music;

import cn.charlotte.pit.ThePit;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;

@Getter
public class Song {

    private final short songHeight;
    private final short length;
    private final String title;
    private final File path;
    private final String author;
    private final String description;
    private final float speed;
    private final float delay;
    private HashMap<Integer, Layer> layerHashMap = new HashMap<Integer, Layer>();

    public Song(Song other) {
        this.speed = other.getSpeed();
        delay = 20 / speed;
        this.layerHashMap = other.getLayerHashMap();
        this.songHeight = other.getSongHeight();
        this.length = other.getLength();
        this.title = other.getTitle();
        this.author = other.getAuthor();
        this.description = other.getDescription();
        this.path = other.getPath();
    }

    public Song(float speed, HashMap<Integer, Layer> layerHashMap,
                short songHeight, final short length, String title, String author,
                String description, File path) {
        this.speed = speed;
        delay = 20 / speed;
        this.layerHashMap = layerHashMap;
        this.songHeight = songHeight;
        this.length = length;
        this.title = title;
        this.author = author;
        this.description = description;
        this.path = path;
    }

    public static Song loadSong(String resourceName) {
        return NBSDecoder.parse(ThePit.getInstance().getClass().getClassLoader().getResourceAsStream(resourceName));
    }

}

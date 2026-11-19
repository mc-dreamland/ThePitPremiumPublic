package cn.charlotte.pit.util.music;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Setter
@Getter
public class Layer {

    private HashMap<Integer, Note> hashMap = new HashMap<>();
    private byte volume = 100;
    private String name = "";

    public Note getNote(int tick) {
        return hashMap.get(tick);
    }

    public void setNote(int tick, Note note) {
        hashMap.put(tick, note);
    }

}

package cn.charlotte.pit.util.nametag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BufferedNametag {

    private String groupName, prefix, suffix;
    private boolean friendlyInvis;
    private String player;

}

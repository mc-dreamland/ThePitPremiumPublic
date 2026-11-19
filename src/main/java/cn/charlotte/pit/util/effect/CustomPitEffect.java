package cn.charlotte.pit.util.effect;

import cn.charlotte.pit.util.cooldown.Cooldown;
import lombok.Data;

/**
 * @Author: Misoryan
 * @Created_In: 2021/4/9 14:40
 */
@Data
public class CustomPitEffect {

    private CustomPitEffectType type;
    private int level;
    private Cooldown duration;

    public CustomPitEffect(CustomPitEffectType type, int level, Cooldown duration) {
        this.type = type;
        this.level = level;
        this.duration = duration;
    }
}

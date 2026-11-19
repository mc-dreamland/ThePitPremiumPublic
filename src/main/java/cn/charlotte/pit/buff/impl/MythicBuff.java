package cn.charlotte.pit.buff.impl;

import cn.charlotte.pit.buff.AbstractPitBuff;

import java.util.Collections;
import java.util.List;

public class MythicBuff extends AbstractPitBuff {
    @Override
    public String getInternalBuffName() {
        return "mythic_buff";
    }

    @Override
    public String getDisplayName() {
        return "&d神话狂潮";
    }

    @Override
    public int getMaxTier() {
        return 1;
    }

    @Override
    public List<String> getDescription() {
        return Collections.singletonList("&7神话掉落基础概率翻倍(&c不可叠加&7)");
    }
}

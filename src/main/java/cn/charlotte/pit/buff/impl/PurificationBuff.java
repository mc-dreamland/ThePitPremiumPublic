package cn.charlotte.pit.buff.impl;

import cn.charlotte.pit.buff.AbstractPitBuff;

import java.util.List;

public class PurificationBuff extends AbstractPitBuff {
    @Override
    public String getInternalBuffName() {
        return "purification_buff";
    }

    @Override
    public String getDisplayName() {
        return "净化";
    }

    @Override
    public int getMaxTier() {
        return 1;
    }

    @Override
    public List<String> getDescription() {
        return List.of();
    }
}

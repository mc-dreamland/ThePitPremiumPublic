package cn.charlotte.pit.buff.impl;

import cn.charlotte.pit.buff.AbstractPitBuff;

import java.util.List;

public class VenomDeBuff extends AbstractPitBuff {

    @Override
    public String getInternalBuffName() {
        return "venom_debuff";
    }

    @Override
    public String getDisplayName() {
        return "&c沉默";
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

package cn.charlotte.pit.addon.impl;

import cn.charlotte.pit.addon.Addon;

public final class EnchantBook implements Addon {

    public static final EnchantBook INSTANCE = new EnchantBook();

    private boolean enchantBook;

    private EnchantBook() {
    }

    @Override
    public String name() {
        return "enchant_book";
    }

    @Override
    public void enable() {
        enchantBook = true;
    }

    public boolean getEnchantBook() {
        return enchantBook;
    }
}

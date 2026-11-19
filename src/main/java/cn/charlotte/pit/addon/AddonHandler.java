package cn.charlotte.pit.addon;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.addon.impl.EnchantBook;

import java.util.ArrayList;
import java.util.List;

public final class AddonHandler {

    private static final List<Addon> addons = new ArrayList<>();

    private AddonHandler() {
        // 单例工具类，不允许实例化
    }

    public static void start() {
        addons.addAll(List.of(
//                GachaPool.INSTANCE,
                EnchantBook.INSTANCE
        ));

        for (Addon addon : addons) {
            ThePit.getInstance().getLogger().info("Loading addon " + addon.name() + "...");
            addon.enable();
        }
    }
}

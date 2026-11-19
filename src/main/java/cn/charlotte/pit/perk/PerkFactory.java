package cn.charlotte.pit.perk;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.parm.AutoRegister;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 22:26
 */
@Getter
@Slf4j
public class PerkFactory {
    public final Map<String, TypedPerk<?>> perkMap;
    private final List<TypedPerk<?>> perks;


    public PerkFactory() {
        this.perks = new ArrayList<>();
        this.perkMap = new HashMap<>();
    }

    @SneakyThrows
    public void init() {
        for (PerksBoostsEnum value : PerksBoostsEnum.values()) {
            if (value.getPerk() == null) {
                continue;
            }
            initPerk(value.getPerk(), value.name());
        }
        for (PerksBundlesEnum value : PerksBundlesEnum.values()) {
            if (value.getPerk() == null) {
                continue;
            }
            initPerk(value.getPerk(), value.name());
        }

        for (PerksPassivesEnum value : PerksPassivesEnum.values()) {
            if (value.getPerk() == null) {
                continue;
            }
            initPerk(value.getPerk(), value.name());
        }

        for (PerksPrestigeEnum value : PerksPrestigeEnum.values()) {
            if (value.getPerk() == null) {
                continue;
            }
            initPerk(value.getPerk(), value.name());
        }

        for (PerksShopEnum value : PerksShopEnum.values()) {
            if (value.getPerk() == null) {
                continue;
            }
            initPerk(value.getPerk(), value.name());
        }

        log.info("{} perk loaded!", "" + perkMap.size());
    }

    public void initPerk(TypedPerk<?> perk, String perkName) {
        log.info("Init Perk - {}", perk.getType().name());
        perks.add(perk);
        perkMap.put(perkName, perk);
        if (perk instanceof Listener && perk.getClass().isAnnotationPresent(AutoRegister.class)) {
            log.info("Register Perk To Listener - {}", perk.getType().name());
            Bukkit.getPluginManager().registerEvents((Listener) perk, ThePit.getInstance());
        }
    }
}

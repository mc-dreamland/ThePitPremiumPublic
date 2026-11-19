package cn.charlotte.pit.perk;

import cn.charlotte.pit.perk.type.shop.*;
import lombok.Getter;

public enum PerksShopEnum implements PitPerkType {
    IRON_PACK(new IronPackShopPerk()),
    OBSIDIAN_STACK(new ObsidianStackShopPerk()),
    COMBAT_SPADE(new CombatSpadePerk()),
    DIAMOND_LEGGINGS(new DiamondLeggingsShopPerk()),
    BOUNTY_SOLVENT(new BountySolventShopPerk()),
    FIRST_AID_EGG(new FirstAidEggPerk()),
    JUMP_BOOST_IV(new JumpBoostShopPerk()),
    TACTICAL_INSERTION(new TacticalInsertionsShopPerk()),
    PANTS_BUNDLE(new PantsBundleShopPerk()),
    SWORD_BUNDLE(new SwordBundleShopPerk()),
    BOW_BUNDLE(new BowBundleShopPerk()),

    GOLDEN_PICKAXE(new GoldPickaxePerk()),


    ;

    @Getter
    private final TypedPerk<PerksShopEnum> perk;

    PerksShopEnum(TypedPerk<PerksShopEnum> abstractPerk) {
        this.perk = abstractPerk;
        if (this.perk != null) {
            this.perk.setType(this);
        }
    }

    @Override
    public PerkGroup getCategory() {
        return PerkGroup.SHOP;
    }
}

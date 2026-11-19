package cn.charlotte.pit.perk;

import cn.charlotte.pit.perk.type.passives.*;
import cn.charlotte.pit.perk.type.passives.quests.CoinContractPassivePerk;
import cn.charlotte.pit.perk.type.passives.quests.XPContractPassivePerk;
import lombok.Getter;

public enum PerksPassivesEnum implements PitPerkType {
    XP_BOOST(new XPPassivePerk()),
    GOLD_BOOST(new CoinPassivePerk()),
    MELEE_DAMAGE(new MeleePassivePerk()),
    BOW_DAMAGE(new BowPassivePerk()),
    DAMAGE_REDUCTION(new DmgReducePassivePerk()),
    BUILD_BATTLER(new BuildBattlerPassivePerk()),
    EL_GATO(new ElGatoPassivePerk()),

    XP_CONTRACT(new XPContractPassivePerk()),
    GOLD_CONTRACT(new CoinContractPassivePerk()),
    ;

    @Getter
    private final TypedPerk<PerksPassivesEnum> perk;

    PerksPassivesEnum(TypedPerk<PerksPassivesEnum> abstractPerk) {
        this.perk = abstractPerk;
        if (this.perk != null) {
            this.perk.setType(this);
        }
    }

    @Override
    public PerkGroup getCategory() {
        return PerkGroup.PASSIVE;
    }
}

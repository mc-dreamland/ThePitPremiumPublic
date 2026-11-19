package cn.charlotte.pit.perk;

import cn.charlotte.pit.perk.type.boosts.*;
import cn.charlotte.pit.perk.type.boosts.based.*;
import lombok.Getter;

public enum PerksBoostsEnum implements PitPerkType {
    GOLDEN_HEADS(new GoldenHeadPerk()),
    GOLDEN_MINER(new GoldMinerPerk()),
    FISHING_ROD(new FishingRodPerk()),
    LAVA_BUCKET(new LavaBucketPerk()),
    STRENGTH_CHAINING(new StrengthChainingPerk()),
    SAFETY_FIRST(new SafetyFirstPerk()),
    MINE_MAN(new MineManPerk()),
    BONK(new BonkPerk()),
    TRICKLE_DOWN(new TrickleDownPerk()),
    LUCKY_DIAMOND(new LuckyDiamondPerk()),
    //    NETHERITE_BLESS(new NetheriteBless()),
    SPAMMER(new SpammerPerk()),
    BOUNTY_HUNTER(new BountyHunterPerk()),
    STREAKER(new StreakerPerk()),
    GLADIATOR(new GladiatorPerk()),
    VAMPIRE(new VampirePerk()),


    OVER_HEAL(new OverHealPerk()),
    BARBARIAN(new BarbarianPerk()),
    DIRTY(new DirtyPerk()),
    RAMBO(new RamboPerk()),
    OLYMPUS(new OlympusPerk()),
    ASSISTANT_STREAKER(new AssistantToTheStreakerPerk()),
    FIRST_STRIKE(new FirstStrikePerk()),
    CO_OP_CAT(new CoopCatPerk()),
    MARATHON(new MarathonPerk()),
    SOUP(new SoupPerk()),
    RECON(new ReconPerk()),
    CONGLOMERATE(new ConglomeratePerk()),
    KUNG_FU_KNOWLEDGE(new KungFuKnowledgePerk()),
    THICK(new ThickPerk()),


    //疑似新增?
    ARROW_RECOVERY(new ArrowRecoveryPerk()),


    ;

    @Getter
    private final TypedPerk<PerksBoostsEnum> perk;

    PerksBoostsEnum(TypedPerk<PerksBoostsEnum> abstractPerk) {
        this.perk = abstractPerk;
        if (this.perk != null) {
            this.perk.setType(this);
        }
    }

    @Override
    public PerkGroup getCategory() {
        return PerkGroup.BOOST;
    }
}

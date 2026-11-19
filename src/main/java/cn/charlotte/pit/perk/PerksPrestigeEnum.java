package cn.charlotte.pit.perk;

import cn.charlotte.pit.perk.type.prestige.*;
import lombok.Getter;

public enum PerksPrestigeEnum implements PitPerkType {
    TENACITY(new TenacityPerk()),
    AUTO_BUY(new AutoBuyPerk()),
    SCAM_ARTIST(new ScamArtistPerk()),
    RENOWN_GOLD_BOOST(new CoinPrestigeBoostPerk()),
    RENOWN_XP_BUMP(new XPPrestigeBoostPerk()),
    MYSTICISM(new MysticismPerk()),
    OCCULTDOCTRINE(new OccultDoctrine()),
    CONTRACTOR(new ContractorPerk()),
    SUPERB_LAUNCH_TRAIL(null),
    EXTRA_PERK_SLOT(new ExtraPerkSlotPerk()),
    IMPATIENT(new ImpatientPerk()),
    FISHING_CLUB(new FishClubPerk()),
    EXTRA_HEARTS(new ExtraHeartPerk()),
    EXTRA_KILL_STREAK_SLOT(new ExtraKillStreakSlotPerk()),
    ARMORY(new ArrowArmoryPerk()),
    FANCY_HAT(new FancyHatPerk()),
    YUMMY(new YummyPerk()),
    HERESY(new HeresyPerk()),
    DIVINE_INTERVENTION(new DivineInterventionPerk()),
    BLAZING_LAUNCH_TRAIL(null),
    APOLLO(null),
    THE_WAY(new TheWayPerk()),
    VERY_YUMMY(new VeryYummyPerk()),
    DO_YOUR_OWN_CARPENTRY(null),
    PURE_RAGE(new PureRagePerk()),
    SELF_CONFIDENCE(new SelfConfidencePerk()),
    BANG_LAUNCH_TRAIL(null),
    FAST_PASS(new FastPassPerk()),
    COOL_CONCLAVE(new CoolConclavePerk()),
    //    INVENTORY_MANAGEMENT(new ExtraEnderchestPerk()),
    PROMOTION(new PromotionPerk()),
    RAW_NUMBERS(new RawNumbersPerk()),
    EXPERIENCE_INDUSTRIAL_COMPLEX(null),
    ONE_UPPER(null),
    BOUNTY_CLOAK(null),
    CELEBRITY(new CelebrityPerk()),

    QUEST_MASTER(new QuestMasterPerk()),
    ;

    @Getter
    private final TypedPerk<PerksPrestigeEnum> perk;

    PerksPrestigeEnum(TypedPerk<PerksPrestigeEnum> abstractPerk) {
        this.perk = abstractPerk;
        if (this.perk != null) {
            this.perk.setType(this);
        }
    }

    @Override
    public PerkGroup getCategory() {
        return PerkGroup.PRESTIGE;
    }
}

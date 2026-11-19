package cn.charlotte.pit.perk;

import cn.charlotte.pit.perk.type.bundles.beastmode.*;
import cn.charlotte.pit.perk.type.bundles.hermit.*;
import cn.charlotte.pit.perk.type.bundles.highlander.GoldNanoFactoryKillStreak;
import cn.charlotte.pit.perk.type.bundles.highlander.HighlanderMegaStreak;
import cn.charlotte.pit.perk.type.bundles.highlander.KhanateKillStreak;
import cn.charlotte.pit.perk.type.bundles.highlander.WitherCraftKillStreak;
import cn.charlotte.pit.perk.type.bundles.magnumopus.ApostleForTheGesusKillStreak;
import cn.charlotte.pit.perk.type.bundles.magnumopus.AssuredStrikeKillStreak;
import cn.charlotte.pit.perk.type.bundles.magnumopus.LeechKillStreak;
import cn.charlotte.pit.perk.type.bundles.magnumopus.MagnumOpusMegaStreak;
import cn.charlotte.pit.perk.type.bundles.overdrive.*;
import cn.charlotte.pit.perk.type.bundles.tothemoon.GoldStack;
import cn.charlotte.pit.perk.type.bundles.tothemoon.SuperStreaker;
import cn.charlotte.pit.perk.type.bundles.tothemoon.ToTheMoonMegaStreak;
import cn.charlotte.pit.perk.type.bundles.tothemoon.XPStack;
import cn.charlotte.pit.perk.type.bundles.uber.UberStreak;
import cn.charlotte.pit.util.PlayerUtil;
import lombok.Getter;
import org.bukkit.entity.Player;

public enum PerksBundlesEnum implements PitPerkType {
    //BEAST_MODE
    BEAST_MODE(new BeastModeMegaStreak(), null),
    MONSTER(new MonsterKillStreak(), BEAST_MODE),
    R_AND_R(new RAndRKillStreak(), BEAST_MODE),
    TACTICAL_RETREAT(new TacticalRetreatKillStreak(), BEAST_MODE),
    TOUGH_SKIN(new ToughSkinKillStreak(), BEAST_MODE),
    AAA_BEEF_STREAK(new AAABeefStreak(), BEAST_MODE),

    //HERMIT
    HERMIT(new HermitMegaStreak(), null),
    PUNGENT(new PungentKillStreak(), HERMIT),
    GLASS_PICKAXE(new GlassPickaxeKillStreak(), HERMIT),
    AURA_OF_PROTECTION(new AuraOfProtectionKillStreak(), HERMIT),
    ICE_CUBE(new IceCubeKillStreak(), HERMIT),

    //HIGHLANDER
    HIGH_LANDER(new HighlanderMegaStreak(), null),
    KHANATE(new KhanateKillStreak(), HIGH_LANDER),
    GOLD_NANO_FACTORY(new GoldNanoFactoryKillStreak(), HIGH_LANDER),
    RUSH(null, HIGH_LANDER),
    WITHER_CRAFT(new WitherCraftKillStreak(), HIGH_LANDER),

    //MAGNUMOPUS
    MAGNUM_OPUS(new MagnumOpusMegaStreak(), null),
    LEECH(new LeechKillStreak(), MAGNUM_OPUS),
    ASSURED_STRIKE(new AssuredStrikeKillStreak(), MAGNUM_OPUS),
    APOSTLE_TO_RNGESUS(new ApostleForTheGesusKillStreak(), MAGNUM_OPUS),

    //OVERDRIVE
    OVERDRIVE(new OverDriveMegaStreak(), null),
    ARQUEBUSIER(new ArquebusierKillStreak(), OVERDRIVE),
    EXPLICIOUS(new ExpliciousKillStreak(), OVERDRIVE),
    FIGHT_OR_FLIGHT(new FightOrFlightKillStreak(), OVERDRIVE),
    SECOND_GAPPLE(new SecondGappleKillStreak(), OVERDRIVE),
    SPONGE_STEVE(new SpongeSteveKillStreak(), OVERDRIVE),

    //TO THE MOON
    TO_THE_MOON(new ToTheMoonMegaStreak(), null),
    GOLD_STACK(new GoldStack(), TO_THE_MOON),
    SUPER_STREAKER(new SuperStreaker(), TO_THE_MOON),
    XP_STACK(new XPStack(), TO_THE_MOON),

    //UBER
    UBER(new UberStreak(), null)

    //Redwarm
//    RED_WARM(new RedwarmStreak(),null);
    ;

    @Getter
    private TypedPerk<PerksBundlesEnum> perk = null;
    @Getter
    private PerksBundlesEnum parent = null;

    PerksBundlesEnum(TypedPerk<PerksBundlesEnum> abstractPerk, PerksBundlesEnum parentPerk) {

        if (abstractPerk == null) {
            return;
        }
        this.perk = abstractPerk;
        this.perk.setType(this);
        if (parentPerk != null) {
            this.parent = parentPerk;
        }

    }

    public boolean hasUnlockParent(Player player) {
        if (parent == null) {
            return this.equals(PerksBundlesEnum.OVERDRIVE) || PlayerUtil.isPlayerUnlockedPerk(player, this);
        }
        if (parent.equals(PerksBundlesEnum.OVERDRIVE)) {
            return true;
        }
        return PlayerUtil.isPlayerUnlockedPerk(player, parent);
    }

    @Override
    public PerkGroup getCategory() {
        return PerkGroup.STREAK;
    }

}

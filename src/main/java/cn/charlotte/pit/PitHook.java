package cn.charlotte.pit;

import cn.charlotte.pit.actionbar.ActionBarManager;
import cn.charlotte.pit.activity.activityNPC;
import cn.charlotte.pit.activity.newyear.event.BossEvent;
import cn.charlotte.pit.activity.newyear.event.RandomRedEnvelopesEvent;
import cn.charlotte.pit.activity.newyear.npc.NewYearNPC;
import cn.charlotte.pit.addon.AddonHandler;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.CDKData;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.type.aqua.GrandmasterEnchant;
import cn.charlotte.pit.enchantment.type.aqua.LuckOfPondEnchant;
import cn.charlotte.pit.enchantment.type.aqua.RogueEnchant;
import cn.charlotte.pit.enchantment.type.dark_normal.GrimReaperEnchant;
import cn.charlotte.pit.enchantment.type.dark_normal.HedgeFundEnchant;
import cn.charlotte.pit.enchantment.type.dark_normal.MindAssaultEnchant;
import cn.charlotte.pit.enchantment.type.dark_normal.MiseryEnchant;
import cn.charlotte.pit.enchantment.type.dark_normal.SanguisugeEnchant;
import cn.charlotte.pit.enchantment.type.dark_normal.SomberEnchant;
import cn.charlotte.pit.enchantment.type.dark_normal.SpiteEnchant;
import cn.charlotte.pit.enchantment.type.dark_rare.ComboDazzlingGoldEnchant;
import cn.charlotte.pit.enchantment.type.dark_rare.ComboUnpredictablyEnchant;
import cn.charlotte.pit.enchantment.type.dark_rare.ComboVenomEnchant;
import cn.charlotte.pit.enchantment.type.dark_rare.GoldenHandcuffsEnchant;
import cn.charlotte.pit.enchantment.type.genesis.EvilWithinEnchant;
import cn.charlotte.pit.enchantment.type.genesis.GuardianEnchant;
import cn.charlotte.pit.enchantment.type.genesis.JerryEnchant;
import cn.charlotte.pit.enchantment.type.genesis.JerryEnchant2;
import cn.charlotte.pit.enchantment.type.genesis.JerryEnchant3;
import cn.charlotte.pit.enchantment.type.genesis.JerryEnchant4;
import cn.charlotte.pit.enchantment.type.genesis.JerryEnchant5;
import cn.charlotte.pit.enchantment.type.genesis.JerryEnchant6;
import cn.charlotte.pit.enchantment.type.genesis.JerryEnchant7;
import cn.charlotte.pit.enchantment.type.normal.*;
import cn.charlotte.pit.enchantment.type.op.BlazingAngelEnchant;
import cn.charlotte.pit.enchantment.type.op.BloodthirstypersonEnchant;
import cn.charlotte.pit.enchantment.type.op.DJBundlePVZEnchant;
import cn.charlotte.pit.enchantment.type.op.EchoOfSnowlandPEnchant;
import cn.charlotte.pit.enchantment.type.op.EchoOfSnowlandWEnchant;
import cn.charlotte.pit.enchantment.type.op.EmergencyColonyEnchant;
import cn.charlotte.pit.enchantment.type.op.StarJudgementEnchant;
import cn.charlotte.pit.enchantment.type.rage.AceOfSpadesEnchant;
import cn.charlotte.pit.enchantment.type.rage.BrakesEnchant;
import cn.charlotte.pit.enchantment.type.rage.BreachingChargeEnchant;
import cn.charlotte.pit.enchantment.type.rage.DrillBreakingStrike;
import cn.charlotte.pit.enchantment.type.rage.NewDealEnchant;
import cn.charlotte.pit.enchantment.type.rage.ReallyToxicEnchant;
import cn.charlotte.pit.enchantment.type.ragerare.KillAngelsEnchant;
import cn.charlotte.pit.enchantment.type.ragerare.MicroscopicAntiGravityEnchant;
import cn.charlotte.pit.enchantment.type.ragerare.RegularityEnchant;
import cn.charlotte.pit.enchantment.type.ragerare.SpeedForceEnchant;
import cn.charlotte.pit.enchantment.type.ragerare.ThinkOfThePeopleEnchant;
import cn.charlotte.pit.enchantment.type.rare.AbsorptionEnchant;
import cn.charlotte.pit.enchantment.type.rare.AssassinEnchant;
import cn.charlotte.pit.enchantment.type.rare.BillionaireEnchant;
import cn.charlotte.pit.enchantment.type.rare.ComboStrikeEnchant;
import cn.charlotte.pit.enchantment.type.rare.ComboStunEnchant;
import cn.charlotte.pit.enchantment.type.rare.DevilChickenEnchant;
import cn.charlotte.pit.enchantment.type.rare.DivineMiracleEnchant;
import cn.charlotte.pit.enchantment.type.rare.EnderBowEnchant;
import cn.charlotte.pit.enchantment.type.rare.ExecutionerEnchant;
import cn.charlotte.pit.enchantment.type.rare.FightOrDieEnchant;
import cn.charlotte.pit.enchantment.type.rare.GambleEnchant;
import cn.charlotte.pit.enchantment.type.rare.GomrawsHeartEnchant;
import cn.charlotte.pit.enchantment.type.rare.HealShieldEnchant;
import cn.charlotte.pit.enchantment.type.rare.HealerEnchant;
import cn.charlotte.pit.enchantment.type.rare.HemorrhageEnchant;
import cn.charlotte.pit.enchantment.type.rare.JudgmentShot;
import cn.charlotte.pit.enchantment.type.rare.LastShadowLeapForwardEnchant;
import cn.charlotte.pit.enchantment.type.rare.LonelinessEnchant;
import cn.charlotte.pit.enchantment.type.rare.LuckyShotEnchant;
import cn.charlotte.pit.enchantment.type.rare.MegaLongBowEnchant;
import cn.charlotte.pit.enchantment.type.rare.MysticRealmEnchant;
import cn.charlotte.pit.enchantment.type.rare.NightFallEnchant;
import cn.charlotte.pit.enchantment.type.rare.PaparazziEnchant;
import cn.charlotte.pit.enchantment.type.rare.PullBowEnchant;
import cn.charlotte.pit.enchantment.type.rare.SnowballsEnchant;
import cn.charlotte.pit.enchantment.type.rare.SolitudeEnchant;
import cn.charlotte.pit.enchantment.type.rare.SoulEarterEnchant;
import cn.charlotte.pit.enchantment.type.rare.SpeedyHitEnchant;
import cn.charlotte.pit.enchantment.type.rare.ThePunchEnchant;
import cn.charlotte.pit.enchantment.type.rare.TheSwiftWindEnchant;
import cn.charlotte.pit.enchantment.type.rare.VitalEchoEnchant;
import cn.charlotte.pit.enchantment.type.rare.VolleyEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.FarmerEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.GargoyleEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.HateEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.LaborerEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.LifeContractEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.MaliciousEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.StenchEnchant;
import cn.charlotte.pit.enchantment.type.sewer_normal.WhitebaitEnchant;
import cn.charlotte.pit.enchantment.type.special.SoulRipperEnchant;
import cn.charlotte.pit.events.impl.AuctionEvents;
import cn.charlotte.pit.events.impl.CakeEvents;
import cn.charlotte.pit.events.impl.CarePackageEvents;
import cn.charlotte.pit.events.impl.DamagePlusEvents;
import cn.charlotte.pit.events.impl.DragonEggsEvents;
import cn.charlotte.pit.events.impl.EnchantingBangEvents;
import cn.charlotte.pit.events.impl.EveOneBountyEvents;
import cn.charlotte.pit.events.impl.ExperienceblessedlandEvents;
import cn.charlotte.pit.events.impl.KingOfTheHillEvents;
import cn.charlotte.pit.events.impl.QuickMathEvents;
import cn.charlotte.pit.events.impl.major.BlockHeadEvents;
import cn.charlotte.pit.events.impl.major.HamburgerEvents;
import cn.charlotte.pit.events.impl.major.LuckyWheel;
import cn.charlotte.pit.events.impl.major.PatronageEvents;
import cn.charlotte.pit.events.impl.major.RagePitEvents;
import cn.charlotte.pit.events.impl.major.RedVSBlueEvents;
import cn.charlotte.pit.events.impl.major.SpireEvents;
import cn.charlotte.pit.impl.PlayerPointsAPIImpl;
import cn.charlotte.pit.listener.PacketListener;
import cn.charlotte.pit.nametag.NameTagImpl;
import cn.charlotte.pit.npc.type.CakeBakeNPC;
import cn.charlotte.pit.npc.type.ChickenNPC;
import cn.charlotte.pit.npc.type.ContractNPC;
import cn.charlotte.pit.npc.type.GenesisAngelNpc;
import cn.charlotte.pit.npc.type.GenesisDemonNpc;
import cn.charlotte.pit.npc.type.HelpNPC;
import cn.charlotte.pit.npc.type.KeeperNPC;
import cn.charlotte.pit.npc.type.KingNPC;
import cn.charlotte.pit.npc.type.LeaderNPC;
import cn.charlotte.pit.npc.type.MailNpc;
import cn.charlotte.pit.npc.type.PerkNPC;
import cn.charlotte.pit.npc.type.PrestigeNPC;
import cn.charlotte.pit.npc.type.QuestNpc;
import cn.charlotte.pit.npc.type.ShopNPC;
import cn.charlotte.pit.npc.type.SilverfishNPC;
import cn.charlotte.pit.npc.type.StatusNPC;
import cn.charlotte.pit.npc.type.TradeNPC;
import cn.charlotte.pit.quest.type.BountyHunter;
import cn.charlotte.pit.quest.type.DeepInfiltration;
import cn.charlotte.pit.quest.type.DestoryArmor;
import cn.charlotte.pit.quest.type.DryBlood;
import cn.charlotte.pit.quest.type.Fish;
import cn.charlotte.pit.quest.type.HighValueTarget;
import cn.charlotte.pit.quest.type.KeepSilence;
import cn.charlotte.pit.quest.type.LowEfficiency;
import cn.charlotte.pit.quest.type.LowHealth;
import cn.charlotte.pit.quest.type.SinkingMoonlight;
import cn.charlotte.pit.runnable.ActionBarDisplayRunnable;
import cn.charlotte.pit.runnable.BountyRunnable;
import cn.charlotte.pit.runnable.FreeCeRunnable;
import cn.charlotte.pit.runnable.GameRunnable;
import cn.charlotte.pit.runnable.GoldDropRunnable;
import cn.charlotte.pit.runnable.NightVisionRunnable;
import cn.charlotte.pit.runnable.ProtectRunnable;
import cn.charlotte.pit.runnable.SewersRunnable;
import cn.charlotte.pit.scoreboard.Scoreboard;
import cn.charlotte.pit.sound.impl.DoubleStreakSound;
import cn.charlotte.pit.sound.impl.QuadraStreakSound;
import cn.charlotte.pit.sound.impl.StreakSound;
import cn.charlotte.pit.sound.impl.SuccessfullySound;
import cn.charlotte.pit.sound.impl.TripleStreakSound;
import cn.charlotte.pit.util.nametag.NametagHandler;
import cn.charlotte.pit.util.scoreboard.Assemble;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.spigotmc.AsyncCatcher;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class PitHook {

    private static final PitHook INSTANCE = new PitHook();
    public static boolean isLoaded = false;

    private PitHook() {
    }

    public static PitHook getInstance() {
        return INSTANCE;
    }

    public static void init() {
        try {
            NewConfiguration.loadFile();
            NewConfiguration.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        registerActionBarManager();
        loadEnchants();
        loadPerks();
        loadNameTag();
        loadScoreBoard();
        loadQuests();
        loadEvents();
        registerListeners();
        loadRunnable();
        registerSounds();

        loadCommands();
        loadNpcs();
        loadAPI();

        AddonHandler.start();
        ActionBarDisplayRunnable.start();

        CDKData.loadAllCDKFromData();

        SewersRunnable sewersRunnable = new SewersRunnable();
        Bukkit.getPluginManager().registerEvents(sewersRunnable, ThePit.getInstance());
        sewersRunnable.runTaskTimerAsynchronously(ThePit.getInstance(), 20L, 20L * 60 * 5);

        ThePit.getInstance().info("Loading complete.");
        isLoaded = true;
    }

    private static void loadAPI() {
        ThePit.getInstance().setPointsAPI(PlayerPointsAPIImpl.INSTANCE);
    }

    private static void loadCommands() {
        // CommandHandler.init(ThePit.getInstance());
        // CommandHandler.registerClass(PitAdminCommand.class);
    }

    private static void loadRunnable() {
        new FreeCeRunnable().runTaskTimer(ThePit.getInstance(), 20L, 36000L);
        new GameRunnable().runTaskTimer(ThePit.getInstance(), 1L, 1L);
        new GoldDropRunnable().runTaskTimer(ThePit.getInstance(), 20L, 20L);
        new ProtectRunnable().runTaskTimer(ThePit.getInstance(), 20L, 20L);
        new NightVisionRunnable().runTaskTimer(ThePit.getInstance(), 20L, 20L);

        new ScheduledThreadPoolExecutor(1)
                .scheduleWithFixedDelay(new BountyRunnable(), 100, 100, TimeUnit.MILLISECONDS);
    }

    private static void loadEnchants() {
        var enchantmentFactor = ThePit.getInstance().getEnchantmentFactor();
        Collection<Class<?>> classes = new ArrayList<>(List.of(
                EnergyEnchant.class,
                LonelinessEnchant.class,
                BloodthirstypersonEnchant.class,
                GargoyleEnchant.class,
                LifeContractEnchant.class,
                WhitebaitEnchant.class,
                StenchEnchant.class,
                HateEnchant.class,
                DrippingStoneEnchant.class,
                DrillBreakingStrike.class,
                KillAngelsEnchant.class,
                VitalEchoEnchant.class,
                GrassHopperEnchant.class,
                DevilChickenEnchant.class,
                JudgmentShot.class,
                LastStandEnchant.class,
                BreakTheMirror.class,
                ComboUnpredictablyEnchant.class,
                ComboDazzlingGoldEnchant.class,
                NightFallEnchant.class,
                MysticRealmEnchant.class,
                TheSwiftWindEnchant.class,
                SoulEarterEnchant.class,
                PhantomShieldEnchant.class,
                KingKillersEnchant.class,
                //end ClubRodEnchant.class,
                GrandmasterEnchant.class,
                LuckOfPondEnchant.class,
                RogueEnchant.class,
                RegularityEnchant.class,
                MicroscopicAntiGravityEnchant.class,
                TrotEnchant.class, LaborerEnchant.class,
                ThinkOfThePeopleEnchant.class,
                NewDealEnchant.class,
                ReallyToxicEnchant.class,
                // SingularityEnchant.class,
                GrimReaperEnchant.class,
                HedgeFundEnchant.class,
                MindAssaultEnchant.class,
                MiseryEnchant.class,
                SanguisugeEnchant.class,
                SomberEnchant.class,
                SpiteEnchant.class,
                ComboVenomEnchant.class,
                GoldenHandcuffsEnchant.class,
                EvilWithinEnchant.class,
                GuardianEnchant.class,
                JerryEnchant.class,
                JerryEnchant2.class,
                JerryEnchant3.class,
                JerryEnchant4.class,
                JerryEnchant5.class,
                JerryEnchant6.class,
                JerryEnchant7.class,
                AntiAbsorptionEnchant.class,
                AntiBowSpammerEnchantP.class,
                AntiBowSpammerEnchantW.class,
                AntiMythicismEnchant.class,
                ArrowArmoryEnchant.class,
                BerserkerEnchant.class,
                BillyEnchant.class,
                BooBooEnchant.class,
                BountyHunterEnchant.class,
                BowComboEnchant.class,
                FarmerEnchant.class,
                // BruiserEnchant.class,
                // BulletTimeEnchant.class,
                ComboDamageEnchant.class,
                ComboHealEnchant.class,
                ComboSwiftEnchant.class,
                CounterJanitorEnchant.class,
//                CounterOffensiveEnchant.class,
                CreativeEnchant.class,
                CriticallyFunkyEnchant.class,
                CriticallyRichEnchant.class,
                CrushEnchant.class,
                DavidAndGoliathEnchant.class,
                DiamondAllergyEnchant.class,
                DiamondBreakerEnchant.class,
                ElectrolytesEnchant.class,
                EndlessQuiverEnchant.class,
                FractionalReserveEnchant.class,
                GoldExplorerEnchant.class,
                GutsEnchant.class,
                HermesEnchant.class,
                HuntTheHunterEnchant.class,
                LifeStealEnchant.class,
                // LureEnchant.class,
                MirrorEnchant.class,
                MixedCombatEnchant.class,
                NotGladiatorEnchant.class,
                OverHealEnchant.class,
                PantsRadarEnchant.class,
                ParasiteEnchant.class,
                PebbleEnchant.class,
                PeroxideEnchant.class,
                PitMBAEnchant.class,
                PitPocketEnchant.class,
                PowerEnchant.class,
                ProtectionEnchant.class,
                PurpleGoldEnchant.class,
                ReaperEnchant.class,
                ResentmentEnchant.class,
                RespawnAbsorptionEnchant.class,
                RespawnResistanceEnchant.class,
                RustBowEnchant.class,
                SelfCheckoutEnchant.class,
                SharkEnchant.class,
                SharpnessEnchant.class,
                SierraEnchant.class,
                SniperEnchant.class,
                SpeedyKillEnchant.class,
                SprintDrainEnchant.class,
                StrikeGoldEnchant.class,
                ThornsEnchant.class,
                ThumpEnchant.class,
                TNTEnchant.class,
                TrueDamageArrowEnchant.class,
                // UnBreakEnchant.class,
                WaspEnchant.class,
                WisdomEnchant.class,
                BlazingAngelEnchant.class,
                //BounceBowEnchant.class,
                DJBundlePVZEnchant.class,
                EchoOfSnowlandPEnchant.class,
                EchoOfSnowlandWEnchant.class,
                EmergencyColonyEnchant.class,
//                LaserEnchant.class,
//                MultiExchangeLocationEnchant.class,
//                OPDamageEnchant.class,
                // PowerAngelEnchant.class,
                StarJudgementEnchant.class,
//                SuperLaserEnchant.class,
                // SuperSlimeEnchant.class,
//                TestEnchant.class,
                AbsorptionEnchant.class,
                // ArchangelEnchant.class,
                AssassinEnchant.class,
                BillionaireEnchant.class,
                ComboStrikeEnchant.class,
                ComboStunEnchant.class,
                DivineMiracleEnchant.class,
                EnderBowEnchant.class,
                ExecutionerEnchant.class,
                FightOrDieEnchant.class,
                GambleEnchant.class,
                GomrawsHeartEnchant.class,
                HealerEnchant.class,
                HealShieldEnchant.class,
                HemorrhageEnchant.class,
                LuckyShotEnchant.class,
                MegaLongBowEnchant.class,
                PaparazziEnchant.class,
                PullBowEnchant.class,
                // SlimeEnchant.class,
                SnowballsEnchant.class,
                SolitudeEnchant.class,
                SpeedyHitEnchant.class,
                ThePunchEnchant.class,
                VolleyEnchant.class,
                // AegisEnchant.class,
                MaliciousEnchant.class,
                SoulRipperEnchant.class,
                AceOfSpadesEnchant.class,
                BrakesEnchant.class,
                HappyNewYearEnchant.class,
                // WitheredAndPiercingThroughTheHeart.class,
                LastShadowLeapForwardEnchant.class,
//                RealManEnchant.class,
                BreachingChargeEnchant.class,
                SpeedForceEnchant.class
        ));

        filterDisabledEnchantments(classes);

        enchantmentFactor.init(classes);
        Assembly.enchantmentInt = enchantmentFactor.getEnchantmentMap().size();

        File file = new File(ThePit.getInstance().getDataFolder(), "Enable_Enchantment_List.txt");
        if (file.exists()) file.delete();
        try {
            file.createNewFile();
            java.nio.file.Files.writeString(file.toPath(), String.join("\n", Assembly.enchantNames));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Assembly.all = new ArrayList<>();
        for (Class<?> clazz : classes) {
            try {
                AbstractEnchantment instance = (AbstractEnchantment) clazz.getDeclaredConstructor().newInstance();
                Assembly.all.add(instance);
            } catch (Exception ignored) {
            }
        }

        Assembly.music.addAll(List.of(
                new JerryEnchant(), new JerryEnchant2(), new JerryEnchant3(),
                new JerryEnchant4(), new JerryEnchant5(), new JerryEnchant6(), new JerryEnchant7()
        ));

        Assembly.weapon = filterEnchantmentList(classes, "WeaponOnly");
        Assembly.bow = filterEnchantmentList(classes, "BowOnly");
        Assembly.armor = filterEnchantmentList(classes, "ArmorOnly");

        ThePit.getInstance().getLogger().info("开启附魔列表已保存至: " + file.getAbsolutePath());
    }

    private static void filterDisabledEnchantments(Collection<Class<?>> classes) {
        var pitConfig = ThePit.getInstance().getPitConfig();
        Iterator<Class<?>> iterator = classes.iterator();

        while (iterator.hasNext()) {
            Class<?> enchantClass = iterator.next();
            try {
                var method = enchantClass.getMethod("getEnchantName");
                String enchantName = (String) method.invoke(enchantClass.getDeclaredConstructor().newInstance());

                if (pitConfig.disableEnchants.isEmpty()) {
                    ThePit.getInstance().getLogger().info("未禁用任何附魔");
                    return;
                }
                if (pitConfig.disableEnchants.contains(enchantName)) {
                    ThePit.getInstance().getLogger().info("移除附魔: " + enchantName);
                    iterator.remove();
                } else {
                    Assembly.enchantNames.add(enchantName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<AbstractEnchantment> filterEnchantmentList(Collection<Class<?>> classes, String annotationName) {
        List<AbstractEnchantment> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                for (Annotation annotation : clazz.getDeclaredAnnotations()) {
                    if (annotation.annotationType().getSimpleName().equals(annotationName) && instance instanceof AbstractEnchantment) {
                        result.add((AbstractEnchantment) instance);
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static void loadScoreBoard() {
        Assemble assemble = new Assemble(ThePit.getInstance(), new Scoreboard());
        assemble.setTicks(4);
    }

    private static void loadNameTag() {
        ThePit.getInstance().setNametagHandler(new NametagHandler(ThePit.getInstance(), new NameTagImpl()));
    }

    private static void loadPerks() {
        var perkFactory = ThePit.getInstance().getPerkFactory();
        perkFactory.init();
        Assembly.perkInt = perkFactory.perkMap.size();
        AsyncCatcher.enabled = false;
    }

    private static void registerSounds() {
        List.of(
                DoubleStreakSound.INSTANCE,
                TripleStreakSound.INSTANCE,
                QuadraStreakSound.INSTANCE,
                StreakSound.INSTANCE,
                SuccessfullySound.INSTANCE
        ).forEach(sound -> ThePit.getInstance().getSoundFactory().registerSound(sound));
    }

    private static void loadNpcs() {
        var npc = ThePit.getInstance().getNpcFactory();
        npc.init(List.of(
                GenesisAngelNpc.class, GenesisDemonNpc.class, HelpNPC.class,
                KeeperNPC.class, KingNPC.class, ChickenNPC.class,
                SilverfishNPC.class, CakeBakeNPC.class, MailNpc.class,
                PerkNPC.class, PrestigeNPC.class, QuestNpc.class,
                ShopNPC.class, StatusNPC.class, LeaderNPC.class,
                ContractNPC.class, TradeNPC.class, NewYearNPC.class,
                activityNPC.class
        ));
    }

    private static void loadQuests() {
        var questFactory = ThePit.getInstance().getQuestFactory();
        List<Class<?>> classes = List.of(
                DeepInfiltration.class, DestoryArmor.class, DryBlood.class,
                Fish.class, HighValueTarget.class, KeepSilence.class,
                LowEfficiency.class, LowHealth.class, SinkingMoonlight.class,
                BountyHunter.class
        );
        questFactory.init(classes);
    }

    private static void loadEvents() {
        var eventFactory = ThePit.getInstance().getEventFactory();
        List<Class<?>> classes = List.of(
                PatronageEvents.class, HamburgerEvents.class, RagePitEvents.class,
                /*KingOfTheLadderEvents.class,*/ RedVSBlueEvents.class, BlockHeadEvents.class,
                SpireEvents.class, AuctionEvents.class, CakeEvents.class,
                CarePackageEvents.class, EveOneBountyEvents.class, DragonEggsEvents.class,
                DamagePlusEvents.class, QuickMathEvents.class, KingOfTheHillEvents.class,
                /*                SquadsEvents.class ,*/ LuckyWheel.class, ExperienceblessedlandEvents.class,
                EnchantingBangEvents.class, RandomRedEnvelopesEvent.class, BossEvent.class
        );
        eventFactory.init(classes);
    }

    private static void registerListeners() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener());
    }

    private static void registerActionBarManager() {
        ActionBarManager actionBarManager = new ActionBarManager();
        ThePit.getInstance().setActionBarManager(actionBarManager);
        Bukkit.getScheduler().runTaskTimerAsynchronously(ThePit.getInstance(), actionBarManager::tick, 1, 1);
    }
}

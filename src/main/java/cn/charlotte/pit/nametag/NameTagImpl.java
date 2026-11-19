package cn.charlotte.pit.nametag;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.events.impl.major.HamburgerEvents;
import cn.charlotte.pit.events.impl.major.RedVSBlueEvents;
import cn.charlotte.pit.events.impl.major.SpireEvents;
import cn.charlotte.pit.perk.PerksBundlesEnum;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.nametag.BufferedNametag;
import cn.charlotte.pit.util.nametag.NametagAdapter;
import cn.charlotte.pit.util.nametag.NametagTeamIds;
import cn.charlotte.pit.util.rank.RankUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiFunction;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 15:56
 */
public class NameTagImpl implements NametagAdapter {
    private final NametagTeamIds teamIds = new NametagTeamIds();
    /**
     * Mega-Streak → 显示名 规则映射。
     * key  = PerksBundlesEnum
     * value = (profile, 默认颜色码) -> 返回自定义前缀（不满足条件时返回 null）
     */
    private static final Map<PerksBundlesEnum, BiFunction<PlayerProfile, String, String>> MEGA_STREAK_DISPLAY =
            new EnumMap<>(PerksBundlesEnum.class);

    static {
        MEGA_STREAK_DISPLAY.put(PerksBundlesEnum.OVERDRIVE,
                (p, c) -> p.getStreakKills() >= 50 ? "&e&l超速传动 " + c : null);

        MEGA_STREAK_DISPLAY.put(PerksBundlesEnum.HIGH_LANDER,
                (p, c) -> p.getStreakKills() >= 50 ? "&6&l尊贵血统 " + c : null);

        MEGA_STREAK_DISPLAY.put(PerksBundlesEnum.BEAST_MODE,
                (p, c) -> p.getStreakKills() >= 50 ? "&a&l野兽模式 " + c : null);

        MEGA_STREAK_DISPLAY.put(PerksBundlesEnum.HERMIT,
                (p, c) -> p.getStreakKills() >= 50 ? "&9&l隐士 " + c : null);

        MEGA_STREAK_DISPLAY.put(PerksBundlesEnum.UBER,
                (p, c) -> {
                    if (p.getStreakKills() < 100) return null;
                    int level = (int) Math.min(400, (p.getStreakKills() / 100d) * 100d);
                    return "&d&l登峰造极" + level + " " + c;
                });

        MEGA_STREAK_DISPLAY.put(PerksBundlesEnum.TO_THE_MOON,
                (p, c) -> p.getStreakKills() >= 100 ? "&b&l月球之旅 " + c : null);
    }

    /**
     * 当前 tick 的快照；主线程写，多线程读用 volatile 保证可见性
     */
    private volatile List<BufferedNametag> snapshot = Collections.emptyList();

    @Override
    public void refreshSnapshot() {
        this.snapshot = buildSnapshot();   // 仍在主线程执行，不触碰异步
    }

    @Override
    public List<BufferedNametag> getPlate(Player player) {
        return this.snapshot;
    }


    public List<BufferedNametag> buildSnapshot() {
        List<BufferedNametag> tags = new ArrayList<>();
        List<PlayerProfile> profiles = new ArrayList<>();

        // ↓ 这里仅遍历一次在线玩家，O(n)
        for (Player target : Bukkit.getOnlinePlayers()) {
            profiles.add(PlayerProfile.getPlayerProfileByUuid(target.getUniqueId()));
        }
        teamIds.retain(profiles.stream().map(PlayerProfile::getPlayerUUID).toList());

        if (ThePit.getInstance().getEventFactory() == null) {
            return tags;
        }


        String activeEpicEventName = ThePit.getInstance().getEventFactory().getActiveEpicEventName();
//        profiles.sort((profile1, profile2) -> {
//            int priority1 = profile1.isNicked() ? profile1.getNickPrestige() + 1000 * profile1.getNickLevel() : profile1.getPrestige() + 1000 * profile1.getLevel();
//            int priority2 = profile2.isNicked() ? profile2.getNickPrestige() + 1000 * profile2.getNickLevel() : profile2.getPrestige() + 1000 * profile2.getLevel();
//
//            if ("red_vs_blue".equals(activeEpicEventName)) {
//                RedVSBlueEvent activeEpicEvent = (RedVSBlueEvent) ThePit.getInstance().getEventFactory().getActiveEpicEvent();
//                if (activeEpicEvent.isRedTeam(profile1.getPlayerUuid())) {
//                    priority1 += 10000000;
//                }
//                if (activeEpicEvent.isRedTeam(profile2.getPlayerUuid())) {
//                    priority2 += 10000000;
//                }
//            }
//
//            if ("spire".equals(activeEpicEventName)) {
//                final SpireEvent event = (SpireEvent) ThePit.getInstance().getEventFactory().getActiveEpicEvent();
//                priority1 += event.getRank(Bukkit.getPlayer(profile1.getPlayerUuid())) * 10000000;
//                priority2 += event.getRank(Bukkit.getPlayer(profile2.getPlayerUuid())) * 10000000;
//            }
//            return Integer.compare(priority1, priority2);
//        });
        for (PlayerProfile profile : profiles) {
            // event|level|prestige|
            // 000|000|000
            int teamPrefix = 100_999_999;
            // displayName
            String displayName;
            if ("spire".equals(activeEpicEventName)) {
                final SpireEvents event = (SpireEvents) ThePit.getInstance().getEventFactory().getActiveEpicEvent();
                final Player target = Bukkit.getPlayer(profile.getPlayerUUID());
                final int rank = event.getRank(profile.getPlayerUUID());
                if (rank == -1) {
                    displayName = CC.translate("&7[???&7] " + RankUtil.getPlayerRankColor(profile.getPlayerUUID()));
                    teamPrefix += 899_000_000;
                } else {
                    displayName = CC.translate("&7[&e#" + rank + "&7] " + RankUtil.getPlayerRankColor(profile.getPlayerUUID()));
                    teamPrefix += (rank * 1_000_000);
                }
            } else if ("red_vs_blue".equals(activeEpicEventName)) {
                RedVSBlueEvents activeEpicEvent = (RedVSBlueEvents) ThePit.getInstance().getEventFactory().getActiveEpicEvent();
                if (activeEpicEvent.isRedTeam(profile.getPlayerUUID())) {
                    displayName = CC.translate(profile.getFormattedLevelTag() + " &c");
                } else {
                    displayName = CC.translate(profile.getFormattedLevelTag() + " &9");
                    teamPrefix += 1_000_000;
                }
            } else {
                String rankColor = RankUtil.getPlayerRankColor(profile.getPlayerUUID());
                displayName = CC.translate(profile.getFormattedLevelTag() + " " + rankColor);

                // 使用映射表简化 MegaStreak 判断
                String chose = profile.getChosePerk()[4];
                if (chose != null) {
                    try {
                        PerksBundlesEnum perk = PerksBundlesEnum.valueOf(chose);
                        BiFunction<PlayerProfile, String, String> fn = MEGA_STREAK_DISPLAY.get(perk);
                        if (fn != null) {
                            String custom = fn.apply(profile, rankColor);
                            if (custom != null) {
                                displayName = CC.translate(custom);
                            }
                        }
                    } catch (IllegalArgumentException ignored) {
                        // 理论不会发生：chose 里不是合法枚举名时直接忽略
                    }
                }
            }
            StringBuilder suffix = new StringBuilder();
            if ("spire".equals(activeEpicEventName)) {
                final SpireEvents event = (SpireEvents) ThePit.getInstance().getEventFactory().getActiveEpicEvent();
                final String displayFloor = event.getDisplayFloor(profile.getPlayerUUID());
                suffix.append(" ").append(displayFloor);

            } else if ("ham".equals(activeEpicEventName)) {
                final HamburgerEvents event = (HamburgerEvents) ThePit.getInstance()
                        .getEventFactory()
                        .getActiveEpicEvent();
                final HamburgerEvents.PizzaData data = event.getPizzaDataMap().get(profile.getPlayerUUID());
                if (data != null) {
                    suffix.append(" &c")
                            .append(data.getHamburger())
                            .append("ஐ &6")
                            .append(data.getMoney())
                            .append("$");
                }
            } else {
                if (profile.isSupporter() && profile.getPlayerOption().isSupporterStarDisplay() && !profile.isNicked()) {
                    suffix.append(" &e✬");
                }
                if (ActivityType.GENES.isActive()) {
                    if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.ANGEL) {
                        //♆♨
                        suffix.append(" &b");
                    }
                    if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.DEMON) {
                        //♨
                        suffix.append(" &c");
                    }
                    if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.NONE) {
                        suffix.append(" &6");
                    }
                    if (profile.getBounty() != 0) {
                        suffix.append("&l").append(profile.getBounty()).append("g");
                    } else {
                        if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.ANGEL) {
                            suffix.append("♆");
                        }
                        if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.DEMON) {
                            suffix.append("♨");
                        }
                    }
                } else if (profile.getBounty() != 0) {
                    suffix.append("&6&l ").append(profile.getBounty()).append("g");
                } else {
                    suffix.append(" ");
                }
            }
            if (profile.isNicked()) {
                teamPrefix -= profile.getNickPrestige();
                teamPrefix -= (1000 * profile.getNickLevel());
            } else {
                teamPrefix -= profile.getPrestige();
                teamPrefix -= (1000 * profile.getLevel());
            }

            tags.add(new BufferedNametag(
                    teamIds.name(teamPrefix, profile.getPlayerUUID()),
                    //&7 refers to Prefix
                    displayName,
                    CC.translate(suffix.toString()),
                    false,
                    profile.getPlayerName()));
        }

        return tags;
    }

    @Override
    public boolean showHealthBelowName(Player player) {
        return ThePit.getInstance().getEventFactory() != null && "spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName());
    }
}

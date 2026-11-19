package cn.charlotte.pit.commands.player;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.rank.RankUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CommandAlias("killRecap|killrecap|recap")
public class KillRecap extends BaseCommand {
    private final PitCommands pitCommands;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public KillRecap(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    public static String getGoldBreakdown(ResourceManager manager) {
        StringBuilder builder = new StringBuilder();

        double baseGold = 0.0;
        double extraGold = 0.0;
        double bounty = 0.0;

        boolean hasBaseSources = false;
        for (Map.Entry<String, Double> entry : manager.getGoldAdditions().entrySet()) {
            if (entry.getValue() != 0) {
                hasBaseSources = true;
                break;
            }
        }

        if (hasBaseSources) {
            builder.append("§f基础来源").append("\n");
            for (Map.Entry<String, Double> entry : manager.getGoldAdditions().entrySet()) {
                if (entry.getKey().contains("赏金")) {
                    bounty += entry.getValue();
                } else {
                    baseGold += entry.getValue();
                }
                if (entry.getValue() != 0) {
                    builder.append(CC.translate("  &f- " + entry.getKey() + ": &6" + (entry.getValue() > 0 ? "+" : "-") + PitCommands.numFormat.format(entry.getValue())))
                            .append("\n");
                }
            }
        }

        boolean hasExtraSources = false;
        for (Map.Entry<String, Double> entry : manager.getAdditionalGold().entrySet()) {
            if (entry.getValue() != 0) {
                hasExtraSources = true;
                extraGold += entry.getValue();
            }
        }

        if (hasExtraSources) {
            builder.append("§f额外来源").append("\n");
            for (Map.Entry<String, Double> entry : manager.getAdditionalGold().entrySet()) {
                if (entry.getValue() != 0) {
                    builder.append(CC.translate("  &f- " + entry.getKey() + ": &6" + (entry.getValue() > 0 ? "+" : "-") + PitCommands.numFormat.format(entry.getValue())))
                            .append("\n");
                }
            }
        }

        double bonus = 0.0;
        double penalty = 1.0;
        double bountyBonus = 1.0;
        boolean hasBonus = false;
        boolean hasPenalty = false;
        List<String> bonusLines = new ArrayList<>();
        List<String> penaltyLines = new ArrayList<>();

        for (Map.Entry<String, Double> entry : manager.getGoldModifiers().entrySet()) {
            double val = entry.getValue();
            if (val > 1.0) {
                double added = val - 1.0;
                if (entry.getKey().contains("赏金溶剂") || entry.getKey().contains("金手铐") || entry.getKey().contains("阵营赏金加成")) {
                    bountyBonus += added;
                } else {
                    bonus += added;
                    bonusLines.add(CC.translate("  &a- " + entry.getKey() + ": &a+" + PitCommands.numFormat.format(added * 100) + "%"));
                    hasBonus = true;
                }
            } else if (val < 1.0) {
                penalty *= val;
                penaltyLines.add(CC.translate("  &c- " + entry.getKey() + ": &c-" + PitCommands.numFormat.format((1 - val) * 100) + "%"));
                hasPenalty = true;
            }
        }

        if (hasBonus) {
            builder.append("§a增益倍率").append("\n");
            for (String line : bonusLines) {
                builder.append(line).append("\n");
            }
        }
        if (hasPenalty) {
            builder.append("§c减益倍率").append("\n");
            for (String line : penaltyLines) {
                builder.append(line).append("\n");
            }
        }

        double finalModifier = (1.0 + bonus);
        double journeyStationMultiplier = manager.isHasCelebrityPerk() ? 2 : 1;
        double gold = (baseGold * finalModifier * journeyStationMultiplier + extraGold) * penalty;
        double cappedGold = Math.min(gold, manager.getMaxGold()) + bounty * bountyBonus;

        if (baseGold > 0) {
            builder.append(CC.translate("&f基础金币: &6" + PitCommands.numFormat.format(baseGold))).append("\n");
        }
        if (extraGold > 0) {
            builder.append(CC.translate("&f额外金币: &6" + PitCommands.numFormat.format(extraGold))).append("\n");
        }
        if (finalModifier > 1.0) {
            builder.append(CC.translate("&f倍率: &e×" + PitCommands.numFormat.format(finalModifier))).append("\n");
        }
        if (bounty > 0) {
            builder.append(CC.translate("&f赏金: &6" + PitCommands.numFormat.format(bounty))).append("\n");
        }
        if (bountyBonus > 1.0) {
            builder.append(CC.translate("&f赏金倍率: &e×" + PitCommands.numFormat.format(bountyBonus))).append("\n");
        }
        if (penalty < 1.0) {
            builder.append(CC.translate("&f减益倍率: &c×" + PitCommands.numFormat.format(penalty))).append("\n");
        }
        if (manager.isHasCelebrityPerk()) {
            builder.append(CC.translate("&f旅途的中途站: &a&l✔")).append("\n");
        }

        builder.append(CC.translate("&f金币计算过程:")).append("\n");
        builder.append(CC.translate("  &7(" + PitCommands.numFormat.format(baseGold)));


        if (finalModifier > 1.0) {
            builder.append(CC.translate(" × " + PitCommands.numFormat.format(finalModifier)));
        }


        if (extraGold > 0) {
            builder.append(CC.translate(" + " + PitCommands.numFormat.format(extraGold)));
        }

        builder.append(CC.translate(")"));

        if (manager.isHasCelebrityPerk()) {
            builder.append(CC.translate(" × 2"));
        }

        if (penalty < 1.0) {
            builder.append(CC.translate(" × " + PitCommands.numFormat.format(penalty)));
        }

        if (bounty > 0) {
            builder.append(CC.translate(" + " + PitCommands.numFormat.format(bounty) + " × " + PitCommands.numFormat.format(bountyBonus)));
        }

        builder.append("\n");
        builder.append(CC.translate("&f实际金币: &6" + PitCommands.numFormat.format(cappedGold)));

        if (gold > manager.getMaxGold()) {
            builder.append(CC.translate(" &7(已达上限)"));
        }

        return builder.toString();
    }

    public static String getExpBreakdown(ResourceManager manager) {
        StringBuilder builder = new StringBuilder();

        double baseExp = 0.0;
        double extraExp = 0.0;

        boolean hasBaseSources = false;
        for (Map.Entry<String, Double> entry : manager.getExpAdditions().entrySet()) {
            if (entry.getValue() != 0) {
                hasBaseSources = true;
                baseExp += entry.getValue();
            }
        }

        if (hasBaseSources) {
            builder.append("§f基础来源").append("\n");
            for (Map.Entry<String, Double> entry : manager.getExpAdditions().entrySet()) {
                if (entry.getValue() != 0) {
                    builder.append(CC.translate("&f- " + entry.getKey() + ": &b+" + PitCommands.numFormat.format(entry.getValue())))
                            .append("\n");
                }
            }
        }

        boolean hasExtraSources = false;
        for (Map.Entry<String, Double> entry : manager.getAdditionalExperience().entrySet()) {
            if (entry.getValue() != 0) {
                hasExtraSources = true;
                extraExp += entry.getValue();
            }
        }

        if (hasExtraSources) {
            builder.append("§f额外来源").append("\n");
            for (Map.Entry<String, Double> entry : manager.getAdditionalExperience().entrySet()) {
                if (entry.getValue() != 0) {
                    builder.append(CC.translate("&f- " + entry.getKey() + ": &b+" + PitCommands.numFormat.format(entry.getValue())))
                            .append("\n");
                }
            }
        }

        double bonus = 0.0;
        double penalty = 1.0;
        boolean hasBonus = false;
        boolean hasPenalty = false;
        List<String> bonusLines = new ArrayList<>();
        List<String> penaltyLines = new ArrayList<>();

        for (Map.Entry<String, Double> entry : manager.getExpModifiers().entrySet()) {
            double val = entry.getValue();
            if (val > 1.0) {
                double added = val - 1.0;
                bonus += added;
                bonusLines.add(CC.translate("  &a- " + entry.getKey() + ": &a+" + PitCommands.numFormat.format(added * 100) + "%"));
                hasBonus = true;
            } else if (val < 1.0) {
                penalty *= val;
                penaltyLines.add(CC.translate("  &c- " + entry.getKey() + ": &c-" + PitCommands.numFormat.format(100 - val * 100) + "%"));
                hasPenalty = true;
            }
        }

        if (hasBonus) {
            builder.append("§a增益倍率").append("\n");
            for (String line : bonusLines) {
                builder.append(line).append("\n");
            }
        }
        if (hasPenalty) {
            builder.append("§c减益倍率").append("\n");
            for (String line : penaltyLines) {
                builder.append(line).append("\n");
            }
        }

        double finalModifier = (1.0 + bonus);
        double finalExp = (baseExp * finalModifier + extraExp) * penalty;
        double cappedExp = Math.min(Math.ceil(finalExp), manager.getMaxExp());

        if (baseExp > 0) {
            builder.append(CC.translate("&f基础经验: &b" + PitCommands.numFormat.format(baseExp))).append("\n");
        }
        if (extraExp > 0) {
            builder.append(CC.translate("&f额外经验: &b" + PitCommands.numFormat.format(extraExp))).append("\n");
        }
        if (finalModifier > 1.0) {
            builder.append(CC.translate("&f倍率: &e×" + PitCommands.numFormat.format(finalModifier))).append("\n");
        }
        if (penalty < 1.0) {
            builder.append(CC.translate("&f减益倍率: &c×" + PitCommands.numFormat.format(penalty))).append("\n");
        }

        builder.append(CC.translate("&f经验计算过程:")).append("\n");
        builder.append(CC.translate("  &7(" + PitCommands.numFormat.format(baseExp)));

        if (finalModifier > 1.0) {
            builder.append(CC.translate(" × " + PitCommands.numFormat.format(finalModifier)));
        }

        if (extraExp > 0) {
            builder.append(CC.translate(" + " + PitCommands.numFormat.format(extraExp)));
        }

        builder.append(CC.translate(")"));

        if (penalty < 1.0) {
            builder.append(CC.translate(" × " + PitCommands.numFormat.format(penalty)));
        }

        builder.append("\n");
        builder.append(CC.translate("&f实际经验: &b" + PitCommands.numFormat.format(cappedExp)));

        if (finalExp > manager.getMaxExp()) {
            builder.append(CC.translate(" &7(已达上限)"));
        }

        return builder.toString();
    }

    @Default
    public void onCheckKillRecap(Player player) {

        try {
            cn.charlotte.pit.data.sub.KillRecap killRecap = cn.charlotte.pit.data.sub.KillRecap.recapMap.get(player.getUniqueId());
            if (killRecap == null || killRecap.getKiller() == null || killRecap.getAssistData() == null) {
                player.sendMessage(CC.translate("&c&l错误! &7未找到有效的近期死亡回放数据,抱歉!"));
                return;
            }

            ItemStack recapBook = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta bookMeta = (BookMeta) recapBook.getItemMeta();

            bookMeta.setTitle(player.getName());
            bookMeta.setAuthor("ThePit");

            Component newLine = Component.text("\n");
            PlayerProfile killerProfile = PlayerProfile.getPlayerProfileByUuid(killRecap.getKiller());

            List<Component> texts = new ArrayList<>();
            texts.add(Component.text("§c§l战斗日志"));
            texts.add(newLine);
            texts.add(Component.text("§8" + dateTimeFormatter.format(LocalDateTime.now())));
            texts.add(newLine);
            texts.add(Component.text(RankUtil.getPlayerColoredName(player.getUniqueId())));
            texts.add(newLine);
            texts.add(Component.text("击杀者: "));
            texts.add(newLine);
            Component killerName = LegacyComponentSerializer.legacyAmpersand().deserialize(RankUtil.getPlayerColoredName(killRecap.getKiller()));


            List<TypedPerk<?>> collect = ThePit.getInstance()
                    .getPerkFactory()
                    .getPerks()
                    .stream()
                    .filter(abstractPerk -> killRecap.getPerk().contains(abstractPerk.getInternalPerkName()))
                    .toList();
            StringBuilder killerHover = new StringBuilder();
            killerHover.append(killerProfile.getFormattedName())
                    .append("\n");
            for (TypedPerk<?> perk : collect) {
                killerHover.append(CC.translate("&e"))
                        .append(perk.getDisplayName())
                        .append("\n");
            }


            texts.add(killerName.hoverEvent(HoverEvent.showText(Component.text(CC.translate(killerHover.toString())))));


            String goldBreakdown = getGoldBreakdown(killRecap.getResourceManager());
            String expBreakdown = getExpBreakdown(killRecap.getResourceManager());

            TextComponent coinsInfo = Component.text(CC.translate("&6+" + PitCommands.numFormat.format(killRecap.getResourceManager().calculateGold()) + "硬币"))
                    .hoverEvent(HoverEvent.showText(Component.text(goldBreakdown)));
            TextComponent xpInfo = Component.text(CC.translate(" &b+" + PitCommands.numFormat.format(killRecap.getResourceManager().calculateEXP()) + "经验值"))
                    .hoverEvent(HoverEvent.showText(Component.text(expBreakdown)));
            texts.add(coinsInfo);
            texts.add(xpInfo);
            texts.add(newLine);

            int i = 1;
            texts.add(Component.text(CC.translate("&0助攻 (" + killRecap.getAssistData().size() + "):")));
            texts.add(newLine);

            for (cn.charlotte.pit.data.sub.KillRecap.AssistData assistData : killRecap.getAssistData()) {
                StringBuilder assistCoin = new StringBuilder();
                assistCoin.append(CC.translate("&f基础奖励: &6+" + PitCommands.numFormat.format(killRecap.getBaseCoin() * assistData.getPercentage())))
                        .append("\n");
                if (assistData.getStreakCoin() > 0) {
                    assistCoin.append(CC.translate("&f连杀奖励: &6+" + PitCommands.numFormat.format(assistData.getStreakCoin())))
                            .append("\n");
                }
                if (assistData.getLevelDisparityCoin() > 0) {
                    assistCoin.append(CC.translate("&f等级/装备差距: &6+" + PitCommands.numFormat.format(assistData.getLevelDisparityCoin())))
                            .append("\n");
                }
                StringBuilder assistExp = new StringBuilder();
                assistExp.append(CC.translate("&f基础奖励: &b+" + PitCommands.numFormat.format(killRecap.getBaseExp() * assistData.getPercentage())))
                        .append("\n");
                if (assistData.getStreakExp() > 0) {
                    assistExp.append(CC.translate("&f连杀奖励: &b+" + PitCommands.numFormat.format(assistData.getStreakExp())))
                            .append("\n");
                }
                if (assistData.getLevelDisparityExp() > 0) {
                    assistExp.append(CC.translate("&f等级/装备差距: &b+" + PitCommands.numFormat.format(assistData.getLevelDisparityExp())))
                            .append("\n");
                }
                texts.add(Component.text(CC.translate("&0" + PitCommands.numFormat.format(assistData.getPercentage() * 100) + "% " + assistData.getDisplayName())));
                texts.add(newLine);
                texts.add(Component.text(CC.translate("&6+" + PitCommands.numFormat.format(assistData.getTotalCoin()) + "硬币"))
                        .hoverEvent(HoverEvent.showText(Component.text(assistCoin.toString())))
                        .append(
                                Component.text(CC.translate(" &b+" + PitCommands.numFormat.format(assistData.getTotalExp()) + "经验值"))
                                        .hoverEvent(HoverEvent.showText(Component.text(assistExp.toString())))
                        )
                );
                texts.add(newLine);
                texts.add(newLine);

                i++;
            }

            i = 1;

            texts.add(Component.text(CC.translate("&0&l伤害日志 (" + killRecap.getDamageLogs().size() + "):")));
            texts.add(newLine);

            for (cn.charlotte.pit.data.sub.KillRecap.DamageData damageData : killRecap.getDamageLogs()) {

                TextComponent info = Component.text(
                        CC.translate("&7" + ((killRecap.getCompleteTime() - damageData.getTimer().getStart()) / 1000)
                                + "秒前  &c"
                                + PitCommands.numFormat.format(damageData.getDamage())
                                + " "
                                + (damageData.isLava() ? "§c岩浆" : (damageData.isMelee() ? "§b近战" : "§d远程"))
                        )
                );

                if (damageData.getUsedItem() != null && damageData.getUsedItem().getType() != Material.AIR) {
                    info.hoverEvent(damageData.getUsedItem().asHoverEvent());
                }

                texts.add(info);
                texts.add(newLine);
                String damageHover = CC.translate((damageData.isAttack() ? "&7攻击目标 §b" + damageData.getDisplayName() : "&7来自 §b" + damageData.getDisplayName() + " §7的攻击")) +
                        "\n" +
                        CC.translate("§7剩余血量: &c" + PitCommands.numFormat.format(damageData.getAfterHealth()) +
                                "\n" +
                                CC.translate("&7附魔/天赋 附加伤害: &c" + PitCommands.numFormat.format(damageData.getBoostDamage())));

                texts.add(Component.text(CC.translate((damageData.isLava() ? "☼" : (damageData.isAttack() ? "&0⚔ " : "&0☬ ")) + damageData.getDisplayName()))
                        .hoverEvent(HoverEvent.showText(Component.text(damageHover)))
                );
                texts.add(newLine);
                texts.add(newLine);

                i++;
                if (i >= 5) {
                    i = 0;
                }
            }
            if (i > 0) {
            }

            i = 0;
            TextComponent.Builder text = Component.text();
            for (Component component : texts) {
                text.append(component);
                if (i == 13) {
                    bookMeta.addPages(text.asComponent());
                    text = Component.text();
                    i = 0;
                    continue;
                }
                if (!newLine.equals(component)) {
                    i++;
                }
            }
            bookMeta.addPages(text.asComponent());

            recapBook.setItemMeta(bookMeta);

            player.openBook(recapBook);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

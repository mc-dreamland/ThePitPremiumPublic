package cn.charlotte.pit.commands.admin;


import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.menu.cdk.generate.CDKMenu;
import cn.charlotte.pit.menu.cdk.view.CDKViewMenu;
import cn.charlotte.pit.menu.genesis.GenesisBuyMenu;
import cn.charlotte.pit.menu.mail.MailMenu;
import cn.charlotte.pit.menu.perk.normal.choose.PerkChooseMenu;
import cn.charlotte.pit.menu.perk.prestige.PrestigePerkBuyMenu;
import cn.charlotte.pit.menu.prestige.PrestigeMainMenu;
import cn.charlotte.pit.menu.quest.main.QuestMenu;
import cn.charlotte.pit.perk.PerkGroup;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


@CommandAlias("openmenu")
@CommandPermission("pit.admin")
public class OpenMenu extends BaseCommand {
    private final PitCommands pitCommands;

    public OpenMenu(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    @CommandCompletion("shop|perkBuy|prestigePerkBuy|prestige|ench|quest|mail|cdk|allCdk|GenesisBuyMenu")
    public void checkMaxHealth(Player player, String menu) {
        if (menu.equalsIgnoreCase("shop")) {
            ThePit.api.openMenu(player, "shop");
        }
        if (menu.equalsIgnoreCase("perkBuy")) {
            new PerkChooseMenu().openMenu(player);
        }
        if (menu.equalsIgnoreCase("prestigePerkBuy")) {
            try {
                new PrestigePerkBuyMenu(PerkGroup.PRESTIGE).openMenu(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (menu.equalsIgnoreCase("prestige")) {
            new PrestigeMainMenu().openMenu(player);
        }
        if (menu.equalsIgnoreCase("ench")) {
            try {
                ThePit.getApi().openMythicWellMenu(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (menu.equalsIgnoreCase("quest")) {
            try {
                new QuestMenu().openMenu(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (menu.equalsIgnoreCase("mail")) {
            try {
                new MailMenu().openMenu(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (menu.equalsIgnoreCase("cdk")) {
            try {
                new CDKMenu().openMenu(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (menu.equalsIgnoreCase("allCdk")) {
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                try {
                    new CDKViewMenu().openMenu(player);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        if (menu.equalsIgnoreCase("GenesisBuyMenu")) {
            try {
                new GenesisBuyMenu().openMenu(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
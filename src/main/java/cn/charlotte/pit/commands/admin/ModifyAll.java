package cn.charlotte.pit.commands.admin;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.level.LevelUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("modifyall")
@CommandPermission("pit.admin")
public class ModifyAll extends BaseCommand {
    private final PitCommands pitCommands;

    public ModifyAll(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Subcommand("coin")
    @Syntax("<set|add|take> <amount>")
    @CommandCompletion("set|add|take @range:1-5")
    public void giveCoin(CommandSender sender, String type, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            double coins = profile.getCoins();
            switch (type) {
                case "add" -> coins += amount;
                case "take" -> coins -= amount;
                case "set" -> coins = amount;
            }
            profile.setCoins(Math.max(0, coins));
            sender.sendMessage("okay");
        }
    }

    @Subcommand("grindcoin")
    @Syntax("<set|add|take> <amount>")
    @CommandCompletion("set|add|take @range:1-5")
    public void giveGrindCoin(CommandSender sender, String type, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            double coins = profile.getGrindedCoins();
            switch (type) {
                case "add" -> coins += amount;
                case "take" -> coins -= amount;
                case "set" -> coins = amount;
            }
            profile.setGrindedCoins(Math.max(0, coins));
            sender.sendMessage("okay");
        }
    }

    @Subcommand("prestige")
    @CommandCompletion("set|add|take @range:1-5")
    public void setPrestige(CommandSender player, String type, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            int prestige = profile.getPrestige();
            switch (type) {
                case "add" -> prestige += amount;
                case "take" -> prestige -= amount;
                case "set" -> prestige = amount;
            }
            profile.setPrestige(Math.max(0, prestige));
        }
    }

    @Subcommand("renown")
    @CommandCompletion("set|add|take @range:1-5")
    public void setRenown(CommandSender player, String type, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            int renown = profile.getRenown();
            switch (type) {
                case "add" -> renown += amount;
                case "take" -> renown -= amount;
                case "set" -> renown = amount;
            }
            profile.changeRenown(Math.max(0, renown));
        }
    }

    @Subcommand("streak")
    @CommandCompletion("set|add|take @range:1-5")
    public void setStreak(CommandSender player, String type, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            double streak = profile.getStreakKills();
            switch (type) {
                case "add" -> streak += amount;
                case "take" -> streak -= amount;
                case "set" -> streak = amount;
            }
            profile.setStreakKills(Math.max(0, streak), true);
        }
    }

    @Subcommand("actionbounty")
    @CommandCompletion("set|add|take @range:1-5")
    public void setActionBounty(CommandSender player, String type, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            int actionbounty = profile.getActionBounty();
            switch (type) {
                case "add" -> actionbounty += amount;
                case "take" -> actionbounty -= amount;
                case "set" -> actionbounty = amount;
            }
            profile.setActionBounty(Math.max(0, actionbounty));
        }
    }

    @Subcommand("level")
    @CommandCompletion("@range:1-5")
    public void setLevel(CommandSender player, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            double levelExpRequired = LevelUtil.getLevelTotalExperience(profile.getPrestige(), amount);
            profile.setExperience(levelExpRequired);
            profile.applyExperienceToPlayer(onlinePlayer);
        }
    }

    @Subcommand("bounty")
    @CommandCompletion("set|add|take  @range:1-5")
    public void setBounty(CommandSender player, String type, int amount) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            int bounty = profile.getBounty();
            switch (type) {
                case "add" -> bounty += amount;
                case "take" -> bounty -= amount;
                case "set" -> bounty = amount;
            }
            profile.setBounty(Math.max(0, bounty));
        }
    }

    @Subcommand("maxhealth")
    @CommandCompletion("@players")
    public void setMaxHealth(CommandSender player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getUniqueId());
            onlinePlayer.setMaxHealth(20.0 + profile.getExtraMaxHealthValue());
        }
    }

}

package cn.charlotte.pit.commands.admin;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.level.LevelUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("modify")
@CommandPermission("pit.admin")
public class Modify extends BaseCommand {
    private final PitCommands pitCommands;

    public Modify(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Subcommand("coin")
    @Syntax("<set|add|take> <player> <amount>")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void giveCoin(CommandSender sender, String type, OnlinePlayer player, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.player.getUniqueId());
        double coins = profile.getCoins();
        switch (type) {
            case "add" -> coins += amount;
            case "take" -> coins -= amount;
            case "set" -> coins = amount;
        }
        profile.setCoins(Math.max(0, coins));
        sender.sendMessage("okay");
    }

    @Subcommand("bountyStreak")
    @Syntax("<set|add|take> <player> <amount>")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setbountyStreak(CommandSender sender, String type, OnlinePlayer player, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.player.getUniqueId());
        int bountyStreak = profile.getBountyStreak();
        switch (type) {
            case "add" -> bountyStreak += amount;
            case "take" -> bountyStreak -= amount;
            case "set" -> bountyStreak = amount;
        }
        profile.setBountyStreak(Math.max(0, bountyStreak));
        sender.sendMessage("okay");
    }

    @Subcommand("grindcoin")
    @Syntax("<set|add|take> <player> <amount>")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void giveGrindCoin(CommandSender sender, String type, OnlinePlayer player, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.player.getUniqueId());
        double coins = profile.getGrindedCoins();
        switch (type) {
            case "add" -> coins += amount;
            case "take" -> coins -= amount;
            case "set" -> coins = amount;
        }
        profile.setGrindedCoins(Math.max(0, coins));
        sender.sendMessage("okay");
    }

    @Subcommand("prestige")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setPrestige(CommandSender player, String type, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        int prestige = profile.getPrestige();
        switch (type) {
            case "add" -> prestige += amount;
            case "take" -> prestige -= amount;
            case "set" -> prestige = amount;
        }
        profile.setPrestige(Math.max(0, prestige));
    }

    @Subcommand("renown")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setRenown(CommandSender player, String type, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        int renown = profile.getRenown();
        switch (type) {
            case "add" -> renown += amount;
            case "take" -> renown -= amount;
            case "set" -> renown = amount;
        }
        profile.changeRenown(Math.max(0, renown));
    }

    @Subcommand("streak")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setStreak(CommandSender player, String type, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        double streak = profile.getStreakKills();
        switch (type) {
            case "add" -> streak += amount;
            case "take" -> streak -= amount;
            case "set" -> streak = amount;
        }
        profile.setStreakKills(Math.max(0, streak), true);
    }

    @Subcommand("actionbounty")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setActionBounty(CommandSender player, String type, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        int actionbounty = profile.getActionBounty();
        switch (type) {
            case "add" -> actionbounty += amount;
            case "take" -> actionbounty -= amount;
            case "set" -> actionbounty = amount;
        }
        profile.setActionBounty(Math.max(0, actionbounty));
    }

    @Subcommand("level")
    @CommandCompletion("@players @range:1-5")
    public void setLevel(CommandSender player, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        double levelExpRequired = LevelUtil.getLevelTotalExperience(profile.getPrestige(), amount);
        profile.setExperience(levelExpRequired);
        Player onlinePlayer = Bukkit.getPlayer(profile.getUuid());
        if (onlinePlayer != null) {
            profile.applyExperienceToPlayer(onlinePlayer);
        }
    }

    @Subcommand("bounty")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setBounty(CommandSender player, String type, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        int bounty = profile.getBounty();
        switch (type) {
            case "add" -> bounty += amount;
            case "take" -> bounty -= amount;
            case "set" -> bounty = amount;
        }
        profile.setBounty(Math.max(0, bounty));
    }

    //这是设置阵营活动点数的,不是玩家点券!!!!!!!!!!!
    @Subcommand("Points")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setPoints(CommandSender player, String type, OnlinePlayer target, long amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        Long bounty = profile.getActivityPlayerData().getGenesisData().getPoints();
        switch (type) {
            case "add" -> bounty += amount;
            case "take" -> bounty -= amount;
            case "set" -> bounty = amount;
        }
        profile.getActivityPlayerData().getGenesisData().setPoints(Math.max(0, bounty));
    }

    @Subcommand("maxhealth")
    @CommandCompletion("@players")
    public void setMaxHealth(CommandSender player, OnlinePlayer target) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        target.player.setMaxHealth(20.0 + profile.getExtraMaxHealthValue());
    }

    @Subcommand("NewPoints")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setNewPoints(CommandSender player, String type, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        long bounty = profile.getActivityPlayerData().getNewYearData().getPoints();
        switch (type) {
            case "add" -> bounty += amount;
            case "take" -> bounty -= amount;
            case "set" -> bounty = amount;
        }
        profile.getActivityPlayerData().getNewYearData().setPoints(Math.max(0, bounty));
    }

    @Subcommand("NewCoins")
    @CommandCompletion("set|add|take @players @range:1-5")
    public void setNewCoins(CommandSender player, String type, OnlinePlayer target, int amount) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.player.getUniqueId());
        long bounty = profile.getActivityPlayerData().getNewYearData().getCoins();
        switch (type) {
            case "add" -> bounty += amount;
            case "take" -> bounty -= amount;
            case "set" -> bounty = amount;
        }
        profile.getActivityPlayerData().getNewYearData().setCoins(Math.max(0, bounty));
    }
}

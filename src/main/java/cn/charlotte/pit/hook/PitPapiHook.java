package cn.charlotte.pit.hook;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.util.chat.CC;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PitPapiHook extends PlaceholderExpansion {

    @NotNull
    @Override
    public String getIdentifier() {
        return "pit";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "XiaoYuZao";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Nullable
    @Override
    public String onPlaceholderRequest(Player p, @NotNull String params) {
        // 获取玩家的 PitProfile（如果未加载则置为 null）

        PlayerProfile profile = Util.getPitProfile(p);
        if (!profile.isLoaded()) {
            profile = null;
        }

        return switch (params) {
            case "level_tag_roman" -> CC.translate(profile != null
                    ? profile.getFormattedLevelTagWithRoman()
                    : "&7[0]");
            case "level_tag" -> CC.translate(profile != null
                    ? profile.getFormattedLevelTag()
                    : "&7[0]");
            case "genesis_tag" -> {
                if (ActivityType.GENES.isActive()) {
                    if (profile != null && profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.ANGEL) {
                        yield "&b♆";
                    }
                    if (profile != null && profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.DEMON) {
                        yield "&c♨";
                    }
                }
                yield "";
            }
            case "coins" -> profile != null
                    ? String.valueOf(profile.getCoins())
                    : "0.0";
            case "exp" -> profile != null
                    ? String.valueOf(profile.getExperience())
                    : "0.0";
            case "bounty" -> profile != null
                    ? String.valueOf(profile.getBounty())
                    : "0";
            case "prestige" -> profile != null
                    ? String.valueOf(profile.getPrestige())
                    : "0";
            case "level" -> profile != null
                    ? String.valueOf(profile.getLevel())
                    : "0";
            case "forceprestige_require" -> profile != null
                    ? String.valueOf(profile.getPrestige() * (profile.getPrestige() >= 10 ? 1000 : 500) + 1000)
                    : "0";
            case "has_bonus_exp" -> profile != null
                    ? String.valueOf(profile.hasExtraExpBonus())
                    : "false";
            case "has_bonus_gold" -> profile != null
                    ? String.valueOf(profile.hasExtraGoldBonus())
                    : "false";
            case "is_supporter" -> profile != null
                    ? String.valueOf(profile.isSupporter())
                    : "false";
            default -> null;
        };
    }
}

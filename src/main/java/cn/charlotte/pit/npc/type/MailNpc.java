package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerMailData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.menu.mail.MailMenu;
import cn.charlotte.pit.npc.Skin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/25 19:03
 */
public class MailNpc extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "mail";
    }


    @Override
    public List<String> getHoloInfo() {
        return List.of(
                "&e%pitnpcholo_" + getNpcInternalName() + "_0%",
                "&e%pitnpcholo_" + getNpcInternalName() + "_1%",
                "&e%pitnpcholo_" + getNpcInternalName() + "_2%"
        );
    }

    @Override
    public List<String> getNpcDisplayName(Player player) {
        List<String> hologram = new ArrayList<>();
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        final PlayerMailData mailData = profile.getMailData();

        int unread = 0;
        for (Mail mail : mailData.getMails()) {
            if (!mail.isClaimed()) {
                unread++;
            }
        }
        hologram.add("&e&l邮件");
        if (unread > 0) {
            hologram.add((System.currentTimeMillis() % 2 == 0 ? "&a" : "&2") + "您有 " + unread + " 封未读邮件");
        }

        return hologram;
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getMailNpcLocation();
    }

    @Override
    public Skin getNpcSkin() {
        return new Skin(
                "ewogICJ0aW1lc3RhbXAiIDogMTczMjk1ODQ4MzM3MywKICAicHJvZmlsZUlkIiA6ICJlYWNlOTNlY2ZjZDI0OTg3YjhiNGQyY2RjZjc3M2E0NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJmaXNoZXJtYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzYxMWEyMjcwM2ZiNTZmZmIyMGU3MjU5YmQ3YWIxNjRjMGE0MjU2ZGExZGYyMzgyODc5N2IwNWUyYmYwYTY0NSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                "CMpt7U+93/0WuWs6vik4S7b87hwavv86uSPnk7RFoNH/5FMJqqTscZkgNWyznj5P7r5DR630AwkVysF6pnpAdTfMyxqp4E2Eq0CCn+zyuoIK9+kK95KfBiSQJuvAeefVKU1ZSi/6Lmz96YbGSndHzLMqGSFzUG05k7X/hFiGOwOG9TX61xZC8Um3NT/neyE8dfAgdr2K3JpYsifZ6S76gDSoZ5Lwp5am09lyQNNvOrxpx3308c3NK4uuYptYO88qXwW922tWLJBxZC8h7ayA/gVz1nThiWuGEpjtfze3BgC9lYeHrZ3DxllNZrM4cPzaCSrzleaY7LN223l7b7tjT341DxcYTZ8dsTSIGLaFtojORulPIlNhv+U3/EklIhUPz4oTqcAzJn0irF1f3FsnLT11yW5sRSjt3ltMisCXgb5j+EUZ2r6jz0sNdscxRY8TBxumoDtUCLpNgZmxvIy4xV4nOIt/n/RzoH/9rdsgO/RGnBBjRqY/sOQSYF6RLI7+Aot1UA1MujztbnCaOo76lVIAVMT98PIIHQhxrRhWgnZNYnZoyP/TvaEhRWEQmOxU4RC7a8wGVtfHjDgKVsbdCJT5UqjXlbYmf/o7382Cb21njGigCacNsZhOKekAz/9O7VoQRTvdPZEtLPE44tLJgxF3eT2RiOurVRPPi2fy+60="
        );
    }

    @Override
    public void handlePlayerInteract(Player player) {
        new MailMenu().openMenu(player);
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return new ItemStack(Material.CHEST);
    }
}

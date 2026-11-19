package cn.charlotte.pit.menu.admin.backpack;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/4 20:43
 */
@Slf4j
public class BackPackViewMenu extends Menu {

    private static final int ITEMS_PER_PAGE = 45;

    private final PlayerProfile targetProfile;
    private List<PlayerInvBackup> pageBackups = List.of();
    private int page;
    private boolean hasNextPage;
    private boolean loading;
    private boolean loadingBackup;

    public BackPackViewMenu(Player player, PlayerProfile profile, PlayerProfile targetProfile) {
        this.targetProfile = targetProfile;
        loadPage(player, 0);
    }

    @Override
    public String getTitle(Player player) {
        return targetProfile.getPlayerName() + " 的背包备份 (" + (page + 1) + ")";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttonMap = new HashMap<>();
        for (int slot = 0; slot < pageBackups.size(); slot++) {
            buttonMap.put(slot, new BackPackButton(pageBackups.get(slot)));
        }

        if (page > 0) {
            buttonMap.put(45, new PreviousPageButton());
        }
        if (hasNextPage) {
            buttonMap.put(53, new NextPageButton());
        }
        return buttonMap;
    }

    @Override
    public int getSize() {
        return 54;
    }

    private void loadPage(Player player, int requestedPage) {
        if (loading || requestedPage < 0) {
            return;
        }
        loading = true;

        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            List<PlayerInvBackup> loadedBackups = new ArrayList<>(ITEMS_PER_PAGE + 1);
            try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance()
                    .getMongoDB()
                    .getInvCollection()
                    .find(Filters.eq("uuid", targetProfile.getUuid()))
                    // The list view only needs metadata. Loading the complete
                    // inventory/ender chest for 45 backups made this command
                    // painfully slow for players with many large backups.
                    .projection(Projections.include("uuid", "timeStamp", "backupUuid"))
                    .sort(Sorts.descending("timeStamp"))
                    .skip(requestedPage * ITEMS_PER_PAGE)
                    .limit(ITEMS_PER_PAGE + 1)
                    .iterator()) {
                while (cursor.hasNext()) {
                    loadedBackups.add(cursor.next());
                }
            } catch (Exception exception) {
                log.error("Failed to load inventory backup page {} for {}",
                        requestedPage, targetProfile.getUuid(), exception);
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    loading = false;
                    if (player.isOnline()) {
                        player.sendMessage("§c读取背包备份失败，请查看服务器日志。");
                    }
                });
                return;
            }

            boolean nextPage = loadedBackups.size() > ITEMS_PER_PAGE;
            if (nextPage) {
                loadedBackups.remove(loadedBackups.size() - 1);
            }

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                loading = false;
                if (!player.isOnline()) {
                    return;
                }
                if (requestedPage > 0 && loadedBackups.isEmpty()) {
                    player.sendMessage("§c没有更多背包备份了。");
                    return;
                }
                page = requestedPage;
                pageBackups = loadedBackups;
                hasNextPage = nextPage;
                openMenu(player);
            });
        });
    }

    private class BackPackButton extends Button {
        private final PlayerInvBackup playerInvBackup;

        private BackPackButton(PlayerInvBackup playerInvBackup) {
            this.playerInvBackup = playerInvBackup;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(playerInvBackup.getTimeStamp()));
            return new ItemBuilder(Material.CHEST)
                    .name("&7备份时间: " + formattedTime)
                    .lore(
                            "&6" + playerInvBackup.getUuid(),
                            "&7左键查看背包，右键查看仓库"
                    )
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            if (loadingBackup) {
                return;
            }
            if (playerInvBackup.getBackupUuid() == null) {
                player.sendMessage("§c该备份缺少唯一标识，无法读取。请查看服务器日志。");
                return;
            }

            loadingBackup = true;
            player.sendMessage("§7正在读取该背包备份...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                PlayerInvBackup fullBackup = null;
                Exception failure = null;
                try {
                    fullBackup = ThePit.getInstance()
                            .getMongoDB()
                            .getInvCollection()
                            .find(Filters.eq("backupUuid", playerInvBackup.getBackupUuid()))
                            .first();
                } catch (Exception exception) {
                    failure = exception;
                    log.error("Failed to load inventory backup {}", playerInvBackup.getBackupUuid(), exception);
                }

                PlayerInvBackup result = fullBackup;
                Exception resultFailure = failure;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    loadingBackup = false;
                    if (!player.isOnline()) {
                        return;
                    }
                    if (resultFailure != null || result == null) {
                        player.sendMessage("§c读取背包备份失败，请查看服务器日志。");
                        return;
                    }
                    new BackupShowMenu(
                            targetProfile,
                            result,
                            clickType.isRightClick(),
                            BackPackViewMenu.this
                    ).openMenu(player);
                });
            });
        }
    }

    private class PreviousPageButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.FEATHER)
                    .name("&6上一页")
                    .lore("&a当前页: &f" + (page + 1))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            loadPage(player, page - 1);
        }
    }

    private class NextPageButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.FEATHER)
                    .name("&6下一页")
                    .lore("&a当前页: &f" + (page + 1))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            loadPage(player, page + 1);
        }
    }
}

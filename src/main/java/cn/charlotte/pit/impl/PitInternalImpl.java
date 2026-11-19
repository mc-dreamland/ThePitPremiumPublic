package cn.charlotte.pit.impl;

import cn.charlotte.pit.Assembly;
import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.newyear.event.BossEvent;
import cn.charlotte.pit.activity.newyear.event.RandomRedEnvelopesEvent;
import cn.charlotte.pit.api.PitInternalHook;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.TradeData;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.menu.MythicWellMenu;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.events.impl.*;
import cn.charlotte.pit.events.impl.major.*;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.*;
import cn.charlotte.pit.menu.admin.backpack.BackupShowMenu;
import cn.charlotte.pit.menu.admin.item.AdminEnchantMenu;
import cn.charlotte.pit.menu.admin.item.AdminItemMenu;
import cn.charlotte.pit.menu.admin.item.AdminRuneMenu;
import cn.charlotte.pit.menu.admin.trade.TradeTrackerMenu;
import cn.charlotte.pit.menu.genesis.GenesisMenu;
import cn.charlotte.pit.menu.heresy.HeresyMenu;
import cn.charlotte.pit.menu.main.AuctionMenu;
import cn.charlotte.pit.menu.shop.ShopMenu;
import cn.charlotte.pit.util.DecentHologramImpl;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.hologram.Hologram;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public final class PitInternalImpl implements PitInternalHook {

    /**
     * 单例实例
     */
    public static final PitInternalImpl INSTANCE = new PitInternalImpl();

    /**
     * 标识插件是否已初始化完毕
     */
    private static boolean loaded = false;

    /* ==========  构造&单例  ========== */

    private PitInternalImpl() {
        // 私有构造，防止外部实例化
    }

    public static PitInternalImpl getInstance() {
        return INSTANCE;
    }

    /* ==========  菜单相关  ========== */

    @Override
    public void openMythicWellMenu(Player player) {
        new MythicWellMenu(player).openMenu(player);
    }

    @Override
    public void openAuctionMenu(Player player) {
        new AuctionMenu().openMenu(player);
    }

    @Override
    public void openAngelMenu(Player player) {
        new GenesisMenu(GenesisTeam.ANGEL).openMenu(player);
    }

    @Override
    public void openDemonMenu(Player player) {
        new GenesisMenu(GenesisTeam.DEMON).openMenu(player);
    }

    @Override
    public void openTradeTrackMenu(Player player, PlayerProfile profile, List<TradeData> data) {
        new TradeTrackerMenu(profile, data).openMenu(player);
    }

    @Override
    public void openBackupShowMenu(Player player, PlayerProfile profile, PlayerInvBackup backup, boolean enderChest) {
        Menu backMenu = Menu.currentlyOpenedMenus.get(player.getUniqueId());
        new BackupShowMenu(profile, backup, enderChest, backMenu).openMenu(player);
    }

    @Override
    public void openMenu(Player player, String menuName) {
        if (menuName == null) return;

        switch (menuName) {
            case "shop" -> new ShopMenu().openMenu(player);
            case "admin_enchant" -> new AdminEnchantMenu().openMenu(player);
            case "admin_item" -> new AdminItemMenu().openMenu(player);
            case "rune_item" -> new AdminRuneMenu().openMenu(player);
            case "heresy" -> new HeresyMenu().openMenu(player);
            default -> { /* no-op */ }
        }
    }

    /* ==========  事件相关  ========== */

    @Override
    public boolean openEvent(Player player, String eventName) {
        return openEvent(player, eventName, false);
    }

    @Override
    public boolean openEvent(Player player, String eventName, boolean force) {
        return openEvent(player, eventName, force, -1);
    }

    @Override
    public boolean openEvent(Player player, String eventName, boolean force, int waitStartSeconds) {
        if (eventName == null) return false;

        INormalEvent normalEvent = null;
        IEpicEvent epicEvent = null;

        var factory = ThePit.getInstance().getEventFactory();
        switch (eventName.toLowerCase()) {
            case "疯狂天坑", "rage" -> epicEvent = factory.allEpicEvent.get(RagePitEvents.class);
            case "红蓝大战", "rvb" -> epicEvent = factory.allEpicEvent.get(RedVSBlueEvents.class);
            case "速算", "qm" -> normalEvent = factory.allNormalEvent.get(QuickMathEvents.class);
            case "空投", "package" -> normalEvent = factory.allNormalEvent.get(CarePackageEvents.class);
            case "全员通缉", "100g" -> normalEvent = factory.allNormalEvent.get(EveOneBountyEvents.class);
            case "经验福地", "experience_blessed_land" ->
                    normalEvent = factory.allNormalEvent.get(ExperienceblessedlandEvents.class);
            case "龙蛋", "dragon_egg" -> normalEvent = factory.allNormalEvent.get(DragonEggsEvents.class);
            case "庇护", "patronage" -> normalEvent = factory.allNormalEvent.get(PatronageEvents.class);
            case "猎杀", "hunt" -> normalEvent = factory.allNormalEvent.get(DamagePlusEvents.class);
            case "附魔爆发", "enchanting_bang_event" ->
                    normalEvent = factory.allNormalEvent.get(EnchantingBangEvents.class);
            case "蛋糕争夺战", "cake" -> normalEvent = factory.allNormalEvent.get(CakeEvents.class);
            case "天坑外卖", "hamburger" -> epicEvent = factory.allEpicEvent.get(HamburgerEvents.class);
            case "尖塔夺魁", "spire" -> epicEvent = factory.allEpicEvent.get(SpireEvents.class);
            case "旗帜争夺战", "squads" -> epicEvent = factory.allEpicEvent.get(SquadsEvents.class);
            case "竞拍", "auction" -> normalEvent = factory.allNormalEvent.get(AuctionEvents.class);
            case "方块划地战", "bh" -> epicEvent = factory.allEpicEvent.get(BlockHeadEvents.class);
            case "占山为王", "koth" -> normalEvent = factory.allNormalEvent.get(KingOfTheHillEvents.class);
            case "幸运轮盘", "lucky_wheel" -> epicEvent = factory.allEpicEvent.get(LuckyWheel.class);
            case "高梯之王", "leader" -> normalEvent = factory.allNormalEvent.get(KingOfTheLadderEvents.class);
            case "红包大战" -> normalEvent = factory.allNormalEvent.get(RandomRedEnvelopesEvent.class);
            case "boss" -> epicEvent = factory.allEpicEvent.get(BossEvent.class);
            case "cancel" -> {
                factory.cancelMajorEvent();
                if (factory.getActiveNormalEvent() != null) {
                    factory.inactiveEvent(factory.getActiveNormalEvent());
                }
                return true;
            }

            default -> {
                return false;
            }
        }

        if (epicEvent != null) {
            if (factory.isMajorEventBusy()) {
                player.sendMessage(CC.translate("&c大型事件正在准备、运行或恢复，请等待完成；恢复失败可用 /startevent cancel 重试。"));
                return false;
            }
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.getPlayerOption().isDebugDamageMessage()) {
                if (force) {
                    factory.pushEvent(epicEvent, true, waitStartSeconds);
                } else {
                    factory.activeEvent(epicEvent);
                }
            } else {
                factory.pushEvent(epicEvent, true, waitStartSeconds);
            }
        } else {
            if (factory.getActiveNormalEvent() != null) {
                factory.inactiveEvent(factory.getActiveNormalEvent());
            }
            factory.activeEvent(normalEvent);
        }

        return true;
    }

    /* ==========  Hologram / 配置  ========== */

    @Override
    public Hologram createHologram(Location location, String text) {
        return new DecentHologramImpl(location, text);
    }

    @Override
    public UUID getRunningKingsQuestsUuid() {
        return NewConfiguration.getKingsQuestsMarker();
    }

    @Override
    public String getPitSupportPermission() {
        return NewConfiguration.getPitSupportPermission();
    }

    @Override
    public boolean getRemoveSupportWhenNoPermission() {
        return NewConfiguration.isRemoveSupportWhenNoPermission();
    }

    /* ==========  物品相关  ========== */

    @Override
    public ItemStack reformatPitItem(ItemStack itemStack) {
        if (itemStack == null) return null;

        String internalName = ItemUtil.getInternalName(itemStack);
        if (internalName == null) {
            return null;
        }
        IMythicItem item = switch (internalName) {
            case "mythic_sword" -> new MythicSwordItem();
            case "mythic_bow" -> new MythicBowItem();
            case "mythic_leggings" -> new MythicLeggingsItem();
            case "angel_chestplate" -> new AngelChestplate();
            case "armageddon_boots" -> new ArmageddonBoots();
            default -> null;
        };

        if (item == null) return itemStack;

        item.loadFromItemStack(itemStack);
        if (item.isEnchanted() && (item.getMaxLive() <= 0 || item.getLive() <= 0)) {
            return null; // 失效物品：直接丢弃
        }
        return item.toItemStack();
    }

    @Override
    public ItemStack generateItem(String item) {
        if (item == null) return null;
        return switch (item) {
            case "chunk_of_vile" -> PitItems.CHUNK_OF_VILE.toItemStack();
            case "mythic_leggings" -> PitItems.MYTHIC_LEGGING.toItemStack();
            default -> null;
        };
    }

    @Override
    public int getItemEnchantLevel(ItemStack item, String enchantName) {
        return Utils.getEnchantLevel(item, enchantName);
    }

    /**
     * @return 0：未持有物品；1：未找到附魔/参数无效；2：并非 Mythic 物品；3：成功
     */
    @Override
    public int addItemInHandEnchant(Player player, String enchantName, int enchantLevel) {
        if (player == null) return 1;
        ItemStack item = player.getInventory().getItemInHand();
        if (item.getType() == Material.AIR) return 0;

        AbstractEnchantment enchant = ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap().get(enchantName);
        if (enchant == null) return 1;

        AbstractPitItem mythicItem = Utils.getMythicItem(item);
        if (mythicItem == null) return 2;

        mythicItem.getEnchantments().put(enchant, enchantLevel);
        player.getInventory().setItemInMainHand(mythicItem.toItemStack());
        return 3;
    }

    @Override
    public ItemStack getMythicItemItemStack(String itemName) {
        if (itemName == null) return new ItemStack(Material.AIR);
        Class<? extends AbstractPitItem> itemClass = ThePit.getInstance().getItemFactor().getItemMap().get(itemName);
        if (itemClass == null) return new ItemStack(Material.AIR);

        try {
            AbstractPitItem item = itemClass.getDeclaredConstructor().newInstance();
            return item.toItemStack();
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack(Material.AIR);
        }
    }

    /* ==========  附魔集合  ========== */

    @Override
    public List<AbstractEnchantment> allEnchantments() {
        return Assembly.all;
    }

    @Override
    public List<AbstractEnchantment> armorEnchantments() {
        return Assembly.armor;
    }

    @Override
    public List<AbstractEnchantment> bowEnchantments() {
        return Assembly.bow;
    }

    @Override
    public List<AbstractEnchantment> weaponEnchantments() {
        return Assembly.weapon;
    }

    @Override
    public List<AbstractEnchantment> musicEnchantments() {
        return Assembly.music;
    }

    /* ==========  其它辅助  ========== */

    @Override
    public String getWaterMark() {
        return CC.translate(NewConfiguration.getWaterMark());
    }

    @Override
    public String getBotName() {
        return CC.translate(NewConfiguration.getBotName());
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 供外部设置插件加载状态
     */
    public static void setLoaded(boolean loaded) {
        PitInternalImpl.loaded = loaded;
    }
}

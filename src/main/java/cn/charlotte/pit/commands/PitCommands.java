package cn.charlotte.pit.commands;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.commands.admin.*;
import cn.charlotte.pit.commands.player.*;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.util.configuration.annotations.ConfigData;
import cn.charlotte.pit.util.configuration.annotations.ConfigSerializer;
import cn.charlotte.pit.util.configuration.serializer.LocationSerializer;
import cn.charlotte.pit.util.cooldown.Cooldown;
import co.aikar.commands.PaperCommandManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PitCommands {
    public static final Random random = new Random();
    public static final String PATTEN_DEFAULT_YMD = "yyyy-MM-dd";
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(PATTEN_DEFAULT_YMD);
    public static final DecimalFormat numFormat = new DecimalFormat("0.00");
    public final Map<UUID, Cooldown> COOLDOWN_SHOW = new HashMap<>();
    public final Cache<UUID, Long> viewCooldown = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();


    public final Gson gson = new Gson();
    public final DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public final List<UUID> requestDrop = new ArrayList<>();
    public final List<UUID> confirmDrop = new ArrayList<>();

    public PitCommands(ThePit thePit) {

        PaperCommandManager manager = new PaperCommandManager(thePit);

        manager.getCommandCompletions().registerCompletion("locationFields", c -> {
            List<String> locationFields = new ArrayList<>();

            for (Field declaredField : PitConfig.class.getDeclaredFields()) {
                ConfigSerializer annotation = declaredField.getAnnotation(ConfigSerializer.class);
                if (annotation == null || annotation.serializer() != LocationSerializer.class) {
                    continue;
                }

                ConfigData fieldAnnotation = declaredField.getAnnotation(ConfigData.class);
                if (fieldAnnotation != null) {
                    locationFields.add(fieldAnnotation.path());
                }
            }

            return locationFields;
        });

        // 注册玩家命令
        manager.registerCommand(new BakeMaster(this));
//        manager.registerCommand(new Cdk(this));
        manager.registerCommand(new Cool(this));
        manager.registerCommand(new Events(this));
        manager.registerCommand(new KillRecap(this));
        manager.registerCommand(new KingQuestGui(this));
        manager.registerCommand(new Setting(this));
        manager.registerCommand(new Show(this));
        manager.registerCommand(new ShowGold(this));
        manager.registerCommand(new Spawn(this));
        // 交易、拍卖相关命令合并(后面拍卖GUI独立)
        manager.registerCommand(new Trade(manager));
        manager.registerCommand(new View(this));

        // 注册管理员命令
        manager.registerCommand(new Check(this));
        manager.registerCommand(new Debug(this));
//        manager.registerCommand(new Gacha(this));
        manager.registerCommand(new Kaboom(this));
        manager.registerCommand(new Modify(this));
        manager.registerCommand(new ModifyAll(this));
        manager.registerCommand(new OpenMenu(this));
        manager.registerCommand(new Pack(this));
        manager.registerCommand(new PitItemsCommand(this));
        manager.registerCommand(new ScanCommand(this));
        manager.registerCommand(new Setup(this));
        manager.registerCommand(new StartEvent(this));
        manager.registerCommand(new Supporter(this));
        manager.registerCommand(new PitTest(this));
        manager.registerCommand(new ForcePrestige(this));
        manager.registerCommand(new ExtraBonus(this));
        manager.registerCommand(new TrashBin());
        manager.registerCommand(new PitBotCommand());
        manager.registerCommand(new NameHidingCommand());
    }
}

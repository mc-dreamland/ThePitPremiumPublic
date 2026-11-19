package cn.charlotte.pit.menu.Help.button;

import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class UpdateMenu extends Menu {

    @Getter
    @Setter
    public static double Ver = 1.8;
    @Getter
    private static final Map<Double, List<Uplog>> uplogMap = Map.ofEntries(
            Map.entry(1.0, List.of(
                    new Uplog(0.0,
                            List.of("版本由1.8 升至 1.20.6", "性能优化"),
                            List.of("新增岩浆桶增益/天赋", "副手相关功能调整与适配"),
                            List.of("部分大/小型事件相关问题")
                    )
            )),
            Map.entry(1.1, List.of(
                    new Uplog(0.0,
                            List.of(
                                    "对小鸡和下水道之鱼新增了交互逻辑",
                                    "天坑增值包新增事件声望 + 1 金币 + 50% 的特权",
                                    "天坑增值包对于樱桃的掉落概率 * 10",
                                    "天坑增值包免费解锁行动礼盒,其余玩家需要将 契约者 声望升级升到 3 级 解锁"
                            ),
                            List.of("对在线福利进行了小幅度更改"),
                            List.of()
                    )
            )),
            Map.entry(1.2, List.of(
                    new Uplog(0.0,
                            List.of(
                                    "天坑物品图鉴(部分)",
                                    "下水道之甲的专属附魔(机制与暴怒附魔逻辑相同,仅2个)"
                            ),
                            List.of(),
                            List.of(
                                    "部分事件未启用但是仍然显示需求人数的Bug",
                                    "阵营自动加基础赏金而不是赏金加成",
                                    "有关于附魔台的显示Bug",
                                    "AAA牛排伤害过高的Bug以及Map未及时清理导致的内存泄露问题"
                            )
                    )
            )),
            Map.entry(1.3, List.of(
                    // 1.3.0 初始版本
                    new Uplog(0.0,
                            List.of(
                                    "天坑物品图鉴(部分)",
                                    "物品-增益旗帜",
                                    "玩家进服文本",
                                    "新下水道附魔: 劳动者",
                                    "空投及下水道之箱新增了周围3格无法放置方块的限制"
                            ),
                            List.of(
                                    "下水道之甲的附魔机制 50%可以正常附魔反之附魔对应下水道附魔(仅第一次)",
                                    "削弱了暗影步伐 从背后1.5格变成4.0格",
                                    "下水道附魔：恶毒 伤害上调5% 但羽毛损失减伤至1根(仅3级)",
                                    "下调行动礼盒的价格"
                            ),
                            List.of(
                                    "拍卖中获得的仙人掌无法使用的问题",
                                    "钓鱼金币无法算入转生金币中",
                                    "龙蛋事件龙蛋瞬移导致的卡墙"
                            )
                    ),
                    // 1.3.1 补丁更新
                    new Uplog(0.01,
                            List.of(),
                            List.of("全员赏金事件新增变异体(1%概率触发) 全员增加 250 赏金"),
                            List.of("尝试性修复尖塔夺魁,装备消失,带出尖塔装备,层数不一致的Bug")
                    ),
                    // 1.3.2 补丁更新
                    new Uplog(0.02,
                            List.of("自定义计分板功能(在OPT去自定义吧)"),
                            List.of(
                                    "对登峰造极掉落物文本提示做出了小修改",
                                    "削弱了致命节奏",
                                    "增强金手铐"
                            ),
                            List.of()
                    ),
                    // 1.3.3 平衡性调整与更新
                    new Uplog(0.03,
                            List.of(
                                    "牛排 剩余时间在计分板上的显示",
                                    "沉默 剩余时间在计分板上的显示"
                            ),
                            List.of(
                                    "强力击：神速力 从1颗❤必中改为0.6必中 以及从2次攻击改为3次攻击",
                                    "强力击：紊乱 从虚弱4更改为虚弱1",
                                    "精神攻击 从-60%伤害改为-50%伤害",
                                    "新年快乐 从1.5秒增加到3秒",
                                    "移除千万富翁",
                                    "生命回响 从0.5颗心改为0.25颗心",
                                    "毒液 对自身造成24秒的负面状态以及从3次攻击改为4次攻击"
                            ),
                            List.of()
                    ),
                    // 1.3.4 平衡性调整与更新
                    new Uplog(0.04,
                            List.of(
                                    "嗜血 在计分板上的显示",
                                    "ShowGold指令你可以使用该指令用于展示你的金币数量",
                                    "对附魔图鉴进行了更改新增了可选择切换显示方式功能"
                            ),
                            List.of("对任务机制进行了修改,现在拥有更多的任务了"),
                            List.of("尝试性修复尖塔吞装备问题")
                    ),
                    // 1.3.5 附魔机制调整
                    new Uplog(0.05,
                            List.of(
                                    "普通暴怒附魔 破钻之击",
                                    "下水道普通附魔 憎恶",
                                    "弓普通附魔 滴水石穿",
                                    "简易工匠包并对拍卖进行了小幅度修改"
                            ),
                            List.of(
                                    "超级连杀部分增益效果无法在宵禁期间使用",
                                    "针对拍卖进行了小幅度修改",
                                    "对附魔图鉴进行了修改使其能够适配神话天坑材质包的CIT显示以及去除了OP专属附魔",
                                    "部分附魔的文本显示"
                            ),
                            List.of("AAA牛排可以带进坑上的Bug")
                    ),
                    // 1.3.6 更新机制
                    new Uplog(0.06,
                            List.of(
                                    "更多的物品图鉴",
                                    "宵禁期间无法获得糖和小麦的定义",
                                    "会员可以得到下水道箱子刷新的通知(opt可以开关此选项)"
                            ),
                            List.of(
                                    "下水道箱子延长了时间以及增加了数量",
                                    "阵营开放限制 只有达到P5才可以选择阵营",
                                    "AAA牛排食用方法仅右键可食用"
                            ),
                            List.of(
                                    "带亿万富翁的剑挖掘小麦时会扣钱的Bug",
                                    "增益附魔如探金之人等裤子与刀的属性不叠加的Bug",
                                    "不知道何时去掉的干草块兑换逻辑"
                            )
                    ),
                    // 1.3.7 更新
                    new Uplog(0.07,
                            List.of(
                                    "更多的物品图鉴",
                                    "下水道普通附魔 恶臭",
                                    "下水道普通附魔 石像鬼",
                                    "神话之甲普通附魔 药剂师",
                                    "下水道稀有附魔 生命契约"
                            ),
                            List.of(
                                    "烘焙大师以及国王任务的所需",
                                    "下水道箱子的金币以及经验值",
                                    "下水道废弃之物产出数量",
                                    "阵营最低加入精通等级",
                                    "西瓜的恢复时间调整至 6小时",
                                    "阵营所需的点数"
                            ),
                            List.of(
                                    "没到达精通等级III也能加入阵营",
                                    "岩浆桶可以舀水的Bug",
                                    "阵营增益是负数",
                                    "小麦无法通过工作台兑换成干草块的Bug",
                                    "方块划地战的钻石套装会卡掉国王头盔的Bug"
                            )
                    )
            )),
            Map.entry(1.4, List.of(
                    new Uplog(0.0,
                            List.of(
                                    "宵禁禁用活动点数的增加",
                                    "更多的物品图鉴",
                                    "垃圾桶无法在坑下打开"
                            ),
                            List.of("阵营所需的点数削减至原来的20%"),
                            List.of(
                                    "任务中的兑换的增益无法生效",
                                    "方块划地战的钻石套装会卡掉终末之鞋的Bug",
                                    "方块划地战会卡事件的Bug"
                            )
                    ),
                    new Uplog(0.01,
                            List.of(),
                            List.of(
                                    "下水道稀有附魔 生命契约 ->增强",
                                    "任务获得的暗聚块的门槛",
                                    "任务大师给予的暗聚块",
                                    "任务的奖励"
                            ),
                            List.of("任务介绍")
                    ),
                    new Uplog(0.02,
                            List.of("精通后天赋,连杀天赋,超级连杀需要重新购买"),
                            List.of(
                                    "天使恶魔阵营所需的点数",
                                    "神话武器的前缀名"
                            ),
                            List.of(
                                    "AAA牛排可以带上坑",
                                    "方块划地战相关Bug",
                                    "战术重部部署装置的相关Bug"
                            )
                    ),
                    // 1.4.3 更新与调整
                    new Uplog(0.03,
                            List.of("放弃任务也算任务数用于作为惩罚"),
                            List.of(
                                    "天使恶魔阵营所需的点数",
                                    "下水道之箱和空投箱的禁止放方块的限制范围"
                            ),
                            List.of(
                                    "精通后精通声望天赋消失的Bug",
                                    "附魔神话装备必定播报的Bug",
                                    "背包满但依旧可以取物品和获取物品大量吞物品的问题",
                                    "部分文本问题"
                            )
                    ),
                    // 1.4.4 更新与调整
                    new Uplog(0.04,
                            List.of(
                                    "不愈之毒显示在计分板",
                                    "赏金溶剂显示在计分板上"
                            ),
                            List.of("部分事件的奖励机制"),
                            List.of(
                                    "高级鸡蛋可以被扔出",
                                    "不愈之毒的层数可以无限叠加",
                                    "大量状态栏时间不整齐"
                            )
                    ),
                    // 1.4.5 更新与调整
                    new Uplog(0.05,
                            List.of(),
                            List.of("有关于国王任务相关物品的兑换逻辑"),
                            List.of(
                                    "部分文本问题",
                                    "无法进入尖塔的问题",
                                    "鱼竿突然消失的问题",
                                    "可dupe内容"
                            )
                    ),
                    // 1.4.6 更新与调整
                    new Uplog(0.06,
                            List.of(
                                    "下水道新附魔 普通 银鱼",
                                    "下水道专属物品 下水道磁石",
                                    "实验性质的反刷机制"
                            ),
                            List.of(
                                    "赏金计算机制",
                                    "助攻奖励计算",
                                    "下水道之甲的附魔概率",
                                    "行动礼盒的奖励",
                                    "AAA牛排的伤害"
                            ),
                            List.of(
                                    "有关国王任务的内容",
                                    "VIP经验加成不匹配的问题",
                                    "当应用金头天赋时第二个金苹果天赋依旧获取金苹果的问题"
                            )
                    ),
                    // 1.4.7 更新与调整
                    new Uplog(0.07,
                            List.of("宵禁对于钓鱼的影响"),
                            List.of(
                                    "下水道之箱的物品概率",
                                    "刷头机制,拒绝刷头!从你我他开始!"
                            ),
                            List.of(
                                    "下水道之鱼的兑换方法",
                                    "鱼竿伤害问题"
                            )
                    ),
                    // 1.4.8 更新与调整
                    new Uplog(0.08,
                            List.of(),
                            List.of(
                                    "部分附魔机制以及部分连杀天赋机制",
                                    "下水道箱子的机制"
                            ),
                            List.of(
                                    "鱼竿伤害问题",
                                    "部分问题"
                            )
                    ),
                    // 1.4.9 更新与调整
                    new Uplog(0.09,
                            List.of("海龟壳(尚未增加获取途径)"),
                            List.of(
                                    "发言机制",
                                    "钓鱼机制",
                                    "调整反刷头机制",
                                    "部分游戏机制",
                                    "下水道箱子",
                                    "下水道磁石以及下水道彩蛋机制"
                            ),
                            List.of()
                    )
            )),
            Map.entry(1.5, List.of(
                    new Uplog(0.0,
                            List.of(),
                            List.of("白版掉落概率"),
                            List.of("猛汉击杀不触发恢复血量")
                    ),
                    new Uplog(0.01,
                            List.of(),
                            List.of(
                                    "猎杀给予的声望逻辑,不能重复击杀,以及跳跃高度",
                                    "部分事件的奖励机制",
                                    "部分神话附魔的机制"
                            ),
                            List.of(
                                    "拍卖事件返回的邮件可以刷累计金币",
                                    "有关下水道箱子的相关BUG"
                            )
                    ),
                    // 1.5.2 更新与调整
                    new Uplog(0.02,
                            List.of(
                                    "蛋糕事件 美味曲奇物品掉落",
                                    "蛋糕事件 美味樱桃物品掉落",
                                    "下水道箱子 方块掉落"
                            ),
                            List.of(
                                    "慢速聊天模式",
                                    "附魔属性",
                                    "高梯之王不增加累计",
                                    "龙蛋事件奖励获取",
                                    "下水道箱子刷新机制",
                                    "下水道方块放置限制",
                                    "可汗的伤害",
                                    "行动礼盒的购买所需",
                                    "竞拍的稀有概率",
                                    "国王任务的声望所需"
                            ),
                            List.of(
                                    "疾跑附魔无用",
                                    "钻石回收者,刀弓裤无法叠加属性",
                                    "占山为王实际增益不到4倍",
                                    "大量事件获得声望无法加到国王任务中"
                            )
                    ),
                    // 1.5.3 更新与调整
                    new Uplog(0.03,
                            List.of("大型事件_幸运转盘"),
                            List.of(
                                    "删掉了慢速聊天模式",
                                    "恶臭附魔属性"
                            ),
                            List.of(
                                    "下水道区域判定",
                                    "蛋糕事件给予的物品可以带上坑",
                                    "部分文本问题"
                            )
                    ),
                    // 1.5.4 更新与调整
                    new Uplog(0.04,
                            List.of(),
                            List.of(
                                    "助攻计算规则",
                                    "中坑范围"
                            ),
                            List.of()
                    ),
                    // 1.5.5 更新与调整
                    new Uplog(0.05,
                            List.of(),
                            List.of(
                                    "侦察兵 计数规则",
                                    "银鱼 伤害加成",
                                    "精神攻击 伤害加成",
                                    "宵禁 计算规则，降低了人数判定"
                            ),
                            List.of()
                    ),
                    // 1.5.6 更新与调整
                    new Uplog(0.06,
                            List.of(
                                    "装备 计数器",
                                    "装备 改名卡"
                            ),
                            List.of(),
                            List.of(
                                    "部分附魔在宵禁期间异常问题",
                                    "部分天赋结算异常"
                            )
                    ),
                    // 1.5.7 更新与调整
                    new Uplog(0.07,
                            List.of("BOT 检测"),
                            List.of(),
                            List.of("鲸鲨 加成BUG")
                    ),
                    // 1.58.0 初始版本
                    new Uplog(0.08,
                            List.of(),
                            List.of(
                                    "下水道附魔银鱼的伤害值",
                                    "天赋-吸血鬼的机制",
                                    "神话之弓附魔-吸血鬼的机制",
                                    "神话之剑附魔-鲸鲨 的机制",
                                    "天赋-力量 的伤害加成"
                            ),
                            List.of(
                                    "击杀奖励结算错误问题",
                                    "药水效果低阶效果会覆盖高阶效果",
                                    "新大型幸运轮盘的各种Bug现已修复可正常使用"
                            )
                    ),
                    new Uplog(0.081,
                            List.of("P0玩家无法使用神话装备"),
                            List.of("登峰造极的价格"),
                            List.of("国王任务")
                    ),
                    new Uplog(0.082,
                            List.of(),
                            List.of(
                                    "登峰造极的价格",
                                    "药水机制"
                            ),
                            List.of("可口浓汤无法生效")
                    ),
                    new Uplog(0.083,
                            List.of(),
                            List.of(
                                    "助攻增益",
                                    "击杀增益(加了一个精通等级差距奖励)"
                            ),
                            List.of()
                    ),
                    new Uplog(0.084,
                            List.of("小型事件 经验福地"),
                            List.of(
                                    "占山为王增益效果",
                                    "铁质装备的精通要求",
                                    "月球的增益,现在开启月球可以直接获得2000的存储经验",
                                    "回调鲸鲨"
                            ),
                            List.of("累计问题")
                    )
            )),
            Map.entry(1.6, List.of(
                    new Uplog(0.0,
                            List.of(
                                    "增值包用户提升3级任务概率",
                                    "高梯之王附近4格不能放置方块"
                            ),
                            List.of(
                                    "增强了破釜沉舟",
                                    "3级任务的时长以及更多的3级任务",
                                    "2级任务有概率获得暗聚块"
                            ),
                            List.of(
                                    "购买钻石斧,死后不会给予铁斧头",
                                    "幸运轮盘大型事件 杀人不给予轮盘兑换券",
                                    "乱七八糟的BUG"
                            )
                    ),
                    new Uplog(0.01,
                            List.of(
                                    "普通.精力",
                                    "稀有.孤独",
                                    "稀有.嗜血者"
                            ),
                            List.of(),
                            List.of("大量的乱七八糟的BUG")
                    ),
                    new Uplog(0.02,
                            List.of(
//                                    "防挂机系统",
                                    "新的任务 赏金猎手"
                            ),
                            List.of(
//                                    "重写了更新日志的逻辑",
                                    "对于强力击:闪电附魔进行雷声调小",
                                    "对于声望天赋 旅途的中途站 添加了增加最终击杀奖励这个机制",
                                    "增益旗帜增益加成",
//                                    "天使恶魔活动逻辑,每轮活动之后将持续一个月",
                                    "调整了灵魂吞噬者附魔伤害",
                                    "对于部分事件的奖励进行了调整",
                                    "调整了大量有关经验以及金币加成相关的内容"
//                                    "去掉了刷头机制但依旧保留天外来客机制"
                            ),
                            List.of(
//                                    "修复了不能给予行动礼盒的问题(使用/qd功能用来领取礼盒)",
                                    "修复了吸血鬼附魔问题",
                                    "修复了登峰造极的神话装备掉落概率未增加问题",
                                    "修复隐士相关问题"
                            )
                    ),
                    new Uplog(0.021,
                            List.of(
                                    "事件记录档案",
                                    "增值包玩家可以查询离线玩家数据"
                            ),
                            List.of(
                                    "金币以及经验机制"
                            ),
                            List.of(
                                    "修复了尖塔夺魁相关问题,回调至1.51版本代码",
                                    "修复隐士相关问题"
                            )
                    ),
                    new Uplog(0.022,
                            List.of(
                                    "旅途的中途站添加了解锁后全服提醒",
                                    "死神添加负增益"
                            ),
                            List.of(
                                    "钻石回收者的奖励范围",
                                    "野兽模式的受伤机制",
                                    "部分简介内容修改",
                                    "回调了嗜血者",
                                    "调整了灵魂吞噬者的伤害以及计算方式"
                            ),
                            List.of(
                                    "赏金上限问题",
                                    "护甲值异常以及神话之裤护甲异常",
                                    "区分了连杀奖励以及普通连杀该Bug导致经验变少",
                                    "万箭齐发不再触发鱼竿效果",
                                    "修复了国王的头盔护甲值相关问题",
                                    "修复尖塔事件上第6层却没有奖励的问题",
                                    "修复了国王的头盔护甲值相关问题"
                            )
                    ),
                    new Uplog(0.03,
                            List.of(
                                    "Events界面添加了查询现在执行的事件",
                                    "国王头盔添加盔甲韧性值",
                                    "新增稀有附魔附魔台有粒子效果",
                                    "幸运轮盘事件添加了时间判断以及追踪"
                            ),
                            List.of(
                                    "金币的连杀奖励",
                                    "国王头盔的增益以及介绍",
                                    "重写了精力附魔",
                                    "附魔爆发回归",
                                    "永久显示连杀数",
                                    "迅捷之风增加了伤害",
                                    "尝试性对于死神的冲击波进行了调整(不触发可汗,吸血鬼,力量)",
                                    "野兽模式受伤值调整",
                                    "登峰造极超级连杀受伤加成"
                            ),
                            List.of(
                                    "尝试修复疯狂天坑死一次后血量减半",
                                    "修复增值包用户无法获得行动礼盒"
                            )
                    ),
                    new Uplog(0.04,
                            List.of(
                                    "新增随机神话兑换券",
                                    "邮件NPC新增一键删除可删除邮件按钮",
                                    "神话附魔帮助图新增计算神话掉落概率条目",
                                    "新增增益Buff效果",
                                    "商店新增购买好看的头盔功能(无法交易)",
                                    "幸运轮盘事件新增方块阻挡机制",
                                    "5分钟内没人答对速算会自行结束事件",
                                    "新增自定义设置 延长显示对方状态"
                            ),
                            List.of(
                                    "经验福地事件延长时间",
                                    "暂时移除了图鉴中天坑物品图鉴",
                                    "Events查询事件标题调整",
                                    "调整蜂毒附魔调整为低版本效果",
                                    "液体方块周围是否能放置方块的范围",
                                    "登峰造极超级连杀的伤害",
                                    "精通等级压制击杀以及等级压制击杀增益",
                                    "尊贵血统的奖励",
                                    "奔月的奖励",
                                    "宝石之剑的稀有概率",
                                    "登峰造极凋落物内容",
                                    "尖塔夺魁灵魂机制以及击杀增益",
                                    "灵魂吞噬者伤害下调"
                            ),
                            List.of(
                                    "经验福地不生效问题",
                                    "每次附魔都会有稀有附魔粒子的问题",
                                    "尝试修复疯狂天坑死后最大血量异常问题",
                                    "修复部分小问题",
                                    "功夫知识可以用于尖塔夺魁",
                                    "尖塔夺魁中掉落的神话物品以邮件形式发送"
                            )
                    ),
                    new Uplog(0.05,
                            List.of(
                                    "精通商店增加返回上一页功能"
                            ),
                            List.of(
                                    "移除了一个Option项目",
                                    "战术重部署装置部分内容",
                                    "超级连杀触发给予发光效果",
                                    "show指令无法展示改名全券"
                            ),
                            List.of(
                                    "野兽模式顶掉皮革头盔问题"
                            )
                    )
            )),
            Map.entry(1.7, List.of(
                    new Uplog(0.0,
                            List.of(
                                    "&7更新 &e新年 &7大型活动"
                            ),
                            List.of(
                                    "部分物品以及附魔"
                            ),
                            List.of(
                                    "大量已知问题"
                            )
                    )
            )),
            Map.entry(
                    1.8,List.of(
                            new Uplog(0.0,
                                    List.of(
                                            "大型活动 &b天使&c恶魔 &7活动开启"
                                    ),
                                    List.of(
                                            "天使恶魔活动部分内容调整",
                                            "神秘学说内容调整",
                                            "神秘盒子内容调整"
                                    ),
                                    List.of(
                                            "大量已知内容"
                                    )
                            ),
                            new Uplog(0.1,
                                    List.of(
                                            "基岩版玩家商店添加切换自动购买按钮",
                                            "简易工匠包和改名券修改原有的使用逻辑"
                                    ),
                                    List.of(
                                            "部分数值调整",
                                            "调整部分内容"
                                    ),
                                    List.of(
                                            "狂暴连击弓箭触发BUG",
                                            "尝试修复暴击产生二次攻击问题",
                                            "樱桃异常给予问题",
                                            "暗影步伐相关问题"
                                    )
                            )
                    )
            )
    );

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public String getTitle(Player player) {
        return "天坑更新日志|目前版本:" + CombatListener.numFormat.format(Ver);
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<Double> versions = new ArrayList<>(uplogMap.keySet());
        versions.sort(Collections.reverseOrder());
        int slot = 0;
        for (Double version : versions) {
            buttons.put(slot, getVersionButton(version));
            slot++;

            if (slot % 9 == 0) {
                slot += 2;
            }
        }

        return buttons;
    }

    private Button getVersionButton(double version) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                if (version == Ver) {
                    return new ItemBuilder(Material.ENCHANTED_BOOK)
                            .name("&6&l版本: " + CombatListener.numFormat.format(version) + " &a(当前版本)")
                            .lore("", "&f该版本拥有 &a" + uplogMap.get(version).size() + " &f个小版本", "", "&7点击查看详细更新内容").build();
                }

                return new ItemBuilder(Material.BOOKSHELF)
                        .name("&e版本: " + CombatListener.numFormat.format(version))
                        .lore("", "&f该版本拥有 &a" + uplogMap.get(version).size() + " &f个小版本", "", "&7点击查看详细更新内容").build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new BigverMenu(version, uplogMap.get(version)).openMenu(player);
            }
        };
    }

    @Data
    private static class Uplog {

        private double smallver;
        private List<String> adjust;
        private List<String> renewal;
        private List<String> repair; // 修复

        public Uplog(double smallver, List<String> adjust, List<String> renewal, List<String> repair) {
            this.smallver = smallver;
            this.adjust = adjust != null ? adjust : new ArrayList<>();
            this.renewal = renewal != null ? renewal : new ArrayList<>();
            this.repair = repair != null ? repair : new ArrayList<>();
        }
    }

    private static class BigverMenu extends Menu {

        private final double bigVersion;
        private final List<Uplog> smallVersions;

        public BigverMenu(double bigVersion, List<Uplog> smallVersions) {
            this.bigVersion = bigVersion;
            this.smallVersions = smallVersions;
        }

        @Override
        public String getTitle(Player player) {
            return "版本 " + CombatListener.numFormat.format(bigVersion) + " 更新日志";
        }

        @Override
        public int getSize() {
            return 54;
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();

            List<Uplog> sortedVersions = new ArrayList<>(smallVersions);
            sortedVersions.sort((a, b) -> Double.compare(b.getSmallver(), a.getSmallver()));

            int slot = 0;
            for (Uplog uplog : sortedVersions) {
                buttons.put(slot, getSmallVersionButton(uplog));
                slot++;

                if (slot % 9 == 0) {
                    slot += 1;
                }
            }

            buttons.put(26, getBackButton());

            return buttons;
        }

        private Button getSmallVersionButton(Uplog uplog) {
            return new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {

                    double fullVersion = bigVersion + uplog.getSmallver();

                    List<String> lore = new ArrayList<>();
                    lore.add("&7版本号: &f" + String.format(Locale.US, "%.3f", fullVersion));
                    lore.add("");

                    if (!uplog.getRenewal().isEmpty()) {
                        lore.add("");
                        lore.add("&a✔ 新增内容: &f" + uplog.getAdjust().size() + " 项");
                        for (String string : uplog.getAdjust()) {
                            lore.add(" &a• " + string);
                        }
                    }
                    if (!uplog.getAdjust().isEmpty()) {
                        lore.add("");
                        lore.add("&e⚖ 调整内容: &f" + uplog.getRenewal().size() + " 项");
                        for (String string : uplog.getRenewal()) {
                            lore.add(" &e• " + string);
                        }
                    }
                    if (!uplog.getRepair().isEmpty()) {
                        lore.add("");
                        lore.add("&c⚙ 修复内容: &f" + uplog.getRepair().size() + " 项");
                        for (String string : uplog.getRepair()) {
                            lore.add(" &c• " + string);
                        }
                    }

                    lore.add("");
                    lore.add("&e点击查看完整更新日志");

                    return new ItemBuilder(Material.BOOK)
                            .name("&6版本 " + CombatListener.numFormat.format(fullVersion))
                            .lore(lore).build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    showUpdateDetails(player, uplog);
                }
            };
        }

        private Button getBackButton() {
            return new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.ARROW)
                            .name("&c返回主菜单")
                            .lore("", "&7点击返回更新日志主菜单").build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    new UpdateMenu().openMenu(player);
                }
            };
        }

        private void showUpdateDetails(Player player, Uplog uplog) {
            player.closeInventory();

            double fullVersion = bigVersion + uplog.getSmallver();

            player.sendMessage("§6§l=== 版本 " + fullVersion + " 更新日志 ===");
            player.sendMessage("");

            if (!uplog.getRenewal().isEmpty()) {
                player.sendMessage("§a§l✔ 新增内容:");
                for (String renewal : uplog.getAdjust()) {
                    player.sendMessage("§a  • " + renewal);
                }
                player.sendMessage("");
            }

            if (!uplog.getAdjust().isEmpty()) {
                player.sendMessage("§e§l⚖ 调整内容:");
                for (String adjust : uplog.getRenewal()) {
                    player.sendMessage("§e  • " + adjust);
                }
                player.sendMessage("");
            }

            if (!uplog.getRepair().isEmpty()) {
                player.sendMessage("§c§l⚙ 修复内容:");
                for (String repair : uplog.getRepair()) {
                    player.sendMessage("§c  • " + repair);
                }
                player.sendMessage("");
            }
        }
    }
}
package cn.charlotte.pit.util.chat;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/14 15:33
 */
public enum MessageType {
    //不需要归类的消息请直接不带类型参数发送(如权限不足提示等)
    BOUNTY, //赏金
    STREAK, //连杀
    SEWERS_CHEST,
    PRESTIGE, //精通消息
    World, //世界
    FreeCE, //在线福利
    EVENT, //活动消息 仅限小事件开始/倒计时提醒 其余请用ANNOUNCEMENT
    COMBAT, //战斗消息
    CHAT, //聊天
    MISC, //杂项消息 暂时留空
    QUEST,//任务
    COUNTER, //计数器消息
    MEDAL, //成就
    ACTIVITY, //大型事件
    DEBUG
}

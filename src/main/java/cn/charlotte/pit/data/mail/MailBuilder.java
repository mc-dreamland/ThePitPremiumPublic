package cn.charlotte.pit.data.mail;

import cn.charlotte.pit.data.sub.PlayerInv;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MailBuilder {
    private String uuid;
    private String title;
    private String content;
    private double exp;
    private double coins;
    private int renown;
    private PlayerInv item;
    private Long sendTime;
    private Long expireTime;
    private boolean claimed;

    public MailBuilder uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public MailBuilder title(String title) {
        this.title = title;
        return this;
    }

    public MailBuilder content(String content) {
        this.content = content;
        return this;
    }

    public MailBuilder content(String... content){
        StringBuilder builder = new StringBuilder();
        for (String string : content) builder.append(string).append("\\n");
        this.content = builder.toString();
        return this;
    }

    public MailBuilder exp(double exp) {
        this.exp = exp;
        return this;
    }

    public MailBuilder coins(double coins) {
        this.coins = coins;
        return this;
    }

    public MailBuilder renown(int renown) {
        this.renown = renown;
        return this;
    }

    public MailBuilder item(PlayerInv item) {
        this.item = item;
        return this;
    }

    public MailBuilder item(ItemStack... item){
        PlayerInv contents = new PlayerInv();
        contents.setContents(item);
        this.item = contents;
        return this;
    }

    public MailBuilder item(List<ItemStack> item){
        PlayerInv contents = new PlayerInv();
        contents.setContents(item.toArray(new ItemStack[0]));
        this.item = contents;
        return this;
    }

    public MailBuilder sendTime(Long sendTime) {
        this.sendTime = Objects.requireNonNullElseGet(sendTime, System::currentTimeMillis);
        return this;
    }

    public MailBuilder expireTime(long expireTime) {
        this.expireTime = expireTime;
        return this;
    }

    public MailBuilder claimed(boolean claimed) {
        this.claimed = claimed;
        return this;
    }

    public Mail build() {
        Mail mail = new Mail();
        if (uuid != null) mail.setUuid(uuid);
        else mail.setUuid(UUID.randomUUID().toString());
        if (title != null) mail.setTitle(title);
        else mail.setTitle("NONE");
        if (content != null) mail.setContent(content);
        mail.setExp(exp);
        mail.setCoins(coins);
        mail.setRenown(renown);
        if (item != null) mail.setItem(item);
        if(sendTime != null) mail.setSendTime(sendTime);
        else mail.setSendTime(System.currentTimeMillis());
        if(expireTime != null) mail.setExpireTime(expireTime);
        else mail.setExpireTime(System.currentTimeMillis() + 1000L * 60 * 60 * 3);
        mail.setClaimed(claimed);
        return mail;
    }
}
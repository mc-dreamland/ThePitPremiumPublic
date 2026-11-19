package cn.charlotte.pit.data.mail;

import cn.charlotte.pit.data.sub.PlayerInv;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/16 22:39
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Mail {
    private String uuid;
    private String title;
    private String content;
    private double exp;
    private double coins;
    private int renown;
    private PlayerInv item;
    private long sendTime;
    private long expireTime;
    private boolean claimed;

    public Mail() {
        this.item = new PlayerInv();
        this.build();
    }

    public void build() {
        this.uuid = UUID.randomUUID().toString();
    }
}

package cn.charlotte.pit.data;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.mail.Mail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/16 22:51
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerMailData {
    private String uuid;
    private String name;
    private String nameLower;
    private List<Mail> mails;

    public PlayerMailData() {
        this.mails = new ArrayList<>();
    }

    public PlayerMailData(UUID uuid, String name) {
        this();

        this.uuid = uuid.toString();
        this.name = name;
        this.nameLower = name.toLowerCase();
    }

    public void cleanUp() {
        final List<Mail> list = this.mails
                .stream()
                .filter(mail -> mail.getUuid() == null || mail.getUuid().equals("null"))
                .collect(Collectors.toList());

        mails.removeAll(list);
    }

    public void sendMail(Mail mail) {
        if (mails.size() >= 99) {
            this.mails = this.mails.subList(0, 97);
        }
        this.mails.add(mail);
        save();
    }

    /**
     * Shouldn't call this method on primary thread
     * when player claimed mail
     * or sent mail to player
     * you should call this method.DONT FORGET!!!
     */
    public void save() {
        ThePit.getInstance()
                .getMongoDB()
                .getMailCollection()
                .replaceOne(Filters.eq("uuid", this.uuid), this, new ReplaceOptions().upsert(true));
    }
}

package cn.charlotte.pit.webhook;

import org.json.simple.JSONObject;

import java.util.UUID;

public class CustomNameValidWebhook extends Webhook {

    public CustomNameValidWebhook(String url) {
        super(url);
    }

    public void notify(UUID uuid, String playerName, String customName) {
        JSONObject let = new JSONObject();
        let.put("uuid", uuid.toString());
        let.put("playerName", playerName);
        let.put("customName", customName);
        post(let);
    }
}

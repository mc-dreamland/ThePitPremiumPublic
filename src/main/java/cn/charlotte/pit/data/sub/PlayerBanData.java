package cn.charlotte.pit.data.sub;

import lombok.Data;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/14 15:59
 */
@Data
public class PlayerBanData {

    private String executor = "none";
    private String reason = "none";
    private long start = 0;
    private long duration = 0;
    private long end = 0;

    public PlayerBanData() {
    }
}

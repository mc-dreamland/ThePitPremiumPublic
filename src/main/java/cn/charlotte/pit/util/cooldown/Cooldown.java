package cn.charlotte.pit.util.cooldown;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Misoryan
 * @Created_In: 2020/12/30 19:07
 */
@Data
public class Cooldown {
    private long start = System.currentTimeMillis();
    private long expire;
    private boolean notified;

    private long duration;

    /**
     * 构造一个冷却时间对象
     *
     * @param duration 冷却持续时间（毫秒）
     */
    public Cooldown(long duration) {
        this.duration = duration;
        this.expire = this.start + duration;
        if (duration == 0L) {
            this.notified = true;
        }
    }

    /**
     * 构造一个冷却时间对象
     *
     * @param duration 冷却持续时间
     * @param timeUnit 时间单位
     */
    public Cooldown(long duration, TimeUnit timeUnit) {
        this(timeUnit.toMillis(duration));
    }

    /**
     * 获取已过去的时间
     *
     * @return 从冷却开始到现在已过去的时间（毫秒）
     */
    public long getPassed() {
        return System.currentTimeMillis() - this.start;
    }

    /**
     * 获取剩余时间
     *
     * @return 距离冷却结束的剩余时间（毫秒）
     */
    public long getRemaining() {
        return this.expire - System.currentTimeMillis();
    }

    /**
     * 检查冷却是否已过期
     *
     * @return true-冷却已结束，false-冷却未结束
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() - this.expire >= 0L;
    }

    /**
     * 重置冷却时间
     * 将开始时间设为当前时间，重新计算到期时间
     */
    public void reset() {
        this.start = System.currentTimeMillis();
        this.expire = this.start + this.duration;
        if (duration == 0L) {
            this.notified = true;
        }
    }
}
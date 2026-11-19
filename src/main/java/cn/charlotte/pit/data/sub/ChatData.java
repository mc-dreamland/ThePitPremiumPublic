package cn.charlotte.pit.data.sub;

import java.util.Objects;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/18 22:49
 */
public class ChatData {
    private long timestamp;
    private String message;

    public ChatData(long timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public ChatData() {
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChatData other)) return false;
        if (!other.canEqual(this)) return false;
        if (this.getTimestamp() != other.getTimestamp()) return false;
        final Object this$message = this.getMessage();
        final Object other$message = other.getMessage();
        return Objects.equals(this$message, other$message);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ChatData;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $timestamp = this.getTimestamp();
        result = result * PRIME + (int) ($timestamp >>> 32 ^ $timestamp);
        final Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    public String toString() {
        return "ChatData(timestamp=" + this.getTimestamp() + ", message=" + this.getMessage() + ")";
    }
}

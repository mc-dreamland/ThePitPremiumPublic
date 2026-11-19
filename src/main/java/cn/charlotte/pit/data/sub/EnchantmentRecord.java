package cn.charlotte.pit.data.sub;

import java.util.Objects;

public final class EnchantmentRecord {

    private final String enchanter;
    private final String description;
    private final long timestamp;

    public EnchantmentRecord(String enchanter, String description, long timestamp) {
        this.enchanter = enchanter;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getEnchanter() {
        return enchanter;
    }

    public String getDescription() {
        return description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnchantmentRecord that)) {
            return false;
        }
        return timestamp == that.timestamp
                && Objects.equals(enchanter, that.enchanter)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enchanter, description, timestamp);
    }

    @Override
    public String toString() {
        return "EnchantmentRecord{" +
                "enchanter='" + enchanter + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

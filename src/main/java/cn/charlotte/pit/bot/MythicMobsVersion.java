package cn.charlotte.pit.bot;

/** Match the tested API version while allowing the build hashes used by official MM jars. */
public final class MythicMobsVersion {
    public static final String REQUIRED = "5.8.2";

    private MythicMobsVersion() { }

    public static boolean supported(String version) {
        if (version == null) return false;
        String value = version.trim();
        return value.equals(REQUIRED) || value.startsWith(REQUIRED + "-") || value.startsWith(REQUIRED + "+");
    }
}

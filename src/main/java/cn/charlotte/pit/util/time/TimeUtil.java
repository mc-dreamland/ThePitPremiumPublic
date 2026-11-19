package cn.charlotte.pit.util.time;

import cn.charlotte.pit.ThePit;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @Author: Misoryan
 * @Created_In: 2020/12/30 19:10
 */
public final class TimeUtil {
    private static final JsonParser parse = new JsonParser();
    private static final String HOUR_FORMAT = "%02d:%02d:%02d";
    private static final String MINUTE_FORMAT = "%02d:%02d";
    private static final SimpleDateFormat TIME_TO_REQUEST_FORMAT = new SimpleDateFormat("MM-dd");
    public static boolean IS_HOLIDAY = false;
    public static String HOLIDAY_NAME;
    private static JsonObject HOLIDAY_INFO;

    private TimeUtil() {
        throw new RuntimeException("Cannot instantiate a utility class.");
    }

    public static void initHolidayData() throws Exception {
        int year = java.time.Year.now().getValue();
        File dataFolder = ThePit.getInstance().getDataFolder();

        File cacheFile = new File(dataFolder, "holidays_" + year + ".json");

        String json;
        if (cacheFile.exists()) {
            ThePit.getInstance().getLogger().info("从本地缓存读取节假日数据...");
            json = readFile(cacheFile);
        } else {
            ThePit.getInstance().getLogger().info("缓存不存在，开始请求 API...");
            json = fetchFromApi(year);
            writeFile(cacheFile, json);
        }

        HOLIDAY_INFO = JsonParser.parseString(json).getAsJsonObject();
    }

    private static String fetchFromApi(int year) throws IOException {
        String urlStr = "http://timor.tech/api/holiday/year/" + year;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (InputStream in = conn.getInputStream();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static void writeFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(content);
        }
    }

    /**
     * 判断某天是否为节假日
     *
     * @param date 格式 MM-dd
     */
    public static boolean isHoliday(String date) {
        if (HOLIDAY_INFO == null) return false;
        JsonObject days = HOLIDAY_INFO.getAsJsonObject("holiday");
        if (days == null) return false;
        JsonObject day = days.getAsJsonObject(date);
        if (day == null) return false;
        return day.get("holiday").getAsBoolean(); // true 表示节假日
    }

    public static long getMinecraftDay(long mills) {
        return Math.floorDiv(mills, 36 * 60 * 1000L);
    }

    public static long getMinecraftDay() {
        return getMinecraftDay(System.currentTimeMillis());
    }

    //获取游戏内昼夜时间 (0 ~ 24000 其中0~12000为白天 12000~24000为黑夜)
    public static long getMinecraftTick(long mills) {
        //0~36min
        long time = mills % (36 * 60 * 1000);
        double percent;
        if (time <= 24 * 60 * 1000) { //0~24min
            percent = ((double) time) / (24 * 60 * 1000);
        } else {
            percent = 1 + ((double) time - 24 * 60 * 1000) / (12 * 60 * 1000);
        }
        return Double.valueOf(percent * 12000).longValue();
    }

    public static long getMinecraftTick() {
        return getMinecraftTick(System.currentTimeMillis());
    }

    public static Date getNextDayDate() {
        final long daySpan = 24 * 60 * 60 * 1000;
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd '00:00:00'");
        Date startTime;
        try {
            startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sdf.format(System.currentTimeMillis()));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        if (System.currentTimeMillis() > startTime.getTime())
            startTime = new Date(startTime.getTime() + daySpan);

        return startTime;
    }

    /**
     * 判断当前时间距离第二天凌晨的毫秒数
     *
     * @return 返回值单位为[ms:毫秒]
     */
    public static Long getMillisecondNextEarlyMorning() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() - System.currentTimeMillis();
    }

    public static String millisToTimer(long millis) {
        long seconds = millis / 1000L;

        if (seconds > 3600L) {
            return String.format(HOUR_FORMAT, seconds / 3600L, seconds % 3600L / 60L, seconds % 60L);
        } else {
            return String.format(MINUTE_FORMAT, seconds / 60L, seconds % 60L);
        }
    }

    /**
     * Return the amount of seconds from milliseconds.
     * Note: We explicitly use 1000.0F (float) instead of 1000L (long).
     *
     * @param millis the amount of time in milliseconds
     * @return the seconds
     */
    public static String millisToSeconds(long millis) {
        return new DecimalFormat("#0.0").format(millis / 1000.0F);
    }

    public static String dateToString(Date date, String secondaryColor) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return new SimpleDateFormat("MMM dd yyyy " + (secondaryColor == null ? "" : secondaryColor) +
                "(hh:mm aa zz)").format(date);
    }

    public static Timestamp addDuration(long duration) {
        return truncateTimestamp(new Timestamp(System.currentTimeMillis() + duration));
    }

    public static Timestamp truncateTimestamp(Timestamp timestamp) {
        if (timestamp.toLocalDateTime().getYear() > 2037) {
            timestamp.setYear(2037);
        }

        return timestamp;
    }

    public static Timestamp addDuration(Timestamp timestamp) {
        return truncateTimestamp(new Timestamp(System.currentTimeMillis() + timestamp.getTime()));
    }

    public static Timestamp fromMillis(long millis) {
        return new Timestamp(millis);
    }

    public static Timestamp getCurrentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static String millisToRoundedTime(long millis) {
        millis += 1L;

        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0) {
            return days + " 天 " + (hours - 24 * days) + " 小时";
        } else if (hours > 0) {
            return hours + " 小时 " + (minutes - 60 * hours) + " 分钟";
        } else if (minutes > 0) {
            return minutes + " 分钟 " + (seconds - 60 * minutes) + " 秒";
        } else {
            return seconds + " 秒";
        }
    }

    public static long parseTime(String time) {
        long totalTime = 0L;
        boolean found = false;
        Matcher matcher = Pattern.compile("\\d+\\D+").matcher(time);

        while (matcher.find()) {
            String s = matcher.group();
            Long value = Long.parseLong(s.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")[0]);
            String type = s.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")[1];

            switch (type) {
                case "s":
                    totalTime += value;
                    found = true;
                    break;
                case "m":
                    totalTime += value * 60;
                    found = true;
                    break;
                case "h":
                    totalTime += value * 60 * 60;
                    found = true;
                    break;
                case "d":
                    totalTime += value * 60 * 60 * 24;
                    found = true;
                    break;
                case "w":
                    totalTime += value * 60 * 60 * 24 * 7;
                    found = true;
                    break;
                case "M":
                    totalTime += value * 60 * 60 * 24 * 30;
                    found = true;
                    break;
                case "y":
                    totalTime += value * 60 * 60 * 24 * 365;
                    found = true;
                    break;
            }
        }

        return !found ? -1 : totalTime * 1000;
    }


    public static void checkHoliday() {

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        if (HOLIDAY_INFO == null) {
            IS_HOLIDAY = false;
            HOLIDAY_NAME = "";
            return;
        }
        JsonObject days = HOLIDAY_INFO.getAsJsonObject("holiday");
        if (days == null) {
            IS_HOLIDAY = false;
            HOLIDAY_NAME = "";
            return;
        }
        JsonObject day = days.getAsJsonObject(date);
        if (day == null) {
            IS_HOLIDAY = false;
            HOLIDAY_NAME = "";
            return;
        }
        IS_HOLIDAY = day.get("holiday").getAsBoolean(); // true 表示节假日
        String name = day.get("name").getAsString();
        if (name.equals("除夕") || name.startsWith("初")) {
            name = "春节";
        }
        HOLIDAY_NAME = name;
    }
}

package cn.charlotte.pit.util.time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/19 15:30
 */
public record Duration(long value) {

    public static Duration fromString(String source) {
        if (source.equalsIgnoreCase("perm") || source.equalsIgnoreCase("permanent")) {
            return new Duration(Integer.MAX_VALUE);
        }

        long totalTime = 0L;
        boolean found = false;
        Matcher matcher = Pattern.compile("\\d+\\D+").matcher(source);

        while (matcher.find()) {
            String s = matcher.group();
            long value = Long.parseLong(s.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")[0]);
            String type = s.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")[1];

            found = switch (type) {
                case "s" -> {
                    totalTime += value;
                    yield true;
                }
                case "m" -> {
                    totalTime += value * 60;
                    yield true;
                }
                case "h" -> {
                    totalTime += value * 60 * 60;
                    yield true;
                }
                case "d" -> {
                    totalTime += value * 60 * 60 * 24;
                    yield true;
                }
                case "w" -> {
                    totalTime += value * 60 * 60 * 24 * 7;
                    yield true;
                }
                case "M" -> {
                    totalTime += value * 60 * 60 * 24 * 30;
                    yield true;
                }
                case "y" -> {
                    totalTime += value * 60 * 60 * 24 * 365;
                    yield true;
                }
                default -> found;
            };
        }

        return new Duration(!found ? -1 : totalTime * 1000);
    }
}

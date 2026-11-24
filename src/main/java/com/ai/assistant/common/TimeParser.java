package com.ai.assistant.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具参数的时间解析器
 *
 * Function Calling 中模型生成的参数不可靠：可能是标准格式，也可能是"昨天""最近一周"这类自然语言表达。
 * 解析失败时抛出带引导信息的 IllegalArgumentException，由工具层捕获后把错误说明回传给模型，
 * 引导模型修正参数重新调用，形成"调用失败 → 错误回传 → 自愈重试"闭环。
 *
 * 支持的表达：
 * - 标准完整格式：yyyy-MM-dd HH:mm:ss
 * - 仅日期：yyyy-MM-dd（起始边界取当天 00:00:00，结束边界取当天 23:59:59）
 * - 相对表达：今天、昨天、最近N天（如"最近7天"）、最近一周、最近一个月、最近三个月
 *   （"最近N天"作为开始边界解析为 now-N天，作为结束边界解析为 now）
 */
public final class TimeParser {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern RECENT_DAYS = Pattern.compile("最近(\\d+)天");
    private static final Pattern RECENT_NAMED = Pattern.compile("最近(一周|一个月|三个月)");

    private TimeParser() {
    }

    /** 解析查询区间的开始时间 */
    public static LocalDateTime parseStart(String input, LocalDateTime now) {
        return parse(input, true, now);
    }

    /** 解析查询区间的结束时间 */
    public static LocalDateTime parseEnd(String input, LocalDateTime now) {
        return parse(input, false, now);
    }

    private static LocalDateTime parse(String input, boolean isStart, LocalDateTime now) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("时间参数为空。请使用 yyyy-MM-dd HH:mm:ss 格式，或'昨天'、'最近一周'等相对表达");
        }

        // 1. 标准完整格式
        try {
            return LocalDateTime.parse(text, DATETIME);
        } catch (DateTimeParseException ignored) {
            // 不是完整格式，继续尝试其他表达
        }

        // 2. 仅日期：按查询边界补全时刻
        try {
            LocalDate date = LocalDate.parse(text, DATE);
            return boundary(date, isStart);
        } catch (DateTimeParseException ignored) {
            // 不是纯日期，继续尝试相对表达
        }

        // 3. 相对表达：今天/昨天
        if ("今天".equals(text)) {
            return boundary(now.toLocalDate(), isStart);
        }
        if ("昨天".equals(text)) {
            return boundary(now.toLocalDate().minusDays(1), isStart);
        }

        // 4. 相对表达：最近N天 / 最近一周 / 最近一个月 / 最近三个月
        Matcher days = RECENT_DAYS.matcher(text);
        if (days.matches()) {
            return recent(Long.parseLong(days.group(1)), isStart, now);
        }
        Matcher named = RECENT_NAMED.matcher(text);
        if (named.matches()) {
            long n = switch (named.group(1)) {
                case "一周" -> 7;
                case "一个月" -> 30;
                default -> 90; // 三个月
            };
            return recent(n, isStart, now);
        }

        // 5. 都不匹配：抛出带引导信息的异常，供工具层回传给模型（自愈的关键：错误信息要告诉模型"怎么改"）
        throw new IllegalArgumentException(
                "无法识别的时间表达式：" + text + "。请使用 yyyy-MM-dd HH:mm:ss 格式，或'昨天'、'最近一周'、'最近一个月'等相对表达");
    }

    /** "最近N天"的边界语义：开始 = now-N天，结束 = now */
    private static LocalDateTime recent(long n, boolean isStart, LocalDateTime now) {
        return isStart ? now.minusDays(n) : now;
    }

    private static LocalDateTime boundary(LocalDate date, boolean isStart) {
        return isStart ? date.atStartOfDay() : date.atTime(23, 59, 59);
    }
}

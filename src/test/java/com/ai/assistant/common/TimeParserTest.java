package com.ai.assistant.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeParser 单元测试
 * now 由调用方传入（纯函数设计），断言完全确定，不依赖当前时间
 */
class TimeParserTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 15, 30, 0);

    @Test
    void testParseStandardFormat() {
        LocalDateTime start = TimeParser.parseStart("2026-11-01 08:00:00", NOW);
        assertEquals(LocalDateTime.of(2026, 11, 1, 8, 0, 0), start);
    }

    @Test
    void testParseDateOnly_RespectsBoundary() {
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0, 0), TimeParser.parseStart("2026-11-01", NOW));
        assertEquals(LocalDateTime.of(2026, 11, 1, 23, 59, 59), TimeParser.parseEnd("2026-11-01", NOW));
    }

    @Test
    void testParseYesterday() {
        assertEquals(LocalDateTime.of(2026, 11, 23, 0, 0, 0), TimeParser.parseStart("昨天", NOW));
        assertEquals(LocalDateTime.of(2026, 11, 23, 23, 59, 59), TimeParser.parseEnd("昨天", NOW));
    }

    @Test
    void testParseToday() {
        assertEquals(LocalDateTime.of(2026, 11, 24, 0, 0, 0), TimeParser.parseStart("今天", NOW));
        assertEquals(LocalDateTime.of(2026, 11, 24, 23, 59, 59), TimeParser.parseEnd("今天", NOW));
    }

    @Test
    void testParseRecentDays() {
        // "最近N天"的边界语义：开始 = now-N天，结束 = now
        assertEquals(NOW.minusDays(7), TimeParser.parseStart("最近7天", NOW));
        assertEquals(NOW, TimeParser.parseEnd("最近7天", NOW));
        assertEquals(NOW.minusDays(7), TimeParser.parseStart("最近一周", NOW));
        assertEquals(NOW, TimeParser.parseEnd("最近一周", NOW));
        assertEquals(NOW.minusDays(30), TimeParser.parseStart("最近一个月", NOW));
        assertEquals(NOW.minusDays(90), TimeParser.parseStart("最近三个月", NOW));
    }

    @Test
    void testParseInvalid_ThrowsWithGuidance() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TimeParser.parseStart("前天上午", NOW));
        // 错误信息必须包含引导模型修正的说明——这是自愈闭环的关键输入
        assertTrue(ex.getMessage().contains("无法识别"));
        assertTrue(ex.getMessage().contains("yyyy-MM-dd HH:mm:ss"));
    }
}

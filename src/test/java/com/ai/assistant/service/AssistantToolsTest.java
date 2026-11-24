package com.ai.assistant.service;

import com.ai.assistant.dto.BestSellingProduct;
import com.ai.assistant.dto.OrderSummary;
import com.ai.assistant.dto.OrdersByDateResult;
import com.ai.assistant.dto.TotalSpent;
import com.ai.assistant.model.Order;
import com.ai.assistant.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AssistantTools 单元测试
 * 使用 Mockito 模拟 OrderRepository，不依赖数据库
 * 工具返回结构化 DTO 后，测试直接断言数据字段，不依赖任何文案格式
 */
@ExtendWith(MockitoExtension.class)
class AssistantToolsTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AssistantTools assistantTools;

    @Test
    void testQueryOrders_WithResults() {
        Order order1 = new Order("张三", "iPhone 15", new BigDecimal("5999.00"));
        Order order2 = new Order("张三", "AirPods", new BigDecimal("1999.00"));
        when(orderRepository.findByUsername("张三")).thenReturn(List.of(order1, order2));

        List<OrderSummary> result = assistantTools.queryOrders("张三");

        assertEquals(2, result.size());
        assertEquals("iPhone 15", result.get(0).product());
        assertEquals(0, result.get(0).amount().compareTo(new BigDecimal("5999.00")));
        assertNotNull(result.get(0).orderTime());
        verify(orderRepository, times(1)).findByUsername("张三");
    }

    @Test
    void testQueryOrders_NoResults() {
        when(orderRepository.findByUsername("王五")).thenReturn(Collections.emptyList());

        List<OrderSummary> result = assistantTools.queryOrders("王五");

        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryTotalSpent_WithResults() {
        when(orderRepository.getTotalAmountByUsername("张三")).thenReturn(new BigDecimal("7998.00"));
        when(orderRepository.countByUsername("张三")).thenReturn(2L);

        TotalSpent result = assistantTools.queryTotalSpent("张三");

        assertEquals("张三", result.username());
        assertEquals(0, result.totalAmount().compareTo(new BigDecimal("7998.00")));
        assertEquals(2L, result.orderCount());
    }

    @Test
    void testQueryTotalSpent_NoResults() {
        when(orderRepository.getTotalAmountByUsername("王五")).thenReturn(null);
        when(orderRepository.countByUsername("王五")).thenReturn(0L);

        TotalSpent result = assistantTools.queryTotalSpent("王五");

        assertEquals(0, result.totalAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0L, result.orderCount());
    }

    @Test
    void testQueryOrdersByDate_WithResults() {
        Order order = new Order("张三", "iPhone 15", new BigDecimal("5999.00"));
        when(orderRepository.findByUsernameAndOrderTimeBetween(anyString(), any(), any()))
                .thenReturn(List.of(order));

        OrdersByDateResult result = assistantTools.queryOrdersByDate(
                "张三", "2026-01-01 00:00:00", "2026-12-31 23:59:59");

        assertNull(result.error());
        assertEquals(1, result.orders().size());
        assertEquals("iPhone 15", result.orders().get(0).product());
    }

    @Test
    void testQueryOrdersByDate_RelativeExpression() {
        // 模型把"昨天"这类自然语言直接当参数传入，应被 TimeParser 正确解析
        when(orderRepository.findByUsernameAndOrderTimeBetween(anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        OrdersByDateResult result = assistantTools.queryOrdersByDate("张三", "昨天", "昨天");

        assertNull(result.error());
        assertTrue(result.orders().isEmpty());
        verify(orderRepository).findByUsernameAndOrderTimeBetween(anyString(), any(), any());
    }

    @Test
    void testQueryOrdersByDate_InvalidInput_ReturnsErrorGuidance() {
        // 解析失败时不抛异常、不查库，而是返回带引导信息的结构化错误（自愈闭环）
        OrdersByDateResult result = assistantTools.queryOrdersByDate("张三", "前天上午", "昨天");

        assertNotNull(result.error());
        assertTrue(result.error().contains("无法识别"));
        assertTrue(result.orders().isEmpty());
        verify(orderRepository, never()).findByUsernameAndOrderTimeBetween(anyString(), any(), any());
    }

    @Test
    void testQueryOrdersByDate_ReversedRange_ReturnsError() {
        OrdersByDateResult result = assistantTools.queryOrdersByDate(
                "张三", "2026-12-31 00:00:00", "2026-01-01 00:00:00");

        assertNotNull(result.error());
        assertTrue(result.error().contains("晚于"));
        verify(orderRepository, never()).findByUsernameAndOrderTimeBetween(anyString(), any(), any());
    }

    @Test
    void testBestSellingProduct_WithResults() {
        when(orderRepository.findBestSellingProduct())
                .thenReturn(List.<Object[]>of(new Object[]{"iPhone 15", 5L}));

        BestSellingProduct result = assistantTools.bestSellingProduct();

        assertEquals("iPhone 15", result.product());
        assertEquals(5L, result.salesCount());
    }

    @Test
    void testBestSellingProduct_NoResults() {
        when(orderRepository.findBestSellingProduct()).thenReturn(Collections.emptyList());

        BestSellingProduct result = assistantTools.bestSellingProduct();

        assertNull(result.product());
        assertEquals(0L, result.salesCount());
    }
}

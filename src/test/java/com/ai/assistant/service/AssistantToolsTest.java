package com.ai.assistant.service;

import com.ai.assistant.model.Order;
import com.ai.assistant.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AssistantTools 单元测试
 * 使用 Mockito 模拟 OrderRepository，不依赖数据库
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

        String result = assistantTools.queryOrders("张三");

        assertTrue(result.contains("iPhone 15"));
        assertTrue(result.contains("AirPods"));
        verify(orderRepository, times(1)).findByUsername("张三");
    }

    @Test
    void testQueryOrders_NoResults() {
        when(orderRepository.findByUsername("王五")).thenReturn(Collections.emptyList());

        String result = assistantTools.queryOrders("王五");

        assertTrue(result.contains("暂无订单"));
    }

    @Test
    void testQueryTotalSpent_WithResults() {
        when(orderRepository.getTotalAmountByUsername("张三")).thenReturn(new BigDecimal("7998.00"));

        String result = assistantTools.queryTotalSpent("张三");

        assertTrue(result.contains("7998.00"));
    }

    @Test
    void testQueryTotalSpent_NoResults() {
        when(orderRepository.getTotalAmountByUsername("王五")).thenReturn(null);

        String result = assistantTools.queryTotalSpent("王五");

        assertTrue(result.contains("暂无消费记录"));
    }

    @Test
    void testBestSellingProduct_WithResults() {
        when(orderRepository.findBestSellingProduct())
                .thenReturn(List.<Object[]>of(new Object[]{"iPhone 15", 5L}));

        String result = assistantTools.bestSellingProduct();

        assertTrue(result.contains("iPhone 15"));
        assertTrue(result.contains("5"));
    }

    @Test
    void testBestSellingProduct_NoResults() {
        when(orderRepository.findBestSellingProduct()).thenReturn(Collections.emptyList());

        String result = assistantTools.bestSellingProduct();

        assertTrue(result.contains("暂无销售数据"));
    }
}

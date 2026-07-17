package com.ai.assistant.repository;

import com.ai.assistant.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderRepository 单元测试
 * @DataJpaTest 会自动配置内嵌 H2 内存数据库，不依赖外部 MySQL
 * 每个测试方法执行后自动回滚，测试之间互不影响
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.save(new Order("张三", "iPhone 15", new BigDecimal("5999.00")));
        orderRepository.save(new Order("张三", "AirPods Pro", new BigDecimal("1999.00")));
        orderRepository.save(new Order("李四", "iPhone 15", new BigDecimal("5999.00")));
    }

    @Test
    void testFindByUsername() {
        List<Order> orders = orderRepository.findByUsername("张三");
        assertEquals(2, orders.size());
        assertTrue(orders.stream().allMatch(o -> o.getUsername().equals("张三")));
    }

    @Test
    void testFindByUsernameNotFound() {
        List<Order> orders = orderRepository.findByUsername("王五");
        assertTrue(orders.isEmpty());
    }

    @Test
    void testGetTotalAmountByUsername() {
        BigDecimal total = orderRepository.getTotalAmountByUsername("张三");
        assertEquals(new BigDecimal("7998.00"), total);
    }

    @Test
    void testFindByUsernameAndOrderTimeBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<Order> orders = orderRepository.findByUsernameAndOrderTimeBetween("张三", start, end);
        assertEquals(2, orders.size());
    }

    @Test
    void testFindBestSellingProduct() {
        List<Object[]> result = orderRepository.findBestSellingProduct();
        assertFalse(result.isEmpty());
        Object[] row = result.get(0);
        assertEquals("iPhone 15", row[0]);
        assertEquals(2L, row[1]);
    }
}

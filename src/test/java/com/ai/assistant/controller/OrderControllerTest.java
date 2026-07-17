package com.ai.assistant.controller;

import com.ai.assistant.common.GlobalExceptionHandler;
import com.ai.assistant.model.Order;
import com.ai.assistant.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderController 单元测试
 * @WebMvcTest 只加载 Web 层（Controller + MVC 配置），不加载 Service/Repository
 * 用 @MockBean 模拟 OrderRepository
 */
@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    void testGetAllOrders() throws Exception {
        Order order = new Order("张三", "iPhone", new BigDecimal("5999.00"));
        order.setId(1L);
        when(orderRepository.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].username").value("张三"));
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("订单不存在，ID: 999"));
    }

    @Test
    void testCreateOrder_ValidationFail() throws Exception {
        String json = "{\"username\":\"\",\"product\":\"\",\"amount\":0}";

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        String json = "{\"username\":\"张三\",\"product\":\"iPhone\",\"amount\":5999.00}";

        Order saved = new Order("张三", "iPhone", new BigDecimal("5999.00"));
        saved.setId(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testDeleteOrder_NotFound() throws Exception {
        when(orderRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/orders/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}

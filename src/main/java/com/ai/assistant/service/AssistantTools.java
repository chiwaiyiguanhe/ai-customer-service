package com.ai.assistant.service;

import com.ai.assistant.dto.BestSellingProduct;
import com.ai.assistant.dto.OrderSummary;
import com.ai.assistant.dto.TotalSpent;
import com.ai.assistant.model.Order;
import com.ai.assistant.repository.OrderRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 工具集
 * 通过 @Tool 注解暴露给 LLM，LLM 会根据用户意图自动选择调用哪个工具
 * 这就是 Function Calling 机制
 *
 * 设计约定：工具返回结构化数据（record 经框架 Jackson 序列化为 JSON 回传给模型），
 * 由模型基于原始数据组织回答语言，工具层不拼接任何"客服话术"。
 * 好处：同一份工具数据可支撑无限种问法，模型可以对数据做二次推理（比较、排序、追问）。
 */
@Component
public class AssistantTools {

    private static final Logger log = LoggerFactory.getLogger(AssistantTools.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderRepository orderRepository;

    public AssistantTools(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Tool("根据用户名查询该用户的所有订单")
    public List<OrderSummary> queryOrders(@P("用户名") String username) {
        log.info("调用工具: queryOrders(username={})", username);
        return orderRepository.findByUsername(username).stream()
                .map(AssistantTools::toSummary)
                .toList();
    }

    @Tool("查询用户的历史总消费金额及订单数")
    public TotalSpent queryTotalSpent(@P("用户名") String username) {
        log.info("调用工具: queryTotalSpent(username={})", username);
        BigDecimal total = orderRepository.getTotalAmountByUsername(username);
        long count = orderRepository.countByUsername(username);
        // SUM 在无记录时返回 null，统一归零，让模型看到 0 而不是 null
        return new TotalSpent(username, total == null ? BigDecimal.ZERO : total, count);
    }

    @Tool("查询用户在指定时间段内的订单，日期格式为 yyyy-MM-dd HH:mm:ss")
    public List<OrderSummary> queryOrdersByDate(
            @P("用户名") String username,
            @P("开始日期，格式 yyyy-MM-dd HH:mm:ss") String startDate,
            @P("结束日期，格式 yyyy-MM-dd HH:mm:ss") String endDate) {
        log.info("调用工具: queryOrdersByDate(username={}, start={}, end={})", username, startDate, endDate);
        LocalDateTime start = LocalDateTime.parse(startDate, FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endDate, FORMATTER);
        return orderRepository.findByUsernameAndOrderTimeBetween(username, start, end).stream()
                .map(AssistantTools::toSummary)
                .toList();
    }

    @Tool("查询销量最高的商品")
    public BestSellingProduct bestSellingProduct() {
        log.info("调用工具: bestSellingProduct()");
        List<Object[]> result = orderRepository.findBestSellingProduct();
        if (result.isEmpty()) {
            // 无数据时返回空语义（product=null），由模型组织"暂无销售数据"的话术
            return new BestSellingProduct(null, 0);
        }
        Object[] row = result.get(0);
        return new BestSellingProduct((String) row[0], (Long) row[1]);
    }

    private static OrderSummary toSummary(Order o) {
        return new OrderSummary(o.getProduct(), o.getAmount(), o.getOrderTime().format(FORMATTER));
    }
}

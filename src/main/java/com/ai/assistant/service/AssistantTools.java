package com.ai.assistant.service;

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
import java.util.stream.Collectors;

/**
 * AI 工具集
 * 通过 @Tool 注解暴露给 LLM，LLM 会根据用户意图自动选择调用哪个工具
 * 这就是 Function Calling 机制
 */
@Component
public class AssistantTools {

    private static final Logger log = LoggerFactory.getLogger(AssistantTools.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderRepository orderRepository;

    public AssistantTools(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Tool("查询指定城市的当前温度")
    public double getTemperature(@P("城市名称") String city) {
        log.info("调用工具: getTemperature(city={})", city);
        return 22.5;
    }

    @Tool("根据用户名查询该用户的所有订单")
    public String queryOrders(@P("用户名") String username) {
        log.info("调用工具: queryOrders(username={})", username);
        List<Order> orders = orderRepository.findByUsername(username);
        if (orders.isEmpty()) {
            return "用户 " + username + " 暂无订单。";
        }
        return orders.stream()
                .map(o -> o.getProduct() + " 价格：" + o.getAmount() + "元")
                .collect(Collectors.joining("\n"));
    }

    @Tool("查询用户的历史总消费金额")
    public String queryTotalSpent(@P("用户名") String username) {
        log.info("调用工具: queryTotalSpent(username={})", username);
        BigDecimal total = orderRepository.getTotalAmountByUsername(username);
        if (total == null) {
            return "用户 " + username + " 暂无消费记录";
        }
        return "用户 " + username + " 的历史总消费金额为：" + total + " 元";
    }

    @Tool("查询用户在指定时间段内的订单，日期格式为 yyyy-MM-dd HH:mm:ss")
    public String queryOrdersByDate(
            @P("用户名") String username,
            @P("开始日期，格式 yyyy-MM-dd HH:mm:ss") String startDate,
            @P("结束日期，格式 yyyy-MM-dd HH:mm:ss") String endDate) {
        log.info("调用工具: queryOrdersByDate(username={}, start={}, end={})", username, startDate, endDate);
        LocalDateTime start = LocalDateTime.parse(startDate, FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endDate, FORMATTER);
        List<Order> orders = orderRepository.findByUsernameAndOrderTimeBetween(username, start, end);
        if (orders.isEmpty()) {
            return "用户 " + username + " 在 " + startDate + " 至 " + endDate + " 期间没有订单。";
        }
        return orders.stream()
                .map(o -> o.getProduct() + " 价格：" + o.getAmount() + "元，时间：" + o.getOrderTime().format(FORMATTER))
                .collect(Collectors.joining("\n"));
    }

    @Tool("查询销量最高的商品")
    public String bestSellingProduct() {
        log.info("调用工具: bestSellingProduct()");
        List<Object[]> result = orderRepository.findBestSellingProduct();
        if (result.isEmpty()) {
            return "暂无销售数据。";
        }
        Object[] row = result.get(0);
        String product = (String) row[0];
        Long count = (Long) row[1];
        return "目前销量最高的商品是：" + product + "，共售出 " + count + " 件。";
    }
}

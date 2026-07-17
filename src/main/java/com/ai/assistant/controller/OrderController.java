package com.ai.assistant.controller;

import com.ai.assistant.common.BusinessException;
import com.ai.assistant.common.Result;
import com.ai.assistant.dto.OrderRequest;
import com.ai.assistant.model.Order;
import com.ai.assistant.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理接口
 * 提供订单的 CRUD 操作
 */
@Tag(name = "订单管理", description = "订单增删改查接口")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Operation(summary = "查询所有订单")
    @GetMapping
    public Result<List<Order>> list() {
        return Result.success(orderRepository.findAll());
    }

    @Operation(summary = "根据 ID 查询订单")
    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "订单不存在，ID: " + id));
        return Result.success(order);
    }

    @Operation(summary = "根据用户名查询订单")
    @GetMapping("/user/{username}")
    public Result<List<Order>> getByUsername(@PathVariable String username) {
        return Result.success(orderRepository.findByUsername(username));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<Order> create(@Valid @RequestBody OrderRequest request) {
        Order order = new Order(
                request.getUsername(),
                request.getProduct(),
                request.getAmount()
        );
        return Result.success(orderRepository.save(order));
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!orderRepository.existsById(id)) {
            throw new BusinessException(404, "订单不存在，ID: " + id);
        }
        orderRepository.deleteById(id);
        return Result.success();
    }
}

package com.ai.assistant.repository;

import com.ai.assistant.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单数据访问层
 * 继承 JpaRepository 自动获得 CRUD 方法
 * 通过方法命名约定或 @Query 自定义查询
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据用户名查询订单（Spring Data 方法命名查询，自动生成 SQL）
     */
    List<Order> findByUsername(String username);

    /**
     * 查询某用户在某时间段内的订单
     * Between 关键字自动翻译为 SQL 的 BETWEEN ... AND ...
     */
    List<Order> findByUsernameAndOrderTimeBetween(String username, LocalDateTime start, LocalDateTime end);

    /**
     * 计算某用户的订单总金额（JPQL 查询）
     */
    @Query("SELECT SUM(o.amount) FROM Order o WHERE o.username = :username")
    BigDecimal getTotalAmountByUsername(@Param("username") String username);

    /**
     * 统计某用户的订单数（Spring Data 方法命名派生查询，自动生成 COUNT SQL）
     */
    long countByUsername(String username);

    /**
     * 查询销量最高的商品（原生 SQL 查询）
     */
    @Query(value = "SELECT product, COUNT(*) as cnt FROM orders GROUP BY product ORDER BY cnt DESC LIMIT 1", nativeQuery = true)
    List<Object[]> findBestSellingProduct();
}

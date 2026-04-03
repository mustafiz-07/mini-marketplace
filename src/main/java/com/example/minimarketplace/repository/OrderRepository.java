package com.example.minimarketplace.repository;

import com.example.minimarketplace.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByOrderDateDesc(Long buyerId);

    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.buyerId = :buyerId")
    List<Order> findByBuyerIdWithItems(@Param("buyerId") Long buyerId);

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.items i
            WHERE EXISTS (
                SELECT 1
                FROM Product p
                WHERE p.id = i.productId
                  AND p.sellerId = :sellerId
            )
            ORDER BY o.orderDate DESC
            """)
    List<Order> findOrdersForSellerWithItems(@Param("sellerId") Long sellerId);
}

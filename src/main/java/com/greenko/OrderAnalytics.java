package com.greenko;

import com.greenko.model.Order;
import com.greenko.model.OrderStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.greenko.util.OrderLoader.loadOrders;

public class OrderAnalytics {
//  1.  Total revenue for DELIVERED orders only
    public static double totalDeliveredRevenue(List<Order> inputData){
        return inputData.stream()
                .filter(o -> o.getStatus().equals(OrderStatus.DELIVERED))
                .mapToDouble(Order::getAmount)
                .sum();
    }

    public static Map<String, Double> revenuePerCity(List<Order> inputData){
        return inputData.stream()
                .filter(o -> o.getStatus().equals(OrderStatus.DELIVERED))
                .collect(Collectors.groupingBy(
                        Order::getCity,
                        Collectors.summingDouble(Order::getAmount)
                ));
    }

    public static Map<String, Double> top3CategoriesByRevenue(List<Order> inputData){
        return inputData.stream()
                .filter(o -> o.getStatus().equals(OrderStatus.DELIVERED))
                .collect(Collectors.groupingBy(
                        Order::getCategory,
                        Collectors.summingDouble(Order::getAmount)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new   // keeps descending order
                ));
    }

    public static void computeAnalyticsParallel() {
        List<Order> csvData;
        try {
            csvData = loadOrders("orders.csv");
        } catch (Exception e){
            throw new RuntimeException("CSV Data loading error: "+e.getMessage());
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);

        CompletableFuture<Double> totalRevenue =
                CompletableFuture.supplyAsync(
                        () -> OrderAnalytics.totalDeliveredRevenue(csvData), executor);

        CompletableFuture<Map<String, Double>> revenuePerCity =
                CompletableFuture.supplyAsync(
                        () -> OrderAnalytics.revenuePerCity(csvData), executor);

        CompletableFuture<Map<String, Double>> topCategories =
                CompletableFuture.supplyAsync(
                        () -> OrderAnalytics.top3CategoriesByRevenue(csvData), executor);

        CompletableFuture.allOf(totalRevenue, revenuePerCity, topCategories).join();

        System.out.println(totalRevenue.join());
        System.out.println(revenuePerCity.join());
        System.out.println(topCategories.join());

        executor.shutdown();

    }
}

package service;

import model.Product;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ProductStatsService {

    private final ProductService productService = new ProductService();

    public long totalCount() throws SQLException {
        return productService.recuperer().size();
    }

    public long inStockCount() throws SQLException {
        return productService.recuperer().stream()
                .filter(p -> p.getStock() > 0)
                .count();
    }

    public long outOfStockCount() throws SQLException {
        return productService.recuperer().stream()
                .filter(p -> p.getStock() <= 0)
                .count();
    }

    public double averagePrice() throws SQLException {
        return productService.recuperer().stream()
                .mapToDouble(Product::getPrice)
                .average().orElse(0);
    }

    public int totalStock() throws SQLException {
        return productService.recuperer().stream()
                .mapToInt(Product::getStock).sum();
    }

    public double totalInventoryValue() throws SQLException {
        return productService.recuperer().stream()
                .mapToDouble(p -> p.getPrice() * p.getStock()).sum();
    }

    public Map<String, Long> topArtists(int limit) throws SQLException {
        return productService.recuperer().stream()
                .filter(p -> p.getArtistName() != null && !p.getArtistName().isBlank())
                .collect(Collectors.groupingBy(Product::getArtistName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<Product> mostExpensive() throws SQLException {
        return productService.recuperer().stream()
                .max(Comparator.comparingDouble(Product::getPrice));
    }

    public List<Product> getLowStock(int threshold) throws SQLException {
        return productService.recuperer().stream()
                .filter(p -> p.getStock() > 0 && p.getStock() <= threshold)
                .sorted(Comparator.comparingInt(Product::getStock))
                .collect(Collectors.toList());
    }
}

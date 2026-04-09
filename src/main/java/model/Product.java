package model;

import java.time.LocalDateTime;

public class Product {

    private int id;
    private int categoryId;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String image;
    private String artistName;
    private boolean isAvailable;
    private LocalDateTime createdAt;
    private String barcode;

    public Product() {
        this.isAvailable = true;
        this.createdAt = LocalDateTime.now();
    }

    public Product(String name, String description, double price, int stock, String artistName, int categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.artistName = artistName;
        this.categoryId = categoryId;
        this.isAvailable = true;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", artistName='" + artistName + '\'' +
                ", categoryId=" + categoryId +
                ", isAvailable=" + isAvailable +
                '}';
    }
}

package service;

import model.CartItem;
import model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CartService {

    private static CartService instance;
    private final List<CartItem> items = new ArrayList<>();
    private Runnable onChangeCallback;

    private CartService() {}

    public static CartService getInstance() {
        if (instance == null) instance = new CartService();
        return instance;
    }

    public void setOnChange(Runnable callback) { this.onChangeCallback = callback; }

    private void notifyChange() {
        if (onChangeCallback != null) onChangeCallback.run();
    }

    public void addItem(Product product) {
        items.stream()
                .filter(ci -> ci.getProduct().getId() == product.getId())
                .findFirst()
                .ifPresentOrElse(
                        ci -> ci.setQuantity(ci.getQuantity() + 1),
                        () -> items.add(new CartItem(product, 1))
                );
        notifyChange();
    }

    public void removeItem(int productId) {
        items.removeIf(ci -> ci.getProduct().getId() == productId);
        notifyChange();
    }

    public void increaseQty(int productId) {
        items.stream()
                .filter(ci -> ci.getProduct().getId() == productId)
                .findFirst()
                .ifPresent(ci -> ci.setQuantity(ci.getQuantity() + 1));
        notifyChange();
    }

    public void decreaseQty(int productId) {
        CartItem found = items.stream()
                .filter(ci -> ci.getProduct().getId() == productId)
                .findFirst().orElse(null);
        if (found == null) return;
        if (found.getQuantity() > 1) found.setQuantity(found.getQuantity() - 1);
        else items.remove(found);
        notifyChange();
    }

    public List<CartItem> getItems()  { return Collections.unmodifiableList(items); }
    public int    getTotalItems()     { return items.stream().mapToInt(CartItem::getQuantity).sum(); }
    public double getTotal()          { return items.stream().mapToDouble(CartItem::getSubtotal).sum(); }
    public boolean isEmpty()          { return items.isEmpty(); }

    public void clear() {
        items.clear();
        notifyChange();
    }
}

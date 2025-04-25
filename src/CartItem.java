package model;

public class CartItem {
    private String movieId;
    private String title;
    private double price;
    private int quantity;

    public CartItem(String movieId, String title, double price, int quantity) {
        this.movieId = movieId;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters
    public String getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    // Setters
    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Utility method to get total price for this item
    public double getTotalPrice() {
        return this.price * this.quantity;
    }

    // Convenience method to increment quantity
    public void incrementQuantity() {
        this.quantity += 1;
    }

    // Convenience method to decrement quantity (not below 1)
    public void decrementQuantity() {
        if (this.quantity > 1) {
            this.quantity -= 1;
        }
    }
}

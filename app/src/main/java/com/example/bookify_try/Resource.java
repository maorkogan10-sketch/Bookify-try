package com.example.bookify_try;

public class Resource {
    private String name;
    private int capacity; // כמה אנשים יכול להכיל כל משאב
    private int quantity; // כמה יש מכל משאב כזה

    public Resource() {
    }

    public Resource(String name, int capacity, int quantity) {
        this.name = name;
        this.capacity = capacity;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
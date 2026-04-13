package com.example.bookify_try;

public class User {
    private String fullName;
    private String email;
    private String userType; //לקוח או בעל עסק

    public User() {
    }

    public User(String fullName, String email, String userType) {
        this.fullName = fullName;
        this.email = email;
        this.userType = userType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
package com.attentive.androidsdk;

public class UserData {
    private String email;
    private String phone;

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserData setEmail(String email) {
        this.email = email;
        return this;
    }

    public UserData setPhone(String phone) {
        this.phone = phone;
        return this;
    }
}

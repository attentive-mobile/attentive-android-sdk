package com.attentive.androidsdk.internal.network;

import com.attentive.androidsdk.UserIdentifiers;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Metadata {
    private String phone;
    private String email;
    private String source = "msdk";

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void enrichWithIdentifiers(UserIdentifiers userIdentifiers) {
        if (this.getEmail() == null && userIdentifiers.getEmail() != null) {
            this.setEmail(userIdentifiers.getEmail());
        }
        if (this.getPhone() == null && userIdentifiers.getPhone() != null) {
            this.setPhone(userIdentifiers.getPhone());
        }
    }
}

package com.attentive.androidsdk.events;

import com.attentive.androidsdk.ParameterValidation;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CustomEvent extends Event {
    private String type;
    private Map<String, String> properties;

    private CustomEvent(String type, Map<String, String> properties) {
        this.type = type;
        this.properties = properties;
    }

    public static final class Builder {
        private String type;
        private Map<String, String> properties;

        /**
         * @param type The type (aka name) of the CustomEvent e.g. "User Logged In".
         *             The type is case-sensitive - "User Logged In" and "User logged in" are different events.
         * @param properties Any metadata associated with the event.
         *                   Keys and values are case-sensitive.
         */
        public Builder(String type, Map<String, String> properties) {
            ParameterValidation.verifyNotNull(type, "type");
            ParameterValidation.verifyNotNull(properties, "properties");

            this.type = type;
            this.properties = new HashMap<>(properties);
        }

        public CustomEvent build() {
            return new CustomEvent(type, properties);
        }
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}

package com.attentive.androidsdk.events;

import androidx.annotation.Nullable;
import com.attentive.androidsdk.ParameterValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(builder = CustomEvent.Builder.class)
public class CustomEvent extends Event {
    private String type;
    private Map<String, String> properties;

    private CustomEvent(String type, Map<String, String> properties) {
        this.type = type;
        this.properties = properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String type;
        private Map<String, String> properties;

        /**
         * @param type The type (aka name) of the CustomEvent e.g. "User Logged In".
         *             The type is case-sensitive - "User Logged In" and "User logged in" are different events.
         * @param properties Any metadata associated with the event.
         *                   Keys and values are case-sensitive.
         */
        @JsonCreator
        public Builder(@JsonProperty("type") String type, @JsonProperty("properties") Map<String, String> properties) {
            ParameterValidation.verifyNotEmpty(type, "type");
            ParameterValidation.verifyNotNull(properties, "properties");

            String invalidChar = findInvalidCharactersInType(type);
            if (invalidChar != null) {
                throw new IllegalArgumentException(String.format("The 'type' parameter contains an invalid character: '%s'.", invalidChar));
            }

            for (String key : properties.keySet()) {
                String invalidKeyChar = findInvalidCharacterInPropertiesKey(key);
                if (invalidKeyChar != null) {
                    throw new IllegalArgumentException(String.format("The properties key '%s' contains an invalid character: '%s'.", key, invalidKeyChar));
                }
            }

            this.type = type;
            this.properties = new HashMap<>(properties);
        }

        @Nullable
        private String findInvalidCharactersInType(String type) {
            String[] specialCharacters = {"\"", "'", "(", ")", "{", "}", "[", "]", "\\", "|", ","};
            return findInvalidCharacter(type, specialCharacters);
        }

        @Nullable
        private String findInvalidCharacterInPropertiesKey(String key) {
            String[] specialCharacters = {"\"", "{", "}", "[", "]", "\\", "|"};
            return findInvalidCharacter(key, specialCharacters);
        }

        @Nullable
        private String findInvalidCharacter(String subject, String[] chars) {
            for (String character : chars) {
                if (subject.contains(character)) {
                    return character;
                }
            }
            return null;
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

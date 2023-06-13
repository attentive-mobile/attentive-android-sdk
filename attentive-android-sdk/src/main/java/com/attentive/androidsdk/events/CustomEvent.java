package com.attentive.androidsdk.events;

import android.util.Log;
import androidx.annotation.Nullable;
import com.attentive.androidsdk.ParameterValidation;
import java.util.HashMap;
import java.util.Map;

public class CustomEvent extends Event {
    private final String type;
    private final Map<String, String> properties;

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
            this.type = null;
            this.properties = null;

            boolean typeEmpty = !ParameterValidation.verifyNotEmpty(type, "type");
            boolean propsEmpty = !ParameterValidation.verifyNotNull(properties, "properties");
            if (typeEmpty || propsEmpty) {
                return;
            }
            String invalidChar = findInvalidCharactersInType(type);
            if (invalidChar != null) {
                Log.e(this.getClass().getName(), String.format("The 'type' parameter contains an invalid character: '%s'.", invalidChar));
                return;
            }

            for (String key : properties.keySet()) {
                String invalidKeyChar = findInvalidCharacterInPropertiesKey(key);
                if (invalidKeyChar != null) {
                    Log.e(this.getClass().getName(), String.format("The properties key '%s' contains an invalid character: '%s'.", key, invalidKeyChar));
                    return;
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
            if (subject == null) {
                return null;
            }
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

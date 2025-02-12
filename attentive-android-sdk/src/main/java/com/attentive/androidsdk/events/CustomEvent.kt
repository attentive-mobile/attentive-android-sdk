package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder

@JsonDeserialize(builder = CustomEvent.Builder::class)
class CustomEvent private constructor(@JvmField val type: String, @JvmField val properties: Map<String, String>) :
    Event() {
    @JsonPOJOBuilder(withPrefix = "")
    class Builder @JsonCreator constructor(
        @JsonProperty("type") type: String,
        @JsonProperty("properties") properties: Map<String, String>
    ) {
        private val type: String
        private val properties: Map<String, String>

        /**
         * @param type The type (aka name) of the CustomEvent e.g. "User Logged In".
         * The type is case-sensitive - "User Logged In" and "User logged in" are different events.
         * @param properties Any metadata associated with the event.
         * Keys and values are case-sensitive.
         */
        init {
            ParameterValidation.verifyNotEmpty(type, "type")
            ParameterValidation.verifyNotNull(properties, "properties")

            val invalidChar = findInvalidCharactersInType(type)
            require(invalidChar == null) {
                String.format(
                    "The 'type' parameter contains an invalid character: '%s'.",
                    invalidChar
                )
            }

            for (key in properties.keys) {
                val invalidKeyChar = findInvalidCharacterInPropertiesKey(key)
                require(invalidKeyChar == null) {
                    String.format(
                        "The properties key '%s' contains an invalid character: '%s'.",
                        key,
                        invalidKeyChar
                    )
                }
            }

            this.type = type
            this.properties = HashMap(properties)
        }

        private fun findInvalidCharactersInType(type: String): String? {
            val specialCharacters = arrayOf("\"", "'", "(", ")", "{", "}", "[", "]", "\\", "|", ",")
            return findInvalidCharacter(type, specialCharacters)
        }

        private fun findInvalidCharacterInPropertiesKey(key: String): String? {
            val specialCharacters = arrayOf("\"", "{", "}", "[", "]", "\\", "|")
            return findInvalidCharacter(key, specialCharacters)
        }

        private fun findInvalidCharacter(subject: String, chars: Array<String>): String? {
            for (character in chars) {
                if (subject.contains(character)) {
                    return character
                }
            }
            return null
        }

        fun build(): CustomEvent {
            return CustomEvent(type, properties)
        }
    }
}

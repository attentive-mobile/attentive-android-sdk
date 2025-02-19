package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

@Serializable
data class CustomEvent(
    val type: String,
    val properties: Map<String, String>
) : Event() {

    init {
        ParameterValidation.verifyNotEmpty(type, "type")
        ParameterValidation.verifyNotNull(properties, "properties")

        val invalidChar = findInvalidCharactersInType(type)
        if (invalidChar != null) {
            throw IllegalArgumentException(
                String.format(
                    "The 'type' parameter contains an invalid character: '%s'.",
                    invalidChar
                )
            )
        }

        for (key in properties.keys) {
            val invalidKeyChar = findInvalidCharacterInPropertiesKey(key)
            if (invalidKeyChar != null) {
                throw IllegalArgumentException(
                    String.format(
                        "The properties key '%s' contains an invalid character: '%s'.",
                        key,
                        invalidKeyChar
                    )
                )
            }
        }
    }

    @Serializable
    class Builder(
        private var type: String? = null,
        private var properties: Map<String, String> = emptyMap()
    ) {

        /**
         * @param type The type (aka name) of the CustomEvent e.g. "User Logged In".
         * The type is case-sensitive - "User Logged In" and "User logged in" are different events.
         * @param properties Any metadata associated with the event.
         * Keys and values are case-sensitive.
         */
        fun type(type: String): Builder {
            this.type = type
            return this
        }

        fun properties(properties: Map<String, String>): Builder {
            this.properties = properties
            return this
        }

        fun buildIt(): CustomEvent {
            val type = this.type ?: throw IllegalStateException("Type must be set")
            return CustomEvent(type, properties)
        }
    }

    private companion object {
        fun findInvalidCharactersInType(type: String): String? {
            val specialCharacters = arrayOf("\"", "'", "(", ")", "{", "}", "[", "]", "\\", "|", ",")
            return findInvalidCharacter(type, specialCharacters)
        }

        fun findInvalidCharacterInPropertiesKey(key: String): String? {
            val specialCharacters = arrayOf("\"", "{", "}", "[", "]", "\\", "|")
            return findInvalidCharacter(key, specialCharacters)
        }

        fun findInvalidCharacter(subject: String, chars: Array<String>): String? {
            for (character in chars) {
                if (subject.contains(character)) {
                    return character
                }
            }
            return null
        }
    }
}
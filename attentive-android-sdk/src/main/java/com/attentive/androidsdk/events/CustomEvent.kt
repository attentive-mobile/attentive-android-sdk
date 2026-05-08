package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

/**
 * A custom, app-specific event. Use this when no built-in event type matches your use case.
 *
 * Each custom event has a [type] (e.g. `"User Logged In"`) and a map of string [properties]
 * for any associated metadata.
 *
 * @property type The type/name of the event. Case-sensitive. Must not contain any of:
 *   `"`, `'`, `(`, `)`, `{`, `}`, `[`, `]`, `\`, `|`, `,`.
 * @property properties Metadata associated with the event. Keys and values are case-sensitive.
 *   Keys must not contain any of: `"`, `{`, `}`, `[`, `]`, `\`, `|`.
 */
@Serializable
data class CustomEvent(
    val type: String,
    val properties: Map<String, String>,
) : Event() {
    init {
        ParameterValidation.verifyNotEmpty(type, "type")
        ParameterValidation.verifyNotNull(properties, "properties")

        val invalidChar = findInvalidCharactersInType(type)
        if (invalidChar != null) {
            throw IllegalArgumentException(
                String.format(
                    "The 'type' parameter contains an invalid character: '%s'.",
                    invalidChar,
                ),
            )
        }

        for (key in properties.keys) {
            val invalidKeyChar = findInvalidCharacterInPropertiesKey(key)
            if (invalidKeyChar != null) {
                throw IllegalArgumentException(
                    String.format(
                        "The properties key '%s' contains an invalid character: '%s'.",
                        key,
                        invalidKeyChar,
                    ),
                )
            }
        }
    }

    /**
     * Builder for [CustomEvent].
     */
    @Serializable
    class Builder(
        private var type: String? = null,
        private var properties: Map<String, String> = emptyMap(),
    ) {
        /**
         * Sets the event type (aka name). Required. The type is case-sensitive —
         * `"User Logged In"` and `"User logged in"` are different events.
         *
         * @param type The event name.
         */
        fun type(type: String): Builder {
            this.type = type
            return this
        }

        /**
         * Sets the event properties. Keys and values are case-sensitive.
         *
         * @param properties Event metadata.
         */
        fun properties(properties: Map<String, String>): Builder {
            this.properties = properties
            return this
        }

        /**
         * Builds the [CustomEvent].
         *
         * @throws IllegalStateException if [type] was not set.
         * @throws IllegalArgumentException if [type] or any property key contains an invalid character.
         */
        fun build(): CustomEvent {
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

        fun findInvalidCharacter(
            subject: String,
            chars: Array<String>,
        ): String? {
            for (character in chars) {
                if (subject.contains(character)) {
                    return character
                }
            }
            return null
        }
    }
}

package com.attentive.androidsdk.internal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.StdConverter;

// Some of our DTOs need to serialize nested objects into strings
//
// For example:
// Instead of this: {"key": ["one", "two"]}
//    We want this: {"key": "[\"one\", \"two\"]"}
public class ObjectToRawJsonStringConverter extends StdConverter<Object, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convert(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
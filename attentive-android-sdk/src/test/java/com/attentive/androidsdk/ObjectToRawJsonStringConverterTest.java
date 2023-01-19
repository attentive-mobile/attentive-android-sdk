package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;

import com.attentive.androidsdk.internal.util.ObjectToRawJsonStringConverter;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ObjectToRawJsonStringConverterTest {

    private ObjectToRawJsonStringConverter objectToRawJsonStringConverter;

    @Before
    public void setup() {
        objectToRawJsonStringConverter = new ObjectToRawJsonStringConverter();
    }

    @Test
    public void convert_nestedObject_serializesToValidJsonString(){
        Object objectToConvert = Map.of("key1", List.of("listItem1", "listItem2"));

        String convertedObject = objectToRawJsonStringConverter.convert(objectToConvert);

        assertEquals("{\"key1\":[\"listItem1\",\"listItem2\"]}", convertedObject);
    }

    @Test
    public void convert_list_serializesToValidJsonString(){
        Object objectToConvert = List.of("listItem1", "listItem2");

        String convertedObject = objectToRawJsonStringConverter.convert(objectToConvert);

        assertEquals("[\"listItem1\",\"listItem2\"]", convertedObject);
    }
}

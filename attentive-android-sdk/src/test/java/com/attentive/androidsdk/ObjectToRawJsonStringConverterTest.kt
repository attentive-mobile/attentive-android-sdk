package com.attentive.androidsdk

import com.attentive.androidsdk.internal.util.ObjectToRawJsonStringConverter
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.Map

class ObjectToRawJsonStringConverterTest {



    @Before
    fun setup() {
    }

    @Test
    fun convert_nestedObject_serializesToValidJsonString() {
        val objectToConvert = Map.of("key1", listOf("listItem1", "listItem2"))

        val convertedObject = Json.encodeToString(objectToConvert)

        Assert.assertEquals("{\"key1\":[\"listItem1\",\"listItem2\"]}", convertedObject)
    }

    @Test
    fun convert_list_serializesToValidJsonString() {
        val objectToConvert = listOf("listItem1", "listItem2")

        val convertedObject = Json.encodeToString(objectToConvert)

        Assert.assertEquals("[\"listItem1\",\"listItem2\"]", convertedObject)
    }
}

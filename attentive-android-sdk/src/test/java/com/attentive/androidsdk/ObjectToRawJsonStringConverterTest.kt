package com.attentive.androidsdk

import com.attentive.androidsdk.internal.util.ObjectToRawJsonStringConverter
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.Map

class ObjectToRawJsonStringConverterTest {
    private var objectToRawJsonStringConverter: ObjectToRawJsonStringConverter? = null

    @Before
    fun setup() {
        objectToRawJsonStringConverter = ObjectToRawJsonStringConverter()
    }

    @Test
    fun convert_nestedObject_serializesToValidJsonString() {
        val objectToConvert: Any = Map.of("key1", listOf("listItem1", "listItem2"))

        val convertedObject = objectToRawJsonStringConverter!!.convert(objectToConvert)

        Assert.assertEquals("{\"key1\":[\"listItem1\",\"listItem2\"]}", convertedObject)
    }

    @Test
    fun convert_list_serializesToValidJsonString() {
        val objectToConvert: Any = listOf("listItem1", "listItem2")

        val convertedObject = objectToRawJsonStringConverter!!.convert(objectToConvert)

        Assert.assertEquals("[\"listItem1\",\"listItem2\"]", convertedObject)
    }
}

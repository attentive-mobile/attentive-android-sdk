package com.attentive.androidsdk.internal.network;

import com.attentive.androidsdk.internal.util.ObjectToRawJsonStringConverter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;

public class CustomEventMetadataDto extends Metadata {
    private String type;

    /**
     * We expect this map of properties to be a string containing a valid json map rather than a List object
     * For example:
     *      Instead of this: {"properties": { "Color": "Blue" }}
     *      We want this:    {"properties": "{ \"Color\": \"Blue\" }" }
     **/
    @JsonSerialize(converter = ObjectToRawJsonStringConverter.class)
    private Map<String, String> properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}

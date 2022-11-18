package com.attentive.androidsdk;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class VisitorServiceTest {

    @Test
    public void generateVisitorId() {
        PersistentStorage mock = mock(PersistentStorage.class);
        VisitorService visitorService = new VisitorService(mock);
    }
}
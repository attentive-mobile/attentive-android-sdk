package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VisitorServiceTest {
    private PersistentStorage persistentStorage;
    private VisitorService visitorService;

    @Before
    public void setup() {
       persistentStorage = new PersistentStorage(InstrumentationRegistry.getInstrumentation().getTargetContext());
       // clear the storage before each test
       persistentStorage.deleteAll();
       visitorService = new VisitorService(persistentStorage);
    }

    @Test
    public void getVisitorId_firstTime_getsNewVisitorId() {
        // Arrange
        // Act
        String visitorId = visitorService.getVisitorId();

        // Assert
        verifyVisitorId(visitorId);
    }

    @Test
    public void getVisitorId_multipleTimes_getsSameVisitorId() {
        // Arrange
        // Act
        String visitorIdFromFirstCall = visitorService.getVisitorId();
        String visitorIdFromSecondCall = visitorService.getVisitorId();

        // Assert
        assertEquals(visitorIdFromFirstCall, visitorIdFromSecondCall);
        verifyVisitorId(visitorIdFromFirstCall);
    }

    @Test
    public void createNewVisitorId_afterVisitorIdAlreadyCreated_createsAnotherVisitorId() {
        // Arrange
        // Act
        String oldVisitorId = visitorService.getVisitorId();
        String newVisitorId = visitorService.createNewVisitorId();

        // Assert
        assertNotEquals(oldVisitorId, newVisitorId);
        verifyVisitorId(oldVisitorId);
        verifyVisitorId(newVisitorId);
    }

    @Test
    public void createNewVisitorId_noVisitorIdRetrievedYet_createsAValidVisitorId() {
        // Arrange
        // Act
        String visitorId = visitorService.createNewVisitorId();

        // Assert
        verifyVisitorId(visitorId);
    }

    private void verifyVisitorId(String visitorId) {
        assertEquals(32, visitorId.length());
        assertEquals('4', visitorId.charAt(12));
    }
}

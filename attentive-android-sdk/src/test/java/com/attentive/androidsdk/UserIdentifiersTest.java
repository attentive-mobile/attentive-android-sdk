package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Map;


public class UserIdentifiersTest {

    @Test
    public void merge_withNewIdentifiers_identifiersAreMerged() {
        // Arrange
        UserIdentifiers userIdentifiers = buildUserIdentifiers();
        
        UserIdentifiers newUserIdentifiers = new UserIdentifiers.Builder()
                .withClientUserId("newClientId")
                .withPhone("+14158889999")
                .withEmail("newEmail@gmail.com")
                .withShopifyId("67890")
                .withKlaviyoId("09876")
                .withCustomIdentifiers(Map.of("key1", "newValue1", "extraKey", "extraValue"))
                .build();
        
        // Act
        UserIdentifiers mergedUserIdentifiers = UserIdentifiers.merge(userIdentifiers, newUserIdentifiers);

        // Assert
        assertEquals("newClientId", mergedUserIdentifiers.getClientUserId());
        assertEquals("+14158889999", mergedUserIdentifiers.getPhone());
        assertEquals("newEmail@gmail.com", mergedUserIdentifiers.getEmail());
        assertEquals("67890", mergedUserIdentifiers.getShopifyId());
        assertEquals("09876", mergedUserIdentifiers.getKlaviyoId());
        assertEquals(
                Map.of("key1", "newValue1", "key2", "value2", "extraKey", "extraValue"),
                mergedUserIdentifiers.getCustomIdentifiers());
    }

    @Test
    public void merge_withNullIdentifiers_newIdentifiersSet() {
        // Arrange
        UserIdentifiers userIdentifiers = new UserIdentifiers.Builder().build();
        UserIdentifiers newUserIdentifiers = buildUserIdentifiers();

        // Act
        UserIdentifiers mergedUserIdentifiers = UserIdentifiers.merge(userIdentifiers, newUserIdentifiers);

        // Assert
        verifyUserIdentifiers(mergedUserIdentifiers);
    }

    @Test
    public void merge_withNullNewIdentifiers_keepOldIdentifiers() {
        // Arrange
        UserIdentifiers userIdentifiers = buildUserIdentifiers();
        UserIdentifiers newUserIdentifiers = new UserIdentifiers.Builder().build();

        // Act
        UserIdentifiers mergedUserIdentifiers = UserIdentifiers.merge(userIdentifiers, newUserIdentifiers);

        // Assert
        verifyUserIdentifiers(mergedUserIdentifiers);
    }

    private void verifyUserIdentifiers(UserIdentifiers userIdentifiersToVerify) {
        assertEquals("clientId", userIdentifiersToVerify.getClientUserId());
        assertEquals("+14156667777", userIdentifiersToVerify.getPhone());
        assertEquals("email@gmail.com", userIdentifiersToVerify.getEmail());
        assertEquals("12345", userIdentifiersToVerify.getShopifyId());
        assertEquals("54321", userIdentifiersToVerify.getKlaviyoId());
        assertEquals(
                Map.of("key1", "value1", "key2", "value2"),
                userIdentifiersToVerify.getCustomIdentifiers());
    }

    private UserIdentifiers buildUserIdentifiers() {
        return new UserIdentifiers.Builder()
                .withClientUserId("clientId")
                .withPhone("+14156667777")
                .withEmail("email@gmail.com")
                .withShopifyId("12345")
                .withKlaviyoId("54321")
                .withCustomIdentifiers(Map.of("key1", "value1", "key2", "value2"))
                .build();
    }
}

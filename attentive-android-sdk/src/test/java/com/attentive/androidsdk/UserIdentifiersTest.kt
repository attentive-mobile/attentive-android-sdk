package com.attentive.androidsdk

import org.junit.Assert
import org.junit.Test
import java.util.Map

class UserIdentifiersTest {
    @Test
    fun merge_withNewIdentifiers_identifiersAreMerged() {
        // Arrange
        val userIdentifiers = buildUserIdentifiers()

        val newUserIdentifiers = UserIdentifiers.Builder()
            .withClientUserId("newClientId")
            .withPhone("+14158889999")
            .withEmail("newEmail@gmail.com")
            .withShopifyId("67890")
            .withKlaviyoId("09876")
            .withCustomIdentifiers(Map.of("key1", "newValue1", "extraKey", "extraValue"))
            .build()


        // Act
        val mergedUserIdentifiers = UserIdentifiers.merge(userIdentifiers, newUserIdentifiers)

        // Assert
        Assert.assertEquals("newClientId", mergedUserIdentifiers.clientUserId)
        Assert.assertEquals("+14158889999", mergedUserIdentifiers.phone)
        Assert.assertEquals("newEmail@gmail.com", mergedUserIdentifiers.email)
        Assert.assertEquals("67890", mergedUserIdentifiers.shopifyId)
        Assert.assertEquals("09876", mergedUserIdentifiers.klaviyoId)
        Assert.assertEquals(
            Map.of("key1", "newValue1", "key2", "value2", "extraKey", "extraValue"),
            mergedUserIdentifiers.customIdentifiers
        )
    }

    @Test
    fun merge_withNullIdentifiers_newIdentifiersSet() {
        // Arrange
        val userIdentifiers = UserIdentifiers.Builder().build()
        val newUserIdentifiers = buildUserIdentifiers()

        // Act
        val mergedUserIdentifiers = UserIdentifiers.merge(userIdentifiers, newUserIdentifiers)

        // Assert
        verifyUserIdentifiers(mergedUserIdentifiers)
    }

    @Test
    fun merge_withNullNewIdentifiers_keepOldIdentifiers() {
        // Arrange
        val userIdentifiers = buildUserIdentifiers()
        val newUserIdentifiers = UserIdentifiers.Builder().build()

        // Act
        val mergedUserIdentifiers = UserIdentifiers.merge(userIdentifiers, newUserIdentifiers)

        // Assert
        verifyUserIdentifiers(mergedUserIdentifiers)
    }

    @Test
    fun merge_withIncompleteSetOfIdentifiers(){
        val userIdentifiers = UserIdentifiers.Builder().withClientUserId("oldUserId").build()
        val newUserIdentifiers = UserIdentifiers.Builder().withClientUserId("newUserId").build()

        val mergedUserIdentifiers = UserIdentifiers.merge(userIdentifiers, newUserIdentifiers)
        Assert.assertEquals("newUserId", mergedUserIdentifiers.clientUserId)
    }

    private fun verifyUserIdentifiers(userIdentifiersToVerify: UserIdentifiers) {
        Assert.assertEquals("clientId", userIdentifiersToVerify.clientUserId)
        Assert.assertEquals("+14156667777", userIdentifiersToVerify.phone)
        Assert.assertEquals("email@gmail.com", userIdentifiersToVerify.email)
        Assert.assertEquals("12345", userIdentifiersToVerify.shopifyId)
        Assert.assertEquals("54321", userIdentifiersToVerify.klaviyoId)
        Assert.assertEquals(
            Map.of("key1", "value1", "key2", "value2"),
            userIdentifiersToVerify.customIdentifiers
        )
    }

    private fun buildUserIdentifiers(): UserIdentifiers {
        return UserIdentifiers.Builder()
            .withClientUserId("clientId")
            .withPhone("+14156667777")
            .withEmail("email@gmail.com")
            .withShopifyId("12345")
            .withKlaviyoId("54321")
            .withCustomIdentifiers(Map.of("key1", "value1", "key2", "value2"))
            .build()
    }
}

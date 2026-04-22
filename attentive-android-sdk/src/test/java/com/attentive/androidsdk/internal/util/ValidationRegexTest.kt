package com.attentive.androidsdk.internal.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationRegexTest {
    private val phoneValidator = DefaultPhoneValidator()
    private val emailValidator = DefaultEmailValidator()

    // Phone number validation - E.164 format

    @Test
    fun phone_validE164_returnsTrue() {
        assertTrue(phoneValidator.isPhoneNumber("+14155552671"))
        assertTrue(phoneValidator.isPhoneNumber("+442071234567"))
        assertTrue(phoneValidator.isPhoneNumber("+61291234567"))
        assertTrue(phoneValidator.isPhoneNumber("+8613800138000"))
        assertTrue(phoneValidator.isPhoneNumber("+12"))
        assertTrue(phoneValidator.isPhoneNumber("+123456789012345"))
    }

    @Test
    fun phone_missingPlusPrefix_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("14155552671"))
        assertFalse(phoneValidator.isPhoneNumber("4155552671"))
    }

    @Test
    fun phone_startsWithZero_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("+0155552671"))
    }

    @Test
    fun phone_tooLong_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("+1234567890123456"))
    }

    @Test
    fun phone_empty_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber(""))
    }

    @Test
    fun phone_containsLetters_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("+1415555abc1"))
    }

    @Test
    fun phone_containsSpaces_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("+1 415 555 2671"))
    }

    @Test
    fun phone_containsDashes_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("+1-415-555-2671"))
    }

    @Test
    fun phone_containsParentheses_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("+1(415)5552671"))
    }

    @Test
    fun phone_emailInPhoneField_returnsFalse() {
        assertFalse(phoneValidator.isPhoneNumber("user@example.com"))
    }

    // Email validation

    @Test
    fun email_validStandard_returnsTrue() {
        assertTrue(emailValidator.isEmail("user@example.com"))
        assertTrue(emailValidator.isEmail("test.user@example.com"))
        assertTrue(emailValidator.isEmail("test+tag@example.com"))
        assertTrue(emailValidator.isEmail("user@sub.domain.com"))
        assertTrue(emailValidator.isEmail("user@example.co.uk"))
        assertTrue(emailValidator.isEmail("user123@example.com"))
        assertTrue(emailValidator.isEmail("first-last@example.com"))
        assertTrue(emailValidator.isEmail("user_name@example.com"))
        assertTrue(emailValidator.isEmail("user'tag@example.com"))
    }

    @Test
    fun email_validMinimal_returnsTrue() {
        assertTrue(emailValidator.isEmail("a@b.co"))
    }

    @Test
    fun email_missingAt_returnsFalse() {
        assertFalse(emailValidator.isEmail("userexample.com"))
    }

    @Test
    fun email_missingDomain_returnsFalse() {
        assertFalse(emailValidator.isEmail("user@"))
    }

    @Test
    fun email_missingLocal_returnsFalse() {
        assertFalse(emailValidator.isEmail("@example.com"))
    }

    @Test
    fun email_missingTld_returnsFalse() {
        assertFalse(emailValidator.isEmail("user@example"))
    }

    @Test
    fun email_singleCharTld_returnsFalse() {
        assertFalse(emailValidator.isEmail("user@example.c"))
    }

    @Test
    fun email_leadingDot_returnsFalse() {
        assertFalse(emailValidator.isEmail(".user@example.com"))
    }

    @Test
    fun email_consecutiveDots_returnsFalse() {
        assertFalse(emailValidator.isEmail("user..name@example.com"))
    }

    @Test
    fun email_empty_returnsFalse() {
        assertFalse(emailValidator.isEmail(""))
    }

    @Test
    fun email_phoneInEmailField_returnsFalse() {
        assertFalse(emailValidator.isEmail("+14155552671"))
    }

    @Test
    fun email_domainLabelEndingWithHyphen_returnsFalse() {
        assertFalse(emailValidator.isEmail("user@example-.com"))
        assertFalse(emailValidator.isEmail("user@e-.co"))
    }

    @Test
    fun email_domainLabelWithInternalHyphen_returnsTrue() {
        assertTrue(emailValidator.isEmail("user@my-domain.com"))
        assertTrue(emailValidator.isEmail("user@sub-domain.example.com"))
    }

    @Test
    fun email_singleCharDomainLabel_returnsTrue() {
        assertTrue(emailValidator.isEmail("user@a.com"))
    }

    @Test
    fun email_spacesInEmail_returnsFalse() {
        assertFalse(emailValidator.isEmail("user @example.com"))
        assertFalse(emailValidator.isEmail("user@ example.com"))
    }

    // Extension function tests

    @Test
    fun extensionFunction_isPhoneNumber_works() {
        assertTrue("+14155552671".isPhoneNumber())
        assertFalse("not-a-phone".isPhoneNumber())
    }

    @Test
    fun extensionFunction_isEmail_works() {
        assertTrue("user@example.com".isEmail())
        assertFalse("not-an-email".isEmail())
    }
}

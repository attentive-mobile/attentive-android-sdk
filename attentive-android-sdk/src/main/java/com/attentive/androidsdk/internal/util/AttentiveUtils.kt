package com.attentive.androidsdk.internal.util


object AttentiveUtils {

    var phoneValidator: PhoneValidator = DefaultPhoneValidator()
}

interface PhoneValidator {
    fun isPhoneNumber(phone: String): Boolean
}

class DefaultPhoneValidator : PhoneValidator {
    override fun isPhoneNumber(phone: String): Boolean {
        return android.telephony.PhoneNumberUtils.isGlobalPhoneNumber(phone)
    }
}

fun String.isPhoneNumber(): Boolean = AttentiveUtils.phoneValidator.isPhoneNumber(this)


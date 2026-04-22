package com.attentive.androidsdk.internal.util

object AttentiveUtils {
    var phoneValidator: PhoneValidator = DefaultPhoneValidator()
    var emailValidator: EmailValidator = DefaultEmailValidator()
}

interface PhoneValidator {
    fun isPhoneNumber(phone: String): Boolean
}

interface EmailValidator {
    fun isEmail(email: String): Boolean
}

class DefaultPhoneValidator : PhoneValidator {
    private val e164Regex = Regex("""^\+[1-9]\d{1,14}$""")

    override fun isPhoneNumber(phone: String): Boolean {
        return e164Regex.matches(phone)
    }
}

class DefaultEmailValidator : EmailValidator {
    private val emailRegex = Regex(
        """^(?!\.)(?!.*\.\.)([a-zA-Z0-9_'+\-.]*)[a-zA-Z0-9_'+\-]@([a-zA-Z0-9][a-zA-Z0-9\-]*\.)+[a-zA-Z]{2,}$""",
    )

    override fun isEmail(email: String): Boolean {
        return emailRegex.matches(email)
    }
}

fun String.isPhoneNumber(): Boolean = AttentiveUtils.phoneValidator.isPhoneNumber(this)

fun String.isEmail(): Boolean = AttentiveUtils.emailValidator.isEmail(this)

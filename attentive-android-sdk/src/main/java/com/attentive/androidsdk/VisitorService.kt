package com.attentive.androidsdk

import java.util.Random

class VisitorService(private val persistentStorage: PersistentStorage) {
    val visitorId: String
        get() {
            val existingVisitorId =
                persistentStorage.read(VISITOR_ID_KEY)
            return existingVisitorId ?: createNewVisitorId()
        }

    fun createNewVisitorId(): String {
        val newVisitorId = generateVisitorId()
        persistentStorage.save(VISITOR_ID_KEY, newVisitorId)
        return newVisitorId
    }

    companion object {
        private const val VISITOR_ID_KEY = "visitorId"

        private fun generateVisitorId(): String {
            // This generation logic matches the generation logic of the tag's visitor id creation code on the backend
            val builder = StringBuilder()
            val rand = Random()
            var d = System.currentTimeMillis()
            val format = "xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx"
            for (element in format) {
                if (element == '4') {
                    builder.append('4')
                    continue
                }

                val r = ((d + rand.nextDouble() * 16) % 16).toLong()
                d /= 16
                builder.append(
                    java.lang.Long.toHexString(
                        if (element == 'x')
                            r
                        else
                            (r and 0x3L) or 0x8L
                    )
                )
            }
            return builder.toString()
        }
    }
}

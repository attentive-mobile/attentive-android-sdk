package com.attentive.androidsdk

import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.ClassFactory.buildAttentiveApi
import com.attentive.androidsdk.ClassFactory.buildOkHttpClient
import com.attentive.androidsdk.ClassFactory.buildPersistentStorage
import com.attentive.androidsdk.ClassFactory.buildVisitorService
import com.attentive.androidsdk.PersistentStorage
import com.attentive.androidsdk.VisitorService
import okhttp3.OkHttpClient
import org.mockito.ArgumentMatchers
import org.mockito.MockedStatic
import org.mockito.MockedStatic.Verification
import org.mockito.Mockito
import org.mockito.kotlin.any

class FactoryMocks private constructor(
    private val classFactoryMockedStatic: MockedStatic<ClassFactory>,
    val persistentStorage: PersistentStorage,
    val visitorService: VisitorService, val okHttpClient: OkHttpClient,
    val attentiveApi: AttentiveApi
) : AutoCloseable {
    override fun close() {
        classFactoryMockedStatic.close()
    }

    companion object {
        fun mockFactoryObjects(): FactoryMocks {
            val classFactoryMockedStatic = Mockito.mockStatic(
                ClassFactory::class.java
            )

            val persistentStorage = Mockito.mock(
                PersistentStorage::class.java
            )
            classFactoryMockedStatic.`when`<Any> {
                buildPersistentStorage(
                    any()
                )
            }.thenReturn(persistentStorage)

            val visitorService = Mockito.mock(VisitorService::class.java)
            classFactoryMockedStatic.`when`<Any> {
                buildVisitorService(
                    any()
                )
            }.thenReturn(visitorService)

            val okHttpClient = Mockito.mock(OkHttpClient::class.java)
            classFactoryMockedStatic.`when`<Any> {
                buildOkHttpClient(
                    any(),
                    any()
                )
            }.thenReturn(okHttpClient)

            val attentiveApi = Mockito.mock(AttentiveApi::class.java)
            classFactoryMockedStatic.`when`<Any> {
                buildAttentiveApi(
                    any()
                )
            }.thenReturn(attentiveApi)

            return FactoryMocks(
                classFactoryMockedStatic,
                persistentStorage,
                visitorService,
                okHttpClient,
                attentiveApi
            )
        }
    }
}

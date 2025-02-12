package com.attentive.androidsdk

import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.ClassFactory.buildAttentiveApi
import com.attentive.androidsdk.ClassFactory.buildObjectMapper
import com.attentive.androidsdk.ClassFactory.buildOkHttpClient
import com.attentive.androidsdk.ClassFactory.buildPersistentStorage
import com.attentive.androidsdk.ClassFactory.buildVisitorService
import com.attentive.androidsdk.PersistentStorage
import com.attentive.androidsdk.VisitorService
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import org.mockito.ArgumentMatchers
import org.mockito.MockedStatic
import org.mockito.MockedStatic.Verification
import org.mockito.Mockito

class FactoryMocks private constructor(
    private val classFactoryMockedStatic: MockedStatic<ClassFactory>,
    val persistentStorage: PersistentStorage,
    val visitorService: VisitorService, val okHttpClient: OkHttpClient,
    val attentiveApi: AttentiveApi, val objectMapper: ObjectMapper
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
                    ArgumentMatchers.any()
                )
            }.thenReturn(persistentStorage)

            val visitorService = Mockito.mock(VisitorService::class.java)
            classFactoryMockedStatic.`when`<Any> {
                buildVisitorService(
                    ArgumentMatchers.any()
                )
            }.thenReturn(visitorService)

            val objectMapper = Mockito.spy(ObjectMapper())
            classFactoryMockedStatic.`when`<Any>(Verification {buildObjectMapper() })
                .thenReturn(objectMapper)

            val okHttpClient = Mockito.mock(OkHttpClient::class.java)
            classFactoryMockedStatic.`when`<Any> {
                buildOkHttpClient(
                    ArgumentMatchers.any()
                )
            }.thenReturn(okHttpClient)

            val attentiveApi = Mockito.mock(AttentiveApi::class.java)
            classFactoryMockedStatic.`when`<Any> {
                buildAttentiveApi(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                )
            }.thenReturn(attentiveApi)

            return FactoryMocks(
                classFactoryMockedStatic,
                persistentStorage,
                visitorService,
                okHttpClient,
                attentiveApi,
                objectMapper
            )
        }
    }
}

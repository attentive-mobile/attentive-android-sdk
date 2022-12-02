package com.attentive.androidsdk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class FactoryMocks implements AutoCloseable {
    private final MockedStatic<ClassFactory> classFactoryMockedStatic;
    private final PersistentStorage persistentStorage;
    private final VisitorService visitorService;
    private final OkHttpClient okHttpClient;
    private final AttentiveApi attentiveApi;
    private final ObjectMapper objectMapper;

    private FactoryMocks(MockedStatic<ClassFactory> classFactoryMockedStatic,
                         PersistentStorage persistentStorage,
                         VisitorService visitorService, OkHttpClient okHttpClient,
                         AttentiveApi attentiveApi, ObjectMapper objectMapper) {
        this.classFactoryMockedStatic = classFactoryMockedStatic;
        this.persistentStorage = persistentStorage;
        this.visitorService = visitorService;
        this.okHttpClient = okHttpClient;
        this.attentiveApi = attentiveApi;
        this.objectMapper = objectMapper;
    }

    public static FactoryMocks mockFactoryObjects() {
        MockedStatic<ClassFactory> classFactoryMockedStatic = Mockito.mockStatic(ClassFactory.class);

        PersistentStorage persistentStorage = Mockito.mock(PersistentStorage.class);
        classFactoryMockedStatic.when(() -> ClassFactory.buildPersistentStorage(any())).thenReturn(persistentStorage);

        VisitorService visitorService = Mockito.mock(VisitorService.class);
        classFactoryMockedStatic.when(() -> ClassFactory.buildVisitorService(any())).thenReturn(visitorService);

        ObjectMapper objectMapper = spy(new ObjectMapper());
        classFactoryMockedStatic.when(ClassFactory::buildObjectMapper).thenReturn(objectMapper);

        OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
        classFactoryMockedStatic.when(ClassFactory::buildOkHttpClient).thenReturn(okHttpClient);

        AttentiveApi attentiveApi = Mockito.mock(AttentiveApi.class);
        classFactoryMockedStatic.when(() -> ClassFactory.buildAttentiveApi(any(), any())).thenReturn(attentiveApi);

        return new FactoryMocks(classFactoryMockedStatic, persistentStorage, visitorService, okHttpClient, attentiveApi,
            objectMapper);
    }

    public PersistentStorage getPersistentStorage() {
        return this.persistentStorage;
    }

    public VisitorService getVisitorService() {
        return this.visitorService;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public AttentiveApi getAttentiveApi() {
        return attentiveApi;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void close() {
        this.classFactoryMockedStatic.close();
    }
}

package com.attentive.androidsdk;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class FactoryMocks implements AutoCloseable {
    private final MockedStatic<ClassFactory> classFactoryMockedStatic;
    private final PersistentStorage persistentStorage;
    private final VisitorService visitorService;

    private FactoryMocks(MockedStatic<ClassFactory> classFactoryMockedStatic,
                         PersistentStorage persistentStorage,
                         VisitorService visitorService) {
        this.classFactoryMockedStatic = classFactoryMockedStatic;
        this.persistentStorage = persistentStorage;
        this.visitorService = visitorService;
    }

    public static FactoryMocks mockFactoryObjects() {
        MockedStatic<ClassFactory> classFactoryMockedStatic = Mockito.mockStatic(ClassFactory.class);

        PersistentStorage persistentStorage = Mockito.mock(PersistentStorage.class);
        classFactoryMockedStatic.when(() -> ClassFactory.buildPersistentStorage(any())).thenReturn(persistentStorage);

        VisitorService visitorService = Mockito.mock(VisitorService.class);
        classFactoryMockedStatic.when(() -> ClassFactory.buildVisitorService(any())).thenReturn(visitorService);

        return new FactoryMocks(classFactoryMockedStatic, persistentStorage, visitorService);
    }

    public PersistentStorage getPersistentStorage() {
        return this.persistentStorage;
    }

    public VisitorService getVisitorService() {
        return this.visitorService;
    }

    @Override
    public void close() {
        this.classFactoryMockedStatic.close();
    }
}

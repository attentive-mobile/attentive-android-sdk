package com.attentive.androidsdk;

import androidx.annotation.NonNull;
import java.util.Random;

public class VisitorService {
    private final static String VISITOR_ID_KEY = "visitorId";

    private final PersistentStorage persistentStorage;

    public VisitorService(PersistentStorage persistentStorage) {
        this.persistentStorage = persistentStorage;
    }

    @NonNull
    public String getVisitorId() {
        final String existingVisitorId = this.persistentStorage.read(VISITOR_ID_KEY);
        if (existingVisitorId != null) {
            return existingVisitorId;
        } else {
            return createNewVisitorId();
        }
    }

    @NonNull
    public String createNewVisitorId() {
        final String newVisitorId = generateVisitorId();
        this.persistentStorage.save(VISITOR_ID_KEY, newVisitorId);
        return newVisitorId;
    }

    private String generateVisitorId() {
        // This code should produce the same visitor ids as the tag's visitor id creation code
        StringBuilder builder = new StringBuilder();
        Random rand = new Random();
        long d = System.currentTimeMillis();
        String format = "xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx";
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '4') {
                builder.append('4');
                continue;
            }

            long r = (long) ((d + rand.nextDouble() * 16) % 16);
            d = d / 16;
            builder.append(Long.toHexString((c == 'x' ? r : (r & 0x3)) | 0x8));
        }
        return builder.toString();
    }
}

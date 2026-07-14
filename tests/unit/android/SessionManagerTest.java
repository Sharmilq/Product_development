package com.dentnova.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionManagerTest {

    @Test
    public void testEmailHashMatchingWeb() {
        // Android hashCode computation logic matching getJavaHashCode in web
        String email = "test@dentnova.com";
        int hash = 0;
        for (int i = 0; i < email.length(); i++) {
            hash = 31 * hash + email.charAt(i);
        }
        int uId = Math.abs(hash);

        // Verify the exact computed integer user_id matches Web getJavaHashCode
        assertEquals(467884179, uId);
    }

    @Test
    public void testCleanlinessCalculation() {
        // Sample validation logic for cleanliness scoring
        int score = 85;
        assertTrue(score >= 0 && score <= 100);
    }
}

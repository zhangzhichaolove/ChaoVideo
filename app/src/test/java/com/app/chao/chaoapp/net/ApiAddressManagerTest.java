package com.app.chao.chaoapp.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ApiAddressManagerTest {

    @Test
    public void normalizeTrimsAndAddsTrailingSlash() {
        assertEquals("https://example.com/api/",
                ApiAddressManager.normalize("  https://example.com/api  "));
    }

    @Test
    public void normalizePreservesValidHttpUrl() {
        assertEquals("http://192.168.1.8:8080/",
                ApiAddressManager.normalize("http://192.168.1.8:8080/"));
    }

    @Test
    public void normalizeRejectsUnsupportedOrEmptyValues() {
        assertNull(ApiAddressManager.normalize("ftp://example.com"));
        assertNull(ApiAddressManager.normalize("  "));
        assertNull(ApiAddressManager.normalize(null));
    }
}

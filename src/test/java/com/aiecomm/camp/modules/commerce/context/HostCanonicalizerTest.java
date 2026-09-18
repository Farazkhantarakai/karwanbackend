package com.aiecomm.camp.modules.commerce.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostCanonicalizerTest {

    @Test
    void testCanonicalizeNormalHost() {
        assertEquals("example.com", HostCanonicalizer.canonicalize("example.com"));
    }

    @Test
    void testCanonicalizeHostWithPort() {
        assertEquals("example.com", HostCanonicalizer.canonicalize("example.com:8080"));
        assertEquals("example.com", HostCanonicalizer.canonicalize("WWW.Example.COM:443"));
        assertEquals("localhost", HostCanonicalizer.canonicalize("localhost:3000"));
    }

    @Test
    void testCanonicalizeHostWithWww() {
        assertEquals("example.com", HostCanonicalizer.canonicalize("www.example.com"));
        assertEquals("shop.karwan.pk", HostCanonicalizer.canonicalize("WWW.SHOP.KARWAN.PK"));
    }

    @Test
    void testCanonicalizeBlankAndNull() {
        assertEquals("", HostCanonicalizer.canonicalize(null));
        assertEquals("", HostCanonicalizer.canonicalize("   "));
    }
}

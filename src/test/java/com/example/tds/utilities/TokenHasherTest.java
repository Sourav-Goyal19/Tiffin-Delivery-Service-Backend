package com.example.tds.utilities;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenHasherTest {

    @Test
    void sha256_matchesKnownVectorForHello() throws Exception {
        // Known SHA-256 vector: echo -n "hello" | shasum -a 256
        String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        assertEquals(expected, TokenHasher.sha256("hello"));
    }

    @Test
    void sha256_matchesKnownVectorForEmptyString() throws Exception {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        assertEquals(expected, TokenHasher.sha256(""));
    }

    @Test
    void sha256_isDeterministicForSameInput() {
        String first = TokenHasher.sha256("eyJhbGciOiJIUzI1NiJ9.payload.signature");
        String second = TokenHasher.sha256("eyJhbGciOiJIUzI1NiJ9.payload.signature");
        assertEquals(first, second);
    }

    @Test
    void sha256_producesLowercaseHex() {
        String hash = TokenHasher.sha256("SomeToken");
        assertNotNull(hash);
        // Hex-only, no uppercase letters
        assertTrue(hash.matches("^[0-9a-f]+$"), "Hash must be lowercase hex: " + hash);
    }

    @Test
    void sha256_outputIsSixtyFourHexChars() {
        // SHA-256 always produces 32 bytes -> 64 hex chars
        String hash = TokenHasher.sha256("any-input-string");
        assertEquals(64, hash.length());
    }

    @Test
    void sha256_differentInputsProduceDifferentHashes() {
        String a = TokenHasher.sha256("token-a");
        String b = TokenHasher.sha256("token-b");
        assertEquals(64, a.length());
        assertEquals(64, b.length());
        assertTrue(!a.equals(b), "Different inputs should yield different hashes");
    }

    @Test
    void sha256_matchesManualJavaMessageDigest() throws Exception {
        // Cross-check with raw MessageDigest to verify implementation correctness
        String input = "cross-check-input";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] raw = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }
        assertEquals(hex.toString(), TokenHasher.sha256(input));
    }
}
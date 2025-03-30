package com.hau.identity_service;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateKey {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[64];
        random.nextBytes(key);
        String base64Key = Base64.getEncoder().encodeToString(key);
        System.out.println("Generated Signer Key: " + base64Key);
    }
}

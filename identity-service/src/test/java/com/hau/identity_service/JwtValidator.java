package com.hau.identity_service;

import java.util.Base64;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.MACVerifier;

public class JwtValidator {
    public static boolean isTokenValid(String token, String signerKey) {
        try {
            // Giải mã Base64 signerKey
            byte[] keyBytes = Base64.getDecoder().decode(signerKey);

            // Giải mã token
            JWSObject jwsObject = JWSObject.parse(token);

            // Tạo verifier với signerKey
            MACVerifier verifier = new MACVerifier(keyBytes);

            // Kiểm tra chữ ký
            return jwsObject.verify(verifier);
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Nếu có lỗi thì token không hợp lệ
        }
    }

    public static void main(String[] args) {
        String token =
                "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJpZGVudGl0eS1zZXJ2aWNlIiwic3ViIjoiZHV5ZGF0MTIzIiwiZXhwIjoxNzQzMTMzNTM0LCJpYXQiOjE3NDMwNDcxMzQsImp0aSI6ImRlZGM1NTFkLWZkNTYtNDk3Yi1hY2E2LWJiYjgxMDExYjhiMSJ9.zecGF6tgXbgAvIBG17iRDxEDhNj-euzcDXGDmZq3YQsHFzlt2k-Bp-2CctBW6BVExEVXbMSNLqR45Fmc_dM7MA";
        String signerKey = "1Z2nJb9mkRfQQ42ikhS4z/klBS9/sAh8R68Tesh2hItthNNjOr9tbrYhyHLxJ7EtWmaGmdKbwdGnTPMGPk2YrQ==";

        boolean isValid = isTokenValid(token, signerKey);
        System.out.println("Token hợp lệ? " + isValid);
    }
}

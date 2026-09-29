/*
 * SPDX-FileCopyrightText: Copyright © 2019 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.restassured.RestAssured;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.lessons.cryptography.CryptoUtil;
import org.owasp.webgoat.lessons.cryptography.HashingAssignment;

public class CryptoIntegrationTest extends IntegrationTest {

  @Test
  public void runTests() {
    startLesson("Cryptography");

    checkAssignment2();
    checkAssignment3();

    // Assignment 4
    try {
      checkAssignment4();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      fail();
    }

    try {
      checkAssignmentSigning();
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    checkAssignmentDefaults();

    // HashingAssignment is intentionally left unsolved: its MD5 hashing was fixed (CWE-328), so the
    // md5 hash can no longer be reversed and that assignment cannot be completed. Every other
    // Cryptography assignment must still pass.
    checkResultsExcept("Cryptography", "HashingAssignment");
  }

  private void checkAssignment2() {

      String basicEncoding =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .get(webGoatUrlConfig.url("crypto/encoding/basic"))
            .then()
            .extract()
            .asString();
    basicEncoding = basicEncoding.substring("Authorization: Basic ".length());
    String decodedString = new String(Base64.getDecoder().decode(basicEncoding.getBytes()));
    String answer_user = decodedString.split(":")[0];
    String answer_pwd = decodedString.split(":")[1];
    Map<String, Object> params = new HashMap<>();
    params.clear();
    params.put("answer_user", answer_user);
    params.put("answer_pwd", answer_pwd);
      checkAssignment(webGoatUrlConfig.url("crypto/encoding/basic-auth"), params, true);
  }

  private void checkAssignment3() {
    String answer_1 = "databasepassword";
    Map<String, Object> params = new HashMap<>();
    params.clear();
    params.put("answer_pwd1", answer_1);
      checkAssignment(webGoatUrlConfig.url("crypto/encoding/xor"), params, true);
  }

  private void checkAssignment4() throws NoSuchAlgorithmException {

      String md5Hash =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .get(webGoatUrlConfig.url("crypto/hashing/md5"))
            .then()
            .extract()
            .asString();

    // CWE-328 fix: the md5 endpoint now returns a salted bcrypt hash, not a reversible MD5. The
    // rainbow-table crack below - matching the returned hash against the MD5 of each known secret -
    // therefore no longer recovers the input, so the assignment can no longer be solved.
    String answer_1 = "unknown";
    for (String secret : HashingAssignment.SECRETS) {
      if (md5Hash.equals(HashingAssignment.getHash(secret, "MD5"))) {
        answer_1 = secret;
      }
    }
    assertTrue(md5Hash.startsWith("$2"), "expected a bcrypt hash, got: " + md5Hash);
    assertEquals("unknown", answer_1, "bcrypt hash should not be crackable via an MD5 rainbow table");

    Map<String, Object> params = new HashMap<>();
    params.clear();
    params.put("answer_pwd1", answer_1);
    params.put("answer_pwd2", "unknown");
      checkAssignment(webGoatUrlConfig.url("crypto/hashing"), params, false);
  }

  private void checkAssignmentSigning() throws NoSuchAlgorithmException, InvalidKeySpecException {

      String privatePEM =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .get(webGoatUrlConfig.url("crypto/signing/getprivate"))
            .then()
            .extract()
            .asString();
    PrivateKey privateKey = CryptoUtil.getPrivateKeyFromPEM(privatePEM);

    RSAPrivateKey privk = (RSAPrivateKey) privateKey;
    String modulus = DatatypeConverter.printHexBinary(privk.getModulus().toByteArray());
    String signature = CryptoUtil.signMessage(modulus, privateKey);
    Map<String, Object> params = new HashMap<>();
    params.clear();
    params.put("modulus", modulus);
    params.put("signature", signature);
      checkAssignment(webGoatUrlConfig.url("crypto/signing/verify"), params, true);
  }

  private void checkAssignmentDefaults() {

    String text =
        new String(
            Base64.getDecoder()
                .decode(
                    "TGVhdmluZyBwYXNzd29yZHMgaW4gZG9ja2VyIGltYWdlcyBpcyBub3Qgc28gc2VjdXJl"
                        .getBytes(Charset.forName("UTF-8"))));

    Map<String, Object> params = new HashMap<>();
    params.clear();
    params.put("secretText", text);
    params.put("secretFileName", "default_secret");
      checkAssignment(webGoatUrlConfig.url("crypto/secure/defaults"), params, true);
  }
}

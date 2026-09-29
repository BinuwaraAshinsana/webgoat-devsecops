/*
 * SPDX-FileCopyrightText: Copyright © 2019 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.cryptography;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Regression tests for the CWE-328 fix in {@link HashingAssignment}: the /crypto/hashing/md5
 * endpoint used to return a reversible MD5 digest of a dictionary word, which a rainbow table
 * cracks instantly. It now returns a salted bcrypt hash, so the same crack fails.
 */
class HashingAssignmentTest {

  private final HashingAssignment hashing = new HashingAssignment();

  @Test
  void md5EndpointReturnsBcryptHashThatARainbowTableCannotCrack()
      throws NoSuchAlgorithmException {
    var request = new MockHttpServletRequest();

    String hash = hashing.getMd5(request);

    // A bcrypt hash carries the $2 version prefix and is not a bare 32-character MD5 hex string.
    assertThat(hash).startsWith("$2");
    assertThat(hash).doesNotMatch("[0-9A-Fa-f]{32}");

    // The previous exploit matched the returned hash against the MD5 of each known secret. That
    // lookup no longer recovers the input, because the digest is salted and is not MD5 at all.
    boolean crackedByRainbowTable = false;
    for (String secret : HashingAssignment.SECRETS) {
      if (hash.equals(HashingAssignment.getHash(secret, "MD5"))) {
        crackedByRainbowTable = true;
      }
    }
    assertThat(crackedByRainbowTable).isFalse();
  }

  @Test
  void bcryptHashStillVerifiesAgainstTheStoredSecret() throws NoSuchAlgorithmException {
    var request = new MockHttpServletRequest();

    String hash = hashing.getMd5(request);
    String secret = (String) request.getSession().getAttribute("md5Secret");

    // The digest is a genuine bcrypt hash of the secret: it verifies, even though it cannot be
    // reversed. This is the property that makes bcrypt a correct password hash.
    assertThat(new BCryptPasswordEncoder().matches(secret, hash)).isTrue();
  }
}

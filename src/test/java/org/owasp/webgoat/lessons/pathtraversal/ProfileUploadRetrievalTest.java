/*
 * SPDX-FileCopyrightText: Copyright © 2020 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.pathtraversal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.WithWebGoatUser;
import org.owasp.webgoat.container.plugins.LessonTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WithWebGoatUser
class ProfileUploadRetrievalTest extends LessonTest {

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  /**
   * Regression test for the CWE-22 fix in {@link ProfileUploadRetrieval}.
   *
   * <p>This test previously named {@code solve} asserted the traversal <em>succeeded</em>: it
   * browsed out of the cats directory and read the out-of-directory secret file. After the fix the
   * id is stripped to a bare filename before it builds a path, so the percent-encoded {@code ../../}
   * can no longer reach the parent directory: a bare traversal collapses to the cats directory (no
   * secret listed) and the secret file itself is unreachable. The identical payloads are kept so
   * this fails loudly if the sanitisation is ever removed.
   */
  @Test
  void encodedTraversalCannotEscapeCatsDirectory() throws Exception {
    // A normal request still returns a random cat picture.
    mockMvc
        .perform(get("/PathTraversal/random-picture"))
        .andExpect(status().is(200))
        .andExpect(header().exists("Location"))
        .andExpect(header().string("Location", containsString("?id=")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

    // Percent-encoded ../../ used to slip past the raw-query filter and list the PARENT directory,
    // which exposed path-traversal-secret.jpg. The directory component is now stripped, so the
    // request can only address the cats directory and the secret is no longer listed.
    mockMvc
        .perform(get(new URI("/PathTraversal/random-picture?id=%2E%2E%2F%2E%2E%2F")))
        .andExpect(status().is(404))
        .andExpect(content().string(not(containsString("path-traversal-secret"))));

    // The out-of-directory secret file itself can no longer be retrieved.
    mockMvc
        .perform(
            get(new URI("/PathTraversal/random-picture?id=%2E%2E%2F%2E%2E%2Fpath-traversal-secret")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReceiveRandomPicture() throws Exception {
    mockMvc
        .perform(get("/PathTraversal/random-picture"))
        .andExpect(status().is(200))
        .andExpect(header().exists("Location"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
  }

  @Test
  void unknownFileShouldGiveDirectoryContents() throws Exception {
    mockMvc
        .perform(get("/PathTraversal/random-picture?id=test"))
        .andExpect(status().is(404))
        .andExpect(content().string(containsString("cats" + File.separator + "8.jpg")));
  }
}

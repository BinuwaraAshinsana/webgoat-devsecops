/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.lessons.Category;
import org.springframework.web.servlet.ModelAndView;

class OpenRedirectLessonMetadataTest {

  private final OpenRedirect lesson = new OpenRedirect();
  private final OpenRedirectSecureController secureController = new OpenRedirectSecureController();
  private final OpenRedirectRealRedirect realRedirect = new OpenRedirectRealRedirect();

  @Test
  void lessonMetadataMatchesRegistration() {
    assertThat(lesson.getDefaultCategory()).isEqualTo(Category.GENERAL);
    assertThat(lesson.getTitle()).isEqualTo("openredirect.title");
  }

  @Test
  void safeRedirectUsesMappedDestinationWhenKnown() {
    ModelAndView response = secureController.safe(3);

    assertThat(response.getViewName()).isEqualTo("redirect:/logout");
  }

  @Test
  void safeRedirectFallsBackToWelcomeWhenUnknownId() {
    ModelAndView response = secureController.safe(99);

    assertThat(response.getViewName()).isEqualTo("redirect:/welcome.mvc");
  }

  /**
   * Regression test for the CWE-601 fix in {@link OpenRedirectRealRedirect}. This previously
   * asserted the endpoint redirected to whatever external URL was supplied; it now must refuse an
   * off-site target and fall back to a safe local page.
   */
  @Test
  void realRedirectRejectsExternalUrlAndFallsBackLocally() {
    assertThat(realRedirect.real("https://attacker.example").getViewName())
        .isEqualTo("redirect:/welcome.mvc");
    // Protocol-relative and backslash variants that also escape to another host are refused.
    assertThat(realRedirect.real("//attacker.example").getViewName())
        .isEqualTo("redirect:/welcome.mvc");
    assertThat(realRedirect.real("/\\attacker.example").getViewName())
        .isEqualTo("redirect:/welcome.mvc");
  }

  @Test
  void realRedirectAllowsRelativeLocalPath() {
    assertThat(realRedirect.real("/welcome.mvc").getViewName())
        .isEqualTo("redirect:/welcome.mvc");
  }
}

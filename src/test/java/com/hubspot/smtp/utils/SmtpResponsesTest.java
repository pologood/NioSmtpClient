package com.hubspot.smtp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import io.netty.handler.codec.smtp.DefaultSmtpResponse;
import io.netty.handler.codec.smtp.SmtpResponse;

public class SmtpResponsesTest {
  private static final SmtpResponse OK_RESPONSE = new DefaultSmtpResponse(250, "ARG1", "ARG2");
  private static final SmtpResponse TRANSIENT_ERROR_RESPONSE = new DefaultSmtpResponse(400);
  private static final SmtpResponse PERMANENT_ERROR_RESPONSE = new DefaultSmtpResponse(500);

  @Test
  public void itFormatsResponsesAsAString() {
    assertThat(SmtpResponses.toString(OK_RESPONSE)).isEqualTo("250 ARG1 ARG2");
  }

  @Test
  public void itCanDetectTransientErrors() {
    assertThat(SmtpResponses.isTransientError(OK_RESPONSE)).isFalse();
    assertThat(SmtpResponses.isTransientError(PERMANENT_ERROR_RESPONSE)).isFalse();
    assertThat(SmtpResponses.isTransientError(TRANSIENT_ERROR_RESPONSE)).isTrue();
  }

  @Test
  public void itCanDetectPermanenetErrors() {
    assertThat(SmtpResponses.isPermanentError(OK_RESPONSE)).isFalse();
    assertThat(SmtpResponses.isPermanentError(TRANSIENT_ERROR_RESPONSE)).isFalse();
    assertThat(SmtpResponses.isPermanentError(PERMANENT_ERROR_RESPONSE)).isTrue();
  }

  @Test
  public void itCanDetectErrors() {
    assertThat(SmtpResponses.isError(OK_RESPONSE)).isFalse();
    assertThat(SmtpResponses.isError(TRANSIENT_ERROR_RESPONSE)).isTrue();
    assertThat(SmtpResponses.isError(PERMANENT_ERROR_RESPONSE)).isTrue();
  }
}

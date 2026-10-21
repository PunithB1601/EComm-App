package com.example.identity.exception;

import org.springframework.http.HttpStatus;

/** Rotation replay revocations must commit even when this exception is returned. */
public class InvalidRefreshTokenException extends ApplicationException {
  public InvalidRefreshTokenException() {
    super(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
  }
}

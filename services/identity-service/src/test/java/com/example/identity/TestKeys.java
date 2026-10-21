package com.example.identity;

import java.nio.file.*;
import java.security.*;
import java.util.Base64;

final class TestKeys {
  static final Path DIRECTORY;

  static {
    try {
      DIRECTORY = Files.createTempDirectory("identity-test-keys");
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair pair = generator.generateKeyPair();
      pem("private.pem", "PRIVATE KEY", pair.getPrivate().getEncoded());
      pem("public.pem", "PUBLIC KEY", pair.getPublic().getEncoded());
      DIRECTORY.toFile().deleteOnExit();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static void pem(String file, String type, byte[] bytes) throws Exception {
    Path path = DIRECTORY.resolve(file);
    Files.writeString(
        path,
        "-----BEGIN "
            + type
            + "-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(bytes)
            + "\n-----END "
            + type
            + "-----\n");
    path.toFile().deleteOnExit();
  }
}

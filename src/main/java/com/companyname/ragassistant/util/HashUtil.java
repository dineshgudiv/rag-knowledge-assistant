package com.companyname.ragassistant.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class HashUtil {
  private HashUtil() {}

  public static String sha256(String s) {
    return sha256(s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8));
  }

  public static String sha256(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] out = md.digest(bytes == null ? new byte[0] : bytes);
      StringBuilder sb = new StringBuilder();
      for (byte b : out) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

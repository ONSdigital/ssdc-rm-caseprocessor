package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HashHelperTest {
  private static final String TEST_UAC_HASH =
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
  private final byte[] TEST_STRING_IN_BYTES = {
    -70, 120, 22, -65, -113, 1, -49, -22, 65, 65, 64, -34, 93, -82, 34, 35, -80, 3, 97, -93, -106,
    23, 122, -100, -76, 16, -1, 97, -14, 0, 21, -83
  };

  @Test
  void testStringToBytes() {
    // GIVEN WHEN
    byte[] testEncodedUacHash = HashHelper.stringToBytes("abc".getBytes(StandardCharsets.UTF_8));

    // THEN
    assertThat(testEncodedUacHash).isEqualTo(TEST_STRING_IN_BYTES);
  }

  @Test
  void testBytesToHexString() {
    // GIVEN WHEN
    String hashedResult = HashHelper.bytesToHexString(TEST_STRING_IN_BYTES);

    // THEN
    assertThat(hashedResult).isEqualTo(TEST_UAC_HASH);
  }
}

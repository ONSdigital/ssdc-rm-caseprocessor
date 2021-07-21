package uk.gov.ons.ssdc.caseprocessor.healthcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class HeathCheckIT {
  @Value("${healthcheck.filename}")
  private String fileName;

  @Test
  public void testHappyPath() throws IOException {
    Path path = Paths.get(fileName);

    try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
      String fileLine = bufferedReader.readLine();
      String now = LocalDateTime.now().toString();

      assertEquals(now.substring(0, 15), fileLine.substring(0, 15));
    }
  }
}

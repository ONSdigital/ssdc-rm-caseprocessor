package uk.gov.ons.ssdc.caseprocessor.utils;

import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;

public class JsonHelper {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  public static String convertObjectToJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed converting Object To Json", e);
    }
  }

  public static EventDTO convertJsonBytesToEvent(byte[] bytes) {
    EventDTO event;
    try {
      event = objectMapper.readValue(bytes, EventDTO.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (!EVENT_SCHEMA_VERSION.equals(event.getHeader().getVersion())) {
      throw new RuntimeException(
          String.format(
              "Incorrect message version. Expected %s but got: %s",
              EVENT_SCHEMA_VERSION, event.getHeader()));
    }

    return event;
  }
}

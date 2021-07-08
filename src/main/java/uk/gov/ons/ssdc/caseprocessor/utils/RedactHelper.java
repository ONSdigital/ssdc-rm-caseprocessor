package uk.gov.ons.ssdc.caseprocessor.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

public class RedactHelper {
  private static final String REDACTION_FAILURE = "Failed to redact sensitive data";
  private static final String REDACTION_TARGET = "setSampleSensitive";
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  public static Object redact(Object rootObjectToRedact) {
    try {
      Object rootObjectToRedactDeepCopy =
          objectMapper.readValue(
              objectMapper.writeValueAsString(rootObjectToRedact), rootObjectToRedact.getClass());
      recursivelyRedact(
          rootObjectToRedactDeepCopy,
          REDACTION_TARGET,
          rootObjectToRedactDeepCopy.getClass().getPackageName());
      return rootObjectToRedactDeepCopy;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(REDACTION_FAILURE, e);
    }
  }

  private static void recursivelyRedact(Object object, String methodToFind, String packageName) {
    Arrays.stream(object.getClass().getMethods())
        .filter(item -> Modifier.isPublic(item.getModifiers()))
        .forEach(
            method -> {
              if (method.getName().startsWith("get")
                  && method.getReturnType().getPackageName().equals(packageName)) {
                try {
                  Object invokeResult = method.invoke(object);
                  if (invokeResult != null) {
                    recursivelyRedact(invokeResult, methodToFind, packageName);
                  }
                } catch (IllegalAccessException | InvocationTargetException e) {
                  throw new RuntimeException(REDACTION_FAILURE, e);
                }
              }

              if (method.getName().equals(methodToFind)) {
                try {
                  method.invoke(object, Map.of("REDACTED", "REDACTED"));
                } catch (IllegalAccessException | InvocationTargetException e) {
                  throw new RuntimeException(REDACTION_FAILURE, e);
                }
              }
            });
  }
}

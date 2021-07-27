package uk.gov.ons.ssdc.caseprocessor.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import org.springframework.util.StringUtils;

public class RedactHelper {
  private static final String REDACTION_FAILURE = "Failed to redact sensitive data";
  private static final String REDACTION_TARGET = "getSampleSensitive";
  private static final String REDACTION_TEXT = "REDACTED";
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

              if (method.getName().equals(methodToFind)
                  && method.getReturnType().equals(Map.class)) {
                try {
                  Map<String, String> sensitiveData = (Map<String, String>) method.invoke(object);
                  for (String key : sensitiveData.keySet()) {
                    if (StringUtils.hasText((sensitiveData.get(key)))) {
                      sensitiveData.put(key, REDACTION_TEXT);
                    }
                  }
                } catch (IllegalAccessException | InvocationTargetException e) {
                  throw new RuntimeException(REDACTION_FAILURE, e);
                }
              }
            });
  }
}

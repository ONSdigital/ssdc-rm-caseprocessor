package uk.gov.ons.ssdc.caseprocessor.utils;

import java.util.Set;

public class Constants {
  public static final String OUTBOUND_EVENT_SCHEMA_VERSION = "v0.3_RELEASE";
  public static final Set<String> ALLOWED_INBOUND_EVENT_SCHEMA_VERSIONS =
      Set.of("v0.3_RELEASE", "0.4.0-DRAFT", "0.4.0");
}

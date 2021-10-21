package uk.gov.ons.ssdc.caseprocessor.client;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PartyDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RasRmPartyResponseDTO;

@Component
public class RasRmPartyServiceClient {

  @Value("${ras-rm-party-service.connection.scheme}")
  private String scheme;

  @Value("${ras-rm-party-service.connection.host}")
  private String host;

  @Value("${ras-rm-party-service.connection.port}")
  private String port;

  @Value("${ras-rm-party-service.connection.username}")
  private String username;

  @Value("${ras-rm-party-service.connection.password}")
  private String password;

  public RasRmPartyResponseDTO createParty(
      String sampleUnitRef, UUID sampleSummaryId, Map<String, Object> attributes) {
    PartyDTO partyDTO = new PartyDTO();
    partyDTO.setSampleUnitRef(sampleUnitRef);
    partyDTO.setSampleUnitType("B"); // Hard-coded to be B = business. No need for any other value
    partyDTO.setSampleSummaryId(sampleSummaryId);
    partyDTO.setAttributes(attributes);

    RestTemplate restTemplate = new RestTemplate();
    UriComponents uriComponents = createUriComponents("party-api/v1/parties");

    HttpEntity<PartyDTO> entity = new HttpEntity<>(partyDTO, createHeaders(username, password));

    return restTemplate
        .exchange(uriComponents.toUri(), HttpMethod.POST, entity, RasRmPartyResponseDTO.class)
        .getBody();
  }

  private UriComponents createUriComponents(String path) {
    return UriComponentsBuilder.newInstance()
        .scheme(scheme)
        .host(host)
        .port(port)
        .path(path)
        .build()
        .encode();
  }

  HttpHeaders createHeaders(String username, String password) {
    return new HttpHeaders() {
      {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        set("Authorization", authHeader);
      }
    };
  }
}

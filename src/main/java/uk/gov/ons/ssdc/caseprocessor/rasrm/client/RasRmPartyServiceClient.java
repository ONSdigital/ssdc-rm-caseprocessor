package uk.gov.ons.ssdc.caseprocessor.rasrm.client;

import static uk.gov.ons.ssdc.caseprocessor.rasrm.constants.RasRmConstants.BUSINESS_SAMPLE_UNIT_TYPE;

import java.nio.charset.StandardCharsets;
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
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmPartyDTO;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmPartyLinkDTO;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmPartyResponseDTO;

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
    RasRmPartyDTO partyDTO = new RasRmPartyDTO();
    partyDTO.setSampleUnitRef(sampleUnitRef);
    partyDTO.setSampleUnitType(BUSINESS_SAMPLE_UNIT_TYPE);
    partyDTO.setSampleSummaryId(sampleSummaryId);
    partyDTO.setAttributes(attributes);

    RestTemplate restTemplate = new RestTemplate();
    UriComponents uriComponents = createUriComponents("party-api/v1/parties");

    HttpEntity<RasRmPartyDTO> entity =
        new HttpEntity<>(partyDTO, createHeaders(username, password));

    return restTemplate
        .exchange(uriComponents.toUri(), HttpMethod.POST, entity, RasRmPartyResponseDTO.class)
        .getBody();
  }

  public void linkSampleSummaryToCollex(UUID sampleSummaryId, UUID rasRmCollectionExerciseId) {
    RasRmPartyLinkDTO rasRmPartyLinkDTO = new RasRmPartyLinkDTO();
    rasRmPartyLinkDTO.setCollectionExerciseId(rasRmCollectionExerciseId);

    RestTemplate restTemplate = new RestTemplate();
    UriComponents uriComponents =
        createUriComponents(
            "party-api/v1/businesses/sample/link/{sampleSummaryId}", sampleSummaryId);

    HttpEntity<RasRmPartyLinkDTO> entity =
        new HttpEntity<>(rasRmPartyLinkDTO, createHeaders(username, password));

    restTemplate.exchange(uriComponents.toUri(), HttpMethod.PUT, entity, Void.class);
  }

  private UriComponents createUriComponents(String path, UUID sampleSummaryId) {
    return UriComponentsBuilder.newInstance()
        .scheme(scheme)
        .host(host)
        .port(port)
        .path(path)
        .buildAndExpand(sampleSummaryId)
        .encode();
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
    HttpHeaders headers = new HttpHeaders();

    String auth = username + ":" + password;
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
    String authHeader = "Basic " + new String(encodedAuth);
    headers.set("Authorization", authHeader);

    return headers;
  }
}

package uk.gov.ons.ssdc.caseprocessor.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RasRmCaseIacResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RasRmCaseResponseDTO;

@Component
public class RasRmCaseServiceClient {

  @Value("${ras-rm-case-service.connection.scheme}")
  private String scheme;

  @Value("${ras-rm-case-service.connection.host}")
  private String host;

  @Value("${ras-rm-case-service.connection.port}")
  private String port;

  public RasRmCaseResponseDTO[] getCases(UUID partyId) {
    RestTemplate restTemplate = new RestTemplate();
    UriComponents uriComponents = createUriComponents("/cases/partyid/{id}", partyId);

    return restTemplate.getForObject(uriComponents.toUri(), RasRmCaseResponseDTO[].class);
  }

  public RasRmCaseIacResponseDTO[] getIacs(UUID caseId) {
    RestTemplate restTemplate = new RestTemplate();
    UriComponents uriComponents = createUriComponents("/cases/{id}/iac", caseId);

    return restTemplate.getForObject(uriComponents.toUri(), RasRmCaseIacResponseDTO[].class);
  }

  private UriComponents createUriComponents(String path, UUID id) {
    return UriComponentsBuilder.newInstance()
        .scheme(scheme)
        .host(host)
        .port(port)
        .path(path)
        .buildAndExpand(id)
        .encode();
  }
}

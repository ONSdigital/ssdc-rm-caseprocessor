package uk.gov.ons.ssdc.caseprocessor.client;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;

@Component
public class UacQidServiceClient {

  @Value("${uacservice.connection.scheme}")
  private String scheme;

  @Value("${uacservice.connection.host}")
  private String host;

  @Value("${uacservice.connection.port}")
  private String port;

  public List<UacQidDTO> getUacQids(int numberToCreate) {
    RestTemplate restTemplate = new RestTemplate();

    UriComponents uriComponents = createUriComponents(numberToCreate, "multiple_qids");
    ResponseEntity<UacQidDTO[]> responseEntity =
        restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, null, UacQidDTO[].class);

    return Arrays.asList(responseEntity.getBody());
  }

  private UriComponents createUriComponents(int numberToCreate, String path) {
    return UriComponentsBuilder.newInstance()
        .scheme(scheme)
        .host(host)
        .port(port)
        .path(path)
        .queryParam("numberToCreate", numberToCreate)
        .build()
        .encode();
  }
}

package uk.gov.ons.ssdc.caseprocessor.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.ons.ssdc.caseprocessor.model.dto.SampleSummaryDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.SampleSummaryResponseDTO;

@Component
public class RasRmSampleServiceClient {

  @Value("${ras-rm-sample-service.connection.scheme}")
  private String scheme;

  @Value("${ras-rm-sample-service.connection.host}")
  private String host;

  @Value("${ras-rm-sample-service.connection.port}")
  private String port;

  public SampleSummaryResponseDTO createSampleSummary(
      int totalSampleUnits, int expectedCollectionInstruments) {
    SampleSummaryDTO sampleSummaryDTO = new SampleSummaryDTO();
    sampleSummaryDTO.setTotalSampleUnits(totalSampleUnits);
    sampleSummaryDTO.setExpectedCollectionInstruments(expectedCollectionInstruments);

    RestTemplate restTemplate = new RestTemplate();
    UriComponents uriComponents = createUriComponents("samples/samplesummary");

    return restTemplate.postForObject(
        uriComponents.toUri(), sampleSummaryDTO, SampleSummaryResponseDTO.class);
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
}

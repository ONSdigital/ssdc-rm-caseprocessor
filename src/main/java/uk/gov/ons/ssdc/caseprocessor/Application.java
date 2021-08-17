package uk.gov.ons.ssdc.caseprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubReactiveAutoConfiguration;
import org.springframework.integration.annotation.IntegrationComponentScan;

@SpringBootApplication(exclude = {
    GcpPubSubAutoConfiguration.class,
    GcpPubSubReactiveAutoConfiguration.class})
//@SpringBootApplication
@IntegrationComponentScan
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class);
  }
}

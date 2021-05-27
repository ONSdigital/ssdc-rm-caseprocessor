package uk.gov.ons.ssdc.caseprocessor.config;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import javax.jms.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  public ConnectionFactory jmsConnectionFactory() {
    return new RMQConnectionFactory();
  }
}

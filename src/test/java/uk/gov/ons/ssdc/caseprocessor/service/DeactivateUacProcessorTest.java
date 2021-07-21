package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DeactivateUacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;

@ExtendWith(MockitoExtension.class)
public class DeactivateUacProcessorTest {

  @Mock private RabbitTemplate rabbitTemplate;

  @InjectMocks private DeactivateUacProcessor underTest;

  @Test
  public void testProcessDeactivateUacRow() {
    // Given
    ReflectionTestUtils.setField(underTest, "outboundExchange", "testExchange");
    ReflectionTestUtils.setField(underTest, "deactivateUacRoutingKey", "testRoutingKey");

    Case caze = new Case();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setQid("0123456789");
    caze.setUacQidLinks(List.of(uacQidLink));

    // When
    underTest.process(caze);

    // Then
    ArgumentCaptor<ResponseManagementEvent> responseManagementEventArgumentCaptor =
        ArgumentCaptor.forClass(ResponseManagementEvent.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq("testExchange"),
            eq("testRoutingKey"),
            responseManagementEventArgumentCaptor.capture());
    DeactivateUacDTO actualDeactivateUac =
        responseManagementEventArgumentCaptor.getValue().getPayload().getDeactivateUac();
    assertThat(actualDeactivateUac.getQid()).isEqualTo(uacQidLink.getQid());
  }
}

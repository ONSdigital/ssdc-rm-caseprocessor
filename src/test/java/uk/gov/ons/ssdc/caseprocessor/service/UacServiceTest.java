package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@ExtendWith(MockitoExtension.class)
public class UacServiceTest {
  private static String TEST_MESSAGE_HASH =
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
  @Mock UacQidLinkRepository uacQidLinkRepository;
  @Mock MessageSender messageSender;

  @InjectMocks UacService underTest;

  @Test
  public void saveAndEmitUacUpdatedEvent() {
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setUac("abc");
    uacQidLink.setQid("01234");
    uacQidLink.setActive(true);

    when(uacQidLinkRepository.save(uacQidLink)).thenReturn(uacQidLink);
    underTest.saveAndEmitUacUpdateEvent(uacQidLink);

    verify(uacQidLinkRepository).save(uacQidLink);

    ArgumentCaptor<EventDTO> eventArgumentCaptor = ArgumentCaptor.forClass(EventDTO.class);

    verify(messageSender).sendMessage(any(), eventArgumentCaptor.capture());
    EventDTO actualEvent = eventArgumentCaptor.getValue();

    UacUpdateDTO uacUpdateDto = actualEvent.getPayload().getUacUpdate();
    assertThat(uacUpdateDto.getUacHash()).isEqualTo(TEST_MESSAGE_HASH);
    assertThat(uacUpdateDto.getQid()).isEqualTo(uacUpdateDto.getQid());
  }

  @Test
  public void findByQid() {
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setUac("abc");
    uacQidLink.setQid("01234");
    uacQidLink.setActive(true);

    when(uacQidLinkRepository.findByQid(uacQidLink.getQid())).thenReturn(Optional.of(uacQidLink));

    UacQidLink actualUacQidLink = underTest.findByQid(uacQidLink.getQid());
    assertThat(actualUacQidLink).isEqualTo(uacQidLink);
  }

  @Test
  public void findByQidFails() {
    String qid = "12345";
    String expectedErrorMessage = String.format("Questionnaire Id '%s' not found!", qid);

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.findByQid(qid));

    Assertions.assertThat(thrown.getMessage()).isEqualTo(expectedErrorMessage);
  }

  @Test
  public void existsByQid() {
    String testQid = "12345";
    underTest.existsByQid(testQid);
    verify(uacQidLinkRepository).existsByQid(testQid);
  }
}

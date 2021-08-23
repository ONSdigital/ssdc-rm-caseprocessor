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
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@ExtendWith(MockitoExtension.class)
public class UacServiceTest {
  @Mock UacQidLinkRepository uacQidLinkRepository;
  @Mock MessageSender messageSender;

  @InjectMocks UacService underTest;

  @Test
  void saveAndEmitUacUpdatedEvent() {
    ReflectionTestUtils.setField(underTest, "uacUpdateTopic", "Test topic");
    ReflectionTestUtils.setField(underTest, "sharedPubsubProject", "Test project");

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
    assertThat(uacUpdateDto.getUac()).isEqualTo(uacQidLink.getUac());
    assertThat(uacUpdateDto.getQid()).isEqualTo(uacUpdateDto.getQid());
  }

  @Test
  void findByQid() {
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
  void findByQidFails() {
    String qid = "12345";
    String expectedErrorMessage = String.format("Questionnaire Id '%s' not found!", qid);

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.findByQid(qid));

    Assertions.assertThat(thrown.getMessage()).isEqualTo(expectedErrorMessage);
  }

  @Test
  void existsByQid() {
    String testQid = "12345";
    underTest.existsByQid(testQid);
    verify(uacQidLinkRepository).existsByQid(testQid);
  }

  @Test
  void createLinkAndEmitNewUacQid() {
    // Given
    ReflectionTestUtils.setField(underTest, "uacUpdateTopic", "Test topic");
    ReflectionTestUtils.setField(underTest, "sharedPubsubProject", "Test project");

    String qid = "TEST_QID";
    String uac = "TEST_UAC";
    Case testCase = new Case();
    testCase.setId(UUID.randomUUID());
    UacQidLink expectedSavedUacQidLink = new UacQidLink();
    expectedSavedUacQidLink.setUac(uac);
    expectedSavedUacQidLink.setQid(qid);
    expectedSavedUacQidLink.setCaze(testCase);

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    when(uacQidLinkRepository.save(uacQidLinkCaptor.capture())).thenReturn(expectedSavedUacQidLink);

    // When
    underTest.createLinkAndEmitNewUacQid(testCase, uac, qid);

    // Then
    UacQidLink actualSavedUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualSavedUacQidLink.isActive()).isTrue();
    assertThat(actualSavedUacQidLink.getQid()).isEqualTo(qid);
    assertThat(actualSavedUacQidLink.getUac()).isEqualTo(uac);
    assertThat(actualSavedUacQidLink.getCaze()).isEqualTo(testCase);

    ArgumentCaptor<EventDTO> eventArgumentCaptor = ArgumentCaptor.forClass(EventDTO.class);
    verify(messageSender).sendMessage(any(), eventArgumentCaptor.capture());
    EventDTO actualEvent = eventArgumentCaptor.getValue();
    UacUpdateDTO uacUpdateDto = actualEvent.getPayload().getUacUpdate();
    assertThat(uacUpdateDto.getUac()).isEqualTo(uac);
    assertThat(uacUpdateDto.getQid()).isEqualTo(qid);
    assertThat(uacUpdateDto.getCaseId()).isEqualTo(testCase.getId());
  }
}

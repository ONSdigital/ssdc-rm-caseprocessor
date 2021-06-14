package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@RunWith(MockitoJUnitRunner.class)
public class CaseToProcessProcessorTest {
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private UacQidCache uacQidCache;
  @Mock private UacQidLinkRepository uacQidLinkRepository;
  @Mock private UacService uacService;

  @InjectMocks CaseToProcessProcessor underTest;

  @Value("${queueconfig.print-queue}")
  private String printQueue;

  @Test
  public void testHappyPath() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    WaveOfContact waveOfContact = new WaveOfContact();
    waveOfContact.setType(WaveOfContactType.PRINT);
    waveOfContact.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});
    waveOfContact.setPackCode("test pack code");
    waveOfContact.setPrintSupplier("test print supplier");

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setWaveOfContact(waveOfContact);
    caseToProcess.setCaze(caze);

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac("test uac");
    uacQidDTO.setQid("test qid");

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);

    // When
    underTest.process(caseToProcess);

    // Then
    ArgumentCaptor<PrintRow> printRowArgumentCaptor = ArgumentCaptor.forClass(PrintRow.class);
    verify(rabbitTemplate).convertAndSend(eq(""), eq(printQueue), printRowArgumentCaptor.capture());
    PrintRow actualPrintRow = printRowArgumentCaptor.getValue();
    assertThat(actualPrintRow.getPackCode()).isEqualTo("test pack code");
    assertThat(actualPrintRow.getPrintSupplier()).isEqualTo("test print supplier");
    assertThat(actualPrintRow.getRow()).isEqualTo("\"123\"|\"test uac\"|\"bar\"");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacQidLinkRepository).saveAndFlush(uacQidLinkCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo("test uac");
    assertThat(actualUacQidLink.getQid()).isEqualTo("test qid");
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();

    verify(uacService).saveAndEmitUacUpdatedEvent(uacQidLinkCaptor.getValue());
  }
}

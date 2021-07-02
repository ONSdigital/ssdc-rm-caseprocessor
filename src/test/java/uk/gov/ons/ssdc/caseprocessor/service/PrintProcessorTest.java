package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.*;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@RunWith(MockitoJUnitRunner.class)
public class PrintProcessorTest {
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private UacQidCache uacQidCache;
  @Mock private UacQidLinkRepository uacQidLinkRepository;
  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;

  @InjectMocks PrintProcessor underTest;

  @Value("${queueconfig.print-queue}")
  private String printQueue;

  @Test
  public void testProcessPrintRow() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    ActionRule actionRule = new ActionRule();
    actionRule.setType(ActionRuleType.PRINT);
    actionRule.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});
    actionRule.setPackCode("test pack code");
    actionRule.setPrintSupplier("test print supplier");

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);
    caseToProcess.setBatchId(UUID.fromString("6a127d58-c1cb-489c-a3f5-72014a0c32d6"));

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac("test uac");
    uacQidDTO.setQid("test qid");

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);

    // When
    underTest.processPrintRow(
        actionRule.getTemplate(),
        caze,
        caseToProcess.getBatchId(),
        caseToProcess.getBatchQuantity(),
        caseToProcess.getActionRule().getPackCode(),
        caseToProcess.getActionRule().getPrintSupplier());

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
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            any(OffsetDateTime.class),
            eq(
                "Printed pack code test pack code with batch id 6a127d58-c1cb-489c-a3f5-72014a0c32d6"),
            eq(EventType.PRINTED_PACK_CODE),
            any(EventDTO.class),
            isNull(),
            any(OffsetDateTime.class));
  }

  @Test
  public void testProcessFulfilment() {
    // Given
    PrintTemplate printTemplate = new PrintTemplate();
    printTemplate.setPackCode("TEST_FULFILMENT_CODE");
    printTemplate.setPrintSupplier("FOOBAR_PRINT_SUPPLIER");
    printTemplate.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});

    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setPrintTemplate(printTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setBatchId(UUID.fromString("6a127d58-c1cb-489c-a3f5-72014a0c32d6"));
    fulfilmentToProcess.setBatchQuantity(200);

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac("test uac");
    uacQidDTO.setQid("test qid");

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);

    // When
    underTest.process(fulfilmentToProcess);

    // Then
    ArgumentCaptor<PrintRow> printRowArgumentCaptor = ArgumentCaptor.forClass(PrintRow.class);
    verify(rabbitTemplate).convertAndSend(eq(""), eq(printQueue), printRowArgumentCaptor.capture());
    PrintRow actualPrintRow = printRowArgumentCaptor.getValue();
    assertThat(actualPrintRow.getPackCode()).isEqualTo("TEST_FULFILMENT_CODE");
    assertThat(actualPrintRow.getPrintSupplier()).isEqualTo("FOOBAR_PRINT_SUPPLIER");
    assertThat(actualPrintRow.getRow()).isEqualTo("\"123\"|\"test uac\"|\"bar\"");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacQidLinkRepository).saveAndFlush(uacQidLinkCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo("test uac");
    assertThat(actualUacQidLink.getQid()).isEqualTo("test qid");
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();

    verify(uacService).saveAndEmitUacUpdatedEvent(uacQidLinkCaptor.getValue());
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            any(OffsetDateTime.class),
            eq(
                "Printed pack code TEST_FULFILMENT_CODE with batch id 6a127d58-c1cb-489c-a3f5-72014a0c32d6"),
            eq(EventType.PRINTED_PACK_CODE),
            any(EventDTO.class),
            isNull(),
            any(OffsetDateTime.class));
  }
}

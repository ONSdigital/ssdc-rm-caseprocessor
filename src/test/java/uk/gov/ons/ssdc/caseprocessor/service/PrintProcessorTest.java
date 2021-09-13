package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.common.model.entity.*;

@ExtendWith(MockitoExtension.class)
public class PrintProcessorTest {
  @Mock private MessageSender messageSender;
  @Mock private UacQidCache uacQidCache;
  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;

  @InjectMocks PrintProcessor underTest;

  @Value("${queueconfig.print-topic}")
  private String printTopic;

  @Test
  public void testProcessPrintRow() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    PrintTemplate printTemplate = new PrintTemplate();
    printTemplate.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});
    printTemplate.setPackCode("test pack code");
    printTemplate.setPrintSupplier("test print supplier");

    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setType(ActionRuleType.PRINT);
    actionRule.setPrintTemplate(printTemplate);

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
        printTemplate.getTemplate(),
        caze,
        caseToProcess.getBatchId(),
        caseToProcess.getBatchQuantity(),
        printTemplate.getPackCode(),
        printTemplate.getPrintSupplier(),
        actionRule.getId(),
        null);

    // Then
    ArgumentCaptor<PrintRow> printRowArgumentCaptor = ArgumentCaptor.forClass(PrintRow.class);
    verify(messageSender).sendMessage(eq(printTopic), printRowArgumentCaptor.capture());
    PrintRow actualPrintRow = printRowArgumentCaptor.getValue();
    assertThat(actualPrintRow.getPackCode()).isEqualTo("test pack code");
    assertThat(actualPrintRow.getPrintSupplier()).isEqualTo("test print supplier");
    assertThat(actualPrintRow.getRow()).isEqualTo("\"123\"|\"test uac\"|\"bar\"");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService)
        .saveAndEmitUacUpdateEvent(uacQidLinkCaptor.capture(), eq(actionRule.getId()), isNull());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo("test uac");
    assertThat(actualUacQidLink.getQid()).isEqualTo("test qid");
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();

    ArgumentCaptor<EventHeaderDTO> headerCaptor = ArgumentCaptor.forClass(EventHeaderDTO.class);
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            any(OffsetDateTime.class),
            eq("Print file generated with pack code test pack code"),
            eq(EventType.PRINT_FILE),
            headerCaptor.capture(),
            isNull(),
            any(OffsetDateTime.class));

    EventHeaderDTO actualHeader = headerCaptor.getValue();
    Assertions.assertThat(actualHeader.getCorrelationId()).isEqualTo(actionRule.getId());
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
    fulfilmentToProcess.setCorrelationId(TEST_CORRELATION_ID);
    fulfilmentToProcess.setOriginatingUser(TEST_ORIGINATING_USER);

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac("test uac");
    uacQidDTO.setQid("test qid");

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);

    // When
    underTest.process(fulfilmentToProcess);

    // Then
    ArgumentCaptor<PrintRow> printRowArgumentCaptor = ArgumentCaptor.forClass(PrintRow.class);
    verify(messageSender).sendMessage(eq(printTopic), printRowArgumentCaptor.capture());
    PrintRow actualPrintRow = printRowArgumentCaptor.getValue();
    assertThat(actualPrintRow.getPackCode()).isEqualTo("TEST_FULFILMENT_CODE");
    assertThat(actualPrintRow.getPrintSupplier()).isEqualTo("FOOBAR_PRINT_SUPPLIER");
    assertThat(actualPrintRow.getRow()).isEqualTo("\"123\"|\"test uac\"|\"bar\"");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService)
        .saveAndEmitUacUpdateEvent(
            uacQidLinkCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo("test uac");
    assertThat(actualUacQidLink.getQid()).isEqualTo("test qid");
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();

    ArgumentCaptor<EventHeaderDTO> headerCaptor = ArgumentCaptor.forClass(EventHeaderDTO.class);
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            any(OffsetDateTime.class),
            eq("Print file generated with pack code TEST_FULFILMENT_CODE"),
            eq(EventType.PRINT_FILE),
            headerCaptor.capture(),
            isNull(),
            any(OffsetDateTime.class));

    EventHeaderDTO actualHeader = headerCaptor.getValue();
    Assertions.assertThat(actualHeader.getCorrelationId()).isEqualTo(TEST_CORRELATION_ID);
    Assertions.assertThat(actualHeader.getOriginatingUser()).isEqualTo(TEST_ORIGINATING_USER);
  }
}

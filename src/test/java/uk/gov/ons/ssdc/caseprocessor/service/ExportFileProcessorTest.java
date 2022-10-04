package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_UAC_METADATA;

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
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.collectioninstrument.CollectionInstrumentHelper;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileRowRepository;
import uk.gov.ons.ssdc.common.model.entity.*;

@ExtendWith(MockitoExtension.class)
class ExportFileProcessorTest {
  @Mock private UacQidCache uacQidCache;
  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;
  @Mock private ExportFileRowRepository exportFileRowRepository;
  @Mock private CollectionInstrumentHelper collectionInstrumentHelper;

  @InjectMocks ExportFileProcessor underTest;

  private static final String PACK_CODE = "test pack code";
  private static final String UAC = "test UAC";
  private static final String QID = "test QID";
  private static final String EXPORT_FILE_DESTINATION = "test export file destination";

  @Test
  void testProcessExportFileRow() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});
    exportFileTemplate.setPackCode(PACK_CODE);
    exportFileTemplate.setExportFileDestination(EXPORT_FILE_DESTINATION);

    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setType(ActionRuleType.EXPORT_FILE);
    actionRule.setExportFileTemplate(exportFileTemplate);
    actionRule.setUacMetadata(TEST_UAC_METADATA);

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);
    caseToProcess.setBatchId(UUID.fromString("6a127d58-c1cb-489c-a3f5-72014a0c32d6"));

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac(UAC);
    uacQidDTO.setQid(QID);

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);
    when(collectionInstrumentHelper.getCollectionInstrumentUrl(caze, TEST_UAC_METADATA))
        .thenReturn("testCollectionInstrumentUrl");

    // When
    underTest.processExportFileRow(
        exportFileTemplate.getTemplate(),
        caze,
        caseToProcess.getBatchId(),
        caseToProcess.getBatchQuantity(),
        exportFileTemplate.getPackCode(),
        exportFileTemplate.getExportFileDestination(),
        actionRule.getId(),
        null,
        actionRule.getUacMetadata());

    //    // Then
    ArgumentCaptor<ExportFileRow> exportFileRowArgumentCaptor =
        ArgumentCaptor.forClass(ExportFileRow.class);
    verify(exportFileRowRepository).save(exportFileRowArgumentCaptor.capture());
    ExportFileRow actualExportFileRow = exportFileRowArgumentCaptor.getValue();
    assertThat(actualExportFileRow.getPackCode()).isEqualTo(PACK_CODE);
    assertThat(actualExportFileRow.getExportFileDestination()).isEqualTo(EXPORT_FILE_DESTINATION);
    assertThat(actualExportFileRow.getRow()).isEqualTo("\"123\"|\"" + UAC + "\"|\"bar\"");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService)
        .saveAndEmitUacUpdateEvent(uacQidLinkCaptor.capture(), eq(actionRule.getId()), isNull());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo(UAC);
    assertThat(actualUacQidLink.getQid()).isEqualTo(QID);
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();
    assertThat(actualUacQidLink.getMetadata()).isEqualTo(TEST_UAC_METADATA);
    assertThat(actualUacQidLink.getCollectionInstrumentUrl())
        .isEqualTo("testCollectionInstrumentUrl");

    ArgumentCaptor<EventDTO> eventCaptor = ArgumentCaptor.forClass(EventDTO.class);
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            eq("Export file generated with pack code " + PACK_CODE),
            eq(EventType.EXPORT_FILE),
            eventCaptor.capture(),
            any(OffsetDateTime.class));

    EventHeaderDTO actualHeader = eventCaptor.getValue().getHeader();
    Assertions.assertThat(actualHeader.getCorrelationId()).isEqualTo(actionRule.getId());
  }

  @Test
  void testProcessExportRasRmIacFileRow() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setTemplate(new String[] {"__ras_rm_iac__"});
    exportFileTemplate.setPackCode(PACK_CODE);
    exportFileTemplate.setExportFileDestination(EXPORT_FILE_DESTINATION);

    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setType(ActionRuleType.EXPORT_FILE);
    actionRule.setExportFileTemplate(exportFileTemplate);
    actionRule.setUacMetadata(TEST_UAC_METADATA);

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);
    caseToProcess.setBatchId(UUID.fromString("6a127d58-c1cb-489c-a3f5-72014a0c32d6"));

    // When
    underTest.processExportFileRow(
        exportFileTemplate.getTemplate(),
        caze,
        caseToProcess.getBatchId(),
        caseToProcess.getBatchQuantity(),
        exportFileTemplate.getPackCode(),
        exportFileTemplate.getExportFileDestination(),
        actionRule.getId(),
        null,
        actionRule.getUacMetadata());

    //    // Then
    ArgumentCaptor<ExportFileRow> exportFileRowArgumentCaptor =
        ArgumentCaptor.forClass(ExportFileRow.class);
    verify(exportFileRowRepository).save(exportFileRowArgumentCaptor.capture());
    ExportFileRow actualExportFileRow = exportFileRowArgumentCaptor.getValue();
    assertThat(actualExportFileRow.getPackCode()).isEqualTo(PACK_CODE);
    assertThat(actualExportFileRow.getExportFileDestination()).isEqualTo(EXPORT_FILE_DESTINATION);
    assertThat(actualExportFileRow.getRow()).isEqualTo("\"test IAC\"");

    ArgumentCaptor<EventDTO> eventCaptor = ArgumentCaptor.forClass(EventDTO.class);
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            eq("Export file generated with pack code " + PACK_CODE),
            eq(EventType.EXPORT_FILE),
            eventCaptor.capture(),
            any(OffsetDateTime.class));

    EventHeaderDTO actualHeader = eventCaptor.getValue().getHeader();
    Assertions.assertThat(actualHeader.getCorrelationId()).isEqualTo(actionRule.getId());
  }

  @Test
  void testProcessFulfilment() {
    // Given
    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setPackCode(PACK_CODE);
    exportFileTemplate.setExportFileDestination(EXPORT_FILE_DESTINATION);
    exportFileTemplate.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});

    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setExportFileTemplate(exportFileTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setBatchId(UUID.fromString("6a127d58-c1cb-489c-a3f5-72014a0c32d6"));
    fulfilmentToProcess.setBatchQuantity(200);
    fulfilmentToProcess.setCorrelationId(TEST_CORRELATION_ID);
    fulfilmentToProcess.setOriginatingUser(TEST_ORIGINATING_USER);
    fulfilmentToProcess.setUacMetadata(TEST_UAC_METADATA);

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac(UAC);
    uacQidDTO.setQid(QID);

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);

    // When
    underTest.process(fulfilmentToProcess);

    // Then
    ArgumentCaptor<ExportFileRow> exportFileRowArgumentCaptor =
        ArgumentCaptor.forClass(ExportFileRow.class);
    verify(exportFileRowRepository).save(exportFileRowArgumentCaptor.capture());
    ExportFileRow actualExportFileRow = exportFileRowArgumentCaptor.getValue();
    assertThat(actualExportFileRow.getPackCode()).isEqualTo(PACK_CODE);
    assertThat(actualExportFileRow.getExportFileDestination()).isEqualTo(EXPORT_FILE_DESTINATION);
    assertThat(actualExportFileRow.getRow()).isEqualTo("\"123\"|\"" + UAC + "\"|\"bar\"");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService)
        .saveAndEmitUacUpdateEvent(
            uacQidLinkCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo(UAC);
    assertThat(actualUacQidLink.getQid()).isEqualTo(QID);
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();
    assertThat(actualUacQidLink.getMetadata()).isEqualTo(TEST_UAC_METADATA);

    ArgumentCaptor<EventDTO> eventCaptor = ArgumentCaptor.forClass(EventDTO.class);
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            eq("Export file generated with pack code " + PACK_CODE),
            eq(EventType.EXPORT_FILE),
            eventCaptor.capture(),
            any(OffsetDateTime.class));

    EventHeaderDTO actualHeader = eventCaptor.getValue().getHeader();
    Assertions.assertThat(actualHeader.getCorrelationId()).isEqualTo(TEST_CORRELATION_ID);
    Assertions.assertThat(actualHeader.getOriginatingUser()).isEqualTo(TEST_ORIGINATING_USER);
  }

  @Test
  void testProcessFulfilmentWithPersonalisation() {
    // Given
    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setPackCode(PACK_CODE);
    exportFileTemplate.setExportFileDestination(EXPORT_FILE_DESTINATION);
    exportFileTemplate.setTemplate(
        new String[] {"__caseref__", "__uac__", "__request__.foo", "__request__.spam"});

    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setExportFileTemplate(exportFileTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setBatchId(UUID.fromString("6a127d58-c1cb-489c-a3f5-72014a0c32d6"));
    fulfilmentToProcess.setBatchQuantity(200);
    fulfilmentToProcess.setCorrelationId(TEST_CORRELATION_ID);
    fulfilmentToProcess.setOriginatingUser(TEST_ORIGINATING_USER);
    fulfilmentToProcess.setUacMetadata(TEST_UAC_METADATA);
    fulfilmentToProcess.setPersonalisation(Map.of("foo", "bar", "spam", "eggs"));

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac(UAC);
    uacQidDTO.setQid(QID);

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);

    // When
    underTest.process(fulfilmentToProcess);

    // Then
    ArgumentCaptor<ExportFileRow> exportFileRowArgumentCaptor =
        ArgumentCaptor.forClass(ExportFileRow.class);
    verify(exportFileRowRepository).save(exportFileRowArgumentCaptor.capture());
    ExportFileRow actualExportFileRow = exportFileRowArgumentCaptor.getValue();
    assertThat(actualExportFileRow.getPackCode()).isEqualTo(PACK_CODE);
    assertThat(actualExportFileRow.getExportFileDestination()).isEqualTo(EXPORT_FILE_DESTINATION);
    assertThat(actualExportFileRow.getRow()).isEqualTo("\"123\"|\"" + UAC + "\"|\"bar\"|\"eggs\"");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService)
        .saveAndEmitUacUpdateEvent(
            uacQidLinkCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo(UAC);
    assertThat(actualUacQidLink.getQid()).isEqualTo(QID);
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();
    assertThat(actualUacQidLink.getMetadata()).isEqualTo(TEST_UAC_METADATA);

    ArgumentCaptor<EventDTO> eventCaptor = ArgumentCaptor.forClass(EventDTO.class);
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            eq("Export file generated with pack code " + PACK_CODE),
            eq(EventType.EXPORT_FILE),
            eventCaptor.capture(),
            any(OffsetDateTime.class));

    EventHeaderDTO actualHeader = eventCaptor.getValue().getHeader();
    Assertions.assertThat(actualHeader.getCorrelationId()).isEqualTo(TEST_CORRELATION_ID);
    Assertions.assertThat(actualHeader.getOriginatingUser()).isEqualTo(TEST_ORIGINATING_USER);
  }

  @Test
  void testProcessFulfilmentNullPersonalisation() {
    // Given
    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setPackCode(PACK_CODE);
    exportFileTemplate.setExportFileDestination(EXPORT_FILE_DESTINATION);
    exportFileTemplate.setTemplate(new String[] {"__caseref__", "__uac__", "__request__.foo"});

    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setExportFileTemplate(exportFileTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setBatchId(UUID.fromString("6a127d58-c1cb-489c-a3f5-72014a0c32d6"));
    fulfilmentToProcess.setBatchQuantity(200);
    fulfilmentToProcess.setCorrelationId(TEST_CORRELATION_ID);
    fulfilmentToProcess.setOriginatingUser(TEST_ORIGINATING_USER);
    fulfilmentToProcess.setUacMetadata(TEST_UAC_METADATA);
    fulfilmentToProcess.setPersonalisation(null);

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac(UAC);
    uacQidDTO.setQid(QID);

    when(uacQidCache.getUacQidPair(anyInt())).thenReturn(uacQidDTO);

    // When
    underTest.process(fulfilmentToProcess);

    // Then
    ArgumentCaptor<ExportFileRow> exportFileRowArgumentCaptor =
        ArgumentCaptor.forClass(ExportFileRow.class);
    verify(exportFileRowRepository).save(exportFileRowArgumentCaptor.capture());
    ExportFileRow actualExportFileRow = exportFileRowArgumentCaptor.getValue();
    assertThat(actualExportFileRow.getPackCode()).isEqualTo(PACK_CODE);
    assertThat(actualExportFileRow.getExportFileDestination()).isEqualTo(EXPORT_FILE_DESTINATION);
    assertThat(actualExportFileRow.getRow()).isEqualTo("\"123\"|\"" + UAC + "\"|");

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService)
        .saveAndEmitUacUpdateEvent(
            uacQidLinkCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.getUac()).isEqualTo(UAC);
    assertThat(actualUacQidLink.getQid()).isEqualTo(QID);
    assertThat(actualUacQidLink.getCaze()).isEqualTo(caze);
    assertThat(actualUacQidLink.isActive()).isTrue();
    assertThat(actualUacQidLink.getMetadata()).isEqualTo(TEST_UAC_METADATA);

    ArgumentCaptor<EventDTO> eventCaptor = ArgumentCaptor.forClass(EventDTO.class);
    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            eq("Export file generated with pack code " + PACK_CODE),
            eq(EventType.EXPORT_FILE),
            eventCaptor.capture(),
            any(OffsetDateTime.class));

    EventHeaderDTO actualHeader = eventCaptor.getValue().getHeader();
    Assertions.assertThat(actualHeader.getCorrelationId()).isEqualTo(TEST_CORRELATION_ID);
    Assertions.assertThat(actualHeader.getOriginatingUser()).isEqualTo(TEST_ORIGINATING_USER);
  }
}

package uk.gov.ons.ssdc.caseprocessor.service;

import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;

@RunWith(MockitoJUnitRunner.class)
public class CaseToProcessProcessorTest {
  @Mock private PrintProcessor printProcessor;

  @InjectMocks CaseToProcessProcessor underTest;

  @Test
  public void testProccessPrintWaveOfContact() {
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

    // When
    underTest.process(caseToProcess);

    // Then
    verify(printProcessor)
        .processPrintRow(
            waveOfContact.getTemplate(),
            caze,
            caseToProcess.getBatchId(),
            caseToProcess.getBatchQuantity(),
            caseToProcess.getWaveOfContact().getPackCode(),
            caseToProcess.getWaveOfContact().getPrintSupplier());
  }
}

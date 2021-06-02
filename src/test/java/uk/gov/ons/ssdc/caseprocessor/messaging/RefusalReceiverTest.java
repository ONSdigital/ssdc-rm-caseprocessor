package uk.gov.ons.ssdc.caseprocessor.messaging;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.RefusalType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefusalReceiverTest {
    private static final UUID CASE_ID = UUID.randomUUID();

    @InjectMocks
    RefusalReceiver underTest;

    @Mock
    CaseService caseService;

    @Test
    public void testRefusal() {
        //Given
        RefusalDTO refusalDTO = new RefusalDTO();
        refusalDTO.setCaseId(CASE_ID);
        refusalDTO.setType(RefusalTypeDTO.HARD_REFUSAL);

        PayloadDTO payloadDTO = new PayloadDTO();
        payloadDTO.setRefusal(refusalDTO);

        ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
        responseManagementEvent.setPayload(payloadDTO);

        Case caze = new Case();
        caze.setId(CASE_ID);
        caze.setRefusalReceived(null);

        when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(caze);

        //When
        underTest.receiveMessage(responseManagementEvent);

        //Then
        ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
        verify(caseService).saveCaseAndEmitCaseUpdatedEvent(caseArgumentCaptor.capture());
        Case actualCase = caseArgumentCaptor.getValue();

        assertThat(actualCase.getId()).isEqualTo(CASE_ID);
        assertThat(actualCase.getRefusalReceived()).isEqualTo(RefusalType.HARD_REFUSAL);
    }

}
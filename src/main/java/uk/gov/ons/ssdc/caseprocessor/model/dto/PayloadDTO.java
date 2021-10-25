package uk.gov.ons.ssdc.caseprocessor.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class PayloadDTO {
  private ReceiptDTO receipt;
  private RefusalDTO refusal;
  private CaseUpdateDTO caseUpdate;
  private UacUpdateDTO uacUpdate;
  private InvalidCase invalidCase;
  private ExportFileFulfilmentDTO exportFileFulfilment;
  private TelephoneCaptureDTO telephoneCapture;
  private DeactivateUacDTO deactivateUac;
  private UpdateSampleSensitive updateSampleSensitive;
  private UpdateSample updateSample;
  private EqLaunchDTO eqLaunch;
  private UacAuthenticationDTO uacAuthentication;
  private EnrichedSmsFulfilment enrichedSmsFulfilment;
  private NewCase newCase;
  private SmsRequest smsRequest;
}

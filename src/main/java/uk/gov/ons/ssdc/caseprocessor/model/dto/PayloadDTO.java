package uk.gov.ons.ssdc.caseprocessor.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class PayloadDTO {
  private ResponseDTO response;
  private RefusalDTO refusal;
  private CollectionCase collectionCase;
  private UacDTO uac;
  private InvalidAddress invalidAddress;
  private FulfilmentDTO fulfilment;
  private TelephoneCaptureDTO telephoneCapture;
  private DeactivateUacDTO deactivateUac;
  private UpdateSampleSensitive updateSampleSensitive;
  private SmsFulfilment smsFulfilment;
  private UacQidDTO uacQidDTO;
}

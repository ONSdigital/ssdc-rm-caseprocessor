package uk.gov.ons.ssdc.caseprocessor.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ssdc.caseprocessor.client.UacQidServiceClient;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UacQidCacheTest {

  @Mock UacQidServiceClient uacQidServiceClient;

  @InjectMocks UacQidCache underTest;

  @Test
  void getUacQidPair() {
    ReflectionTestUtils.setField(underTest, "cacheMin", 1);
    ReflectionTestUtils.setField(underTest, "cacheFetch", 2);
    ReflectionTestUtils.setField(underTest, "uacQidGetTimout", 5);

    List<UacQidDTO> listUacDtos = new ArrayList();
    UacQidDTO uacQidDTO1 = new UacQidDTO();
    uacQidDTO1.setQid("12345");
    listUacDtos.add(uacQidDTO1);

    UacQidDTO uacQidDTO2 = new UacQidDTO();
    uacQidDTO1.setQid("54321");
    listUacDtos.add(uacQidDTO2);

    when(uacQidServiceClient.getUacQids(1, 2)).thenReturn(listUacDtos);

    UacQidDTO actualUacDto = underTest.getUacQidPair(1);
    assertThat(actualUacDto.getQid()).isEqualTo(uacQidDTO1.getQid());
  }
}

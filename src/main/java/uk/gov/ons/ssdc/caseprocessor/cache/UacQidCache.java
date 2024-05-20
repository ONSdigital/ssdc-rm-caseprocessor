package uk.gov.ons.ssdc.caseprocessor.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.ons.ssdc.caseprocessor.client.UacQidServiceClient;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;

@Component
@SuppressWarnings("LockOnBoxedPrimitive")
public class UacQidCache {
  private final UacQidServiceClient uacQidServiceClient;

  @Value("${uacservice.uacqid-cache-min}")
  private int cacheMin;

  @Value("${uacservice.uacqid-fetch-count}")
  private int cacheFetch;

  @Value("${uacservice.uacqid-get-timeout}")
  private long uacQidGetTimout;

  private static final Executor executor = Executors.newFixedThreadPool(8);

  private BlockingQueue<UacQidDTO> uacQidLinkQueue = new LinkedBlockingQueue<>();
  private Boolean isToppingUpQueue = false;

  public UacQidCache(UacQidServiceClient uacQidServiceClient) {
    this.uacQidServiceClient = uacQidServiceClient;
  }

  public UacQidDTO getUacQidPair() {
    try {
      topUpCache();
      UacQidDTO uacQidDTO =
              uacQidLinkQueue.poll(uacQidGetTimout, TimeUnit.SECONDS);

      if (uacQidDTO == null) {
        // The cache topper upper is executed in a separate thread, which can fail if uacqid api
        // down
        // So check we get a non null result otherwise throw a RunTimeException to re-enqueue msg
        throw new RuntimeException(
            "Timeout getting UacQidDTO");
      }

      // Put the UAC-QID back into the cache if the transaction rolls back
      if (TransactionSynchronizationManager.isActualTransactionActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                  uacQidLinkQueue.add(uacQidDTO);
                }
              }
            });
      }

      return uacQidDTO;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void topUpCache() {
    synchronized (isToppingUpQueue) {
      if (!isToppingUpQueue &&
              uacQidLinkQueue.size() < cacheMin) {
        isToppingUpQueue = true;
      } else {
        return;
      }
    }

    executor.execute(
        () -> {
          try {
            uacQidLinkQueue
                .addAll(uacQidServiceClient.getUacQids(cacheFetch));
          } finally {
            isToppingUpQueue = false;
          }
        });
  }
}

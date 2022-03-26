package cloud.ptl.paperlesswebdavingester.ingester.ingestion;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.ResourceRepository;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.StatusRepository;
import cloud.ptl.paperlesswebdavingester.ingester.services.UIAsyncRelay;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
@AllArgsConstructor
@Slf4j
public class IngestionTracker {
    private final StatusRepository statusRepository;
    private final ResourceRepository resourceRepository;
    private final UIAsyncRelay uiAsyncRelay;

    public Status addOngoingIngestion(IngestionMode ingestionMode) {
        Status status = new Status();
        status.setRunning(true);
        status.setIngestionMode(ingestionMode);
        status.setIngestedResources(new ArrayList<>());
        return statusRepository.save(status);
    }

    public Status addIngestedResource(Resource resource, Status status) {
        resource.setIngestedIn(status);
        resourceRepository.save(resource);
        callUIAsyncRelay(resource, status);
        return statusRepository.findById(status.getId()).get();
    }

    public Status endIngestion(Status status) {
        status.setEndTime(LocalDateTime.now());
        status.setRunning(false);
        return statusRepository.save(status);
    }

    private void callUIAsyncRelay(Resource resource, Status status) {
        IngestionMode ingestionMode = status.getIngestionMode();
        switch (ingestionMode) {
            case HARD_SYNC_FROM_PAPERLESS -> uiAsyncRelay.callListeners(UIAsyncRelay.Topic.FROM_PAPERLESS, resource);
            case HARD_SYNC_FROM_WEBDAV -> uiAsyncRelay.callListeners(UIAsyncRelay.Topic.FROM_WEBDAV, resource);
            default -> log.warn("Not calling UI to add new files as there is unknown ingestion mode: " + ingestionMode);
        }
    }
}

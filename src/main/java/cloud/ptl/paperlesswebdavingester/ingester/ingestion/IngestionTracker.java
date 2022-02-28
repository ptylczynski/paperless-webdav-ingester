package cloud.ptl.paperlesswebdavingester.ingester.ingestion;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.ResourceRepository;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.StatusRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
@AllArgsConstructor
public class IngestionTracker {
    private final StatusRepository statusRepository;
    private final ResourceRepository resourceRepository;

    public Status addOngoingIngestion(IngestionMode ingestionMode) {
        Status status = new Status();
        status.setRunning(true);
        status.setIngestionMode(ingestionMode);
        status.setIngestedResources(new ArrayList<>());
        return statusRepository.save(status);
    }

    public Status addIngestedResource(Resource resource, Status status) {
        // status.getIngestedResources().add(resource);
        resource.setIngestedIn(status);
        resourceRepository.save(resource);
        return statusRepository.findById(status.getId()).get();
    }

    public Status endIngestion(Status status) {
        status.setEndTime(LocalDateTime.now());
        status.setRunning(false);
        return statusRepository.save(status);
    }
}

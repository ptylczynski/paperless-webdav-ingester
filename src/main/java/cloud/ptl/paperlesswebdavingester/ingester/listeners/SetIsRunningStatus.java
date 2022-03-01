package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.StatusRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@Slf4j
public class SetIsRunningStatus implements StartupAction {
    private final StatusRepository statusRepository;

    @Override
    public void run() {
        List<Status> nonStoppedStatuses = statusRepository.findAllByIsRunningEquals(true);
        for (Status status : nonStoppedStatuses) {
            log.info("Setting status: " + status + " to isRunning=false");
            status.setRunning(false);
            statusRepository.save(status);
        }
    }
}

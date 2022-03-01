package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.StatusRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class StartupListener implements ApplicationRunner {
    private final StatusRepository statusRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Status> nonStoppedStatuses = statusRepository.findAllByIsRunningEquals(true);
        for (Status status : nonStoppedStatuses) {
            log.info("Setting status: " + status + " to isRunning=false");
            status.setRunning(false);
            statusRepository.save(status);
        }
    }
}

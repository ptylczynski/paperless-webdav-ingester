package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import cloud.ptl.paperlesswebdavingester.ingester.services.CorrespondentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CreateCorrespondents implements StartupAction {
    public final CorrespondentService correspondentService;

    public CreateCorrespondents(CorrespondentService correspondentService) {
        this.correspondentService = correspondentService;
    }

    @Override
    public void run() {
        log.info("Checking if all default correspondents exists in Paperless platform");
        correspondentService.createDefaultTags();

        log.info("Syncing correspondents from Paperless to Ingester");
        correspondentService.syncWithPaperless();
    }
}

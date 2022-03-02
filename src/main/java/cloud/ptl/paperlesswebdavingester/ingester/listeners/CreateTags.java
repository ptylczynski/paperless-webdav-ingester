package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import cloud.ptl.paperlesswebdavingester.ingester.services.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@Slf4j
public class CreateTags implements StartupAction {
    private final TagService tagService;

    public CreateTags(TagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public void run() {
        log.info("Checking if all default tags exists in Paperless platform");
        tagService.createDefaultTags();

        log.info("Syncing tags from Paperless to Ingester");
        tagService.syncWithPaperless();
    }
}

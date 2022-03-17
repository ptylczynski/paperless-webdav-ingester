package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.services.ResourceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
public class RemoveLocalFiles implements StartupAction {
    private final ResourceService resourceService;

    @Override
    public void run() {
        log.info("Deleting all local files");
        for (Resource resource : resourceService.findAllByIsLocalResourcePresent()) {
            log.info("Deleting local files for " + resource);
            FileUtils.deleteQuietly(resource.getFile());
            resource.setIsLocalCopyPresent(false);
            resourceService.save(resource);
        }
    }
}

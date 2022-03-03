package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import cloud.ptl.paperlesswebdavingester.ingester.services.DocumentTypeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
public class CreateDocumentTypes implements StartupAction {
    private final DocumentTypeService documentTypeService;

    @Override
    public void run() {
        log.info("Checking if all default document type exists in Paperless platform");
        documentTypeService.createDefaultTags();

        log.info("Syncing document types from Paperless to Ingester");
        documentTypeService.syncWithPaperless();
    }
}

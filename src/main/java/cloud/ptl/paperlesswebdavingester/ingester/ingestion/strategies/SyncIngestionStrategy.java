package cloud.ptl.paperlesswebdavingester.ingester.ingestion.strategies;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.StatusRepository;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionTracker;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.PaperlessService;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.dto.PaperlessDocument;
import cloud.ptl.paperlesswebdavingester.ingester.services.LocalStorageService;
import cloud.ptl.paperlesswebdavingester.ingester.services.ResourceService;
import cloud.ptl.paperlesswebdavingester.ingester.services.WebDavService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class SyncIngestionStrategy implements IngestionStrategy {
    private final StatusRepository statusRepository;
    private final PaperlessService paperlessService;
    private final ResourceService resourceService;
    private final WebDavService webDavService;
    private final IngestionTracker ingestionTracker;
    private final LocalStorageService localStorageService;

    @Override
    public boolean supports(IngestionMode ingestionMode) {
        return ingestionMode.equals(IngestionMode.HARD_SYNC_FROM_PAPERLESS);
    }

    @Override
    public boolean canStart() {
        return !statusRepository.existsByIsRunningEquals(true);
    }

    @Override
    public void ingest(Map<Object, Object> params) throws IngestionException {
        ingest();
    }

    @Override
    public void ingest() throws IngestionException {
        Status status = ingestionTracker.addOngoingIngestion(IngestionMode.HARD_SYNC_FROM_PAPERLESS);
        List<PaperlessDocument> allDocuments = paperlessService.getAllDocuments();
        for (PaperlessDocument paperlessDocument : allDocuments) {
            log.info("Syncing from paperless documetn: " + paperlessDocument);
            if (paperlessService.isChanged(paperlessDocument)) {
                Optional<Resource> resourceOptional = resourceService.findByPaperlessId(paperlessDocument.getId());
                Resource resource = null;
                if (resourceOptional.isEmpty()) {
                    resource = resourceService.create(paperlessDocument);
                } else {
                    resource = resourceOptional.get();
                }
                log.info("Document " + paperlessDocument + " is changed!");
                paperlessService.download(resource, paperlessDocument);
                try {
                    log.info("Downloading resource: " + resource);
                    webDavService.save(resource);
                    status = ingestionTracker.addIngestedResource(resource, status);
                    localStorageService.removeLocalCopy(resource);
                    resource.updateLastEdited();
                    resourceService.save(resource);
                } catch (IOException | URISyntaxException e) {
                    log.error("Cannot save file to webdav because of: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            log.info("Document " + paperlessDocument + " has no changes, not syncing");
        }
        ingestionTracker.endIngestion(status);
    }
}

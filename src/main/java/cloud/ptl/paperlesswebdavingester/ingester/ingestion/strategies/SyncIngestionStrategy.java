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
import cloud.ptl.paperlesswebdavingester.ingester.services.TagService;
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
    private final TagService tagService;

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
        purge();
        process();
    }

    private void purge() {
        try {
            webDavService.purge(webDavService.getDefaultSyncStoragePath());
            resourceService.findAllByIngestionMode(IngestionMode.HARD_SYNC_FROM_PAPERLESS).forEach(e -> {
                log.info("Deleting: " + e);
                resourceService.delete(e);
            });
        } catch (IOException | URISyntaxException e) {
            log.error("Cannot purge files in webdav because of: " + e.getMessage());
        }
    }

    private void process() {
        Status status = ingestionTracker.addOngoingIngestion(IngestionMode.HARD_SYNC_FROM_PAPERLESS);
        List<PaperlessDocument> allDocuments = paperlessService.getAllDocuments();
        if (allDocuments.isEmpty()) {
            log.info("There is no documents in Paperless");
        }
        for (PaperlessDocument paperlessDocument : allDocuments) {
            log.info("Syncing from paperless documetn: " + paperlessDocument);
            boolean isChanged = paperlessService.isChanged(paperlessDocument);
            boolean hasProperTag = tagService.hasAnyDefaultTagForDirection(paperlessDocument,
                    TagService.Direction.PAPERLESS_IMPORT);
            boolean isSupported = true;
            if (isChanged && isSupported && hasProperTag) {
                Resource resource = getResource(paperlessDocument);
                log.info("Document " + paperlessDocument + " is changed and supported!");
                try {
                    log.info("Downloading resource: " + resource);
                    downloadResource(resource, paperlessDocument, status);
                } catch (IOException | URISyntaxException e) {
                    log.error("Cannot save file to webdav because of: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            addRejectionCauseMessageToConsole(isChanged, isSupported, hasProperTag, paperlessDocument);
        }
        ingestionTracker.endIngestion(status);
    }

    private Resource getResource(PaperlessDocument paperlessDocument) {
        Optional<Resource> resourceOptional = resourceService.findByPaperlessId(paperlessDocument.getId());
        Resource resource = null;
        if (resourceOptional.isEmpty()) {
            resource = resourceService.create(paperlessDocument);
        } else {
            resource = resourceOptional.get();
            resource = tagService.addMissingDefaultTags(resource, TagService.Direction.PAPERLESS_IMPORT);
        }
        return resource;
    }

    private void downloadResource(Resource resource, PaperlessDocument paperlessDocument, Status status) throws
            IOException, URISyntaxException {
        paperlessService.download(resource, paperlessDocument);
        paperlessService.updateDocument(resource, TagService.Direction.PAPERLESS_IMPORT);
        log.info("Resource: " + resource + " updated to paperless");
        webDavService.save(resource);
        ingestionTracker.addIngestedResource(resource, status);
        localStorageService.removeLocalCopy(resource);
        resource.updateLastEdited();
        resourceService.save(resource);
    }

    private void addRejectionCauseMessageToConsole(boolean isChanged, boolean isSupported, boolean hasProperTag,
            PaperlessDocument paperlessDocument) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Document ").append(paperlessDocument).append(" is ");
        if (!isChanged) {
            stringBuilder.append("not changed, ");
        }
        if (!isSupported) {
            stringBuilder.append("not supported ");
        }
        if (!hasProperTag) {
            stringBuilder.append("does not have proper tag ");
        }
        stringBuilder.append("not syncing");
        log.info(stringBuilder.toString());
    }
}

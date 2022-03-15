package cloud.ptl.paperlesswebdavingester.ingester.ingestion.strategies;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.StatusRepository;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionTracker;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.Traverser;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.PaperlessService;
import cloud.ptl.paperlesswebdavingester.ingester.services.LocalStorageService;
import cloud.ptl.paperlesswebdavingester.ingester.services.ResourceService;
import cloud.ptl.paperlesswebdavingester.ingester.services.WebDavService;
import com.github.sardine.DavResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;

@Slf4j
@Data
@Component
public class HardIngestionStrategy implements IngestionStrategy {
    private final WebDavService webDavService;
    private final LocalStorageService storageService;
    private final PaperlessService paperlessService;
    private final ResourceService resourceService;
    private final IngestionTracker ingestionTracker;
    private final StatusRepository statusRepository;
    private final Traverser traverser;
    private Status status;

    public HardIngestionStrategy(WebDavService webDavService, LocalStorageService storageService,
            PaperlessService paperlessService, ResourceService resourceService, IngestionTracker ingestionTracker,
            StatusRepository statusRepository, Traverser traverser) {
        this.webDavService = webDavService;
        this.storageService = storageService;
        this.paperlessService = paperlessService;
        this.resourceService = resourceService;
        this.ingestionTracker = ingestionTracker;
        this.statusRepository = statusRepository;
        this.traverser = traverser;
    }

    @Override
    public boolean supports(IngestionMode ingestionMode) {
        return ingestionMode.equals(IngestionMode.HARD_SYNC_FROM_WEBDAV);
    }

    @Override
    public boolean canStart() {
        return !statusRepository.existsByIsRunningEquals(true);
    }

    @Override
    public void ingest(Map<Object, Object> params) throws IngestionException {
        try {
            status = this.ingestionTracker.addOngoingIngestion(IngestionMode.HARD_SYNC_FROM_WEBDAV);
            purge();
            startIngestion(params);
        } catch (IOException | URISyntaxException e) {
            throw new IngestionException(e.getMessage());
        }
    }

    @Override
    public void ingest() throws IngestionException {
        ingest(Map.of());
    }

    private void startIngestion(Map<Object, Object> params) throws IOException, URISyntaxException {
        String root;
        if (params.containsKey(Params.ROOT)) {
            root = (String) params.get(Params.ROOT);
        } else {
            root = "/";
        }
        traverser.traverse(root, this::process);
        status = ingestionTracker.endIngestion(status);
    }

    private void purge() {
        for (Resource resource : resourceService.findAll()) {
            log.info("Removing: " + resource);
            paperlessService.delete(resource);
            resourceService.delete(resource);
            storageService.removeLocalCopy(resource);
        }
    }

    private void process(DavResource webDavResource) {
        try {
            log.info("Checking " + webDavResource.getPath());
            if (!storageService.isSupported(webDavResource)) {
                log.info("This resource is not supported");
                return;
            }
            if (!storageService.isChanged(webDavResource)) {
                log.info("File not changed");
                return;
            }
            log.info("File changed and supported, downloading");
            final InputStream inputStream = webDavService.get(webDavResource);
            Resource resource = storageService.save(inputStream, webDavResource);
            paperlessService.save(resource);
            resource = resourceService.save(resource);
            status = ingestionTracker.addIngestedResource(resource, status);
            log.info("Resource saved as: " + resource);
        } catch (IOException | URISyntaxException e) {
            log.warn("Cannot process resource because of: " + e.getMessage());
        }
    }

    public enum Params {
        ROOT
    }
}

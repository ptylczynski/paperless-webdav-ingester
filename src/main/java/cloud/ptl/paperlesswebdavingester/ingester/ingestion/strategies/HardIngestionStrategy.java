package cloud.ptl.paperlesswebdavingester.ingester.ingestion.strategies;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.ResourceRepository;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.StatusRepository;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionTracker;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.PaperlessService;
import cloud.ptl.paperlesswebdavingester.ingester.services.LocalStorageService;
import cloud.ptl.paperlesswebdavingester.ingester.services.WebDavService;
import com.github.sardine.DavResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@Component
public class HardIngestionStrategy implements IngestionStrategy {
    private final WebDavService webDavService;
    private final LocalStorageService storageService;
    private final PaperlessService paperlessService;
    private final ResourceRepository resourceRepository;
    private final IngestionTracker ingestionTracker;
    private final StatusRepository statusRepository;
    private Status status;

    public HardIngestionStrategy(WebDavService webDavService, LocalStorageService storageService,
            PaperlessService paperlessService, ResourceRepository resourceRepository, IngestionTracker ingestionTracker,
            StatusRepository statusRepository) {
        this.webDavService = webDavService;
        this.storageService = storageService;
        this.paperlessService = paperlessService;
        this.resourceRepository = resourceRepository;
        this.ingestionTracker = ingestionTracker;
        this.statusRepository = statusRepository;
    }

    @Override
    public boolean supports(IngestionMode ingestionMode) {
        return ingestionMode.equals(IngestionMode.HARD);
    }

    @Override
    public boolean canStart() {
        return !statusRepository.existsByIsRunningEquals(true);
    }

    @Override
    public void ingest(Map<Object, Object> params) throws IngestionException {
        try {
            status = this.ingestionTracker.addOngoingIngestion(IngestionMode.HARD);
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
        if (params.containsKey(Params.ROOT)) {
            traverse((String) params.get(Params.ROOT));
        } else {
            traverse("/");
        }
        status = ingestionTracker.endIngestion(status);
    }

    private void purge() {
        for (Resource resource : resourceRepository.findAll()) {
            log.info("Removing: " + resource);
            paperlessService.delete(resource);
            resourceRepository.delete(resource);
            storageService.removeLocalCopy(resource);
        }
    }

    public void traverse(String path) throws IOException, URISyntaxException {
        List<DavResource> resources = webDavService.list(path);
        // first is always path, this will cause endless loop
        resources = resources.subList(1, resources.size());
        for (DavResource resource : resources) {
            if (resource.isDirectory()) {
                traverse(resource.getPath());
            } else {
                process(resource);
            }
        }
    }

    private void process(DavResource webDavResource) throws IOException, URISyntaxException {
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
        status = ingestionTracker.addIngestedResource(resource, status);
    }

    public enum Params {
        ROOT
    }
}

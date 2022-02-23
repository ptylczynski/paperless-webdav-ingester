package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.PaperlessService;
import com.github.sardine.DavResource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class IngestionService {
    private final WebDavService webDavService;
    private final LocalStorageService storageService;
    private final PaperlessService paperlessService;

    public void startIngest() throws IOException, URISyntaxException {
        startIngest("/");
    }

    public void startIngest(String root) throws IOException, URISyntaxException {
        traverse(root);
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
    }
}

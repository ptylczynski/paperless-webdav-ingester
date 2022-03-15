package cloud.ptl.paperlesswebdavingester.ingester.ingestion;

import cloud.ptl.paperlesswebdavingester.ingester.services.WebDavService;
import com.github.sardine.DavResource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

/*
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
 */
@Component
@AllArgsConstructor
@Slf4j
public class Traverser {
    private final WebDavService webDavService;

    public void traverse(String root, Consumer<DavResource> process) {
        try {
            List<DavResource> resources = webDavService.list(root);
            // first is always path, this will cause endless loop
            resources = resources.subList(1, resources.size());
            for (DavResource resource : resources) {
                if (resource.isDirectory()) {
                    traverse(resource.getPath(), process);
                } else {
                    process.accept(resource);
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.warn("Cannot get resources to traverse because of: " + e.getMessage());
        }
    }
}

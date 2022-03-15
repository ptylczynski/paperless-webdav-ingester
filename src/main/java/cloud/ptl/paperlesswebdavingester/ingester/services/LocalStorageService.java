package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.dto.PaperlessDocument;
import com.github.sardine.DavResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class LocalStorageService {
    private final ResourceService resourceService;
    @Value("${storage.base-dir}")
    private String basePath;
    @Value("#{'${storage.formats}'.split(',')}")
    private List<String> supportedFormats;

    public LocalStorageService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    private Path createInternalPath(String path) {
        String fileName = "";
        if (path.contains("/")) {
            fileName = path.substring(path.lastIndexOf("/"));
        } else {
            fileName = path;
        }
        Path internalPath = Path.of(basePath, fileName);
        if (resourceService.existsByInternalPath(internalPath.toString())) {
            // there is more than one file with this name
            // add slug to name
            final String slug = DigestUtils.sha256Hex(path).substring(0, 5);
            return Path.of(basePath, slug, "-", fileName);
        }
        return internalPath;
    }

    public Resource removeLocalCopy(Resource resource) {
        File file = new File(resource.getInternalPath());
        if (FileUtils.deleteQuietly(file)) {
            resource.setIsLocalCopyPresent(false);
            return resourceService.save(resource);
        }
        log.error("Cannot delete faile: " + file.getAbsolutePath());
        return resource;
    }

    public Resource save(InputStream inputStream, DavResource davResource) throws IOException {
        final Path destinationPath = createInternalPath(davResource.getPath());
        Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        IOUtils.closeQuietly(inputStream);
        File file = destinationPath.toFile();
        return resourceService.updateResource(davResource, file);
    }

    public Resource save(Resource resource, Flux<DataBuffer> dataBuffer, PaperlessDocument paperlessDocument) {
        Path internalPath = createInternalPath(paperlessDocument.getTitle());
        DataBufferUtils.write(dataBuffer, internalPath, StandardOpenOption.CREATE).block();
        resource.setInternalPath(internalPath.toAbsolutePath().toString());
        resource.setIsLocalCopyPresent(true);
        return resourceService.save(resource);
    }

    public boolean isSupported(DavResource davResource) {
        final String path = davResource.getPath();
        final String extension = path.substring(path.lastIndexOf(".") + 1);
        return supportedFormats.contains(extension);
    }

    public boolean isChanged(DavResource davResource) {
        final String externalPath = davResource.getPath();
        Optional<Resource> resourceOptional = resourceService.findByExternalPath(externalPath);
        if (resourceOptional.isEmpty()) {
            return true;
        } else {
            return !resourceOptional.get().getEtag().equals(davResource.getEtag());
        }
    }
}

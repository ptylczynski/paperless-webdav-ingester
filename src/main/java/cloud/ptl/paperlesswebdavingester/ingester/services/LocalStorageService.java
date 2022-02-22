package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.ResourceRepository;
import com.github.sardine.DavResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class LocalStorageService {
    private final ResourceRepository resourceRepository;
    @Value("${storage.base-dir}")
    private String basePath;
    @Value("#{'${storage.formats}'.split(',')}")
    private List<String> supportedFormats;

    public LocalStorageService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    private Path createPath(String path) {
        String fileName = path.substring(path.lastIndexOf("/"));
        Path internalPath = Path.of(basePath, fileName);
        if (resourceRepository.existsByInternalPath(internalPath.toString())) {
            // there is more than one file with this name
            // add slug to name
            final String slug = DigestUtils.sha256Hex(path).substring(0, 5);
            return Path.of(basePath, slug, "-", fileName);
        }
        return internalPath;
    }

    public Resource removeLocalCopy(Resource resource) {
        File file = resource.getFile();
        if (FileUtils.deleteQuietly(file)) {
            resource.setFile(null);
            resource.setInternalPath(null);
            return resourceRepository.save(resource);
        }
        log.error("Cannot delete faile: " + file.getAbsolutePath());
        return resource;
    }

    public Resource save(InputStream inputStream, DavResource davResource) throws IOException {
        final Path destinationPath = createPath(davResource.getPath());
        Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        IOUtils.closeQuietly(inputStream);
        File file = destinationPath.toFile();
        return updateResource(davResource, file);
    }

    public boolean isSupported(DavResource davResource) {
        final String path = davResource.getPath();
        final String extension = path.substring(path.lastIndexOf(".") + 1);
        return supportedFormats.contains(extension);
    }

    public boolean isChanged(DavResource davResource) {
        final String externalPath = davResource.getPath();
        Optional<Resource> resourceOptional = resourceRepository.findByExternalPath(externalPath);
        if (resourceOptional.isEmpty()) {
            return true;
        } else {
            return !resourceOptional.get().getEtag().equals(davResource.getEtag());
        }
    }

    private Resource updateResource(DavResource davResource, File file) {
        Optional<Resource> resourceOptional = resourceRepository.findByExternalPath(davResource.getPath());
        if (resourceOptional.isEmpty()) {
            Resource newResource = new Resource();
            newResource.setEtag(davResource.getEtag());
            newResource.setExternalPath(davResource.getPath());
            newResource.setFile(file);
            newResource.setInternalPath(file.getAbsolutePath());
            return resourceRepository.save(newResource);
        } else {
            Resource resource = resourceOptional.get();
            resource.setEtag(davResource.getEtag());
            resource.setFile(file);
            resource.setInternalPath(file.getAbsolutePath());
            return resourceRepository.save(resource);
        }
    }
}

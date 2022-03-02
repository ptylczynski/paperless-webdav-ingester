package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.ResourceRepository;
import com.github.sardine.DavResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ResourceService {
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }

    public Optional<Resource> findByExternalPath(String path) {
        return resourceRepository.findByExternalPath(path);
    }

    public boolean existsByInternalPath(String path) {
        return resourceRepository.existsByInternalPath(path);
    }

    public List<Resource> findAll() {
        return (List<Resource>) resourceRepository.findAll();
    }

    public void delete(Resource resource) {
        resourceRepository.delete(resource);
    }

    public Resource updateResource(DavResource davResource, File file) {
        Optional<Resource> resourceOptional = resourceRepository.findByExternalPath(davResource.getPath());
        if (resourceOptional.isEmpty()) {
            Resource newResource = new Resource();
            newResource.setEtag(davResource.getEtag());
            newResource.setExternalPath(davResource.getPath());
            newResource.setInternalPath(file.getAbsolutePath());
            newResource.setIsLocalCopyPresent(true);
            newResource.setTags(new ArrayList<>());
            return resourceRepository.save(newResource);
        } else {
            Resource resource = resourceOptional.get();
            resource.setEtag(davResource.getEtag());
            resource.setInternalPath(file.getAbsolutePath());
            resource.setIsLocalCopyPresent(true);
            resource.setTags(new ArrayList<>());
            return resourceRepository.save(resource);
        }
    }
}

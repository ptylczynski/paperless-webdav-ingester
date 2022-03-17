package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Correspondent;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.DocumentType;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.ResourceRepository;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.dto.PaperlessDocument;
import com.github.sardine.DavResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final TagService tagService;
    private final CorrespondentService correspondentService;
    private final DocumentTypeService documentTypeService;

    public ResourceService(ResourceRepository resourceRepository, @Lazy TagService tagService,
            CorrespondentService correspondentService, DocumentTypeService documentTypeService) {
        this.resourceRepository = resourceRepository;
        this.tagService = tagService;
        this.correspondentService = correspondentService;
        this.documentTypeService = documentTypeService;
    }

    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }

    public Optional<Resource> findByPaperlessId(Long id) {
        return resourceRepository.findByPaperlessId(id);
    }

    public Optional<Resource> findByExternalPath(String path) {
        return resourceRepository.findByExternalPath(path);
    }

    public List<Resource> findAllByIngestionMode(IngestionMode ingestionMode) {
        return resourceRepository.findAllByIngestedIn_IngestionMode(ingestionMode);
    }

    public List<Resource> findAllByIsLocalResourcePresent() {
        return resourceRepository.findAllByIsLocalCopyPresent(true);
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

    public Resource create(PaperlessDocument paperlessDocument) {
        Correspondent correspondent = correspondentService.findByPaperlessIdOrFetchFromPaperless(
                paperlessDocument.getCorrespondent());
        DocumentType documentType = documentTypeService.findByPaperlessIdOrFetchFromPaperless(
                paperlessDocument.getDocument_type());
        Resource resource = new Resource();
        resource = tagService.addMissingDefaultTags(resource, TagService.Direction.PAPERLESS_IMPORT);
        resource.setPaperlessId(paperlessDocument.getId());
        resource.setCorrespondent(correspondent);
        resource.setDocumentType(documentType);
        resource.setLastEdited(LocalDateTime.of(1900, 1, 1, 1, 1));
        return resourceRepository.save(resource);
    }
}

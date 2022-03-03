package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.DocumentType;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.DocumentTypeRepository;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.PaperlessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DocumentTypeService {
    private final PaperlessService paperlessService;
    private final DocumentTypeRepository documentTypeRepository;

    @Value("${paperless.default-document-type}")
    private String defaultDocumentType;

    public DocumentTypeService(PaperlessService paperlessService, DocumentTypeRepository documentTypeRepository) {
        this.paperlessService = paperlessService;
        this.documentTypeRepository = documentTypeRepository;
    }

    public DocumentType createDocumentType(String name) {
        DocumentType documentType = paperlessService.createDocumentType(name);
        return documentTypeRepository.save(documentType);
    }

    public DocumentType createDocumentType(DocumentType documentType) {
        return documentTypeRepository.save(documentType);
    }

    public List<DocumentType> syncWithPaperless() {
        final List<DocumentType> documentTypes = paperlessService.getAllDocumentTypes();
        final List<DocumentType> storedDocumentTypes = (List<DocumentType>) documentTypeRepository.findAll();
        final List<DocumentType> created = new ArrayList<>();
        for (DocumentType documentType : documentTypes) {
            if (storedDocumentTypes.stream().noneMatch(e -> e.getName().equals(documentType.getName()))) {
                log.info("Ingester does not contains: " + documentType.getName() + " document type");
                created.add(createDocumentType(documentType));
            } else {
                created.add(documentTypeRepository.findByName(documentType.getName()).get());
            }
        }
        List<DocumentType> toRemove = storedDocumentTypes.stream()
                .filter(e -> created.stream().noneMatch(e1 -> e1.getId().equals(e.getId()))).toList();
        if (!toRemove.isEmpty()) {
            log.info("There are some removed from paperless document types: " +
                    String.join(", ", toRemove.stream().map(DocumentType::getName).toList()));
            documentTypeRepository.deleteAll(toRemove);
        }
        return created;
    }

    public List<DocumentType> createMissingDocumentTypes(List<String> documentTypesNamesToCheck) {
        final List<DocumentType> documentTypes = paperlessService.getAllDocumentTypes();
        final List<String> documentTypesAsString = documentTypes.stream().map(DocumentType::getName).toList();
        List<DocumentType> created = new ArrayList<>();
        for (String documentType : documentTypesNamesToCheck) {
            if (!documentTypesAsString.contains(documentType)) {
                log.info("Document type: " + documentType + " is missing, adding it");
                created.add(createDocumentType(documentType));
            }
        }
        return created;
    }

    public List<DocumentType> createDefaultTags() {
        return createMissingDocumentTypes(List.of(defaultDocumentType));
    }

    public List<DocumentType> getDefaultDocumentType() {
        return documentTypeRepository.findAllByNameIn(List.of(defaultDocumentType));
    }
}

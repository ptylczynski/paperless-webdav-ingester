package cloud.ptl.paperlesswebdavingester.ingester.paperless;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Correspondent;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.DocumentType;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Tag;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.ResourceRepository;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.dto.*;
import cloud.ptl.paperlesswebdavingester.ingester.services.CorrespondentService;
import cloud.ptl.paperlesswebdavingester.ingester.services.DocumentTypeService;
import cloud.ptl.paperlesswebdavingester.ingester.services.LocalStorageService;
import cloud.ptl.paperlesswebdavingester.ingester.services.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaperlessService {
    private final PaperlessConnectionInfo paperlessConnectionInfo;
    private WebClient paperlessClient;
    private final ResourceRepository resourceRepository;
    private final TagService tagService;
    private final CorrespondentService correspondentService;
    private final DocumentTypeService documentTypeService;
    private final LocalStorageService localStorageService;

    public PaperlessService(PaperlessConnectionInfo paperlessConnectionInfo, ResourceRepository resourceRepository,
            @Lazy TagService tagService, @Lazy CorrespondentService correspondentService,
            @Lazy DocumentTypeService documentTypeService, @Lazy LocalStorageService localStorageService) {
        this.paperlessConnectionInfo = paperlessConnectionInfo;
        this.resourceRepository = resourceRepository;
        this.tagService = tagService;
        this.correspondentService = correspondentService;
        this.documentTypeService = documentTypeService;
        this.localStorageService = localStorageService;
    }

    @PostConstruct
    public void init() {
        paperlessClient = WebClient.builder().baseUrl(paperlessConnectionInfo.getHost()).exchangeStrategies(
                        ExchangeStrategies.builder().codecs(e -> e.defaultCodecs().maxInMemorySize(Integer.MAX_VALUE)).build())
                .filter(ExchangeFilterFunctions.basicAuthentication(paperlessConnectionInfo.getLogin(),
                        paperlessConnectionInfo.getPassword())).build();
    }

    public void save(Resource resource) {
        try {
            saveWithHashedName(resource);
            getIdFromPaperless(resource, true);
            swapNameTo(resource, resource.getFileName());
        } catch (RestClientException e) {
            log.error("Exception during saving resource to paperless: " + e.getMessage());
        }
    }

    private Resource saveWithHashedName(Resource resource) {
        final MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("document", new FileSystemResource(new File(resource.getInternalPath())));
        multipartBodyBuilder.part("title", resource.getHashedExternalPath());
        paperlessClient.post().uri("api/documents/post_document/").contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build())).retrieve().bodyToMono(String.class)
                .block();
        return localStorageService.removeLocalCopy(resource);
    }

    private Resource getIdFromPaperless(Resource resource, boolean makeBackOff) {
        int retries = 0;
        Map<String, String> requestVariables = Map.of("query", resource.getHashedExternalPath());
        while (retries < 15) {
            log.info("Retry " + retries + " of 15 for document " + resource);
            makeTimeout(retries);
            try {
                PaperlessSearchResponse paperlessSearchResponse = paperlessClient.get()
                        .uri("api/documents/", requestVariables).retrieve().bodyToMono(PaperlessSearchResponse.class)
                        .block();
                Optional<PaperlessDocument> paperlessDocument = paperlessSearchResponse.getResults().stream()
                        .filter(e -> e.getTitle().equals(resource.getHashedExternalPath())).findFirst();
                if (paperlessDocument.isPresent()) {
                    resource.setPaperlessId(paperlessDocument.get().getId());
                    return resourceRepository.save(resource);
                }
            } catch (NullPointerException e) {
                log.warn("Cannot download document from paperless because of: " + e.getMessage() + " Retrying...");
            }
            if (!makeBackOff) {
                log.info(
                        "Paperless does not returned proper document, but makeBackOff was set to false, returning null..");
                return null;
            }
            log.info("Paperless is still processing document");
            retries++;
        }
        throw new RestClientException("Cannot download id of document: " + resource);
    }

    public List<PaperlessDocument> getAllDocuments() {
        try {
            PaperlessSearchResponse paperlessSearchResponse = paperlessClient.get().uri("api/documents/").retrieve()
                    .bodyToMono(PaperlessSearchResponse.class).block();
            return paperlessSearchResponse.getResults();
        } catch (NullPointerException e) {
            log.warn("Paperless returned no documents");
            return Collections.emptyList();
        }
    }

    public boolean isChanged(PaperlessDocument paperlessDocument) {
        Optional<Resource> resourceFromDB = resourceRepository.findByPaperlessId(paperlessDocument.getId());
        return resourceFromDB.map(value -> value.getLastEdited()
                        .isBefore(paperlessDocument.getModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()))
                .orElse(true);
    }

    public File download(Resource resource, PaperlessDocument paperlessDocument) {
        Flux<DataBuffer> dataBufferFlux = paperlessClient.get()
                .uri("/api/documents/" + resource.getPaperlessId() + "/download/").retrieve()
                .bodyToFlux(DataBuffer.class);
        return localStorageService.save(resource, dataBufferFlux, paperlessDocument).getFile();
    }

    public void swapNameTo(Resource resource, String newName) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        Tag tag = tagService.getDefaultTags().get(0);
        Correspondent correspondent = correspondentService.getDefaultCorrespondent().get(0);
        DocumentType documentType = documentTypeService.getDefaultDocumentType().get(0);
        form.add("title", newName);
        form.add("archive_serial_number", resource.getId().toString());
        form.add("correspondent", correspondent.getPaperlessId().toString());
        form.add("document_type", documentType.getPaperlessId().toString());
        form.add("tags", tag.getPaperlessId().toString());
        paperlessClient.put().uri("api/documents/" + resource.getPaperlessId() + "/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(form)).retrieve()
                .bodyToMono(PaperlessDocument.class).block();
        resource.getTags().add(tag);
        resource.setCorrespondent(correspondent);
        resource.setDocumentType(documentType);
    }

    private void makeTimeout(int retry) {
        long timeout = (long) Math.pow(2, retry);
        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void delete(Resource resource) {
        Long id = resource.getPaperlessId();
        if (id == null) {
            log.warn("Resource: " + resource + " has no assigned paperless id, trying to obtain it");
            Resource resource1 = getIdFromPaperless(resource, false);
            if (resource1 == null) {
                log.error("Cannot obtain additional information from paperless about record: " + resource +
                        ". Deleting it from DB");
                resourceRepository.delete(resource);
                return;
            }
            id = resource1.getPaperlessId();
        }
        try {
            paperlessClient.delete().uri("api/documents/" + id + "/").retrieve().bodyToMono(String.class).block();
        } catch (WebClientResponseException ex) {
            deleteIfStatus404(ex, resource);
        }
    }

    private void deleteIfStatus404(WebClientResponseException ex, Resource resource) {
        log.error("Http communication thrown exception: " + ex.getMessage());
        if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.error("Status code from paperless was 404, removing phantom record: " + resource);
            resourceRepository.delete(resource);
        }
    }

    public Tag findTagByPaperlessId(Long id) {
        try {
            PaperlessTag response = paperlessClient.get().uri("api/tags/" + id + "/").retrieve()
                    .bodyToMono(PaperlessTag.class).block();
            return response.toEntity();
        } catch (NullPointerException ex) {
            log.warn("Cannot download tags from paperless because of: " + ex.getMessage());
            return null;
        }
    }

    public List<Tag> getAllTags() {
        try {
            PaperlessTagsResponse response = paperlessClient.get().uri("api/tags/").retrieve()
                    .bodyToMono(PaperlessTagsResponse.class).block();
            return response.getResults().stream().map(PaperlessTag::toEntity).collect(Collectors.toList());
        } catch (NullPointerException ex) {
            log.warn("Cannot download tags from paperless because of: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    public Tag createTag(String name) {
        PaperlessTag response = paperlessClient.post().uri("api/tags/").body(BodyInserters.fromFormData("name", name))
                .retrieve().bodyToMono(PaperlessTag.class).block();
        return response.toEntity();
    }

    public List<Correspondent> getAllCorrespondents() {
        try {
            PaperlessCorrespondentResponse response = paperlessClient.get().uri("api/correspondents/").retrieve()
                    .bodyToMono(PaperlessCorrespondentResponse.class).block();
            return response.getResults().stream().map(PaperlessCorrespondent::toEntity).collect(Collectors.toList());
        } catch (NullPointerException ex) {
            log.warn("Cannot download tags from paperless because of: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    public Correspondent findCorrespondentByPaperlessId(Long id) {
        try {
            PaperlessCorrespondent response = paperlessClient.get().uri("api/correspondents/" + id + "/").retrieve()
                    .bodyToMono(PaperlessCorrespondent.class).block();
            return response.toEntity();
        } catch (NullPointerException ex) {
            log.warn("Cannot download tags from paperless because of: " + ex.getMessage());
            return null;
        }
    }

    public Correspondent createCorrespondent(String name) {
        PaperlessCorrespondent response = paperlessClient.post().uri("api/correspondents/")
                .body(BodyInserters.fromFormData("name", name)).retrieve().bodyToMono(PaperlessCorrespondent.class)
                .block();
        return response.toEntity();
    }

    public List<DocumentType> getAllDocumentTypes() {
        try {
            PaperlessDocumentTypeResponse response = paperlessClient.get().uri("api/document_types/").retrieve()
                    .bodyToMono(PaperlessDocumentTypeResponse.class).block();
            return response.getResults().stream().map(PaperlessDocumentType::toEntity).collect(Collectors.toList());
        } catch (NullPointerException ex) {
            log.warn("Cannot download tags from paperless because of: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    public DocumentType findDocumentTypeByPaperlessId(Long id) {
        try {
            PaperlessDocumentType response = paperlessClient.get().uri("api/document_types/" + id + "/").retrieve()
                    .bodyToMono(PaperlessDocumentType.class).block();
            return response.toEntity();
        } catch (NullPointerException ex) {
            log.warn("Cannot download tags from paperless because of: " + ex.getMessage());
            return null;
        }
    }

    public DocumentType createDocumentType(String name) {
        PaperlessDocumentType response = paperlessClient.post().uri("api/document_types/")
                .body(BodyInserters.fromFormData("name", name)).retrieve().bodyToMono(PaperlessDocumentType.class)
                .block();
        return response.toEntity();
    }
}

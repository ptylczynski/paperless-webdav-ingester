package cloud.ptl.paperlesswebdavingester.ingester.paperless;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.PaperlessConnectionInfo;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.services.LocalStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
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

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PaperlessService {
    private final PaperlessConnectionInfo paperlessConnectionInfo;
    private final LocalStorageService storageService;
    private WebClient paperlessClient;

    public PaperlessService(PaperlessConnectionInfo paperlessConnectionInfo, LocalStorageService storageService) {
        this.paperlessConnectionInfo = paperlessConnectionInfo;
        this.storageService = storageService;
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
            getIdFromPaperless(resource);
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
        return storageService.removeLocalCopy(resource);
    }

    private Resource getIdFromPaperless(Resource resource) {
        int retries = 0;
        PaperlessDocument paperlessDocument;
        Map<String, String> requestVariables = Map.of("query", resource.getHashedExternalPath());
        while (retries < 15) {
            log.info("Retry " + retries + " of 15 for document " + resource);
            makeTimeout(retries);
            paperlessDocument = paperlessClient.get().uri("api/documents/", requestVariables).retrieve()
                    .bodyToMono(PaperlessSearchResponse.class).block().getResults().get(0);
            // peperless will try to fetch anything that's resemble sha value which not always equals real value
            if (paperlessDocument.getTitle().equals(resource.getHashedExternalPath())) {
                resource.setPaperlessId(paperlessDocument.getId());
                return resource;
            }
            retries++;
        }
        throw new RestClientException("Cannot download id of document: " + resource);
    }

    public void swapNameTo(Resource resource, String newName) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("title", newName);
        form.add("archive_serial_number", resource.getId().toString());
        form.add("correspondent", "1");
        form.add("document_type", "1");
        paperlessClient.put().uri("api/documents/" + resource.getPaperlessId() + "/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(form)).retrieve()
                .bodyToMono(PaperlessDocument.class).block();
    }

    private void makeTimeout(int retry) {
        long timeout = (long) Math.pow(2, retry);
        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

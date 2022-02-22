package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.PaperlessConnectionInfo;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;

@Service
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
        paperlessClient = WebClient.builder().baseUrl(paperlessConnectionInfo.getHost())
                .filter(ExchangeFilterFunctions.basicAuthentication(paperlessConnectionInfo.getLogin(),
                        paperlessConnectionInfo.getPassword())).build();
    }

    public void save(Resource resource) {
        final MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("document", new FileSystemResource(resource.getFile()));
        paperlessClient.post().uri("api/documents/post_document/").contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build())).retrieve().bodyToMono(String.class)
                .block();
        storageService.removeLocalCopy(resource);
    }
}

package cloud.ptl.paperlesswebdavingester.ingester.services;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class WebDavService {

    private final StorageService storageService;
    @Value("${webdav.username}")
    private String username;
    @Value("${webdav.password}")
    private String password;

    public WebDavService(StorageService storageService) {
        this.storageService = storageService;
    }

    private Sardine createClient() {
        return SardineFactory.begin(username, password);
    }

    public List<DavResource> list(String path) throws IOException {
        return createClient().list(path);
    }

    public File get(String path) throws IOException {
        Sardine sardine = createClient();
        InputStream inputStream = sardine.get(path);
        return storageService.save(inputStream, path);
    }
}

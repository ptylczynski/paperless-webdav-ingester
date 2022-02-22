package cloud.ptl.paperlesswebdavingester.ingester.services;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

@Component
@Slf4j
public class WebDavService {

    private final LocalStorageService storageService;
    private final Sardine webDavClient;
    @Value("${webdav.host}")
    private String host;

    public WebDavService(LocalStorageService storageService, Sardine webDavClient) {
        this.webDavClient = webDavClient;
        this.storageService = storageService;
    }

    private String assembleFullPath(String path) throws MalformedURLException, URISyntaxException {
        // remove last slash if added
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // encode to proper url, otherwise spaces could break protocol
        URL url = new URL(host + "/" + path);
        return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                url.getQuery(), url.getRef()).toASCIIString();
    }

    public List<DavResource> list(DavResource resource) throws IOException, URISyntaxException {
        return list(resource.getPath());
    }

    public List<DavResource> list(String path) throws IOException, URISyntaxException {
        return webDavClient.list(assembleFullPath(path));
    }

    public InputStream get(DavResource resource) throws IOException, URISyntaxException {
        return get(resource.getPath());
    }

    public InputStream get(String path) throws IOException, URISyntaxException {
        return webDavClient.get(assembleFullPath(path));
    }
}

package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Tag;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
    @Value("${webdav.default.sync-storage-path}")
    private String defaultSyncStoragePath;

    public WebDavService(LocalStorageService storageService, Sardine webDavClient) {
        this.webDavClient = webDavClient;
        this.storageService = storageService;
    }

    private String assembleEncodedPath(String path) throws MalformedURLException, URISyntaxException {
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

    public String assembleExternalPath(Resource resource) {
        List<Tag> tags = resource.getTags();
        Tag pathTag = tags.stream().filter(e -> e.getName().startsWith("f:")).findFirst()
                .orElse(new Tag(null, "f:", null, null));
        String path = pathTag.getName().substring(2);
        if (defaultSyncStoragePath.startsWith("/")) {
            defaultSyncStoragePath = defaultSyncStoragePath.substring(1);
        }
        if (defaultSyncStoragePath.endsWith("/")) {
            defaultSyncStoragePath = defaultSyncStoragePath.substring(0, defaultSyncStoragePath.length() - 1);
        }
        if (path.isBlank()) {
            return "/" + defaultSyncStoragePath + "/" + resource.getFileName();
        }
        return "/" + defaultSyncStoragePath + "/" + pathTag.getName().substring(2) + "/" + resource.getFileName();
    }

    public DavResource save(Resource resource) throws IOException, URISyntaxException {
        if (resource.getExternalPath() == null || resource.getExternalPath().isBlank()) {
            resource.setExternalPath(assembleExternalPath(resource));
        }
        byte[] data = FileUtils.readFileToByteArray(resource.getFile());
        webDavClient.put(assembleEncodedPath(resource.getExternalPath()), data);
        List<DavResource> davResources = webDavClient.list(assembleEncodedPath(resource.getExternalPath()));
        if (davResources.isEmpty()) {
            throw new IOException("Cannot get file from WebDav server");
        }
        return davResources.get(0);
    }

    public List<DavResource> list(DavResource resource) throws IOException, URISyntaxException {
        return list(resource.getPath());
    }

    public List<DavResource> list(String path) throws IOException, URISyntaxException {
        return webDavClient.list(assembleEncodedPath(path));
    }

    public InputStream get(DavResource resource) throws IOException, URISyntaxException {
        return get(resource.getPath());
    }

    public InputStream get(String path) throws IOException, URISyntaxException {
        return webDavClient.get(assembleEncodedPath(path));
    }
}

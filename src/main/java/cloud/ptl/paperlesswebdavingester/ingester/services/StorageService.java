package cloud.ptl.paperlesswebdavingester.ingester.services;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class StorageService {
    @Value("${storage.base-dir}")
    private String basePath;

    private Path createPath(String fileName) {
        return Path.of(basePath, fileName);
    }

    public File save(InputStream inputStream, String fileName) throws IOException {
        final Path destinationPath = createPath(fileName);
        Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        IOUtils.closeQuietly(inputStream);
        return destinationPath.toFile();
    }
}

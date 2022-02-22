package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Data
@ConfigurationProperties(prefix = "paperless")
public class PaperlessConnectionInfo {
    private String host;
    private String login;
    private String password;

    public String base64EncodedCredentials() {
        final String credentials = login + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}

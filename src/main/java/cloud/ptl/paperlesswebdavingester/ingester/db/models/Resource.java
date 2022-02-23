package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "file")
@Data
public class Resource {
    @Id
    @GeneratedValue
    private Long id;
    private Long paperlessId;
    private String externalPath;
    private String internalPath;
    private Boolean isLocalCopyPresent;
    // generated on webdav side
    private String etag;

    public String getHashedExternalPath() {
        return DigestUtils.sha256Hex(externalPath);
    }

    public String getFileName() {
        return internalPath.substring(internalPath.lastIndexOf("/") + 1);
    }
}

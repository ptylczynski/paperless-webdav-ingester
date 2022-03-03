package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;

import javax.persistence.*;
import java.util.List;

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
    @ManyToOne
    private Status ingestedIn;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "tags_resources", joinColumns = @JoinColumn(name = "resource_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags;
    @ManyToOne(fetch = FetchType.EAGER)
    private Correspondent correspondent;
    @ManyToOne
    private DocumentType documentType;

    public String getHashedExternalPath() {
        return DigestUtils.sha256Hex(externalPath);
    }

    public String getFileName() {
        return internalPath.substring(internalPath.lastIndexOf("/") + 1);
    }
}

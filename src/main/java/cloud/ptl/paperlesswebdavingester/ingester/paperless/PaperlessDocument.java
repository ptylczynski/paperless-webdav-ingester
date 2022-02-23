package cloud.ptl.paperlesswebdavingester.ingester.paperless;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PaperlessDocument {
    private Long id;
    private String corespondent;
    private List<String> tags;
    private String documentType;
    private String title;
    private Date created;
    private Date modified;
    private Date added;
    private String archiveSerialNumber;
    private String originalFileName;
    private String archivedFileName;
    private String content;
}

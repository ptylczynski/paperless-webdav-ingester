package cloud.ptl.paperlesswebdavingester.ingester.paperless.dto;

import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Data
public class PaperlessDocument {
    private Long id;
    private String correspondent;
    private List<String> tags;
    private String document_type;
    private String title;
    private Date created;
    private Date modified;
    private Date added;
    private String archiveSerialNumber;
    private String originalFileName;
    private String archivedFileName;
    @ToString.Exclude
    private String content;
}

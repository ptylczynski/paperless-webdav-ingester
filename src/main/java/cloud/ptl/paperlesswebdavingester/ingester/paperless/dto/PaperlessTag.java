package cloud.ptl.paperlesswebdavingester.ingester.paperless.dto;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Tag;
import lombok.Data;

@Data
public class PaperlessTag {
    private Long id;
    private String slug;
    private String name;
    private Long colour;
    private String match;
    private Long matchingAlgorithm;
    private Long documentCount;

    public Tag toEntity() {
        return Tag.builder().paperlessId(id).name(name).build();
    }
}

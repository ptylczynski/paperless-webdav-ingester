package cloud.ptl.paperlesswebdavingester.ingester.paperless;

import lombok.Data;

import java.util.List;

@Data
public class PaperlessSearchResponse {
    private Long count;
    private Long next;
    private Long previous;
    private List<PaperlessDocument> results;
}

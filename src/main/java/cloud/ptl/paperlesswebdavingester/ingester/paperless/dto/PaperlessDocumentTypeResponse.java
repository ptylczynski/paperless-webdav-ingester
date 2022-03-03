package cloud.ptl.paperlesswebdavingester.ingester.paperless.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaperlessDocumentTypeResponse {
    private Long count;
    private List<PaperlessDocumentType> results;
}

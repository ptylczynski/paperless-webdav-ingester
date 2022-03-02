package cloud.ptl.paperlesswebdavingester.ingester.paperless.dto;

import lombok.Data;

import java.util.List;

/**
 * {
 * "count": 1,
 * "next": null,
 * "previous": null,
 * "results": [
 * {
 * "id": 1,
 * "slug": "test",
 * "name": "test",
 * "colour": 1,
 * "match": "",
 * "matching_algorithm": 1,
 * "is_insensitive": false,
 * "is_inbox_tag": false,
 * "document_count": 0
 * }
 * ]
 * }
 */
@Data
public class PaperlessTagsResponse {
    private Long count;
    private List<PaperlessTag> results;
}

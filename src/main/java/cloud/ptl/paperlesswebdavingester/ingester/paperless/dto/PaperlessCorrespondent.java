package cloud.ptl.paperlesswebdavingester.ingester.paperless.dto;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Correspondent;
import lombok.Data;

import java.time.LocalDateTime;

/*
{
            "id": 1,
            "slug": "test_correspondent",
            "name": "test_correspondent",
            "match": "",
            "matching_algorithm": 1,
            "is_insensitive": false,
            "document_count": 9,
            "last_correspondence": "2022-03-03T00:01:03+01:00"
        }
 */
@Data
public class PaperlessCorrespondent {
    private Long id;
    private String slug;
    private String name;
    private String match;
    private Long matchingAlgorithm;
    private boolean isInsensitive;
    private Long documentCount;
    private LocalDateTime lastCorrespondence;

    public Correspondent toEntity() {
        return Correspondent.builder().name(name).paperlessId(id).build();
    }
}

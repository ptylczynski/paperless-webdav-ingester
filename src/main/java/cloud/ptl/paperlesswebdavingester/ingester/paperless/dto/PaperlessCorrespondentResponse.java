package cloud.ptl.paperlesswebdavingester.ingester.paperless.dto;

import lombok.Data;

import java.util.List;

/*
{
    "count": 1,
    "next": null,
    "previous": null,
    "results": [
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
    ]
}
 */
@Data
public class PaperlessCorrespondentResponse {
    private Long count;
    private List<PaperlessCorrespondent> results;
}

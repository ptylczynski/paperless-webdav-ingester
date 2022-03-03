package cloud.ptl.paperlesswebdavingester.ingester.paperless.dto;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.DocumentType;
import lombok.Data;

/*
{
            "id": 1,
            "slug": "test_document_type",
            "name": "test_document_type",
            "match": "",
            "matching_algorithm": 1,
            "is_insensitive": false,
            "document_count": 2
        }
 */
@Data
public class PaperlessDocumentType {
    private Long id;
    private String slug;
    private String name;
    private String match;
    private boolean isInsensitive;
    private Long documentType;

    public DocumentType toEntity() {
        return DocumentType.builder().name(name).paperlessId(id).build();
    }
}

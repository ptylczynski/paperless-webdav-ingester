package cloud.ptl.paperlesswebdavingester.ingester.ingestion;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngestionException extends Exception {
    private final String message;
}

package cloud.ptl.paperlesswebdavingester.ingester.ingestion;

import java.util.Map;

public interface IngestionStrategy {
    boolean supports(IngestionMode ingestionMode);
    void ingest(Map<Object, Object> params) throws IngestionException;
    void ingest() throws IngestionException;
}

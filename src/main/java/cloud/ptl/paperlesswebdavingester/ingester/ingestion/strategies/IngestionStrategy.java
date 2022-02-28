package cloud.ptl.paperlesswebdavingester.ingester.ingestion.strategies;

import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;

import java.util.Map;

public interface IngestionStrategy {
    boolean supports(IngestionMode ingestionMode);
    boolean canStart();
    void ingest(Map<Object, Object> params) throws IngestionException;
    void ingest() throws IngestionException;
}

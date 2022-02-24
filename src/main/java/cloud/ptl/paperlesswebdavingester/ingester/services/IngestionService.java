package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionStrategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class IngestionService {
    private List<IngestionStrategy> ingestionStrategies;

    public void start(IngestionMode ingestionMode, Map<Object, Object> params) throws IngestionException {
        for (IngestionStrategy ingestionStrategy : ingestionStrategies) {
            if (ingestionStrategy.supports(ingestionMode)) {
                ingestionStrategy.ingest(params);
            }
        }
    }
}

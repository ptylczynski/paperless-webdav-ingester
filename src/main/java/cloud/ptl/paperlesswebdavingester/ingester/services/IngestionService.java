package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.strategies.IngestionStrategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class IngestionService {
    private List<IngestionStrategy> ingestionStrategies;

    public void start(IngestionMode ingestionMode) throws IngestionException {
        start(ingestionMode, Collections.emptyMap());
    }

    public void start(IngestionMode ingestionMode, Map<Object, Object> params) throws IngestionException {
        for (IngestionStrategy ingestionStrategy : ingestionStrategies) {
            if (ingestionStrategy.supports(ingestionMode)) {
                if (ingestionStrategy.canStart()) {
                    log.info("Starting ingestion via ingester strategy: " + ingestionMode);
                    ingestionStrategy.ingest(params);
                } else {
                    log.warn("Ingester" + ingestionStrategy.getClass().getSimpleName() +
                            " was found but cannot be used");
                    throw new IngestionException("Ingester" + ingestionStrategy.getClass().getSimpleName() +
                            " was found but cannot be used");
                }
            }
        }
    }
}

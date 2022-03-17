package cloud.ptl.paperlesswebdavingester.ingester.db.repositories;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends CrudRepository<Resource, Long> {
    List<Resource> findAllByIngestedIn_IngestionMode(IngestionMode ingestionMode);
    Optional<Resource> findByPaperlessId(Long paperlessId);
    Optional<Resource> findByExternalPath(String externalPath);
    boolean existsByInternalPath(String internalPath);
}

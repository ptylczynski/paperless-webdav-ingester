package cloud.ptl.paperlesswebdavingester.ingester.db.repositories;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ResourceRepository extends CrudRepository<Resource, Long> {
    Optional<Resource> findByExternalPath(String externalPath);
    boolean existsByInternalPath(String internalPath);
}

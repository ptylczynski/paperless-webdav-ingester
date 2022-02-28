package cloud.ptl.paperlesswebdavingester.ingester.db.repositories;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import org.springframework.data.repository.CrudRepository;

public interface StatusRepository extends CrudRepository<Status, Long> {
    boolean existsByIsRunningEquals(boolean running);
}

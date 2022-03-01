package cloud.ptl.paperlesswebdavingester.ingester.db.repositories;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Status;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StatusRepository extends CrudRepository<Status, Long> {
    boolean existsByIsRunningEquals(boolean running);
    List<Status> findAllByIsRunningEquals(boolean running);
}

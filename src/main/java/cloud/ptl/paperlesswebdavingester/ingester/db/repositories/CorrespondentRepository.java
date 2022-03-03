package cloud.ptl.paperlesswebdavingester.ingester.db.repositories;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Correspondent;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CorrespondentRepository extends CrudRepository<Correspondent, Long> {
    Optional<Correspondent> findByName(String name);
    List<Correspondent> findAllByNameIn(List<String> names);
}

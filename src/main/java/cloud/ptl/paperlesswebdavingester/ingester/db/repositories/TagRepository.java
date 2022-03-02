package cloud.ptl.paperlesswebdavingester.ingester.db.repositories;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Tag;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends CrudRepository<Tag, Long> {
    List<Tag> findAllByNameIn(List<String> names);
    Optional<Tag> findByName(String name);
}

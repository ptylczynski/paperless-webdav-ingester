package cloud.ptl.paperlesswebdavingester.ingester.db.repositories;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.DocumentType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTypeRepository extends CrudRepository<DocumentType, Long> {
    Optional<DocumentType> findByName(String name);
    List<DocumentType> findAllByNameIn(List<String> names);
}

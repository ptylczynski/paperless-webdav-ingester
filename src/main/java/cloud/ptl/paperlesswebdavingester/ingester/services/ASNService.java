package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import org.springframework.stereotype.Service;

@Service
public class ASNService {
    public Long getASN(Resource resource) {
        Long id = resource.getId();
        if (id != null) {
            return id;
        }
        throw new RuntimeException("Cannot create ASN for resource " + resource + " because it has not have id yet");
    }
}

package cloud.ptl.paperlesswebdavingester.ingester.services;

import org.springframework.stereotype.Component;

@Component
public class IngestionService {
    public void startIngest() {
        startIngest("/");
    }

    public void startIngest(String root) {

    }
}

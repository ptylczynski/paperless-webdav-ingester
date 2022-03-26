package cloud.ptl.paperlesswebdavingester.ingester.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.util.function.Consumer;

@Service
@Slf4j
public class UIAsyncRelay {
    private MultiValueMap<Topic, Consumer<Object>> listeners;

    @PostConstruct
    public void init() {
        listeners = new LinkedMultiValueMap<>();
    }

    public void addListener(Topic topic, Consumer<Object> listener) {
        listeners.add(topic, listener);
    }

    public void callListeners(Topic topic, Object object) {
        listeners.get(topic).forEach(e -> e.accept(object));
    }

    public enum Topic {
        FROM_PAPERLESS,
        FROM_WEBDAV;
    }
}

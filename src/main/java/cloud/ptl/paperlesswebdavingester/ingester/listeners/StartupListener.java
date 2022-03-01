package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class StartupListener implements ApplicationRunner {
    private List<StartupAction> actions;

    @Override
    public void run(ApplicationArguments args) {
        for (StartupAction action : actions) {
            action.run();
        }
    }
}

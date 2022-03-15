package cloud.ptl.paperlesswebdavingester.vaadin.pages;

import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.strategies.HardIngestionStrategy;
import cloud.ptl.paperlesswebdavingester.ingester.services.IngestionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Map;

@Route("")
public class MainPage extends VerticalLayout {

    private final IngestionService ingestionService;

    public MainPage(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
        Button harvestWebDav = new Button("Harvest WebDav");
        harvestWebDav.addClickListener(e -> {
            try {
                ingestionService.start(IngestionMode.HARD_SYNC_FROM_WEBDAV,
                        Map.of(HardIngestionStrategy.Params.ROOT, "/piotr"));
            } catch (IngestionException ex) {
                new Notification(ex.getMessage()).open();
            }
        });
        add(harvestWebDav);
        Button harvestPaperless = new Button("Harvest Paperless");
        harvestPaperless.addClickListener(e -> {
            try {
                ingestionService.start(IngestionMode.HARD_SYNC_FROM_PAPERLESS);
            } catch (IngestionException ex) {
                new Notification(ex.getMessage());
            }
        });
        add(harvestPaperless);
    }
}

package cloud.ptl.paperlesswebdavingester.vaadin.tabs;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionException;
import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import cloud.ptl.paperlesswebdavingester.ingester.services.IngestionService;
import cloud.ptl.paperlesswebdavingester.ingester.services.UIAsyncRelay;
import cloud.ptl.paperlesswebdavingester.vaadin.components.AccordionFileComponent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FromWebdavTab extends VerticalLayout {
    private final UI ui;
    private final UIAsyncRelay uiAsyncRelay;
    private final IngestionService ingestionService;
    private Button startHarvestBtn;
    private Accordion harvestedFiles;

    public FromWebdavTab(UI ui, UIAsyncRelay uiAsyncRelay, IngestionService ingestionService) {
        this.ui = ui;
        this.uiAsyncRelay = uiAsyncRelay;
        this.ingestionService = ingestionService;
        add(createStartHarvestButton());
        add(createAccordion());

        uiAsyncRelay.addListener(UIAsyncRelay.Topic.FROM_WEBDAV, arg -> ui.access(() -> addNewFile((Resource) arg)));
    }

    private VerticalLayout createStartHarvestButton() {
        startHarvestBtn = new Button("Start Harvest");
        startHarvestBtn.addClickListener(this::startHarvestEventListener);
        VerticalLayout vl = new VerticalLayout();
        vl.add(startHarvestBtn);
        vl.setWidthFull();
        vl.setAlignItems(Alignment.CENTER);
        return vl;
    }

    private void startHarvestEventListener(ClickEvent<Button> buttonClickEvent) {
        Notification.show("Harvesting");
        remove(harvestedFiles);
        add(createAccordion());
        new Thread(() -> {
            try {
                ingestionService.start(IngestionMode.HARD_SYNC_FROM_WEBDAV);
            } catch (IngestionException e) {
                log.error(e.getMessage());
                ui.access(() -> Notification.show(e.getMessage()));
            }
        }).start();
    }

    private Component createAccordion() {
        harvestedFiles = new Accordion();
        return harvestedFiles;
    }

    private void addNewFile(Resource resource) {
        String filename = resource.getFileName();
        harvestedFiles.add(filename, new AccordionFileComponent(filename));
    }
}

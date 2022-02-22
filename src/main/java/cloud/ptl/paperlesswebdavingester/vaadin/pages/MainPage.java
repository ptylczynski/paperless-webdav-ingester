package cloud.ptl.paperlesswebdavingester.vaadin.pages;

import cloud.ptl.paperlesswebdavingester.ingester.services.IngestionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.net.URISyntaxException;

@Route("")
public class MainPage extends VerticalLayout {

    private final IngestionService ingestionService;

    public MainPage(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
        Button btn = new Button("start");
        btn.addClickListener(e -> {
            try {
                ingestionService.startIngest();
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });
        add(btn);
    }
}

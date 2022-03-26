package cloud.ptl.paperlesswebdavingester.vaadin.pages;

import cloud.ptl.paperlesswebdavingester.ingester.services.IngestionService;
import cloud.ptl.paperlesswebdavingester.ingester.services.UIAsyncRelay;
import cloud.ptl.paperlesswebdavingester.vaadin.UILabels;
import cloud.ptl.paperlesswebdavingester.vaadin.tabs.FromPaperlessTab;
import cloud.ptl.paperlesswebdavingester.vaadin.tabs.FromWebdavTab;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;

@Route("")
@Push
public class MainPage extends AppLayout {
    private final UI ui;
    private final UIAsyncRelay uiAsyncRelay;
    private final IngestionService ingestionService;

    private H3 title;
    private Tabs tabs;

    public MainPage(UI ui, UIAsyncRelay uiAsyncRelay, IngestionService ingestionService) {
        this.ui = ui;
        this.uiAsyncRelay = uiAsyncRelay;
        this.ingestionService = ingestionService;
        bootstrap();
    }

    private void bootstrap() {
        addToNavbar(createTitle());
        addToNavbar(createTabs());
        setOpenDefaultTab();
    }

    private Component createTitle() {
        title = new H3("WebDav Ingester");
        title.getStyle().set("margin-left", "1em");
        return title;
    }

    private Component createTabs() {
        tabs = new Tabs();
        tabs.add(new Tab("From Paperless"), new Tab("From WebDav"));
        tabs.getStyle().set("margin-left", "2em");
        tabs.addSelectedChangeListener(this::selectedTabChangeListener);
        return tabs;
    }

    private void selectedTabChangeListener(Tabs.SelectedChangeEvent event) {
        String label = event.getSelectedTab().getLabel();
        if (label.equals(UILabels.FROM_PAPERLESS_LABEL.getLabel())) {
            setContent(new FromPaperlessTab(ui, uiAsyncRelay, ingestionService));
        } else if (label.equals(UILabels.FROM_WEBDAV_LABEL.getLabel())) {
            setContent(new FromWebdavTab(ui, uiAsyncRelay, ingestionService));
        }
    }

    private void setOpenDefaultTab() {
        tabs.setSelectedIndex(0);
        selectedTabChangeListener(new Tabs.SelectedChangeEvent(tabs, null, false));
    }
}

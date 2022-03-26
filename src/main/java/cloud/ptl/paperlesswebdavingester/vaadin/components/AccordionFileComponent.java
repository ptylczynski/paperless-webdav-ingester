package cloud.ptl.paperlesswebdavingester.vaadin.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class AccordionFileComponent extends VerticalLayout {
    private final String fileName;
    private H5 title;

    public AccordionFileComponent(String fileName) {
        this.fileName = fileName;
        bootstrap();
    }

    private void bootstrap() {
        add(createTitle());
    }

    private Component createTitle() {
        title = new H5(fileName);
        return title;
    }
}

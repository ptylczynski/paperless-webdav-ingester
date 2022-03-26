package cloud.ptl.paperlesswebdavingester.vaadin;

import lombok.Getter;

public enum UILabels {
    FROM_PAPERLESS_LABEL("From Paperless"),
    FROM_WEBDAV_LABEL("From WebDav");

    @Getter
    private String label;

    UILabels(String label) {
        this.label = label;
    }
}

package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import cloud.ptl.paperlesswebdavingester.ingester.db.converters.FileStringConverter;
import lombok.Data;

import javax.persistence.*;
import java.io.File;

@Entity
@Table(name = "file")
@Data
public class Resource {
    @Id
    @GeneratedValue
    private Long id;
    private String externalPath;
    private String internalPath;
    @Convert(converter = FileStringConverter.class)
    private File file;
    // generated on webdav side
    private String etag;
}

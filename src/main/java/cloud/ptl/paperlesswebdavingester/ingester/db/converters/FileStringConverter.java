package cloud.ptl.paperlesswebdavingester.ingester.db.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.File;

@Converter
public class FileStringConverter implements AttributeConverter<File, String> {

    @Override
    public String convertToDatabaseColumn(File file) {
        return file.getAbsolutePath();
    }

    @Override
    public File convertToEntityAttribute(String s) {
        return new File(s);
    }
}

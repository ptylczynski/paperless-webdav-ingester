package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "status")
@Data
public class Status {
    @Id
    @GeneratedValue
    private Long id;
    private boolean isRunning;
    @CreatedDate
    private Date runTime;
    private Date endTime;
}

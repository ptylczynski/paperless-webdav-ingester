package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import cloud.ptl.paperlesswebdavingester.ingester.ingestion.IngestionMode;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "status")
@Data
public class Status {
    @Id
    @GeneratedValue
    private Long id;
    private boolean isRunning;
    @CreationTimestamp
    private LocalDateTime runTime;
    private LocalDateTime endTime;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ingestedIn", fetch = FetchType.EAGER)
    @ToString.Exclude
    private List<Resource> ingestedResources;
    @Enumerated(EnumType.STRING)
    private IngestionMode ingestionMode;
}

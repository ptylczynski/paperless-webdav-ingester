package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "document_type")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long paperlessId;
    private String name;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "documentType")
    @ToString.Exclude
    private List<Resource> resources;
}

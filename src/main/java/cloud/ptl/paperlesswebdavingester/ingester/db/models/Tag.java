package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long paperlessId;
    @ManyToMany(mappedBy = "tags")
    @ToString.Exclude
    private List<Resource> resources;
}

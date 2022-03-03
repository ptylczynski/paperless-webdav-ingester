package cloud.ptl.paperlesswebdavingester.ingester.db.models;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "correspondent")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Correspondent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long paperlessId;
    private String name;

    @OneToMany(mappedBy = "correspondent", fetch = FetchType.EAGER)
    @ToString.Exclude
    private List<Resource> resources;
}

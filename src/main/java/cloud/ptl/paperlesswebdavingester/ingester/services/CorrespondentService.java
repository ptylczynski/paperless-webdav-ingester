package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Correspondent;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.CorrespondentRepository;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.PaperlessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class CorrespondentService {
    private final PaperlessService paperlessService;
    private final CorrespondentRepository correspondentRepository;

    @Value("${paperless.default.correspondent}")
    private String defaultCorrespondent;

    public CorrespondentService(PaperlessService paperlessService, CorrespondentRepository correspondentRepository) {
        this.paperlessService = paperlessService;
        this.correspondentRepository = correspondentRepository;
    }

    public Correspondent createCorrespondent(String name) {
        Correspondent correspondent = paperlessService.createCorrespondent(name);
        return correspondentRepository.save(correspondent);
    }

    public Correspondent createCorrespondent(Correspondent correspondent) {
        return correspondentRepository.save(correspondent);
    }

    public Correspondent findByPaperlessIdOrFetchFromPaperless(String id) {
        if (id == null) {
            return null;
        }
        Optional<Correspondent> correspondentOptional = correspondentRepository.findByPaperlessId(Long.valueOf(id));
        if (correspondentOptional.isEmpty()) {
            return createCorrespondent(paperlessService.findCorrespondentByPaperlessId(Long.valueOf(id)));
        } else {
            return correspondentOptional.get();
        }
    }

    public List<Correspondent> syncWithPaperless() {
        final List<Correspondent> correspondents = paperlessService.getAllCorrespondents();
        final List<Correspondent> storedCorrespondents = (List<Correspondent>) correspondentRepository.findAll();
        final List<Correspondent> created = new ArrayList<>();
        for (Correspondent correspondent : correspondents) {
            if (storedCorrespondents.stream().noneMatch(e -> e.getName().equals(correspondent.getName()))) {
                log.info("Ingester does not contains: " + correspondent.getName() + " correspondent");
                created.add(createCorrespondent(correspondent));
            } else {
                created.add(correspondentRepository.findByName(correspondent.getName()).get());
            }
        }
        List<Correspondent> toRemove = storedCorrespondents.stream()
                .filter(e -> created.stream().noneMatch(e1 -> e1.getId().equals(e.getId()))).toList();
        if (!toRemove.isEmpty()) {
            log.info("There are some removed from paperless correspondents: " +
                    String.join(", ", toRemove.stream().map(Correspondent::getName).toList()));
            correspondentRepository.deleteAll(toRemove);
        }
        return created;
    }

    public List<Correspondent> createMissingCorrespondents(List<String> correspondentsNamesToCheck) {
        final List<Correspondent> correspondents = paperlessService.getAllCorrespondents();
        final List<String> correspondentsAsString = correspondents.stream().map(Correspondent::getName).toList();
        List<Correspondent> created = new ArrayList<>();
        for (String correspondent : correspondentsNamesToCheck) {
            if (!correspondentsAsString.contains(correspondent)) {
                log.info("Correspondent: " + correspondent + " is missing, adding it");
                created.add(createCorrespondent(correspondent));
            }
        }
        return created;
    }

    public List<Correspondent> createDefaultTags() {
        return createMissingCorrespondents(List.of(defaultCorrespondent));
    }

    public List<Correspondent> getDefaultCorrespondent() {
        return correspondentRepository.findAllByNameIn(List.of(defaultCorrespondent));
    }
}

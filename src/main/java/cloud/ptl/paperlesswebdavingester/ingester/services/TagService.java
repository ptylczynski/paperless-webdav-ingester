package cloud.ptl.paperlesswebdavingester.ingester.services;

import cloud.ptl.paperlesswebdavingester.ingester.db.models.Resource;
import cloud.ptl.paperlesswebdavingester.ingester.db.models.Tag;
import cloud.ptl.paperlesswebdavingester.ingester.db.repositories.TagRepository;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.PaperlessService;
import cloud.ptl.paperlesswebdavingester.ingester.paperless.dto.PaperlessDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class TagService {
    private final PaperlessService paperlessService;
    private final TagRepository tagRepository;
    private final ResourceService resourceService;

    @Value("#{'${paperless.default.tags.import-from-paperless}'.split(',')}")
    private List<String> defaultTagsForPaperlessImport;

    @Value("#{'${paperless.default.tags.import-from-webdav}'.split(',')}")
    private List<String> defaultTagsForWebDavImport;

    public TagService(PaperlessService paperlessService, TagRepository tagRepository, ResourceService resourceService) {
        this.paperlessService = paperlessService;
        this.tagRepository = tagRepository;
        this.resourceService = resourceService;
    }

    public List<Tag> createDefaultTags() {
        List<Tag> tags = createMissingTags(defaultTagsForPaperlessImport);
        tags.addAll(createMissingTags(defaultTagsForWebDavImport));
        return tags;
    }

    public Tag createTag(String name) {
        Tag tag = paperlessService.createTag(name);
        return tagRepository.save(tag);
    }

    public Tag createTag(Tag tag) {
        return tagRepository.save(tag);
    }

    public List<Tag> findAllByIdOrFetchFromPaperless(List<String> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        List<Tag> tags = new ArrayList<>();
        for (String id : ids) {
            Optional<Tag> optionalTag = tagRepository.findByPaperlessId(Long.valueOf(id));
            if (optionalTag.isEmpty()) {
                tags.add(createTag(paperlessService.findTagByPaperlessId(Long.valueOf(id))));
            }
            tags.add(optionalTag.get());
        }
        return tags;
    }

    public List<Tag> syncWithPaperless() {
        final List<Tag> tags = paperlessService.getAllTags();
        final List<Tag> storedTags = (List<Tag>) tagRepository.findAll();
        final List<Tag> created = new ArrayList<>();
        for (Tag tag : tags) {
            if (storedTags.stream().noneMatch(e -> e.getName().equals(tag.getName()))) {
                log.info("Ingester does not contains: " + tag.getName() + " tag");
                created.add(createTag(tag));
            } else {
                created.add(tagRepository.findByName(tag.getName()).get());
            }
        }
        List<Tag> toRemove = storedTags.stream()
                .filter(e -> created.stream().noneMatch(e1 -> e1.getId().equals(e.getId()))).toList();
        if (!toRemove.isEmpty()) {
            log.info("There are some removed from paperless tags: " +
                    String.join(", ", toRemove.stream().map(Tag::getName).toList()));
            tagRepository.deleteAll(toRemove);
        }
        return created;
    }

    public List<Tag> createMissingTags(List<String> tagsNamesToCheck) {
        final List<Tag> tags = paperlessService.getAllTags();
        final List<String> tagsAsString = tags.stream().map(Tag::getName).toList();
        List<Tag> created = new ArrayList<>();
        for (String tag : tagsNamesToCheck) {
            if (!tagsAsString.contains(tag)) {
                log.info("Tag: " + tag + " is missing, adding it");
                created.add(createTag(tag));
            }
        }
        return (List<Tag>) tagRepository.findAll();
    }

    public List<Tag> getDefaultTags(Direction direction) {
        switch (direction) {
            case WEBDAV_IMPORT -> {
                return tagRepository.findAllByNameIn(defaultTagsForWebDavImport);
            }
            case PAPERLESS_IMPORT -> {
                return tagRepository.findAllByNameIn(defaultTagsForPaperlessImport);
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    public Resource addMissingDefaultTags(Resource resource, Direction direction) {
        List<Tag> tags = addMissingDefaultTags(resource.getTags(), direction);
        resource.setTags(tags);
        return resourceService.save(resource);
    }

    public List<Tag> addMissingDefaultTags(List<Tag> tags, Direction direction) {
        List<Tag> dbDefaultTags = getDefaultTags(direction);
        if (tags == null || tags.isEmpty()) {
            return dbDefaultTags;
        }
        tags.forEach(dbDefaultTags::remove);
        if (!dbDefaultTags.isEmpty()) {
            tags.addAll(dbDefaultTags);
        }
        return tags;
    }

    public boolean hasAnyDefaultTagForDirection(PaperlessDocument document, Direction direction) {
        List<String> defaultTags = getDefaultTags(direction).stream().map(e -> e.getPaperlessId().toString()).toList();
        return document.getTags().stream().anyMatch(defaultTags::contains);
    }

    public boolean hasTag(PaperlessDocument document, Tag tag) {
        String tagId = tag.getPaperlessId().toString();
        return document.getTags().stream().anyMatch(e -> e.equals(tagId));
    }

    public enum Direction {
        PAPERLESS_IMPORT,
        WEBDAV_IMPORT;
    }
}

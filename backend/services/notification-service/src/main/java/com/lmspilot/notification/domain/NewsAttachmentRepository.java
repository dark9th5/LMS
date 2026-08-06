package com.lmspilot.notification.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsAttachmentRepository extends JpaRepository<NewsAttachmentEntity, NewsAttachmentId> {
    List<NewsAttachmentEntity> findAllByNewsId(UUID id);
    List<NewsAttachmentEntity> findAllByNewsIdInOrderByNewsIdAscSortOrderAsc(Collection<UUID> ids);
    void deleteAllByNewsId(UUID id);
}

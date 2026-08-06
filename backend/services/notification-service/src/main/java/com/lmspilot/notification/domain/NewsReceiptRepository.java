package com.lmspilot.notification.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsReceiptRepository extends JpaRepository<NewsReceiptEntity, NewsReceiptId> {
    Optional<NewsReceiptEntity> findByNewsIdAndUserId(UUID newsId, UUID userId);
    List<NewsReceiptEntity> findAllByNewsIdInAndUserId(Collection<UUID> newsIds, UUID userId);
}

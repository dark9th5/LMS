package com.lmspilot.filestorage.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface FileEditSessionRepository extends JpaRepository<FileEditSessionEntity,UUID>{
    List<FileEditSessionEntity> findAllByFileIdAndStatusOrderByCreatedAtDesc(UUID id,FileEditSessionStatus status);
}

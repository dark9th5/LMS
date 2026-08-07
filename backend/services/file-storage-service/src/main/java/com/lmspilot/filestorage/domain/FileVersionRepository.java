package com.lmspilot.filestorage.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface FileVersionRepository extends JpaRepository<FileVersionEntity,UUID>{
    List<FileVersionEntity> findAllByFileIdOrderByVersionNumberDesc(UUID id);
    Optional<FileVersionEntity> findFirstByFileIdOrderByVersionNumberDesc(UUID id);
}

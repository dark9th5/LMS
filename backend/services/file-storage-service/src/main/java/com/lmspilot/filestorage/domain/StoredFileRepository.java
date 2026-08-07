package com.lmspilot.filestorage.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface StoredFileRepository extends JpaRepository<StoredFileEntity,UUID>{
    Optional<StoredFileEntity> findByStorageKey(String key);
    List<StoredFileEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID owner);
}

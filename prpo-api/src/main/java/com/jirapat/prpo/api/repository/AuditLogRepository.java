package com.jirapat.prpo.api.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jirapat.prpo.api.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a JOIN FETCH a.performedBy WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a JOIN FETCH a.performedBy ORDER BY a.createdAt DESC")
    Page<AuditLog> findAllWithPerformedBy(Pageable pageable);

    @Query("SELECT a FROM AuditLog a JOIN FETCH a.performedBy WHERE a.entityType = :entityType ORDER BY a.createdAt DESC")
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a JOIN FETCH a.performedBy WHERE a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}

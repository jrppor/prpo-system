package com.jirapat.prpo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jirapat.prpo.entity.Attachment;
import com.jirapat.prpo.entity.ReferenceType;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            ReferenceType referenceType, UUID referenceId);

    long countByReferenceTypeAndReferenceId(ReferenceType referenceType, UUID referenceId);
}

package com.app.records.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "record_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    private String fileType;
}

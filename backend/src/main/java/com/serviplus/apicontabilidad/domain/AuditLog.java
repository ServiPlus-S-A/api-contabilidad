package com.serviplus.apicontabilidad.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ADR-S3: Non-repudiation audit trail.
 * Records are WRITE-ONLY — never updated or deleted.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(nullable = false, name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(length = 2000)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}

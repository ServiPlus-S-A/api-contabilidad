package com.serviplus.apicontabilidad.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Pessimistic-locked sequence counter for generating unique document numbers.
 * One row per (tipo, anio) pair — e.g., ("COT", 2026) or ("FAC", 2026).
 */
@Entity
@Table(name = "contadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Long siguiente;
}

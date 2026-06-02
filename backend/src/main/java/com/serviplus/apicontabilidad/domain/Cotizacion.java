package com.serviplus.apicontabilidad.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cotizaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(nullable = false, name = "cliente_id")
    private Long clienteId;

    @Column(nullable = false, name = "cliente_nombre", length = 255)
    private String clienteNombre;

    @Column(nullable = false, name = "fecha_vigencia")
    private LocalDate fechaVigencia;

    @Column(length = 1000)
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal impuesto;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LineaCotizacion> lineas = new ArrayList<>();

    @Column(nullable = false, name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(nullable = false, name = "creado_por", length = 100)
    private String creadoPor;
}

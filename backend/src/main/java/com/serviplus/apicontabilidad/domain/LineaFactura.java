package com.serviplus.apicontabilidad.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lineas_factura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal cantidad;

    @Column(nullable = false, name = "precio_unitario", precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;
}

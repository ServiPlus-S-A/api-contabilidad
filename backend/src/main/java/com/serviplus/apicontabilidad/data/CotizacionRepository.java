package com.serviplus.apicontabilidad.data;

import com.serviplus.apicontabilidad.domain.Cotizacion;
import com.serviplus.apicontabilidad.domain.EstadoCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    List<Cotizacion> findAllByOrderByCreadoEnDesc();

    List<Cotizacion> findByCreadoPorOrderByCreadoEnDesc(String creadoPor);

    @Query("SELECT c FROM Cotizacion c WHERE c.estado = :estado " +
           "AND c.id NOT IN (SELECT f.cotizacionId FROM Factura f WHERE f.cotizacionId IS NOT NULL) " +
           "ORDER BY c.creadoEn DESC")
    List<Cotizacion> findFacturables(@Param("estado") EstadoCotizacion estado);
}

package com.serviplus.apicontabilidad.data;

import com.serviplus.apicontabilidad.domain.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    List<Cotizacion> findAllByOrderByCreadoEnDesc();

    List<Cotizacion> findByCreadoPorOrderByCreadoEnDesc(String creadoPor);
}

package com.serviplus.apicontabilidad.data;

import com.serviplus.apicontabilidad.domain.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByCreadoPorOrderByCreadoEnDesc(String creadoPor);
}

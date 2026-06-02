package com.serviplus.apicontabilidad.data;

import com.serviplus.apicontabilidad.domain.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbonoRepository extends JpaRepository<Abono, Long> {

    List<Abono> findByFacturaIdOrderByFechaDesc(Long facturaId);
}

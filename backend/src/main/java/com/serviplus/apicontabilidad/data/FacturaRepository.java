package com.serviplus.apicontabilidad.data;

import com.serviplus.apicontabilidad.domain.Factura;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByCreadoPorOrderByCreadoEnDesc(String creadoPor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Factura f WHERE f.id = :id")
    Optional<Factura> findByIdForUpdate(@Param("id") Long id);
}

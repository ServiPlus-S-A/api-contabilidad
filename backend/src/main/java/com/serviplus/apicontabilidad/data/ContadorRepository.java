package com.serviplus.apicontabilidad.data;

import com.serviplus.apicontabilidad.domain.Contador;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContadorRepository extends JpaRepository<Contador, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Contador> findByTipoAndAnio(String tipo, Integer anio);
}

package com.serviplus.apicontabilidad.utility;

import com.serviplus.apicontabilidad.data.ContadorRepository;
import com.serviplus.apicontabilidad.domain.Contador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Generates unique sequential document numbers using a pessimistic-locked
 * counter table to prevent duplicates under concurrent requests.
 *
 * Format: PREFIX-YYYY-NNNN (e.g., COT-2026-0001, FAC-2026-0042)
 */
@Service
@RequiredArgsConstructor
public class NumeroGenerator {

    private final ContadorRepository contadorRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String siguiente(String prefijo) {
        int anio = Year.now().getValue();

        Contador contador = contadorRepository.findByTipoAndAnio(prefijo, anio)
                .orElseGet(() -> {
                    Contador nuevo = Contador.builder()
                            .tipo(prefijo)
                            .anio(anio)
                            .siguiente(1L)
                            .build();
                    return contadorRepository.save(nuevo);
                });

        long seq = contador.getSiguiente();
        contador.setSiguiente(seq + 1);
        contadorRepository.save(contador);

        return "%s-%d-%04d".formatted(prefijo, anio, seq);
    }

    public boolean esDelAnioActual(String numero) {
        if (numero == null || numero.isBlank()) return false;
        String[] partes = numero.split("-");
        if (partes.length < 2) return false;
        try {
            return Integer.parseInt(partes[1]) == Year.now().getValue();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

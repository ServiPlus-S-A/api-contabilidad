package com.serviplus.apicontabilidad.utility;

import com.serviplus.apicontabilidad.data.ContadorRepository;
import com.serviplus.apicontabilidad.domain.Contador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NumeroGeneratorTest {

    @Mock
    private ContadorRepository contadorRepository;

    private NumeroGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new NumeroGenerator(contadorRepository);
    }

    @Nested
    @DisplayName("siguiente()")
    class Siguiente {

        @Test
        @DisplayName("formatea el número con prefijo, año y secuencia de 4 dígitos")
        void formatoCorrecto() {
            int anio = Year.now().getValue();
            Contador contador = Contador.builder()
                    .tipo("COT")
                    .anio(anio)
                    .siguiente(1L)
                    .build();
            when(contadorRepository.findByTipoAndAnio("COT", anio)).thenReturn(Optional.of(contador));
            when(contadorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String resultado = generator.siguiente("COT");

            assertThat(resultado).isEqualTo("COT-%d-0001".formatted(anio));
        }

        @Test
        @DisplayName("crea un nuevo contador cuando no existe entrada para el año")
        void creaContadorNuevo() {
            int anio = Year.now().getValue();
            when(contadorRepository.findByTipoAndAnio("FAC", anio)).thenReturn(Optional.empty());
            Contador nuevo = Contador.builder().tipo("FAC").anio(anio).siguiente(1L).build();
            when(contadorRepository.save(any())).thenReturn(nuevo);

            String resultado = generator.siguiente("FAC");

            assertThat(resultado).startsWith("FAC-").contains(String.valueOf(anio));
        }
    }

    @Nested
    @DisplayName("esDelAnioActual()")
    class EsDelAnioActual {

        @Test
        @DisplayName("retorna true para un número del año en curso")
        void anioActualRetornaTrue() {
            String numero = "COT-%d-0042".formatted(Year.now().getValue());
            assertThat(generator.esDelAnioActual(numero)).isTrue();
        }

        @Test
        @DisplayName("retorna false para un número de un año pasado")
        void anioAnteriorRetornaFalse() {
            String numero = "COT-%d-0001".formatted(Year.now().getValue() - 1);
            assertThat(generator.esDelAnioActual(numero)).isFalse();
        }

        @Test
        @DisplayName("retorna false para null")
        void nullRetornaFalse() {
            assertThat(generator.esDelAnioActual(null)).isFalse();
        }

        @Test
        @DisplayName("retorna false para cadena vacía")
        void vaciRetornaFalse() {
            assertThat(generator.esDelAnioActual("")).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando el formato no tiene guion separando el año")
        void formatoSinGuionRetornaFalse() {
            assertThat(generator.esDelAnioActual("INVALIDO")).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando el segundo segmento no es numérico")
        void segmentoNoNumericoRetornaFalse() {
            assertThat(generator.esDelAnioActual("COT-XXXX-0001")).isFalse();
        }
    }
}

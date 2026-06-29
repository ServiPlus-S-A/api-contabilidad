package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.data.UsuarioRepository;
import com.serviplus.apicontabilidad.domain.Usuario;
import com.serviplus.apicontabilidad.serializer.auth.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — unit tests")
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.JwtConfig("test-secret-minimum-32-chars-hmac", 86400000L),
                new AppProperties.IvaConfig(new BigDecimal("0.13")),
                new AppProperties.MinioConfig("http://localhost:9000", "key", "secret", "bucket"),
                new AppProperties.EmailConfig("test@test.com"),
                new AppProperties.CorsConfig("http://localhost:5173"),
                "http://localhost:8000"
        );
        authService = new AuthService(usuarioRepository, passwordEncoder, props);
    }

    @Test
    @DisplayName("login exitoso devuelve token JWT y role")
    void loginExitosoDevuelveTokenYRole() {
        Usuario usuario = Usuario.builder()
                .id(1L).username("admin").password("$2a$hashed").role("ROLE_ADMIN").activo(true)
                .build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass123", "$2a$hashed")).thenReturn(true);

        LoginResponse response = authService.login("admin", "pass123");

        assertThat(response.token()).isNotBlank();
        assertThat(response.role()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("login con usuario inexistente lanza 401 UNAUTHORIZED")
    void loginConUsuarioInexistenteLanza401() {
        when(usuarioRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("noexiste", "pass"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("login con cuenta inactiva lanza 401 UNAUTHORIZED")
    void loginConCuentaInactivaLanza401() {
        Usuario usuario = Usuario.builder()
                .id(2L).username("inactivo").password("$2a$hashed").role("ROLE_CLIENTE").activo(false)
                .build();
        when(usuarioRepository.findByUsername("inactivo")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login("inactivo", "pass"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("login con contraseña incorrecta lanza 401 UNAUTHORIZED")
    void loginConPasswordIncorrectaLanza401() {
        Usuario usuario = Usuario.builder()
                .id(3L).username("user").password("$2a$hashed").role("ROLE_CLIENTE").activo(true)
                .build();
        when(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrongpass", "$2a$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user", "wrongpass"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}

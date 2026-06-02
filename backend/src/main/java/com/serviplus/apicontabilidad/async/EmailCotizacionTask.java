package com.serviplus.apicontabilidad.async;

import com.serviplus.apicontabilidad.async.event.CotizacionAprobadaEvent;
import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.domain.Cotizacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <<Observer Pattern + Command Pattern>> — Listens for CotizacionAprobadaEvent
 * and asynchronously sends a notification email to the client.
 *
 * Known limitation: client email is currently a placeholder.
 * Production fix: call the Clientes microservice to retrieve the actual address.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailCotizacionTask {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Async("taskExecutor")
    @EventListener
    public void onCotizacionAprobada(CotizacionAprobadaEvent event) {
        Cotizacion cotizacion = event.getCotizacion();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.email().from());
            // TODO: resolve actual client email via Clientes microservice
            message.setTo(appProperties.email().from());
            message.setSubject("Cotización " + cotizacion.getNumero() + " ha sido aprobada");
            message.setText(buildBody(cotizacion));
            mailSender.send(message);
            log.info("Email de aprobación enviado para cotización {}", cotizacion.getNumero());
        } catch (Exception e) {
            log.error("Error al enviar email para cotización {}: {}", cotizacion.getNumero(), e.getMessage());
        }
    }

    private String buildBody(Cotizacion cotizacion) {
        return """
                Estimado/a %s,

                Su cotización %s ha sido aprobada.
                Total: %s
                Vigencia: %s

                Gracias por su preferencia.
                """.formatted(
                cotizacion.getClienteNombre(),
                cotizacion.getNumero(),
                cotizacion.getTotal(),
                cotizacion.getFechaVigencia()
        );
    }
}

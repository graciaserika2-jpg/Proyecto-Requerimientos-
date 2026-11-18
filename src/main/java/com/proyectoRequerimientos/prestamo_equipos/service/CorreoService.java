
package com.proyectoRequerimientos.prestamo_equipos.service;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CorreoService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestClient restClient;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.mail.from:}")
    private String correoRemitente;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM:onboarding@resend.dev}")
    private String resendFrom;

    public CorreoService(
            ObjectProvider<JavaMailSender> mailSenderProvider) {

        this.mailSenderProvider = mailSenderProvider;
        this.restClient = RestClient.create();
    }

    public void enviarActivacion(
            String correo,
            String nombre,
            String token) {

        String enlace = construirEnlace("/activar-cuenta", token);

        String asunto = "Activa tu cuenta - Préstamo UPIICSA";

        String mensaje =
                "Hola " + nombre + ",\n\n" +
                "Se ha creado una cuenta para ti en el sistema " +
                "de préstamo de equipos UPIICSA.\n\n" +
                "Para activar tu cuenta y establecer tu contraseña, " +
                "abre el siguiente enlace:\n\n" +
                enlace + "\n\n" +
                "Este enlace tiene una vigencia de 24 horas.\n\n" +
                "Si no solicitaste esta cuenta, puedes ignorar " +
                "este mensaje.\n\n" +
                "Sistema de Préstamo de Equipos UPIICSA.";

        enviar(correo, asunto, mensaje);
    }

    public void enviarRecuperacion(
            String correo,
            String nombre,
            String token) {

        String enlace = construirEnlace(
                "/restablecer-password", token);

        String asunto =
                "Recuperación de contraseña - Préstamo UPIICSA";

        String mensaje =
                "Hola " + nombre + ",\n\n" +
                "Recibimos una solicitud para restablecer " +
                "la contraseña de tu cuenta.\n\n" +
                "Para establecer una nueva contraseña, abre " +
                "el siguiente enlace:\n\n" +
                enlace + "\n\n" +
                "Este enlace tiene una vigencia de 30 minutos " +
                "y solamente puede utilizarse una vez.\n\n" +
                "Si no solicitaste este cambio, ignora este mensaje.\n\n" +
                "Sistema de Préstamo de Equipos UPIICSA.";

        enviar(correo, asunto, mensaje);
    }

    private String construirEnlace(String ruta, String token) {

        String tokenCodificado = URLEncoder.encode(
                token, StandardCharsets.UTF_8);

        return baseUrl.replaceAll("/+$", "") +
                ruta +
                "?token=" +
                tokenCodificado;
    }

    private void enviar(
            String destinatario,
            String asunto,
            String contenido) {

        // En Render se utiliza Resend.
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            enviarConResend(destinatario, asunto, contenido);
            return;
        }

        // Localmente se conserva Gmail SMTP.
        enviarConGmail(destinatario, asunto, contenido);
    }

    private void enviarConResend(
            String destinatario,
            String asunto,
            String contenido) {

        restClient.post()
                .uri("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .body(Map.of(
                        "from", resendFrom,
                        "to", List.of(destinatario),
                        "subject", asunto,
                        "text", contenido
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private void enviarConGmail(
            String destinatario,
            String asunto,
            String contenido) {

        JavaMailSender mailSender =
                mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new IllegalStateException(
                    "El servicio de correo todavía no está configurado.");
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();

        if (correoRemitente != null &&
                !correoRemitente.isBlank()) {
            mensaje.setFrom(correoRemitente);
        }

        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(contenido);

        mailSender.send(mensaje);
    }
}

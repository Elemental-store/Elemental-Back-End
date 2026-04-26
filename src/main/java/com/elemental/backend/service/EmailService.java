package com.elemental.backend.service;

import com.elemental.backend.entity.Order;
import com.elemental.backend.entity.User;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender     = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendOrderConfirmation(String toEmail, Long orderId,
                                      double total, String deliveryDate,
                                      Order order, User user) {
        try {
            byte[] pdf = generateInvoicePdf(order, user, total);

            Context context = new Context();
            context.setVariable("orderId",      orderId);
            context.setVariable("total",        String.format("%.2f", total));
            context.setVariable("deliveryDate", deliveryDate);
            String html = templateEngine.process("emails/order-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from, "ELEMENTAL");
            helper.setTo(toEmail);
            helper.setSubject("Pedido #" + orderId + " confirmado — ELEMENTAL");
            helper.setText(html, true);

            if (pdf != null) {
                String filename = "Factura-ELEMENTAL-" + String.format("F%04d", orderId) + ".pdf";
                helper.addAttachment(filename, new ByteArrayDataSource(pdf, "application/pdf"));
            }

            mailSender.send(message);
            log.info("Email de confirmación enviado a {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de confirmación a {}", toEmail, e);
        }
    }

    private byte[] generateInvoicePdf(Order order, User user, double total) {
        try {
            String invoiceNumber = String.format("F%04d", order.getId());
            String invoiceDate   = order.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("es")));

            String clientName = (user.getFirstName() != null ? user.getFirstName() : "")
                    + " " + (user.getLastName() != null ? user.getLastName() : "");

            String clientAddress = "";
            String clientCity    = "";
            if (order.getAddress() != null) {
                clientAddress = order.getAddress().getStreet();
                clientCity    = order.getAddress().getPostalCode()
                        + " " + order.getAddress().getCity()
                        + ", " + order.getAddress().getCountry();
            }

            double iva      = total - (total / 1.21);
            double subtotal = total / 1.21;

            List<Map<String, Object>> items = order.getDetails().stream().map(d -> Map.<String, Object>of(
                    "productId",   d.getProduct().getId(),
                    "productName", d.getProduct().getName(),
                    "quantity",    d.getQuantity(),
                    "unitPrice",   d.getUnitPrice(),
                    "subtotal",    d.getSubtotal()
            )).toList();

            String logoBase64 = "";
            try {
                ClassPathResource logoResource = new ClassPathResource("static/images/LogoFactura.png");
                byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                logoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(logoBytes);
            } catch (Exception e) {
                log.warn("No se pudo cargar el logo de la factura", e);
            }

            Context ctx = new Context(new Locale("es"));
            ctx.setVariable("invoiceNumber", invoiceNumber);
            ctx.setVariable("invoiceDate",   invoiceDate);
            ctx.setVariable("clientName",    clientName.trim());
            ctx.setVariable("clientEmail",   user.getEmail());
            ctx.setVariable("clientAddress", clientAddress);
            ctx.setVariable("clientCity",    clientCity);
            ctx.setVariable("items",         items);
            ctx.setVariable("subtotal",      subtotal);
            ctx.setVariable("iva",           iva);
            ctx.setVariable("total",         total);
            ctx.setVariable("logo",          logoBase64);

            String invoiceHtml = templateEngine.process("emails/invoice", ctx);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(invoiceHtml, null);
            builder.useDefaultPageSize(148, 297, PdfRendererBuilder.PageSizeUnits.MM);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generando factura PDF para el pedido {}", order.getId(), e);
            return null;
        }
    }

    public void sendPasswordResetEmail(String to, String name, String link) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject("Restablecer contraseña — ELEMENTAL");
            msg.setText("Hola " + name + ",\n\n"
                    + "Hemos recibido una solicitud para restablecer tu contraseña.\n"
                    + "Haz clic en el siguiente enlace (válido 30 minutos):\n\n"
                    + link + "\n\n"
                    + "Si no solicitaste este cambio, ignora este email.\n\n"
                    + "— El equipo de ELEMENTAL");
            mailSender.send(msg);
            log.info("Email de recuperación enviado a {}", to);
        } catch (Exception e) {
            log.error("Error enviando email de recuperación a {}", to, e);
            throw new RuntimeException("Error enviando email");
        }
    }
}

package dev.wuliclaw.automaillogin.mail;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class SmtpMailSender {
    private final AutoMailLoginPlugin plugin;

    public SmtpMailSender(AutoMailLoginPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean send(String to, RenderedMailTemplate template) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", plugin.getConfig().getString("mail.smtp.host", ""));
        properties.put("mail.smtp.port", String.valueOf(plugin.getConfig().getInt("mail.smtp.port", 587)));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", String.valueOf(plugin.getConfig().getBoolean("mail.smtp.ssl", false)));
        properties.put("mail.smtp.starttls.enable", String.valueOf(plugin.getConfig().getBoolean("mail.smtp.starttls", true)));

        String username = plugin.getConfig().getString("mail.smtp.username", "");
        String password = plugin.getConfig().getString("mail.smtp.password", "");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        session.setDebug(plugin.getConfig().getBoolean("mail.smtp.debug", false));

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(
                    plugin.getConfig().getString("mail.from-address", "no-reply@example.com"),
                    plugin.getConfig().getString("mail.from-name", "AutoMailLogin"),
                    StandardCharsets.UTF_8.name()
            ));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(template.subject(), StandardCharsets.UTF_8.name());

            if (template.htmlBody() != null && !template.htmlBody().isBlank()) {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(template.textBody(), StandardCharsets.UTF_8.name());

                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(template.htmlBody(), "text/html; charset=UTF-8");

                MimeMultipart multipart = new MimeMultipart("alternative");
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(htmlPart);
                message.setContent(multipart);
            } else {
                message.setText(template.textBody(), StandardCharsets.UTF_8.name());
            }

            Transport.send(message);
            return true;
        } catch (MessagingException exception) {
            plugin.getLogger().warning("SMTP send failed: " + exception.getMessage());
            return false;
        } catch (Exception exception) {
            plugin.getLogger().warning("SMTP mail build failed: " + exception.getMessage());
            return false;
        }
    }
}

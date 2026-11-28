package edu.vishvaya.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class PortalMailService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(PortalMailService.class);
    private final JavaMailSenderImpl sender = new JavaMailSenderImpl();
    private final String from;
    public PortalMailService(@Value("${PORTAL_SMTP_HOST:}") String host,
            @Value("${PORTAL_SMTP_PORT:587}") int port,
            @Value("${PORTAL_SMTP_USERNAME:}") String username,
            @Value("${PORTAL_SMTP_PASSWORD:}") String password,
            @Value("${PORTAL_MAIL_FROM:}") String from,
            @Value("${PORTAL_SMTP_TLS:true}") boolean tls) {
        sender.setHost(host); sender.setPort(port); sender.setUsername(username); sender.setPassword(password);
        var properties=sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", String.valueOf(!username.isBlank()));
        properties.setProperty("mail.smtp.starttls.enable", String.valueOf(tls));
        properties.setProperty("mail.smtp.starttls.required", String.valueOf(tls));
        properties.setProperty("mail.smtp.connectiontimeout","5000");
        properties.setProperty("mail.smtp.timeout","5000");
        properties.setProperty("mail.smtp.writetimeout","5000");
        this.from=from;
    }
    public void requireConfigured() {
        if(sender.getHost()==null || sender.getHost().isBlank() || from.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is not configured. Contact the administrator.");
    }
    public void send(String to, String subject, String body) {
        requireConfigured();
        var message=new SimpleMailMessage();
        message.setFrom(from); message.setTo(to); message.setSubject(subject); message.setText(body);
        try { sender.send(message); }
        catch (org.springframework.mail.MailException exception) {
            throw deliveryFailure(exception);
        }
    }

    private ResponseStatusException deliveryFailure(Exception exception) {
        StringBuilder diagnostic=new StringBuilder();
        for(Throwable cause=exception;cause!=null;cause=cause.getCause())
            diagnostic.append(cause.getClass().getSimpleName()).append(' ').append(cause.getMessage()).append(' ');
        String details=diagnostic.toString().toLowerCase(java.util.Locale.ROOT);
        String category="DELIVERY_FAILED";
        String message="Email could not be delivered. The administrator can check the mail service logs.";
        if(exception instanceof org.springframework.mail.MailAuthenticationException
                || details.contains("authenticationfailed") || details.contains("535") || details.contains("534")) {
            category="SMTP_AUTHENTICATION_FAILED";
            message="The email service could not sign in to the sending mailbox. The administrator must check the sender address and Google app password.";
        } else if(details.contains("unknownhost") || details.contains("connect") || details.contains("timed out")) {
            category="SMTP_CONNECTION_FAILED";
            message="The email service could not connect to the mail server. Check the internet connection and SMTP settings.";
        } else if(details.contains("starttls") || details.contains("ssl") || details.contains("certificate")) {
            category="SMTP_TLS_FAILED";
            message="The email service could not establish a secure connection. The administrator must check the SMTP TLS settings.";
        }
        log.warn("Email delivery failed with category {}",category);
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,message);
    }
}

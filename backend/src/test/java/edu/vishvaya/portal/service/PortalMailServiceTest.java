package edu.vishvaya.portal.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PortalMailServiceTest {
    @Test
    void authenticationFailureReturnsUsefulMessageWithoutProviderDetails() {
        var mail=new PortalMailService("smtp.gmail.com",587,"sender@example.test",
                "synthetic-password","sender@example.test",true);
        var sender=mock(JavaMailSenderImpl.class);
        when(sender.getHost()).thenReturn("smtp.gmail.com");
        doThrow(new MailAuthenticationException("535 secret provider detail")).when(sender).send(any(SimpleMailMessage.class));
        ReflectionTestUtils.setField(mail,"sender",sender);
        var error=assertThrows(ResponseStatusException.class,
                ()->mail.send("recipient@example.test","Test","Synthetic message"));
        assertEquals(503,error.getStatusCode().value());
        assertTrue(error.getReason().contains("Google app password"));
        assertFalse(error.getReason().contains("secret provider detail"));
        assertFalse(error.getReason().contains("sender@example.test"));
    }
}

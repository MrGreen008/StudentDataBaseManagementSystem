package edu.vishvaya.portal.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;

@Component
public class RegistrationCaptcha {
    private record Challenge(byte[] hash,long expires) {}
    private final SecureRandom random=new SecureRandom();
    private byte[] digest(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    public String create(HttpSession session) {
        String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder answer=new StringBuilder();
        for(int i=0;i<6;i++) answer.append(alphabet.charAt(random.nextInt(alphabet.length())));
        synchronized(session) {
            session.setAttribute("registrationCaptcha",new Challenge(digest(answer.toString()),System.currentTimeMillis()+300_000));
        }
        var image=new BufferedImage(240,80,BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();
        graphics.setColor(new Color(238,244,249));graphics.fillRect(0,0,240,80);
        graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,32));
        for(int i=0;i<6;i++) {
            graphics.setColor(new Color(20+random.nextInt(60),40+random.nextInt(60),80+random.nextInt(60)));
            graphics.drawString(answer.substring(i,i+1),14+i*35,48+random.nextInt(10));
        }
        graphics.setColor(new Color(110,140,165));
        for(int i=0;i<8;i++) graphics.drawLine(random.nextInt(240),random.nextInt(80),random.nextInt(240),random.nextInt(80));
        graphics.dispose();
        try {
            var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);
            return "data:image/png;base64,"+Base64.getEncoder().encodeToString(output.toByteArray());
        } catch(java.io.IOException exception) { throw new IllegalStateException("CAPTCHA unavailable",exception); }
    }
    public boolean consume(HttpSession session,String answer) {
        synchronized(session) {
            Object value=session.getAttribute("registrationCaptcha");
            session.removeAttribute("registrationCaptcha");
            return value instanceof Challenge challenge && challenge.expires()>System.currentTimeMillis()
                && MessageDigest.isEqual(challenge.hash(),digest(answer));
        }
    }
}

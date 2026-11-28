package edu.vishvaya.portal.service;

import edu.vishvaya.portal.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class PortalDocumentService {
    public record Document(String name,byte[] bytes) {
        @Override public String toString() {return "Document[redacted]";}
    }
    private final PortalNoticeRepository notices;
    private final AccountRepository accounts;
    public PortalDocumentService(PortalNoticeRepository notices,AccountRepository accounts) {
        this.notices=notices;this.accounts=accounts;
    }
    @Transactional
    public void approve(UUID id,long revision,boolean approved,UUID actor) {
        notices.lockIdentity();
        if(!notices.approve(id,revision,approved,actor))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Notice changed or is no longer pending. Refresh and review again.");
    }
    @Transactional
    public void upload(UUID id,MultipartFile file,UUID actor) {
        notices.lockIdentity();
        var account=accounts.byId(actor).orElseThrow();
        boolean admin=account.role().equals("ADMIN");
        var notice=notices.attachment(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!admin && (!account.role().equals("FACULTY") || !actor.toString().equals(notice.get("created_by"))))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(file.isEmpty() || file.getSize()>5*1024*1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a PDF of up to 5 MB.");
        try {
            byte[] bytes=file.getBytes();
            if(bytes.length<5 || !new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only PDF documents are supported.");
            String name=Optional.ofNullable(file.getOriginalFilename()).orElse("document.pdf")
                .replaceAll("[^a-zA-Z0-9._ -]","_");
            name=name.substring(0,Math.min(170,name.length()));
            if(!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) name+=".pdf";
            notices.attach(id,name,bytes,actor,admin);
        } catch(java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unable to read document.");
        }
    }
    public Document download(UUID id,UUID actor) {
        var account=accounts.byId(actor).orElseThrow();
        var notice=notices.attachment(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean allowed=account.role().equals("ADMIN")
            || account.role().equals("FACULTY") && actor.toString().equals(notice.get("created_by"))
            || notices.inbox(actor).stream().anyMatch(row->row.get("id").equals(id));
        if(!allowed || notice.get("attachment_data")==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new Document((String)notice.get("attachment_name"),(byte[])notice.get("attachment_data"));
    }
}

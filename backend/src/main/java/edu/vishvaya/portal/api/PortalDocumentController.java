package edu.vishvaya.portal.api;

import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.PortalDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import java.util.UUID;

@RestController
public class PortalDocumentController {
    public record Review(@Min(0) long revision,@NotNull Boolean approved) {}
    private final PortalDocumentService documents;
    public PortalDocumentController(PortalDocumentService documents) {this.documents=documents;}
    @PostMapping("/api/admin/notices/{id}/review") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@PathVariable UUID id,@Valid @RequestBody Review body,
            @AuthenticationPrincipal AccountPrincipal actor) {
        documents.approve(id,body.revision(),body.approved(),actor.id());
    }
    @PostMapping("/api/staff/portal-notices/{id}/attachment") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upload(@PathVariable UUID id,@RequestParam MultipartFile file,
            @AuthenticationPrincipal AccountPrincipal actor) {
        documents.upload(id,file,actor.id());
    }
    @GetMapping("/api/portal/notices/{id}/attachment")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,@AuthenticationPrincipal AccountPrincipal actor) {
        var document=documents.download(id,actor.id());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(document.name()).build().toString())
            .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(document.bytes());
    }
}

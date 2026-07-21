package com.keyxentic.samlsptestharness.web;

import com.keyxentic.samlsptestharness.idp.IdpImportResult;
import com.keyxentic.samlsptestharness.idp.IdpMetadataImportService;
import com.keyxentic.samlsptestharness.idp.InvalidIdpMetadataException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Controller
public class IdpMetadataController {

    private final IdpMetadataImportService importService;

    public IdpMetadataController(IdpMetadataImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/idp-metadata")
    public String importIdpMetadata(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String metadataText,
            RedirectAttributes redirectAttributes) {

        byte[] xml = resolveSubmittedXml(file, metadataText);
        if (xml == null) {
            redirectAttributes.addFlashAttribute("importError", "Choose a file to upload or paste IdP Metadata XML.");
            return "redirect:/";
        }

        try {
            IdpImportResult result = importService.importMetadata(xml);
            redirectAttributes.addFlashAttribute("importResult", result);
        } catch (InvalidIdpMetadataException e) {
            redirectAttributes.addFlashAttribute("importError", e.getMessage());
        }
        return "redirect:/";
    }

    private byte[] resolveSubmittedXml(MultipartFile file, String metadataText) {
        if (file != null && !file.isEmpty()) {
            try {
                return file.getBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (StringUtils.hasText(metadataText)) {
            return metadataText.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }
}

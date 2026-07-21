package com.keyxentic.samlsptestharness.web;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import com.keyxentic.samlsptestharness.credential.SpCredential;
import com.keyxentic.samlsptestharness.idp.IdpImportResult;
import com.keyxentic.samlsptestharness.idp.IdpRegistrationStore;
import com.keyxentic.samlsptestharness.idp.SamlRegistrationFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HarnessProperties properties;
    private final SpCredential spCredential;
    private final IdpRegistrationStore idpRegistrationStore;
    private final SamlRegistrationFactory registrationFactory;

    public HomeController(HarnessProperties properties, SpCredential spCredential,
                           IdpRegistrationStore idpRegistrationStore, SamlRegistrationFactory registrationFactory) {
        this.properties = properties;
        this.spCredential = spCredential;
        this.idpRegistrationStore = idpRegistrationStore;
        this.registrationFactory = registrationFactory;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("entityId", properties.entityId());
        model.addAttribute("acsUrl", properties.assertionConsumerServiceLocation());
        model.addAttribute("spMetadataUrl", "/saml2/service-provider-metadata/" + HarnessProperties.REGISTRATION_ID);
        model.addAttribute("currentIdp", currentIdp());
        return "home";
    }

    private IdpImportResult currentIdp() {
        return idpRegistrationStore.current()
                .map(xml -> registrationFactory.build(properties, spCredential, xml))
                .map(IdpImportResult::from)
                .orElse(null);
    }
}

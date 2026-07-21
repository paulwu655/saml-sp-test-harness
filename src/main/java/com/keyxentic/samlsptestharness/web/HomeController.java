package com.keyxentic.samlsptestharness.web;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HarnessProperties properties;

    public HomeController(HarnessProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("entityId", properties.entityId());
        model.addAttribute("acsUrl", properties.assertionConsumerServiceLocation());
        model.addAttribute("spMetadataUrl", "/saml2/service-provider-metadata/" + HarnessProperties.REGISTRATION_ID);
        return "home";
    }
}

package com.keyxentic.samlsptestharness.login;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security's {@code Saml2AuthenticatedPrincipal} exposes the NameID value but not its
 * Format attribute, so this reads it directly from the raw Response XML the harness already
 * captures for the Result Record's raw-XML view.
 */
public final class NameIdFormatExtractor {

    private NameIdFormatExtractor() {
    }

    public static String extract(String rawResponseXml) {
        if (rawResponseXml == null) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(rawResponseXml.getBytes(StandardCharsets.UTF_8)));
            NodeList nameIds = document.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
            if (nameIds.getLength() == 0) {
                return null;
            }
            Element nameId = (Element) nameIds.item(0);
            String format = nameId.getAttribute("Format");
            return format.isEmpty() ? null : format;
        } catch (Exception e) {
            return null;
        }
    }
}

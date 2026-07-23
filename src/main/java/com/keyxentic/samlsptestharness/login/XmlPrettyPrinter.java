package com.keyxentic.samlsptestharness.login;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Re-indents a Response's XML for display alongside the untouched raw text (see CONTEXT.md:
 * "Result Record" — the raw text stays the byte-for-byte record; this is a readability aid only).
 */
final class XmlPrettyPrinter {

    private XmlPrettyPrinter() {
    }

    static String prettyPrint(String xml) {
        if (xml == null) {
            return null;
        }
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter out = new StringWriter();
            transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
            return out.toString().strip();
        } catch (Exception e) {
            // Not well-formed XML (e.g. garbage content) — no formatted view, the raw tab still has it.
            return null;
        }
    }
}

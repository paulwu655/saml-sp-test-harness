package com.keyxentic.samlsptestharness.login;

import java.util.List;
import java.util.Map;

/**
 * The outcome of a single Login Test attempt — rendered directly after the ACS callback,
 * never persisted (see CONTEXT.md: "Result Record").
 */
public record ResultRecord(
        boolean success,
        String nameId,
        String nameIdFormat,
        ValidationOutcome signatureValidation,
        ValidationOutcome timeValidityValidation,
        Map<String, List<Object>> attributes,
        String rawResponseXml,
        String rawResponseXmlFormatted,
        String failureMessage) {

    public static ResultRecord success(String nameId, String nameIdFormat, Map<String, List<Object>> attributes, String rawResponseXml) {
        return new ResultRecord(true, nameId, nameIdFormat, ValidationOutcome.passed(), ValidationOutcome.passed(),
                attributes, rawResponseXml, XmlPrettyPrinter.prettyPrint(rawResponseXml), null);
    }

    public static ResultRecord failure(ValidationOutcome signatureValidation, ValidationOutcome timeValidityValidation,
                                        String rawResponseXml, String failureMessage) {
        return new ResultRecord(false, null, null, signatureValidation, timeValidityValidation,
                Map.of(), rawResponseXml, XmlPrettyPrinter.prettyPrint(rawResponseXml), failureMessage);
    }
}

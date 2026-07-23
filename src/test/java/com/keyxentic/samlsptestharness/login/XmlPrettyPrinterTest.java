package com.keyxentic.samlsptestharness.login;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XmlPrettyPrinterTest {

    @Test
    void indentsWellFormedXmlOnASingleLine() {
        String formatted = XmlPrettyPrinter.prettyPrint("<root><child>text</child></root>");

        assertThat(formatted).contains("<root>", "<child>text</child>", "</root>");
        assertThat(formatted.lines().count()).isGreaterThan(1);
    }

    @Test
    void returnsNullForNotWellFormedXml() {
        assertThat(XmlPrettyPrinter.prettyPrint("not xml at all")).isNull();
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(XmlPrettyPrinter.prettyPrint(null)).isNull();
    }
}

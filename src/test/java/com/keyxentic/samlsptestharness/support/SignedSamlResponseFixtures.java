package com.keyxentic.samlsptestharness.support;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/** Builds real, signed SAML 2.0 Response XML (base64-encoded, ready to POST as the SAMLResponse parameter). */
public final class SignedSamlResponseFixtures {

    private SignedSamlResponseFixtures() {
    }

    public static String signedResponse(Spec spec) {
        try {
            Response response = build(Response.class);
            response.setID("_" + UUID.randomUUID());
            response.setIssueInstant(Instant.now());
            response.setVersion(SAMLVersion.VERSION_20);
            response.setDestination(spec.acsUrl());
            response.setIssuer(issuer(spec.idpEntityId()));
            response.setStatus(successStatus());
            response.getAssertions().add(assertion(spec));

            Element element = XMLObjectProviderRegistrySupport.getMarshallerFactory()
                    .getMarshaller(response)
                    .marshall(response);
            return Base64.getEncoder().encodeToString(toXmlString(element).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Assertion assertion(Spec spec) throws Exception {
        Assertion assertion = build(Assertion.class);
        assertion.setID("_" + UUID.randomUUID());
        assertion.setIssueInstant(Instant.now());
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssuer(issuer(spec.idpEntityId()));
        assertion.setSubject(subject(spec));
        assertion.setConditions(conditions(spec));
        assertion.getAuthnStatements().add(authnStatement());
        if (!spec.attributes().isEmpty()) {
            assertion.getAttributeStatements().add(attributeStatement(spec.attributes()));
        }

        if (spec.signingKeyPair() != null) {
            Signature signature = build(Signature.class);
            Credential credential = new BasicX509Credential(spec.signingCertificate(), spec.signingKeyPair().getPrivate());
            signature.setSigningCredential(credential);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            assertion.setSignature(signature);

            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
            Signer.signObject(signature);
        }
        return assertion;
    }

    private static Subject subject(Spec spec) {
        NameID nameId = build(NameID.class);
        nameId.setValue(spec.nameId());
        nameId.setFormat(spec.nameIdFormat());

        SubjectConfirmationData confirmationData = build(SubjectConfirmationData.class);
        confirmationData.setRecipient(spec.acsUrl());
        confirmationData.setNotOnOrAfter(spec.notOnOrAfter());

        SubjectConfirmation confirmation = build(SubjectConfirmation.class);
        confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        confirmation.setSubjectConfirmationData(confirmationData);

        Subject subject = build(Subject.class);
        subject.setNameID(nameId);
        subject.getSubjectConfirmations().add(confirmation);
        return subject;
    }

    private static Conditions conditions(Spec spec) {
        Conditions conditions = build(Conditions.class);
        conditions.setNotBefore(spec.notBefore());
        conditions.setNotOnOrAfter(spec.notOnOrAfter());

        Audience audience = build(Audience.class);
        audience.setURI(spec.spEntityId());
        AudienceRestriction restriction = build(AudienceRestriction.class);
        restriction.getAudiences().add(audience);
        conditions.getAudienceRestrictions().add(restriction);
        return conditions;
    }

    private static AuthnStatement authnStatement() {
        AuthnContextClassRef classRef = build(AuthnContextClassRef.class);
        classRef.setURI(AuthnContext.PASSWORD_AUTHN_CTX);
        AuthnContext authnContext = build(AuthnContext.class);
        authnContext.setAuthnContextClassRef(classRef);

        AuthnStatement statement = build(AuthnStatement.class);
        statement.setAuthnInstant(Instant.now());
        statement.setAuthnContext(authnContext);
        return statement;
    }

    private static AttributeStatement attributeStatement(Map<String, String> attributes) {
        AttributeStatement statement = build(AttributeStatement.class);
        attributes.forEach((name, value) -> {
            Attribute attribute = build(Attribute.class);
            attribute.setName(name);
            XSStringBuilder stringBuilder = (XSStringBuilder) XMLObjectProviderRegistrySupport.getBuilderFactory()
                    .getBuilder(XSString.TYPE_NAME);
            XSString attributeValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            attributeValue.setValue(value);
            attribute.getAttributeValues().add(attributeValue);
            statement.getAttributes().add(attribute);
        });
        return statement;
    }

    private static Issuer issuer(String value) {
        Issuer issuer = build(Issuer.class);
        issuer.setValue(value);
        return issuer;
    }

    private static Status successStatus() {
        StatusCode statusCode = build(StatusCode.class);
        statusCode.setValue(StatusCode.SUCCESS);
        Status status = build(Status.class);
        status.setStatusCode(statusCode);
        return status;
    }

    @SuppressWarnings("unchecked")
    private static <T> T build(Class<T> clazz) {
        try {
            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
            QName defaultElementName = (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
            return (T) builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Could not build SAML object " + clazz, e);
        }
    }

    private static String toXmlString(Element element) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        document.appendChild(document.importNode(element, true));
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    public record Spec(
            String idpEntityId,
            String spEntityId,
            String acsUrl,
            String nameId,
            String nameIdFormat,
            Instant notBefore,
            Instant notOnOrAfter,
            Map<String, String> attributes,
            KeyPair signingKeyPair,
            X509Certificate signingCertificate) {

        public static Spec validFor(String idpEntityId, KeyPair idpKeyPair, X509Certificate idpCertificate,
                                     String spEntityId, String acsUrl) {
            Instant now = Instant.now();
            return new Spec(idpEntityId, spEntityId, acsUrl, "test-user@example.test", NameIDType.EMAIL,
                    now.minusSeconds(60), now.plusSeconds(300), Map.of("email", "test-user@example.test"),
                    idpKeyPair, idpCertificate);
        }

        public Spec expired() {
            Instant past = Instant.now().minusSeconds(3600);
            return new Spec(idpEntityId, spEntityId, acsUrl, nameId, nameIdFormat,
                    past.minusSeconds(600), past, attributes, signingKeyPair, signingCertificate);
        }

        public Spec signedWith(KeyPair otherKeyPair, X509Certificate otherCertificate) {
            return new Spec(idpEntityId, spEntityId, acsUrl, nameId, nameIdFormat,
                    notBefore, notOnOrAfter, attributes, otherKeyPair, otherCertificate);
        }
    }
}

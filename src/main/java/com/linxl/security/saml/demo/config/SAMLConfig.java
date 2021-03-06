package com.linxl.security.saml.demo.config;


import com.linxl.security.saml.demo.certificate.KeystoreFactory;
import com.linxl.security.saml.demo.saml.SAMLUserDetailsServiceImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.util.resource.FilesystemResource;
import org.opensaml.util.resource.Resource;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.saml.*;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.*;
import org.springframework.security.saml.processor.*;
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;

@Configuration
@EnableConfigurationProperties(SamlProperties.class)
public class SAMLConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLConfig.class);

    private final SAMLUserDetailsServiceImpl samlUserDetailsServiceImpl;

    private final SamlProperties samlProperties;

    @Autowired
    public SAMLConfig(SAMLUserDetailsServiceImpl samlUserDetailsServiceImpl, SamlProperties samlProperties) {
        this.samlUserDetailsServiceImpl = samlUserDetailsServiceImpl;
        this.samlProperties = samlProperties;
    }

    @Bean
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        SAMLAuthenticationProvider provider = new SAMLAuthenticationProvider();
        // ??????????????? SAML ????????????????????? SAMLUserDetailsServiceImpl
        provider.setUserDetails(samlUserDetailsServiceImpl);
        // ???????????????????????????Authentication??????????????????????????????????????????????????????NameID???NameID????????????????????????????????????true?????????NameID??????????????????
        provider.setForcePrincipalAsString(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Collections.singletonList(samlAuthenticationProvider()));
    }

    @Bean(initMethod = "initialize")
    public StaticBasicParserPool parserPool() {
        return new StaticBasicParserPool();
    }

    @Bean
    public SAMLProcessorImpl processor() {
        HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        ArtifactResolutionProfileImpl artifactResolutionProfile = new ArtifactResolutionProfileImpl(httpClient);
        HTTPSOAP11Binding soapBinding = new HTTPSOAP11Binding(parserPool());
        artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding));

        VelocityEngine velocityEngine = VelocityFactory.getEngine();
        Collection<SAMLBinding> bindings = new ArrayList<>();
        bindings.add(new HTTPRedirectDeflateBinding(parserPool()));
        bindings.add(new HTTPPostBinding(parserPool(), velocityEngine));
        bindings.add(new HTTPArtifactBinding(parserPool(), velocityEngine, artifactResolutionProfile));
        bindings.add(new HTTPSOAP11Binding(parserPool()));
        bindings.add(new HTTPPAOS11Binding(parserPool()));
        return new SAMLProcessorImpl(bindings);
    }

    @Bean
    public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
        SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
        handler.setDefaultTargetUrl("/");
        return handler;
    }

    @Bean
    public SecurityContextLogoutHandler logoutHandler() {
        SecurityContextLogoutHandler handler = new SecurityContextLogoutHandler();
        //handler.setInvalidateHttpSession(true);
        handler.setClearAuthentication(true);
        return handler;
    }

    @Bean
    public SAMLLogoutFilter samlLogoutFilter() {
        SAMLLogoutFilter filter = new SAMLLogoutFilter(successLogoutHandler(), new LogoutHandler[]{logoutHandler()}, new LogoutHandler[]{logoutHandler()});
        // ????????????
        filter.setFilterProcessesUrl("/saml/logout");
        return filter;
    }

    @Bean
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
        SAMLLogoutProcessingFilter filter = new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
        // SLO ??????????????? IDP ??????
        filter.setFilterProcessesUrl("/saml/SingleLogout");
        return filter;
    }

    // ?????? SP ??????????????????
    @Bean
    public MetadataGeneratorFilter metadataGeneratorFilter(MetadataGenerator metadataGenerator) {
        return new MetadataGeneratorFilter(metadataGenerator);
    }

    @Bean
    public MetadataDisplayFilter metadataDisplayFilter() throws Exception {
        MetadataDisplayFilter filter = new MetadataDisplayFilter();
        // ?????? IDP ???????????????
        filter.setFilterProcessesUrl("/saml/metadata");
        return filter;
    }

    @Bean
    public ExtendedMetadataDelegate idpMetadataLoader() {

        if (StringUtils.isBlank(samlProperties.getIdpXml()) || !samlProperties.getIdpXml().endsWith(".xml")) {
            throw new IllegalArgumentException("demo.saml.idp-xml must not be null or empty and must be a xml file.");
        }

        try {
            final File file = ResourceUtils.getFile(samlProperties.getIdpXml());
            Resource idpResource = new FilesystemResource(file.getPath());

            Timer refreshTimer = new Timer(true);
            ResourceBackedMetadataProvider delegate;
            delegate = new ResourceBackedMetadataProvider(refreshTimer, idpResource);
            delegate.setParserPool(parserPool());
            ExtendedMetadata extendedMetadata = extendedMetadata().clone();
            ExtendedMetadataDelegate provider = new ExtendedMetadataDelegate(delegate, extendedMetadata);
            provider.setMetadataTrustCheck(true);
            provider.setMetadataRequireSignature(false);
            String idpName = file.getName().replaceAll(".xml", "");
            extendedMetadata.setAlias(idpName);
            // ?????? IDP ???????????? provider
            LOGGER.info("Loaded Idp Metadata bean {}: {}", idpName, file.getPath());
            return provider;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize IDP Metadata", e);
        }
    }

    // ??????????????????
    @Bean
    public ExtendedMetadata extendedMetadata() {
        ExtendedMetadata metadata = new ExtendedMetadata();
        //set flag to true to present user with IDP Selection screen
//        metadata.setIdpDiscoveryEnabled(true);
        metadata.setRequireLogoutRequestSigned(true);
        metadata.setRequireLogoutResponseSigned(true);
        metadata.setSignMetadata(false);
        return metadata;
    }

    // SP??????????????????
    @Bean
    public MetadataGenerator metadataGenerator(KeyManager keyManager) {
        MetadataGenerator generator = new MetadataGenerator();
//        generator.setEntityId("localhost-demo");
        // SP ??????
        generator.setEntityId("http://localhost:8080/saml/metadata");
        generator.setExtendedMetadata(extendedMetadata());
        // ?????????true????????????????????????????????????????????????????????????????????????IDP????????????????????????
        generator.setIncludeDiscoveryExtension(false);
        generator.setKeyManager(keyManager);
        return generator;
    }

    @Bean
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
        SAMLProcessingFilter filter = new SAMLProcessingFilter();
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationSuccessHandler(successRedirectHandler());
        filter.setAuthenticationFailureHandler(authenticationFailureHandler());
        // ??? IDP ?????????????????? SP ?????????????????????????????????????????????
        filter.setFilterProcessesUrl("/saml/SSO");
        return filter;
    }

    @Bean
    public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
        SAMLWebSSOHoKProcessingFilter filter = new SAMLWebSSOHoKProcessingFilter();
        filter.setAuthenticationSuccessHandler(successRedirectHandler());
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationFailureHandler(authenticationFailureHandler());
        return filter;
    }

    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/home");
        return handler;
    }

    @Bean
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler();
        handler.setUseForward(false);
        //handler.setDefaultFailureUrl("/error");
        return handler;
    }

    // ?????????????????? IDP
//    @Bean
//    public SAMLDiscovery samlIDPDiscovery() {
//        SAMLDiscovery filter = new SAMLDiscovery();
//        filter.setFilterProcessesUrl("/saml/discovery");
//        filter.setIdpSelectionPath("/idpselection");
//        return filter;
//    }

    @Bean
    public SAMLEntryPoint samlEntryPoint() {
        // WebSSOProfileOptions ???????????????AuthnRequest????????????????????????????????????
        WebSSOProfileOptions options = new WebSSOProfileOptions();
        // ????????????true?????????true???????????????????????????????????????
        options.setIncludeScoping(false);
        SAMLEntryPoint entryPoint = new SAMLEntryPoint();
        entryPoint.setDefaultProfileOptions(options);
        entryPoint.setFilterProcessesUrl("/saml/login");
        return entryPoint;
    }

    @Bean
    public KeystoreFactory keystoreFactory() {
        return new KeystoreFactory();
    }

    @Bean
    public KeyManager keyManager(KeystoreFactory keystoreFactory) throws Exception {
        LOGGER.debug("Start to initialize KeyManager for SAML.");
        LOGGER.debug("Check demo.saml.public-key-cert and demo.saml.private-key-cert.");
        if (samlProperties.useCerts()) {
            LOGGER.debug("find demo.saml.public-key-cert and demo.saml.private-key-cert.");
            LOGGER.debug("Use demo.saml.public-key-cert and demo.saml.private-key-cert to initialize KeyManager.");
            return keystoreFactory.getJKSKeyManager(samlProperties.getPublicKeyCert(), samlProperties.getPrivateKeyCert());
        }
        LOGGER.debug("Can't find demo.saml.public-key-cert and demo.saml.private-key-cert.");
        LOGGER.debug("Check demo.saml.key-store and demo.saml.key-alias.");
        if (samlProperties.useKeyStore()) {
            LOGGER.debug("find demo.saml.key-store and demo.saml.key-alias.");
            try {
                return keystoreFactory.getJKSKeyManager(samlProperties.getKeyStore(), samlProperties.getKeyStorePassword(),
                        samlProperties.getKeyPassword());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to initialize KeyManager with keyStore: " + samlProperties.getKeyStore(), e);
            }
        }
        LOGGER.debug("Can't find demo.saml.key-store and demo.saml.key-alias.");
        throw new IllegalArgumentException("Unable to initialize KeyManager because no parameters available.");
    }

    @Bean
    public TLSProtocolConfigurer tlsProtocolConfigurer(KeyManager keyManager) {
        TLSProtocolConfigurer configurer = new TLSProtocolConfigurer();
        configurer.setKeyManager(keyManager);
        return configurer;
    }

}

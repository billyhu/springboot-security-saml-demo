package com.linxl.security.saml.demo.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.saml")
public class SamlProperties {

    /**
     * idp 元数据 xml 所在位置
     */
    private String idpXml;

    /**
     * 用于验证数字签名的公钥证书所在位置，X509 格式
     */
    private String publicKeyCert;

    /**
     * 用于数字签名的私钥所在位置，RSA 算法类型，PKCS8 格式
     */
    private String privateKeyCert;

    /**
     * 包含用于验证数字签名的公钥证书，用于数字签名的私钥 的 JKS 所在位置
     * 需要清楚传入的 JKS 中私钥的别名。
     */
    private String keyStore;

    /**
     * {@link #keyStore} 的密码
     */
    private char[] keyStorePassword;

    /**
     * {@link #privateKeyCert} 或 {@link #keyStore} 的私钥密码
     */
    private char[] keyPassword;

    public boolean useKeyStore() {
        return StringUtils.isNotBlank(keyStore);
    }

    public boolean useCerts() {
        return StringUtils.isNotBlank(publicKeyCert) && StringUtils.isNotBlank(privateKeyCert);
    }

    public String getIdpXml() {
        return idpXml;
    }

    public void setIdpXml(String idpXml) {
        this.idpXml = idpXml;
    }

    public String getPublicKeyCert() {
        return publicKeyCert;
    }

    public void setPublicKeyCert(String publicKeyCert) {
        this.publicKeyCert = publicKeyCert;
    }

    public String getPrivateKeyCert() {
        return privateKeyCert;
    }

    public void setPrivateKeyCert(String privateKeyCert) {
        this.privateKeyCert = privateKeyCert;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(char[] keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public char[] getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(char[] keyPassword) {
        this.keyPassword = keyPassword;
    }
}

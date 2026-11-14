package nz.co.westpac.poc.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtKeyProvider {

    private static final int KEY_SIZE = 2048;

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public JwtKeyProvider(@Value("${app.jwt.public-key:}") String publicKeyPem,
                          @Value("${app.jwt.private-key:}") String privateKeyPem) {
        try {
            if (publicKeyPem.isBlank() && privateKeyPem.isBlank()) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(KEY_SIZE);
                KeyPair keyPair = generator.generateKeyPair();
                this.publicKey = (RSAPublicKey) keyPair.getPublic();
                this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            } else if (publicKeyPem.isBlank() || privateKeyPem.isBlank()) {
                throw new IllegalStateException(
                        "app.jwt.public-key and app.jwt.private-key must be configured together");
            } else {
                this.publicKey = (RSAPublicKey) decodePublicKey(publicKeyPem);
                this.privateKey = (RSAPrivateKey) decodePrivateKey(privateKeyPem);
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load the JWT key pair", ex);
        }
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    private static RSAPublicKey decodePublicKey(String pem) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(stripPemHeaders(pem, "PUBLIC KEY"));
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private static RSAPrivateKey decodePrivateKey(String pem) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(stripPemHeaders(pem, "PRIVATE KEY"));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static String stripPemHeaders(String pem, String type) {
        return pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .trim();
    }
}

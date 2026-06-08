package pe.edu.upc.iam_service.iam.infrastructure.tokens.jwt.services;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads RSA keys from either classpath: or file: paths, supports PKCS#8 and PKCS#1 private keys,
 * and sanitizes PEM contents (LF/CRLF, BOM, whitespace).
 */
public class JwtKeyProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtKeyProvider.class);

    private final ResourceLoader resourceLoader;
    private final String privateKeyPath;
    private final String publicKeyPath;
    private final String privateKeyValue;
    private final String publicKeyValue;
    private final String keyId;

    @Getter
    private PrivateKey privateKey;
    @Getter
    private PublicKey publicKey;
    @Getter
    private RSAKey publicJwk;

    public JwtKeyProvider(ResourceLoader resourceLoader,
                          String privateKeyPath,
                          String publicKeyPath,
                          String privateKeyValue,
                          String publicKeyValue,
                          String keyId) {
        this.resourceLoader = resourceLoader;
        this.privateKeyPath = privateKeyPath;
        this.publicKeyPath = publicKeyPath;
        this.privateKeyValue = privateKeyValue;
        this.publicKeyValue = publicKeyValue;
        this.keyId = keyId;
        init();
    }

    private void init() {
        try {
            if (StringUtils.hasText(privateKeyValue) && StringUtils.hasText(publicKeyValue)) {
                this.privateKey = parsePrivateKey(privateKeyValue);
                this.publicKey = parsePublicKey(publicKeyValue);
            } else if (StringUtils.hasText(privateKeyPath) && StringUtils.hasText(publicKeyPath)) {
                this.privateKey = loadPrivateKey(privateKeyPath);
                this.publicKey = loadPublicKey(publicKeyPath);
            } else {
                LOGGER.warn("JWT RSA keys are not configured. Generating an ephemeral RSA key pair for this process.");
                var generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                var keyPair = generator.generateKeyPair();
                this.privateKey = keyPair.getPrivate();
                this.publicKey = keyPair.getPublic();
            }
            this.publicJwk = new RSAKey.Builder((RSAPublicKey) this.publicKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(keyId)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RSA keys for JWT signing", e);
        }
    }

    // ------------------------
    // Loading helpers
    // ------------------------

    private PrivateKey loadPrivateKey(String path) throws Exception {
        return parsePrivateKey(readKeyFrom(path));
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        return parsePublicKey(readKeyFrom(path));
    }

    private PrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = extractPemBase64(pem,
                "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----",
                "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");

        byte[] der = Base64.getMimeDecoder().decode(base64); // tolerates newlines/spaces

        // Try PKCS#8 first
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception pkcs8Fail) {
            // If it was PKCS#1, wrap it to PKCS#8 on the fly
            byte[] pkcs8 = wrapPkcs1ToPkcs8(der);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        }
    }

    private PublicKey parsePublicKey(String pem) throws Exception {
        String base64 = extractPemBase64(pem,
                "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");

        byte[] der = Base64.getMimeDecoder().decode(base64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private String readKeyFrom(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Extracts the Base64 body between any of the provided header/footer pairs and removes all whitespace/BOM/CR.
     */
    private static String extractPemBase64(String pem, String... headersAndFooters) {
        String normalized = pem
                .replace("\r", "")
                .replace("\uFEFF", "")
                .trim();

        for (int i = 0; i + 1 < headersAndFooters.length; i += 2) {
            String head = headersAndFooters[i];
            String foot = headersAndFooters[i + 1];
            if (normalized.contains(head) && normalized.contains(foot)) {
                String inner = normalized.substring(
                        normalized.indexOf(head) + head.length(),
                        normalized.indexOf(foot));
                return inner.replaceAll("\\s", ""); // remove ALL whitespace
            }
        }
        throw new IllegalArgumentException("PEM headers not found in key file.");
    }

    // ------------------------
    // Minimal DER builders to wrap PKCS#1 -> PKCS#8
    // ------------------------

    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1Der) {
        // AlgorithmIdentifier for rsaEncryption: 1.2.840.113549.1.1.1 + NULL
        byte[] algId = new byte[]{
                0x30, 0x0D,                         // SEQUENCE len 13
                0x06, 0x09,                       // OID len 9
                0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00                        // NULL
        };
        byte[] version = new byte[]{0x02, 0x01, 0x00}; // INTEGER(0)
        byte[] pkcs1Octet = derTag(0x04, pkcs1Der);       // OCTET STRING
        byte[] seqInner = concat(version, algId, pkcs1Octet);
        return derTag(0x30, seqInner);                   // SEQUENCE
    }

    private static byte[] derTag(int tag, byte[] data) {
        byte[] len = derLength(data.length);
        byte[] out = new byte[1 + len.length + data.length];
        out[0] = (byte) tag;
        System.arraycopy(len, 0, out, 1, len.length);
        System.arraycopy(data, 0, out, 1 + len.length, data.length);
        return out;
    }

    private static byte[] derLength(int len) {
        if (len < 128) return new byte[]{(byte) len};
        int tmp = len, bytes = 0;
        while (tmp > 0) {
            tmp >>= 8;
            bytes++;
        }
        byte[] out = new byte[1 + bytes];
        out[0] = (byte) (0x80 | bytes);
        for (int i = bytes; i > 0; i--) {
            out[i] = (byte) (len & 0xFF);
            len >>= 8;
        }
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}

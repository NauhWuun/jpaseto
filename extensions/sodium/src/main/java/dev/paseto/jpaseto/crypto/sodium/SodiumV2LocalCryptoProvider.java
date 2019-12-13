package dev.paseto.jpaseto.crypto.sodium;

import com.google.auto.service.AutoService;
import com.google.common.primitives.Bytes;
import dev.paseto.jpaseto.impl.crypto.PreAuthEncoder;
import dev.paseto.jpaseto.impl.crypto.V2LocalCryptoProvider;
import org.apache.tuweni.crypto.sodium.XChaCha20Poly1305;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@AutoService(V2LocalCryptoProvider.class)
public class SodiumV2LocalCryptoProvider implements V2LocalCryptoProvider {

    private static final byte[] HEADER_BYTES = "v2.local.".getBytes(StandardCharsets.UTF_8);

    @Override
    public byte[] blake2b(byte[] random, byte[] payload) {
        return Blake2b.hash(24, payload, random);
    }

    @Override
    public byte[] encrypt(byte[] payload, byte[] footer, byte[] nonce, SecretKey sharedSecret) {

//        // 2
//        byte[] randomBytes = new byte[24];
//        SecureRandom.getInstanceStrong().nextBytes(randomBytes);
//
//        // 3
//        byte[] nonce = blake2b(payload, randomBytes);

        // 4
        byte[] preAuth = PreAuthEncoder.encode(HEADER_BYTES, nonce, footer);

        // 5
        byte[] payloadCipher = XChaCha20Poly1305.encrypt(payload,
                preAuth,
                XChaCha20Poly1305.Key.fromBytes(sharedSecret.getEncoded()),
                XChaCha20Poly1305.Nonce.fromBytes(nonce));

        // 6
        return Bytes.concat(nonce, payloadCipher);
    }

    @Override
    public byte[] decrypt(byte[] encryptedBytes, byte[] footer, SecretKey sharedSecret) {
        byte[] nonce = Arrays.copyOf(encryptedBytes, 24); // nonce size is 24 bytes
        byte[] encryptedMessage = Arrays.copyOfRange(encryptedBytes, 24, encryptedBytes.length);

        byte[] preAuth = PreAuthEncoder.encode(HEADER_BYTES, nonce, footer);

        byte[] payloadBytes = XChaCha20Poly1305.decrypt(
                encryptedMessage,
                preAuth,
                XChaCha20Poly1305.Key.fromBytes(sharedSecret.getEncoded()),
                XChaCha20Poly1305.Nonce.fromBytes(nonce));

        if (payloadBytes == null) {
            throw new RuntimeException("Decryption failed, likely cause is an invalid sharedSecret or MAC.");
        }

        return payloadBytes;
    }
}

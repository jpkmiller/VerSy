package messaging;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {
    private final Endpoint endpoint;
    private Cipher encrypt;
    private Cipher decrypt;

    public SecureEndpoint() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        this.endpoint = new Endpoint();
        init();
    }

    public SecureEndpoint(int port) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        this.endpoint = new Endpoint(port);
        init();
    }

    private void init() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKey = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(StandardCharsets.UTF_8), "AES");
        this.encrypt = Cipher.getInstance("AES");
        this.encrypt.init(Cipher.ENCRYPT_MODE, secretKey);
        this.decrypt = Cipher.getInstance("AES");
        this.decrypt.init(Cipher.DECRYPT_MODE, secretKey);
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            this.endpoint.send(receiver, new SealedObject(payload, this.encrypt));
        } catch (IOException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    public Message blockingReceive() {
        return decrypt(endpoint.blockingReceive());
    }

    public Message nonBlockingReceive() {
        return decrypt(endpoint.nonBlockingReceive());
    }

    public Message decrypt(Message msg) {
        if (msg == null)
            return null;
        SealedObject encryptedObject = (SealedObject) msg.getPayload();
        try {
            return new Message((Serializable) encryptedObject.getObject(this.decrypt), msg.getSender());
        } catch (IOException | ClassNotFoundException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
}

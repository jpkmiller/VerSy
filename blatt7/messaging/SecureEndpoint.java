package messaging;

import common.msgtypes.KeyExchangeMessage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

public class SecureEndpoint extends Endpoint {

    public enum EncryptionMode {
        ASYMMETRICAL(-1), SYMMETRICAL(+1);
        private int mode;

        EncryptionMode(int mode) {
            this.mode = mode;
        }

        int getMode() {
            return this.mode;
        }
    }

    private final Endpoint endpoint;
    private Cipher encryptSymmetrical;
    private Cipher decryptSymmetrical;
    private Cipher encryptAsymmetrical;
    private Cipher decryptAsymmetrical;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Map<InetSocketAddress, PublicKey> communicationPartners;
    public EncryptionMode encryptionMode = EncryptionMode.ASYMMETRICAL;

    public SecureEndpoint() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        this.endpoint = new Endpoint();
        init();
    }

    public SecureEndpoint(int port) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        this.endpoint = new Endpoint(port);
        init();
    }

    private void init() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (this.encryptionMode == EncryptionMode.SYMMETRICAL) {
            // symmetrical
            SecretKeySpec secretKey = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(StandardCharsets.UTF_8), "AES");
            this.encryptSymmetrical = Cipher.getInstance("AES");
            this.encryptSymmetrical.init(Cipher.ENCRYPT_MODE, secretKey);
            this.decryptSymmetrical = Cipher.getInstance("AES");
            this.decryptSymmetrical.init(Cipher.DECRYPT_MODE, secretKey);
        } else if (this.encryptionMode == EncryptionMode.ASYMMETRICAL) {
            // asymmetrical
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            /*
                Manually setting keyPair size to 4096 bit because 2048 bit is too small for some message types.
                See the following link for further information:
                https://stackoverflow.com/questions/10007147/getting-a-illegalblocksizeexception-data-must-not-be-longer-than-256-bytes-when
             */
            keyPairGen.initialize(4096);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            this.publicKey = keyPair.getPublic();
            this.privateKey = keyPair.getPrivate();
            this.communicationPartners = new HashMap<>();
            this.encryptAsymmetrical = Cipher.getInstance("RSA");
            this.encryptAsymmetrical.init(Cipher.ENCRYPT_MODE, this.privateKey);
            this.decryptAsymmetrical = Cipher.getInstance("RSA");
            this.decryptAsymmetrical.init(Cipher.DECRYPT_MODE, this.publicKey);
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            if (this.encryptionMode == EncryptionMode.SYMMETRICAL) {
                this.endpoint.send(receiver, new SealedObject(payload, this.encryptSymmetrical));
            } else if (this.encryptionMode == EncryptionMode.ASYMMETRICAL) {
                if (!this.communicationPartners.containsKey(receiver)) {
                    System.out.println("Sending public key");
                    this.endpoint.send(receiver, new KeyExchangeMessage(this.publicKey));
                }
                this.endpoint.send(receiver, new SealedObject(payload, this.encryptAsymmetrical));
            }
        } catch (IOException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    public Message blockingReceive() {
        return handleMessage(endpoint.blockingReceive());
    }

    public Message nonBlockingReceive() {
        return handleMessage(endpoint.nonBlockingReceive());
    }

    private Message handleMessage(Message msg) {
        if (msg != null && msg.getPayload() instanceof KeyExchangeMessage) {
            System.out.println("Received KeyExchange");
            InetSocketAddress sender = msg.getSender();
            if (!this.communicationPartners.containsKey(sender)) {
                System.out.println("Adding unknown key");
                this.communicationPartners.put(sender, ((KeyExchangeMessage) msg.getPayload()).getPublicKey());
                this.endpoint.send(sender, new KeyExchangeMessage(this.publicKey));
            }
            return null;
        }
        if (this.encryptionMode == EncryptionMode.SYMMETRICAL)
            return decryptSymmetrical(msg);
        return decryptAsymmetrical(msg);
    }

    private Message decryptSymmetrical(Message msg) {
        if (msg == null)
            return null;
        SealedObject encryptedObject = (SealedObject) msg.getPayload();
        try {
            return new Message((Serializable) encryptedObject.getObject(this.decryptSymmetrical), msg.getSender());
        } catch (IOException | ClassNotFoundException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Message decryptAsymmetrical(Message msg) {
        if (msg == null)
            return null;
        if (!this.communicationPartners.containsKey(msg.getSender())) {
            this.endpoint.send(msg.getSender(), new KeyExchangeMessage(this.publicKey));
            return null;
        }
        PublicKey publicKey = this.communicationPartners.get(msg.getSender());
        SealedObject encryptedObject = (SealedObject) msg.getPayload();
        try {
            return new Message((Serializable) encryptedObject.getObject(publicKey), msg.getSender());
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }
}

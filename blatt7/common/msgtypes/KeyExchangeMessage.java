package common.msgtypes;

import java.io.Serializable;
import java.security.PublicKey;

public class KeyExchangeMessage implements Serializable {

    private final PublicKey publicKey;

    public KeyExchangeMessage(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}

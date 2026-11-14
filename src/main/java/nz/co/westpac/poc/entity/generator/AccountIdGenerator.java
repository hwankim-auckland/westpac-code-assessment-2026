package nz.co.westpac.poc.entity.generator;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class AccountIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ID_BYTES = 32;

    public String next() {
        byte[] bytes = new byte[ID_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

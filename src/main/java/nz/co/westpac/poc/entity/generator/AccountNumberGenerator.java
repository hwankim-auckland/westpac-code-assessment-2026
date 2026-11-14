package nz.co.westpac.poc.entity.generator;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import nz.co.westpac.poc.entity.AccountRepository;
import org.springframework.stereotype.Service;

// Mock: real account numbering rules are unknown. Fixed 03 0187 prefix, random 7 digits,
// per-customer suffix from 000 held in memory only.
@Service
@AllArgsConstructor
public class AccountNumberGenerator {

    private static final String BANK_BRANCH_PREFIX = "03 0187";
    private static final int ACCOUNT_DIGITS_UPPER_BOUND = 10_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository accountRepository;

    public String next(UUID customerId) {
        int accountDigits = RANDOM.nextInt(ACCOUNT_DIGITS_UPPER_BOUND);
        int suffix = accountRepository.countByCustomerId(customerId);
        return "%s %07d %03d".formatted(BANK_BRANCH_PREFIX, accountDigits, suffix);
    }
}

package nz.co.westpac.poc.entity.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import nz.co.westpac.poc.entity.AccountRepository;
import org.junit.jupiter.api.Test;

class AccountNumberGeneratorTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);

    private final AccountNumberGenerator generator = new AccountNumberGenerator(accountRepository);

    @Test
    void followsTheBankBranchAccountNumberFormat() {
        when(accountRepository.countByCustomerId(UUID.randomUUID())).thenReturn(0);

        assertThat(generator.next(UUID.randomUUID()))
                .matches("03 0187 [0-9]{7} [0-9]{3}");
    }

    @Test
    void usesTheNumberOfExistingAccountsAsTheSuffix() {
        UUID customerId = UUID.randomUUID();
        when(accountRepository.countByCustomerId(customerId)).thenReturn(2);

        assertThat(generator.next(customerId)).endsWith(" 002");
    }

    @Test
    void keepsTheSuffixAtZeroForACustomerWithoutAccounts() {
        UUID customerId = UUID.randomUUID();
        when(accountRepository.countByCustomerId(customerId)).thenReturn(0);

        assertThat(generator.next(customerId)).endsWith(" 000");
    }
}

package nz.co.westpac.poc.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import nz.co.westpac.poc.api.dto.AccountResponse;
import nz.co.westpac.poc.entity.Account;
import org.junit.jupiter.api.Test;

class AccountMapperTest {

    @Test
    void mapsEveryAccountFieldToTheResponse() {
        Account account = Account.builder()
                .id("a".repeat(64))
                .customerId(UUID.randomUUID())
                .accountNumber("03 0187 1234567 000")
                .customerName("Hwan Kim")
                .accountNickname("holiday")
                .build();

        AccountResponse response = AccountMapper.toResponse(account);

        assertThat(response.accountId()).isEqualTo("a".repeat(64));
        assertThat(response.accountNumber()).isEqualTo("03 0187 1234567 000");
        assertThat(response.customerName()).isEqualTo("Hwan Kim");
        assertThat(response.accountNickname()).isEqualTo("holiday");
    }

    @Test
    void keepsAMissingNicknameNull() {
        Account account = Account.builder()
                .id("a".repeat(64))
                .customerName("Hwan Kim")
                .build();

        assertThat(AccountMapper.toResponse(account).accountNickname()).isNull();
    }
}

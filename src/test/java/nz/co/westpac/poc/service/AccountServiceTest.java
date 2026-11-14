package nz.co.westpac.poc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nz.co.westpac.poc.api.dto.AccountResponse;
import nz.co.westpac.poc.api.dto.CreateAccountRequest;
import nz.co.westpac.poc.entity.Account;
import nz.co.westpac.poc.entity.AccountRepository;
import nz.co.westpac.poc.entity.Customer;
import nz.co.westpac.poc.entity.generator.AccountIdGenerator;
import nz.co.westpac.poc.entity.generator.AccountNumberGenerator;
import nz.co.westpac.poc.entity.validator.AccountValidator;
import nz.co.westpac.poc.error.AccountLimitExceededException;
import nz.co.westpac.poc.error.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("6d29ceee-e865-49b7-b34f-96f801460f2e");
    private static final String ACCOUNT_ID = "a".repeat(64);
    private static final String ACCOUNT_NUMBER = "03 0187 1234567 000";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountValidator accountValidator;

    @Mock
    private AccountCache accountCache;

    @Mock
    private AccountIdGenerator accountIdGenerator;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createsAnAccountForTheCustomerNameFromTheCustomerRecord() {
        when(customerService.getCustomer(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(accountRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(0);
        when(accountIdGenerator.next()).thenReturn(ACCOUNT_ID);
        when(accountNumberGenerator.next(CUSTOMER_ID)).thenReturn(ACCOUNT_NUMBER);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.create(CUSTOMER_ID, new CreateAccountRequest("holiday"));

        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.accountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(response.customerName()).isEqualTo("Hwan Kim");
        assertThat(response.accountNickname()).isEqualTo("holiday");
        verify(accountValidator).validateAccountNickname("holiday");
    }

    @Test
    void rejectsAFurtherAccountWhenTheCustomerAlreadyHoldsFive() {
        when(accountRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(5);

        assertThatThrownBy(() -> accountService.create(CUSTOMER_ID, new CreateAccountRequest("holiday")))
                .isInstanceOf(AccountLimitExceededException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void rejectsACreationWhenTheCustomerDoesNotExist() {
        when(accountRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(0);
        when(customerService.getCustomer(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.create(CUSTOMER_ID, new CreateAccountRequest("holiday")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void returnsTheCachedAccountWithoutQueryingTheDatabase() {
        AccountResponse cached = response();
        when(accountCache.get(ACCOUNT_ID)).thenReturn(Optional.of(cached));

        assertThat(accountService.get(ACCOUNT_ID)).isEqualTo(cached);
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void readsTheAccountFromTheDatabaseAndCachesItOnAMiss() {
        when(accountCache.get(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account()));

        AccountResponse response = accountService.get(ACCOUNT_ID);

        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
        verify(accountCache).put(ACCOUNT_ID, response);
    }

    @Test
    void rejectsAnUnknownAccount() {
        when(accountCache.get(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.get(ACCOUNT_ID))
                .isInstanceOf(AccountNotFoundException.class);
        verify(accountCache, never()).put(anyString(), any(AccountResponse.class));
    }

    @Test
    void listsTheCustomerAccountsSortedByAccountNumber() {
        when(accountRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of(
                account("03 0187 7654321 001"),
                account("03 0187 1234567 000")));

        assertThat(accountService.getByCustomer(CUSTOMER_ID))
                .extracting(AccountResponse::accountNumber)
                .containsExactly("03 0187 1234567 000", "03 0187 7654321 001");
    }

    private static Customer customer() {
        return Customer.builder().customerId(CUSTOMER_ID).customerName("Hwan Kim").build();
    }

    private static Account account() {
        return account(ACCOUNT_NUMBER);
    }

    private static Account account(String accountNumber) {
        return Account.builder()
                .id(ACCOUNT_ID)
                .customerId(CUSTOMER_ID)
                .accountNumber(accountNumber)
                .customerName("Hwan Kim")
                .accountNickname("holiday")
                .build();
    }

    private static AccountResponse response() {
        return new AccountResponse(ACCOUNT_ID, ACCOUNT_NUMBER, "Hwan Kim", "holiday");
    }
}

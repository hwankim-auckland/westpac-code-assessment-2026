package nz.co.westpac.poc.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import nz.co.westpac.poc.api.AccountMapper;
import nz.co.westpac.poc.api.dto.AccountResponse;
import nz.co.westpac.poc.api.dto.CreateAccountRequest;
import nz.co.westpac.poc.entity.Account;
import nz.co.westpac.poc.entity.AccountRepository;
import nz.co.westpac.poc.entity.generator.AccountIdGenerator;
import nz.co.westpac.poc.entity.generator.AccountNumberGenerator;
import nz.co.westpac.poc.entity.validator.AccountValidator;
import nz.co.westpac.poc.error.AccountLimitExceededException;
import nz.co.westpac.poc.error.AccountNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AccountService {
    static final int MAX_ACCOUNTS_PER_CUSTOMER = 5;

    private final AccountRepository accountRepository;
    private final AccountValidator accountValidator;
    private final AccountCache accountCache;
    private final AccountIdGenerator accountIdGenerator;
    private final AccountNumberGenerator accountNumberGenerator;

    private final CustomerService customerService;


    @Transactional
    public AccountResponse create(UUID customerId, CreateAccountRequest request) {
        accountValidator.validateAccountNickname(request.accountNickname());

        if (accountRepository.countByCustomerId(customerId) >= MAX_ACCOUNTS_PER_CUSTOMER) {
            throw new AccountLimitExceededException(MAX_ACCOUNTS_PER_CUSTOMER);
        }
        Account account = customerService.getCustomer(customerId).map(customer ->
                Account.builder()
                        .id(accountIdGenerator.next())
                        .accountNumber(accountNumberGenerator.next(customerId))
                        .customerId(customerId)
                        .customerName(customer.getCustomerName())
                        .accountNickname(request.accountNickname())
                        .build()
        ).orElseThrow(()-> new AccountNotFoundException("Cannot find customer"));

        return AccountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getByCustomer(UUID customerId) {
        return accountRepository.findAllByCustomerId(customerId).stream()
                .sorted(Comparator.comparing(Account::getAccountNumber))
                .map(AccountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String accountId) {
        Optional<AccountResponse> cached = accountCache.get(accountId);
        if (cached.isPresent()) {
            return cached.get();
        }

        AccountResponse account = accountRepository.findById(accountId)
                .map(AccountMapper::toResponse)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        accountCache.put(accountId, account);
        return account;
    }
}

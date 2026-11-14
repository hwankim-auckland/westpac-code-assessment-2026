package nz.co.westpac.poc.api;

import nz.co.westpac.poc.api.dto.AccountResponse;
import nz.co.westpac.poc.entity.Account;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerName(),
                account.getAccountNickname());
    }
}

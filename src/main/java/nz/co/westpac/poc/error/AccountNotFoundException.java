package nz.co.westpac.poc.error;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Savings bank account %s was not found".formatted(accountId));
    }
}

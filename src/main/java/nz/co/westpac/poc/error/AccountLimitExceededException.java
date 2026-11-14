package nz.co.westpac.poc.error;

public class AccountLimitExceededException extends RuntimeException {

    public AccountLimitExceededException(int maximumAccounts) {
        super("A customer cannot have more than %d savings bank accounts".formatted(maximumAccounts));
    }
}

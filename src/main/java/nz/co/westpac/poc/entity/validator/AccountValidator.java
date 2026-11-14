package nz.co.westpac.poc.entity.validator;

import java.util.Locale;
import nz.co.westpac.poc.entity.Account;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountValidator {

    private static final int NICKNAME_MIN_LENGTH = 5;
    private static final int NICKNAME_MAX_LENGTH = 30;
    private static final String NOT_ALLOWED_NICK_NAME_LIST_KEY = "not_allowed_nick_name_lists";

    private final StringRedisTemplate redisTemplate;

    public AccountValidator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void validate(Account account) {
        validateAccountNickname(account.getAccountNickname());
    }

    public void validateAccountNickname(String accountNickname) {
        if (accountNickname == null) {
            return;
        }
        if (accountNickname.length() < NICKNAME_MIN_LENGTH
                || accountNickname.length() > NICKNAME_MAX_LENGTH) {
            throw new InvalidAccountException(
                    "accountNickname must be between %d and %d characters"
                            .formatted(NICKNAME_MIN_LENGTH, NICKNAME_MAX_LENGTH));
        }
        if (isNotAllowed(accountNickname)) {
            throw new InvalidAccountException("accountNickname is not allowed");
        }
    }

    private boolean isNotAllowed(String accountNickname) {
        String normalized = accountNickname.toLowerCase(Locale.ROOT);
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(NOT_ALLOWED_NICK_NAME_LIST_KEY, normalized));
    }
}

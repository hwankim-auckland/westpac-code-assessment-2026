package nz.co.westpac.poc.entity.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import nz.co.westpac.poc.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class AccountValidatorTest {

    private static final String BLOCKLIST_KEY = "not_allowed_nick_name_lists";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private AccountValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AccountValidator(redisTemplate);
    }

    @Test
    void acceptsAMissingNicknameWithoutCallingRedis() {
        assertThatCode(() -> validator.validateAccountNickname(null)).doesNotThrowAnyException();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void rejectsANicknameShorterThanFiveCharacters() {
        assertThatThrownBy(() -> validator.validateAccountNickname("abcd"))
                .isInstanceOf(InvalidAccountException.class)
                .hasMessageContaining("between 5 and 30");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void rejectsANicknameLongerThanThirtyCharacters() {
        assertThatThrownBy(() -> validator.validateAccountNickname("a".repeat(31)))
                .isInstanceOf(InvalidAccountException.class)
                .hasMessageContaining("between 5 and 30");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void acceptsANicknameThatIsNotOnTheBlocklist() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(BLOCKLIST_KEY, "holiday")).thenReturn(false);

        assertThatCode(() -> validator.validateAccountNickname("holiday")).doesNotThrowAnyException();
    }

    @Test
    void rejectsABlockedNicknameIgnoringCase() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(BLOCKLIST_KEY, "admin")).thenReturn(true);

        assertThatThrownBy(() -> validator.validateAccountNickname("ADMIN"))
                .isInstanceOf(InvalidAccountException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void validatesTheNicknameOfAnAccount() {
        Account account = Account.builder().accountNickname("abcd").build();

        assertThatThrownBy(() -> validator.validate(account))
                .isInstanceOf(InvalidAccountException.class);
    }
}

package nz.co.westpac.poc.entity.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountIdGeneratorTest {

    private final AccountIdGenerator generator = new AccountIdGenerator();

    @Test
    void generatesSixtyFourLowerCaseHexCharacters() {
        assertThat(generator.next()).matches("[0-9a-f]{64}");
    }

    @Test
    void generatesADifferentIdEveryTime() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            ids.add(generator.next());
        }

        assertThat(ids).hasSize(1_000);
    }
}

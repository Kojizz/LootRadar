package dev.lootradar.currency;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class CurrencyTests {
    @Test void acceptsUsdBasedRubAndTengeRates() throws Exception {
        var now = Instant.now();
        var json = new ObjectMapper().readTree("""
            {"result":"success","base_code":"USD","time_last_update_unix":%d,"rates":{"RUB":81.52,"KZT":448.77}}
            """.formatted(now.getEpochSecond()));
        var result = CurrencyController.parseRates(json, now);
        assertThat((Map<?,?>)result.get("rates")).hasSize(3);
        assertThat(result.get("stale")).isEqualTo(false);
        ((com.fasterxml.jackson.databind.node.ObjectNode)json.path("rates")).remove("KZT");
        assertThatThrownBy(() -> CurrencyController.parseRates(json, now)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsOldRates() throws Exception {
        var json = new ObjectMapper().readTree("{\"result\":\"success\",\"base_code\":\"USD\",\"time_last_update_unix\":1}");
        assertThatThrownBy(() -> CurrencyController.parseRates(json, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }
}

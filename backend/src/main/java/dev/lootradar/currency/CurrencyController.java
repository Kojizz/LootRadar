package dev.lootradar.currency;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.*;
import java.math.*;
import java.net.URI;
import java.net.http.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import org.w3c.dom.Element;

@RestController
public class CurrencyController {
    private Map<String,Object> saved;
    private Instant attempted = Instant.EPOCH;
    @GetMapping("/api/currencies") public synchronized Map<String,Object> rates() {
        if (attempted.isAfter(Instant.now().minusSeconds(3600))) return saved == null ? unavailable() : saved;
        attempted = Instant.now();
        try {
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(
                HttpRequest.newBuilder(URI.create("https://www.cbr.ru/scripts/XML_daily.asp")).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IllegalStateException();
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            var doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(response.body()));
            var nodes = doc.getElementsByTagName("Valute");
            Map<String,BigDecimal> rub = new HashMap<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element)nodes.item(i);
                String code = e.getElementsByTagName("CharCode").item(0).getTextContent();
                if (!Set.of("USD", "KZT").contains(code)) continue;
                var value = new BigDecimal(e.getElementsByTagName("Value").item(0).getTextContent().replace(',', '.'));
                var nominal = new BigDecimal(e.getElementsByTagName("Nominal").item(0).getTextContent());
                rub.put(code, value.divide(nominal, 8, RoundingMode.HALF_UP));
            }
            if (rub.size() != 2 || rub.values().stream().anyMatch(v -> v.signum() <= 0)) throw new IllegalStateException();
            String date = doc.getDocumentElement().getAttribute("Date");
            LocalDate published = LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            if (published.isBefore(LocalDate.now(ZoneOffset.UTC).minusDays(7))) throw new IllegalStateException();
            saved = Map.of("rates", Map.of("USD", BigDecimal.ONE, "RUB", rub.get("USD"), "KZT", rub.get("USD").divide(rub.get("KZT"), 6, RoundingMode.HALF_UP)), "date", published.toString(), "stale", false, "source", "Central Bank of Russia");
        } catch (Exception error) {
            if (saved != null) { var stale = new HashMap<>(saved); stale.put("stale", true); saved = stale; }
        }
        return saved == null ? unavailable() : saved;
    }
    private Map<String,Object> unavailable() { return Map.of("rates", Map.of("USD", 1), "date", "", "stale", true, "source", "Central Bank of Russia"); }
}

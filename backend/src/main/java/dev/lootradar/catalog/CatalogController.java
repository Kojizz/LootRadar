package dev.lootradar.catalog;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping("/catalog") public ApiModels.Catalog catalog() { return catalog.catalog(); }
    @GetMapping("/games/{id:[a-zA-Z0-9-]+}") public ApiModels.Detail detail(@PathVariable String id) { return catalog.detail(id); }
}

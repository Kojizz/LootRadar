package dev.lootradar.auth;

import dev.lootradar.catalog.CatalogService;
import dev.lootradar.wishlist.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"lootradar.sync-enabled=false", "spring.config.import=",
    "GOOGLE_CLIENT_ID=test-client", "GOOGLE_CLIENT_SECRET=test-secret", "APP_BASE_URL=http://127.0.0.1:5173",
    "spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class AuthIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired WishlistRepository wishlist;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean SteamOpenIdClient steam;
    @MockitoBean CatalogService catalog;
    @BeforeEach void clear() {
        wishlist.deleteAll();
        jdbc.update("DELETE FROM games");
        jdbc.update("INSERT INTO games (id, title) VALUES ('100', 'Test game')");
    }

    @Test void googleLoginUsesConfiguredCallbackAndBasicScopes() throws Exception {
        mvc.perform(get("/api/auth/session")).andExpect(jsonPath("$.googleEnabled").value(true))
            .andExpect(jsonPath("$.authenticated").value(false));
        String redirect = mvc.perform(get("/oauth2/authorization/google")).andExpect(status().isFound())
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(redirect).startsWith("https://accounts.google.com/")
            .contains("client_id=test-client", "scope=openid%20profile%20email", "state=", "nonce=",
                "redirect_uri=http://127.0.0.1:5173/login/oauth2/code/google");
    }

    @Test void steamRequiresProviderVerificationAndConsumesState() throws Exception {
        var session = (MockHttpSession)mvc.perform(get("/api/auth/steam")).andExpect(status().isFound())
            .andReturn().getRequest().getSession();
        String oldId = session.getId();
        var params = assertion(session);
        when(steam.verify(anyMap())).thenReturn(true);
        mvc.perform(get("/api/auth/steam/callback").session(session).params(params))
            .andExpect(redirectedUrl("http://127.0.0.1:5173/"));
        assertThat(session.getId()).isNotEqualTo(oldId);
        mvc.perform(get("/api/auth/session").session(session)).andExpect(jsonPath("$.authenticated").value(true));
        mvc.perform(get("/api/auth/steam/callback").session(session).params(params))
            .andExpect(redirectedUrl("http://127.0.0.1:5173/?login=failed"));
        verify(steam, times(1)).verify(anyMap());
    }

    @Test void steamRejectsUnverifiedAndMismatchedAssertions() throws Exception {
        var session = (MockHttpSession)mvc.perform(get("/api/auth/steam")).andReturn().getRequest().getSession();
        var params = assertion(session);
        when(steam.verify(anyMap())).thenReturn(false);
        mvc.perform(get("/api/auth/steam/callback").session(session).params(params))
            .andExpect(redirectedUrl("http://127.0.0.1:5173/?login=failed"));
        mvc.perform(get("/api/auth/session").session(session)).andExpect(jsonPath("$.authenticated").value(false));
        clearInvocations(steam);
        session = (MockHttpSession)mvc.perform(get("/api/auth/steam")).andReturn().getRequest().getSession();
        params = assertion(session);
        params.set("openid.return_to", "https://example.com/forged");
        mvc.perform(get("/api/auth/steam/callback").session(session).params(params))
            .andExpect(redirectedUrl("http://127.0.0.1:5173/?login=failed"));
        verifyNoInteractions(steam);
    }

    @Test void accountWishlistIsIsolatedAndGuestImportPreservesExistingTargets() throws Exception {
        String guest = UUID.randomUUID().toString();
        mvc.perform(put("/api/wishlist/100").header("X-Profile-Id", guest).contentType("application/json")
            .content("{\"targetPrice\":5}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/wishlist/100").with(csrf()).header("X-Profile-Id", guest)
            .contentType("application/json").content("{\"targetPrice\":5}")).andExpect(status().isOk());
        mvc.perform(post("/api/wishlist/import").with(csrf()).header("X-Profile-Id", guest))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/wishlist").with(user("steam:alice")).header("X-Profile-Id", guest))
            .andExpect(content().json("[]"));
        mvc.perform(post("/api/wishlist/import").with(user("steam:alice")).with(csrf()).header("X-Profile-Id", guest))
            .andExpect(status().isNoContent());
        mvc.perform(put("/api/wishlist/100").with(user("steam:alice")).with(csrf()).header("X-Profile-Id", guest)
            .contentType("application/json").content("{\"targetPrice\":3}")).andExpect(status().isOk());
        mvc.perform(post("/api/wishlist/import").with(user("steam:alice")).with(csrf()).header("X-Profile-Id", guest))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/wishlist").with(user("steam:alice")))
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].targetPrice").value(3));
        mvc.perform(get("/api/wishlist").with(user("steam:bob")).header("X-Profile-Id", guest))
            .andExpect(content().json("[]"));
        mvc.perform(get("/api/wishlist").header("X-Profile-Id", guest))
            .andExpect(jsonPath("$[0].targetPrice").value(5));
    }

    private LinkedMultiValueMap<String,String> assertion(MockHttpSession session) {
        var params = new LinkedMultiValueMap<String,String>();
        String state = (String)session.getAttribute("steamState");
        params.set("state", state);
        params.set("openid.ns", "http://specs.openid.net/auth/2.0");
        params.set("openid.mode", "id_res");
        params.set("openid.op_endpoint", "https://steamcommunity.com/openid/login");
        params.set("openid.claimed_id", "https://steamcommunity.com/openid/id/76561198000000000");
        params.set("openid.identity", "https://steamcommunity.com/openid/id/76561198000000000");
        params.set("openid.return_to", "http://127.0.0.1:5173/api/auth/steam/callback?state=" + state);
        params.set("openid.response_nonce", Instant.now().truncatedTo(ChronoUnit.SECONDS) + "nonce");
        params.set("openid.assoc_handle", "test-handle");
        params.set("openid.signed", "op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle");
        return params;
    }
}

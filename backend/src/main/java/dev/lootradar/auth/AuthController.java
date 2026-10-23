package dev.lootradar.auth;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final String base;
    private final boolean googleEnabled;
    private final SteamOpenIdClient steam;
    public AuthController(@Value("${APP_BASE_URL:http://127.0.0.1:5173}") String base,
            @Value("${GOOGLE_CLIENT_ID:}") String id, @Value("${GOOGLE_CLIENT_SECRET:}") String secret, SteamOpenIdClient steam) {
        this.base = base.replaceAll("/$", ""); googleEnabled = !id.isBlank() && !secret.isBlank();
        this.steam = steam;
    }
    @GetMapping("/session") public Map<String,Object> session(Authentication auth, CsrfToken csrf) {
        return Map.of("authenticated", Identity.signedIn(auth), "name", Identity.signedIn(auth) ? Identity.name(auth) : "",
            "googleEnabled", googleEnabled, "csrfToken", csrf.getToken(), "csrfHeader", csrf.getHeaderName());
    }
    @GetMapping("/steam") public void steam(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String state = UUID.randomUUID().toString();
        request.getSession().setAttribute("steamState", state);
        request.getSession().setAttribute("steamStarted", Instant.now());
        String callback = base + "/api/auth/steam/callback?state=" + state;
        response.sendRedirect(UriComponentsBuilder.fromUriString("https://steamcommunity.com/openid/login")
            .queryParam("openid.ns", "http://specs.openid.net/auth/2.0").queryParam("openid.mode", "checkid_setup")
            .queryParam("openid.return_to", callback).queryParam("openid.realm", base + "/")
            .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
            .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select").build().encode().toUriString());
    }
    @GetMapping("/steam/callback") public void callback(@RequestParam Map<String,String> params,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        var session = request.getSession(false);
        Object state = session == null ? null : session.getAttribute("steamState");
        Instant started = session == null ? null : (Instant)session.getAttribute("steamStarted");
        if (session != null) {session.removeAttribute("steamState"); session.removeAttribute("steamStarted");}
        try {
            String claimed = params.getOrDefault("openid.claimed_id", "");
            var signed = Set.of(params.getOrDefault("openid.signed", "").split(","));
            if (state == null || !state.equals(params.get("state")) || started == null || started.isBefore(Instant.now().minusSeconds(600))
                || !"id_res".equals(params.get("openid.mode"))
                || !"http://specs.openid.net/auth/2.0".equals(params.get("openid.ns"))
                || !"https://steamcommunity.com/openid/login".equals(params.get("openid.op_endpoint"))
                || !claimed.matches("https://steamcommunity\\.com/openid/id/[0-9]{17}")
                || !claimed.equals(params.get("openid.identity"))
                || !(base + "/api/auth/steam/callback?state=" + state).equals(params.get("openid.return_to"))
                || !signed.containsAll(Set.of("op_endpoint", "claimed_id", "identity", "return_to", "response_nonce", "assoc_handle")))
                throw new IllegalArgumentException("Invalid Steam assertion");
            Instant nonceTime = Instant.parse(params.getOrDefault("openid.response_nonce", "").substring(0,20));
            if (nonceTime.isBefore(Instant.now().minusSeconds(600)) || nonceTime.isAfter(Instant.now().plusSeconds(60))) throw new IllegalArgumentException();
            if (!steam.verify(params)) throw new IllegalArgumentException();
            request.changeSessionId();
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken("steam:" + claimed.substring(claimed.lastIndexOf('/') + 1), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            response.sendRedirect(base + "/");
        } catch (Exception error) { response.sendRedirect(base + "/?login=failed"); }
    }
}

package dev.lootradar.auth;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Identity {
    private Identity() {}
    public static boolean signedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
    public static String profile(Authentication auth, String guest) {
        if (signedIn(auth)) {
            String key = auth.getPrincipal() instanceof OidcUser oidc ? "google:" + oidc.getSubject() : auth.getName();
            return "account:" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }
        try { return UUID.fromString(guest).toString(); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A guest profile is required"); }
    }
    public static String name(Authentication auth) {
        if (auth.getPrincipal() instanceof OidcUser user) return user.getFullName() == null ? "Google player" : user.getFullName();
        return "Steam player " + auth.getName().substring(Math.max(0, auth.getName().length() - 4));
    }
}

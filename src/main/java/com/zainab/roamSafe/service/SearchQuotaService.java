package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
 * Free-tier search allowance for signed-out visitors.
 *
 * Guests get a handful of lookups before being asked to sign up, which is the
 * freemium boundary in the product plan. Two deliberate choices:
 *
 * The count is per session rather than per user, because we have no identity for
 * a guest. That makes it a soft gate - clearing cookies resets it - and that is
 * the right trade: a hard gate would need tracking we don't do, and the point is
 * to prompt sign-up, not to lock people out of safety information.
 *
 * Nothing is ever counted twice for the same query in a session. Re-reading a
 * page you already paid a search for shouldn't cost another one.
 */
@Service
public class SearchQuotaService {

    /** Free lookups before a guest is asked to sign up. */
    public static final int GUEST_SEARCH_LIMIT = 5;

    private static final String SEEN_KEY = "rs.searches.seen";

    /**
     * Records a search and reports whether it's allowed.
     *
     * Signed-in users are never limited here: the free tier is already gated on
     * how much of each report it shows, and stacking a second limit on top would
     * punish people for creating the account we just asked them for.
     */
    public boolean allow(HttpSession session, User user, String queryKey) {
        if (user != null) {
            return true;
        }
        java.util.Set<String> seen = seen(session);
        String key = queryKey == null ? "" : queryKey.trim().toLowerCase();
        if (seen.contains(key)) {
            return true; // already counted, don't charge for a revisit
        }
        if (seen.size() >= GUEST_SEARCH_LIMIT) {
            return false;
        }
        seen.add(key);
        session.setAttribute(SEEN_KEY, seen);
        return true;
    }

    /** Searches used so far, for showing "2 of 5 left" style messaging. */
    public int used(HttpSession session, User user) {
        return user != null ? 0 : seen(session).size();
    }

    public int remaining(HttpSession session, User user) {
        return user != null ? Integer.MAX_VALUE : Math.max(0, GUEST_SEARCH_LIMIT - used(session, user));
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<String> seen(HttpSession session) {
        Object existing = session.getAttribute(SEEN_KEY);
        if (existing instanceof java.util.Set) {
            return (java.util.Set<String>) existing;
        }
        return new java.util.LinkedHashSet<>();
    }
}

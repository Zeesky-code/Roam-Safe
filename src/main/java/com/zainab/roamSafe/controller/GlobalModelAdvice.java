package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.dto.LandingView.PaletteItem;
import com.zainab.roamSafe.service.LandingService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Supplies model attributes needed by the global shell (fragments/shell.html)
 * to every MVC view, so the ⌘K command palette is populated on all pages
 * rather than only the landing page.
 *
 * Scoped to @Controller (Thymeleaf views) — it does not touch @RestController
 * JSON endpoints.
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAdvice {

    private final LandingService landingService;

    public GlobalModelAdvice(LandingService landingService) {
        this.landingService = landingService;
    }

    @ModelAttribute("paletteItems")
    public List<PaletteItem> paletteItems() {
        return landingService.paletteItems();
    }

    /**
     * The signed-in user, or null, on every view.
     *
     * The shell's nav is shared by every page, so it can't rely on individual
     * controllers to say who is signed in — most don't set anything, which is
     * why "Sign in" and "Get started" stayed visible after logging in. Resolving
     * it centrally means the nav is correct everywhere by default.
     */
    @ModelAttribute("currentUser")
    public com.zainab.roamSafe.model.User currentUser(jakarta.servlet.http.HttpSession session) {
        Object user = session.getAttribute("user");
        return user instanceof com.zainab.roamSafe.model.User u ? u : null;
    }

    /**
     * Expose the CSRF token as a plain model attribute so templates can render
     * the hidden field via {@code ${csrfToken.token}}. Accessing the token
     * through the {@code ${_csrf}} request attribute broke under Thymeleaf 3.1,
     * truncating every form; resolving it here (via Spring's argument resolver)
     * and passing it as a model value is reliable.
     */
    @ModelAttribute
    public void csrf(CsrfToken token, Model model) {
        if (token != null) {
            model.addAttribute("csrfToken", token);
        }
    }
}

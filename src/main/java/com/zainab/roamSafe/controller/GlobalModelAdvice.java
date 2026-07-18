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

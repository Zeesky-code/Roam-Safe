package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.dto.LandingView.PaletteItem;
import com.zainab.roamSafe.service.LandingService;
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
}

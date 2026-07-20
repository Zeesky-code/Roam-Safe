package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.Trip;
import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.model.WatchedCity;
import com.zainab.roamSafe.repository.TripRepository;
import com.zainab.roamSafe.repository.WatchedCityRepository;
import com.zainab.roamSafe.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * The signed-in dashboard: watched destinations, the next trip, and recent
 * reports in those places.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final WatchedCityRepository watchedCityRepository;
    private final TripRepository tripRepository;

    public DashboardController(DashboardService dashboardService,
            WatchedCityRepository watchedCityRepository,
            TripRepository tripRepository) {
        this.dashboardService = dashboardService;
        this.watchedCityRepository = watchedCityRepository;
        this.tripRepository = tripRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("view", dashboardService.build(user));
        return "dashboard";
    }

    @PostMapping("/dashboard/watch")
    public String watch(@RequestParam String city, HttpSession session, RedirectAttributes flash) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        String name = city == null ? "" : city.trim();
        if (name.isBlank()) {
            flash.addFlashAttribute("error", "Enter a city to watch.");
            return "redirect:/dashboard";
        }
        // Quietly ignore a city already on the list rather than failing on the
        // unique constraint - re-adding is a no-op, not an error worth showing.
        if (watchedCityRepository.findByUserIdAndCityNameIgnoreCase(user.getId(), name).isEmpty()) {
            watchedCityRepository.save(new WatchedCity(user.getId(), name));
            flash.addFlashAttribute("success", "Now watching " + name + ".");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/unwatch")
    @Transactional
    public String unwatch(@RequestParam String city, HttpSession session, RedirectAttributes flash) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        watchedCityRepository.deleteByUserIdAndCityNameIgnoreCase(user.getId(), city);
        flash.addFlashAttribute("success", "Stopped watching " + city + ".");
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/trip")
    public String addTrip(@RequestParam String city,
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpSession session,
            RedirectAttributes flash) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (end.isBefore(start)) {
                flash.addFlashAttribute("error", "The return date can't be before the departure date.");
                return "redirect:/dashboard";
            }
            tripRepository.save(new Trip(user.getId(), city.trim(), start, end));
            // Watching the destination is the point of adding a trip, so do it
            // rather than making the user say it twice.
            if (watchedCityRepository.findByUserIdAndCityNameIgnoreCase(user.getId(), city.trim()).isEmpty()) {
                watchedCityRepository.save(new WatchedCity(user.getId(), city.trim()));
            }
            flash.addFlashAttribute("success", "Trip to " + city.trim() + " saved.");
        } catch (java.time.format.DateTimeParseException e) {
            flash.addFlashAttribute("error", "Those dates weren't valid.");
        }
        return "redirect:/dashboard";
    }
}

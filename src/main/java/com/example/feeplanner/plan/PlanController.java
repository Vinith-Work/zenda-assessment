package com.example.feeplanner.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class PlanController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final InstalmentPlanner planner;

    public PlanController(InstalmentPlanner planner) {
        this.planner = planner;
    }

    @GetMapping("/api/plans")
    public InstalmentPlanner.Plan plan(
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "3") int instalments,
            @RequestParam(defaultValue = "MONTHLY") Frequency frequency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate firstDueDate) {
        LocalDate first = firstDueDate != null ? firstDueDate : LocalDate.now(ZONE);
        try {
            return planner.plan(amount, instalments, frequency, first);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

package com.example.feeplanner.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Splits in paise; leftover paise go to the first instalments.
 */
@Component
public class InstalmentPlanner {

    public static final int MAX_INSTALMENTS = 12;
    static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000"); // 10 crore

    public record Instalment(int number, LocalDate dueDate, BigDecimal amount) {
    }

    public record Plan(BigDecimal totalAmount, int instalments, Frequency frequency, List<Instalment> schedule) {
    }

    public Plan plan(BigDecimal totalAmount, int instalments, Frequency frequency, LocalDate firstDueDate) {
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (totalAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("amount must be at most " + MAX_AMOUNT.toPlainString());
        }
        if (totalAmount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("amount can have at most 2 decimal places");
        }
        if (instalments < 1 || instalments > MAX_INSTALMENTS) {
            throw new IllegalArgumentException("instalments must be between 1 and " + MAX_INSTALMENTS);
        }
        if (frequency == null || firstDueDate == null) {
            throw new IllegalArgumentException("frequency and first due date are required");
        }

        BigDecimal total = totalAmount.setScale(2);
        long totalPaise = total.movePointRight(2).longValueExact();
        if (totalPaise < instalments) {
            throw new IllegalArgumentException("amount is too small to split into " + instalments + " instalments");
        }

        long basePaise = totalPaise / instalments;
        long leftoverPaise = totalPaise % instalments;

        List<Instalment> schedule = new ArrayList<>(instalments);
        for (int i = 0; i < instalments; i++) {
            long paise = basePaise + (i < leftoverPaise ? 1 : 0);
            // Always counted from the first date, so 31 Jan -> 28 Feb -> 31 Mar (not 28 Mar).
            LocalDate dueDate = firstDueDate.plusMonths((long) i * frequency.months());
            schedule.add(new Instalment(i + 1, dueDate, BigDecimal.valueOf(paise, 2)));
        }
        return new Plan(total, instalments, frequency, List.copyOf(schedule));
    }
}

package com.example.feeplanner.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class InstalmentPlannerTest {

    private final InstalmentPlanner planner = new InstalmentPlanner();

    @Test
    void leftoverPaiseGoToTheFirstInstalmentsAndTheTotalIsExact() {
        InstalmentPlanner.Plan plan = planner.plan(new BigDecimal("1000"), 3, Frequency.MONTHLY, LocalDate.of(2026, 11, 1));

        assertEquals(List.of(new BigDecimal("333.34"), new BigDecimal("333.33"), new BigDecimal("333.33")), amounts(plan));
        assertEquals(new BigDecimal("1000.00"), sum(plan));
    }

    @Test
    void moreThanOneLeftoverPaisa() {
        InstalmentPlanner.Plan plan = planner.plan(new BigDecimal("0.05"), 3, Frequency.MONTHLY, LocalDate.of(2026, 11, 1));

        assertEquals(List.of(new BigDecimal("0.02"), new BigDecimal("0.02"), new BigDecimal("0.01")), amounts(plan));
    }

    @Test
    void quarterlyDueDates() {
        InstalmentPlanner.Plan plan = planner.plan(new BigDecimal("45000"), 4, Frequency.QUARTERLY, LocalDate.of(2026, 4, 10));

        assertEquals(List.of(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 7, 10), LocalDate.of(2026, 10, 10), LocalDate.of(2027, 1, 10)),
                plan.schedule().stream().map(InstalmentPlanner.Instalment::dueDate).toList());
        assertEquals(new BigDecimal("11250.00"), plan.schedule().get(0).amount());
    }

    @Test
    void monthEndDatesAreCountedFromTheFirstDate() {
        InstalmentPlanner.Plan plan = planner.plan(new BigDecimal("300"), 3, Frequency.MONTHLY, LocalDate.of(2026, 1, 31));

        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
                plan.schedule().stream().map(InstalmentPlanner.Instalment::dueDate).toList());
    }

    @Test
    void rejectsInvalidInput() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        assertThrows(IllegalArgumentException.class, () -> planner.plan(BigDecimal.ZERO, 3, Frequency.MONTHLY, date));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(new BigDecimal("10.005"), 3, Frequency.MONTHLY, date));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(new BigDecimal("100"), 0, Frequency.MONTHLY, date));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(new BigDecimal("100"), 13, Frequency.MONTHLY, date));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(new BigDecimal("0.02"), 3, Frequency.MONTHLY, date));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(new BigDecimal("100000000.01"), 3, Frequency.MONTHLY, date));
    }

    private static List<BigDecimal> amounts(InstalmentPlanner.Plan plan) {
        return plan.schedule().stream().map(InstalmentPlanner.Instalment::amount).toList();
    }

    private static BigDecimal sum(InstalmentPlanner.Plan plan) {
        return plan.schedule().stream().map(InstalmentPlanner.Instalment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

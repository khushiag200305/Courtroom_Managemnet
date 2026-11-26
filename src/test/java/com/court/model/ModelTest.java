package com.court.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {

    @Test
    void filedCaseCanBeRegisteredOrReturned() {
        assertTrue(CaseStatus.FILED.canMoveTo(CaseStatus.REGISTERED));
        assertTrue(CaseStatus.FILED.canMoveTo(CaseStatus.DEFECTIVE));
        assertFalse(CaseStatus.FILED.canMoveTo(CaseStatus.DISPOSED));
    }

    @Test
    void disposedCaseCannotChange() {
        for (CaseStatus next : CaseStatus.values()) {
            assertFalse(CaseStatus.DISPOSED.canMoveTo(next));
        }
    }

    @Test
    void courtCaseRefusesInvalidStatusChange() {
        CourtCase c = new CourtCase();
        assertFalse(c.moveTo(CaseStatus.RESERVED));          // FILED -> RESERVED is not allowed
        assertEquals(CaseStatus.FILED, c.getStatus());
        assertTrue(c.moveTo(CaseStatus.REGISTERED));
        assertEquals(CaseStatus.REGISTERED, c.getStatus());
    }

    @Test
    void factoryCreatesTheRightSubclass() {
        assertInstanceOf(Registrar.class, UserFactory.create(Role.REGISTRAR));
        assertInstanceOf(Judge.class, UserFactory.create(Role.JUDGE));
        assertInstanceOf(Lawyer.class, UserFactory.create(Role.LAWYER));
    }

    @Test
    void eachRoleOpensItsOwnDashboard() {
        // polymorphism: the same method call gives a different screen for each subclass
        assertEquals("registrar_dashboard.fxml", UserFactory.create(Role.REGISTRAR).getDashboardFxml());
        assertEquals("judge_dashboard.fxml", UserFactory.create(Role.JUDGE).getDashboardFxml());
        assertEquals("lawyer_dashboard.fxml", UserFactory.create(Role.LAWYER).getDashboardFxml());
    }

    @Test
    void outcomesLeadToTheRightStatus() {
        assertEquals(CaseStatus.PENDING, OrderOutcome.ADJOURNED.getResultingStatus());
        assertEquals(CaseStatus.RESERVED, OrderOutcome.RESERVED.getResultingStatus());
        assertTrue(OrderOutcome.NEXT_HEARING.needsNextDate());
        assertFalse(OrderOutcome.DISPOSED.needsNextDate());
    }
}

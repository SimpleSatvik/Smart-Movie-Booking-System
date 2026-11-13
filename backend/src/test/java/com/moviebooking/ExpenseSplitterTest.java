package com.moviebooking;

import com.moviebooking.dto.AddExpenseRequestDTO;
import com.moviebooking.dto.CreateOutingRequestDTO;
import com.moviebooking.dto.OutingSummaryResponseDTO;
import com.moviebooking.entity.Outing;
import com.moviebooking.entity.User;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.service.ExpenseSplitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExpenseSplitterTest {

    @Autowired
    private ExpenseSplitService expenseSplitService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.moviebooking.repository.BookingRepository bookingRepository;

    @Autowired
    private com.moviebooking.repository.ShowRepository showRepository;

    @Test
    @DisplayName("Feature 3: Complete Outing creation, Popcorn expense, Debt calculation, and Settlement")
    void testGroupExpenseSplitAndSettlement() {
        // 1. Create two test friends if not present
        User varun = userRepository.findByGmailAndIsDeletedFalse("varun@gmail.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Varun Sharma")
                        .gmail("varun@gmail.com")
                        .password("pass123")
                        .role("ROLE_USER")
                        .build()));

        User akshit = userRepository.findByGmailAndIsDeletedFalse("akshit@gmail.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Akshit Gupta")
                        .gmail("akshit@gmail.com")
                        .password("pass123")
                        .role("ROLE_USER")
                        .build()));

        // Satvik is User 1
        User satvik = userRepository.findById(1L).orElseThrow();

        // Create a unique booking for this test to avoid collision on one-to-one outing constraint
        com.moviebooking.entity.Booking testBooking = bookingRepository.save(com.moviebooking.entity.Booking.builder()
                .bookingReference("TKT-SPLIT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(satvik)
                .show(showRepository.findAll().get(0))
                .totalAmountPaid(BigDecimal.valueOf(400.00))
                .status(com.moviebooking.entity.BookingStatus.CONFIRMED)
                .build());

        // 2. Satvik creates an Outing for this Booking (Ticket cost = ₹400)
        CreateOutingRequestDTO createReq = CreateOutingRequestDTO.builder()
                .bookingId(testBooking.getId())
                .name("Jawan Movie Night")
                .memberEmails(List.of("satvik@gmail.com", "varun@gmail.com", "akshit@gmail.com"))
                .build();

        Outing outing = expenseSplitService.createOuting(createReq, satvik.getId());
        assertNotNull(outing.getId());

        // 3. Varun logs an expense: ₹200 for Popcorn
        AddExpenseRequestDTO expenseReq = AddExpenseRequestDTO.builder()
                .paidByUserId(varun.getId())
                .amount(BigDecimal.valueOf(200.00))
                .reason("Popcorn Combo")
                .build();

        expenseSplitService.addExpense(outing.getId(), expenseReq);

        // 4. Calculate Group Summary
        OutingSummaryResponseDTO summary = expenseSplitService.calculateOutingSummary(outing.getId());

        System.out.println("\n========== OUTING EXPENSE SUMMARY ==========");
        System.out.println("Outing: " + summary.getOutingName());
        System.out.println("Total Members: " + summary.getTotalMembers());
        System.out.println("Total Group Spend: ₹" + summary.getTotalGroupSpend());
        System.out.println("Per-Person Share: ₹" + summary.getPerPersonShare());
        System.out.println("--------------------------------------------");
        summary.getMemberBalances().forEach(b -> 
            System.out.printf("Member: %-15s | Paid: ₹%-7s | Net Balance: ₹%s\n", b.getName(), b.getTotalPaid(), b.getNetBalance())
        );
        System.out.println("--------------------------------------------");
        System.out.println("Settlement Instructions:");
        summary.getDebtInstructions().forEach(inst -> 
            System.out.printf("👉 %s owes %s: ₹%s\n", inst.getFromUserName(), inst.getToUserName(), inst.getAmount())
        );
        System.out.println("Is All Settled: " + summary.isAllSettled());
        System.out.println("============================================\n");

        // Assertions
        assertEquals(3, summary.getTotalMembers());
        assertEquals(new BigDecimal("600.00"), summary.getTotalGroupSpend()); // 400 tickets + 200 popcorn
        assertEquals(new BigDecimal("200.00"), summary.getPerPersonShare());
        assertFalse(summary.isAllSettled());

        // Exact debt instruction: Akshit owes Satvik ₹200
        assertEquals(1, summary.getDebtInstructions().size());
        assertEquals("Akshit Gupta", summary.getDebtInstructions().get(0).getFromUserName());
        assertEquals("Satvik Kapoor", summary.getDebtInstructions().get(0).getToUserName());
        assertEquals(new BigDecimal("200.00"), summary.getDebtInstructions().get(0).getAmount());

        // 5. Akshit settles up with Satvik
        expenseSplitService.recordSettlement(outing.getId(), akshit.getId(), satvik.getId(), BigDecimal.valueOf(200.00));

        // 6. Recalculate summary after settlement
        OutingSummaryResponseDTO settledSummary = expenseSplitService.calculateOutingSummary(outing.getId());
        System.out.println("After Akshit paid Satvik ₹200, Is All Settled: " + settledSummary.isAllSettled());
        assertTrue(settledSummary.isAllSettled(), "Group should be 100% settled after payment!");
    }
}

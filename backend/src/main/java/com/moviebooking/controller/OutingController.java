package com.moviebooking.controller;

import com.moviebooking.dto.AddExpenseRequestDTO;
import com.moviebooking.dto.CreateOutingRequestDTO;
import com.moviebooking.dto.MyOutingResponseDTO;
import com.moviebooking.dto.OutingSummaryResponseDTO;
import com.moviebooking.entity.Expense;
import com.moviebooking.entity.Outing;
import com.moviebooking.entity.OutingMember;
import com.moviebooking.entity.Settlement;
import com.moviebooking.service.ExpenseSplitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OutingController {

    private final ExpenseSplitService expenseSplitService;

    /**
     * POST /api/outings?creatorUserId=1
     * Create an outing group attached to a booking
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOuting(
            @Valid @RequestBody CreateOutingRequestDTO request,
            @RequestParam Long creatorUserId) {

        Outing outing = expenseSplitService.createOuting(request, creatorUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "outingId", outing.getId(),
                "name", outing.getName(),
                "message", "Outing created and booking ticket cost logged as Expense #1!"
        ));
    }

    /**
     * GET /api/outings/by-booking/{bookingIdOrRef}
     * Check if an outing exists for a booking ID or reference, and if so, return its full live summary
     */
    @GetMapping("/by-booking/{bookingIdOrRef}")
    public ResponseEntity<?> getOutingByBooking(@PathVariable String bookingIdOrRef) {
        return expenseSplitService.findByBookingIdOrReference(bookingIdOrRef)
                .map(outing -> ResponseEntity.ok(expenseSplitService.calculateOutingSummary(outing.getId())))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * POST /api/outings/{outingId}/expenses
     * Log an additional expense (e.g. Popcorn, Snacks)
     */
    @PostMapping("/{outingId}/expenses")
    public ResponseEntity<Map<String, Object>> addExpense(
            @PathVariable Long outingId,
            @Valid @RequestBody AddExpenseRequestDTO request) {

        Expense expense = expenseSplitService.addExpense(outingId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "expenseId", expense.getId(),
                "amount", expense.getAmount(),
                "reason", expense.getReason(),
                "message", "Expense logged successfully!"
        ));
    }

    /**
     * DELETE /api/outings/{outingId}/expenses/{expenseId}
     * Delete a custom snack/drink expense
     */
    @DeleteMapping("/{outingId}/expenses/{expenseId}")
    public ResponseEntity<Map<String, Object>> removeExpense(
            @PathVariable Long outingId,
            @PathVariable Long expenseId) {

        expenseSplitService.removeExpense(outingId, expenseId);

        return ResponseEntity.ok(Map.of(
                "message", "Expense removed successfully!"
        ));
    }

    /**
     * POST /api/outings/{outingId}/members?email=...
     * Add a real registered user to the outing by email
     */
    @PostMapping("/{outingId}/members")
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable Long outingId,
            @RequestParam String email) {

        OutingMember member = expenseSplitService.addMemberByEmail(outingId, email);

        return ResponseEntity.ok(Map.of(
                "memberId", member.getId(),
                "userId", member.getUser().getId(),
                "name", member.getUser().getName(),
                "email", member.getUser().getGmail(),
                "message", member.getUser().getName() + " added to the movie outing!"
        ));
    }

    /**
     * DELETE /api/outings/{outingId}/members/{userId}
     * Remove a member from the outing
     */
    @DeleteMapping("/{outingId}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable Long outingId,
            @PathVariable Long userId) {

        expenseSplitService.removeMember(outingId, userId);

        return ResponseEntity.ok(Map.of(
                "message", "Member removed from the movie outing."
        ));
    }

    /**
     * GET /api/outings/{outingId}/summary
     * The Expense Dashboard: Calculates net balances and who owes whom
     */
    @GetMapping("/{outingId}/summary")
    public ResponseEntity<OutingSummaryResponseDTO> getOutingSummary(@PathVariable Long outingId) {
        return ResponseEntity.ok(expenseSplitService.calculateOutingSummary(outingId));
    }

    /**
     * GET /api/outings/my-outings?userId=1
     * Fetches all outings the user is a member of (powers the user's Outings dashboard)
     */
    @GetMapping("/my-outings")
    public ResponseEntity<List<MyOutingResponseDTO>> getMyOutings(@RequestParam Long userId) {
        return ResponseEntity.ok(expenseSplitService.getMyOutings(userId));
    }

    /**
     * POST /api/outings/{outingId}/settle
     * Record a settlement when a member pays their debt in real life
     */
    @PostMapping("/{outingId}/settle")
    public ResponseEntity<Map<String, Object>> recordSettlement(
            @PathVariable Long outingId,
            @RequestParam Long payerId,
            @RequestParam Long receiverId,
            @RequestParam BigDecimal amount) {

        Settlement settlement = expenseSplitService.recordSettlement(outingId, payerId, receiverId, amount);

        return ResponseEntity.ok(Map.of(
                "settlementId", settlement.getId(),
                "amount", settlement.getAmount(),
                "message", "Settlement recorded successfully!"
        ));
    }
}

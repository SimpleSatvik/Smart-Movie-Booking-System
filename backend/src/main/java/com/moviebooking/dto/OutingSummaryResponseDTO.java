package com.moviebooking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutingSummaryResponseDTO {

    private Long outingId;
    private String outingName;
    private Long bookingId;
    private Long createdByUserId;
    private int totalMembers;
    private BigDecimal totalGroupSpend;
    private BigDecimal perPersonShare;
    private boolean isAllSettled;

    // What each person paid and their current standing
    private List<MemberBalanceDTO> memberBalances;

    // Clean human-readable instructions: e.g. "Akshit owes Satvik ₹450.00"
    private List<DebtInstructionDTO> debtInstructions;

    // Real list of expenses logged for this outing
    private List<ExpenseItemDTO> expenses;

    // Settlements recorded so far
    private List<SettlementRecordDTO> settlements;

    public List<MemberBalanceDTO> getMemberBalances() {
        return memberBalances != null ? memberBalances : new ArrayList<>();
    }

    public List<DebtInstructionDTO> getDebtInstructions() {
        return debtInstructions != null ? debtInstructions : new ArrayList<>();
    }

    public List<ExpenseItemDTO> getExpenses() {
        return expenses != null ? expenses : new ArrayList<>();
    }

    public List<SettlementRecordDTO> getSettlements() {
        return settlements != null ? settlements : new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberBalanceDTO {
        private Long userId;
        private String name;
        private String email;
        private BigDecimal totalPaid;
        private BigDecimal netBalance; // Positive = owed money, Negative = owes money, Zero = settled
        private boolean isOrganizer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DebtInstructionDTO {
        private Long fromUserId;
        private String fromUserName; // Who owes
        private Long toUserId;
        private String toUserName;   // Who gets paid
        private BigDecimal amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseItemDTO {
        private Long id;
        private String reason;
        private BigDecimal amount;
        private Long paidByUserId;
        private String paidByUserName;
        private boolean isTicket;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SettlementRecordDTO {
        private Long id;
        private Long payerId;
        private String payerName;
        private Long receiverId;
        private String receiverName;
        private BigDecimal amount;
        private LocalDateTime settledAt;
    }
}

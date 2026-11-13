package com.moviebooking.service;

import com.moviebooking.dto.AddExpenseRequestDTO;
import com.moviebooking.dto.CreateOutingRequestDTO;
import com.moviebooking.dto.MyOutingResponseDTO;
import com.moviebooking.dto.OutingSummaryResponseDTO;
import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseSplitService {

    private final OutingRepository outingRepository;
    private final OutingMemberRepository outingMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * 1. Create a new Outing group linked to a booking
     * Automatically logs the booking ticket cost as Expense #1
     */
    @Transactional
    public Outing createOuting(CreateOutingRequestDTO request, Long creatorUserId) {
        log.info("Creating outing: {} for booking: {} (ref: {})", request.getName(), request.getBookingId(), request.getBookingReference());

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId()).orElse(null);
        }
        if (booking == null && request.getBookingReference() != null && !request.getBookingReference().isBlank()) {
            booking = bookingRepository.findByBookingReference(request.getBookingReference().trim()).orElse(null);
        }
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found with ID: " + request.getBookingId() + " or Ref: " + request.getBookingReference());
        }

        // Check if an outing is already created for this booking
        Optional<Outing> existing = outingRepository.findByBookingId(booking.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found with ID: " + creatorUserId));

        // Create Outing
        Outing outing = Outing.builder()
                .name(request.getName())
                .booking(booking)
                .createdBy(creator)
                .build();
        outing = outingRepository.save(outing);

        // Add creator as member #1
        Set<Long> memberUserIds = new HashSet<>();
        memberUserIds.add(creator.getId());
        outingMemberRepository.save(OutingMember.builder().outing(outing).user(creator).build());

        // Add friends by email if provided
        if (request.getMemberEmails() != null) {
            for (String email : request.getMemberEmails()) {
                if (email == null || email.isBlank()) continue;
                String cleanEmail = email.trim().toLowerCase();
                Optional<User> friendOpt = userRepository.findByGmailAndIsDeletedFalse(cleanEmail)
                        .or(() -> userRepository.findByGmailIgnoreCase(cleanEmail));
                if (friendOpt.isPresent()) {
                    User friend = friendOpt.get();
                    if (memberUserIds.add(friend.getId())) { // Prevent duplicate additions
                        outingMemberRepository.save(OutingMember.builder().outing(outing).user(friend).build());
                    }
                } else {
                    log.warn("User with email {} not found, skipping for outing.", email);
                }
            }
        }

        // Automatically log the Ticket Booking as Expense #1 (paid by the booking owner)
        Expense ticketExpense = Expense.builder()
                .outing(outing)
                .paidBy(booking.getUser())
                .amount(booking.getTotalAmountPaid())
                .reason("Movie Tickets (" + booking.getShow().getMovie().getTitle() + ")")
                .build();
        expenseRepository.save(ticketExpense);

        log.info("Outing created with ID: {} and {} members", outing.getId(), memberUserIds.size());
        return outing;
    }

    /**
     * 2. Log an additional expense (e.g. Popcorn, drinks, parking)
     */
    @Transactional
    public Expense addExpense(Long outingId, AddExpenseRequestDTO request) {
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new IllegalArgumentException("Outing not found with ID: " + outingId));

        User payer = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new IllegalArgumentException("Payer user not found with ID: " + request.getPaidByUserId()));

        Expense expense = Expense.builder()
                .outing(outing)
                .paidBy(payer)
                .amount(request.getAmount())
                .reason(request.getReason())
                .build();

        return expenseRepository.save(expense);
    }

    /**
     * 3. Record a settlement (Payer pays back Receiver in real life)
     */
    @Transactional
    public Settlement recordSettlement(Long outingId, Long payerId, Long receiverId, BigDecimal amount) {
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new IllegalArgumentException("Outing not found with ID: " + outingId));

        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new IllegalArgumentException("Payer not found with ID: " + payerId));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found with ID: " + receiverId));

        Settlement settlement = Settlement.builder()
                .outing(outing)
                .payer(payer)
                .receiver(receiver)
                .amount(amount)
                .build();

        return settlementRepository.save(settlement);
    }

    /**
     * 4. Calculate Group Expense Summary using the Greedy Debt Minimization Algorithm
     */
    @Transactional(readOnly = true)
    public OutingSummaryResponseDTO calculateOutingSummary(Long outingId) {
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new IllegalArgumentException("Outing not found with ID: " + outingId));

        List<OutingMember> members = outingMemberRepository.findByOutingId(outingId);
        List<Expense> expenses = expenseRepository.findByOutingId(outingId);
        List<Settlement> settlements = settlementRepository.findByOutingId(outingId);

        int memberCount = members.size();
        if (memberCount == 0) {
            throw new IllegalStateException("Outing has no members");
        }

        // 1. Calculate total spend across all expenses
        BigDecimal totalSpend = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Per-person fair share = totalSpend / memberCount
        BigDecimal perPersonShare = totalSpend.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);

        // 3. Calculate how much each member personally paid
        Map<Long, BigDecimal> amountPaidMap = new HashMap<>();
        for (OutingMember m : members) {
            amountPaidMap.put(m.getUser().getId(), BigDecimal.ZERO);
        }
        for (Expense e : expenses) {
            Long payerId = e.getPaidBy().getId();
            amountPaidMap.put(payerId, amountPaidMap.getOrDefault(payerId, BigDecimal.ZERO).add(e.getAmount()));
        }

        // 4. Calculate raw net balance = (amountPaid - perPersonShare)
        Map<Long, BigDecimal> netBalanceMap = new HashMap<>();
        for (OutingMember m : members) {
            Long uId = m.getUser().getId();
            BigDecimal paid = amountPaidMap.get(uId);
            BigDecimal net = paid.subtract(perPersonShare);
            netBalanceMap.put(uId, net);
        }

        // Adjust for completed settlements:
        // Payer sent money -> their balance increases towards 0 (+amount)
        // Receiver got money -> their balance decreases towards 0 (-amount)
        for (Settlement s : settlements) {
            Long payerId = s.getPayer().getId();
            Long receiverId = s.getReceiver().getId();
            if (netBalanceMap.containsKey(payerId)) {
                netBalanceMap.put(payerId, netBalanceMap.get(payerId).add(s.getAmount()));
            }
            if (netBalanceMap.containsKey(receiverId)) {
                netBalanceMap.put(receiverId, netBalanceMap.get(receiverId).subtract(s.getAmount()));
            }
        }

        // 5. Build memberBalances list for response
        List<OutingSummaryResponseDTO.MemberBalanceDTO> balanceDTOs = new ArrayList<>();
        for (OutingMember m : members) {
            Long uId = m.getUser().getId();
            balanceDTOs.add(OutingSummaryResponseDTO.MemberBalanceDTO.builder()
                    .userId(uId)
                    .name(m.getUser().getName())
                    .email(m.getUser().getGmail())
                    .totalPaid(amountPaidMap.get(uId).setScale(2, RoundingMode.HALF_UP))
                    .netBalance(netBalanceMap.get(uId).setScale(2, RoundingMode.HALF_UP))
                    .isOrganizer(outing.getCreatedBy().getId().equals(uId))
                    .build());
        }

        // Build expenses list
        List<OutingSummaryResponseDTO.ExpenseItemDTO> expenseDTOs = expenses.stream()
                .map(e -> OutingSummaryResponseDTO.ExpenseItemDTO.builder()
                        .id(e.getId())
                        .reason(e.getReason())
                        .amount(e.getAmount().setScale(2, RoundingMode.HALF_UP))
                        .paidByUserId(e.getPaidBy().getId())
                        .paidByUserName(e.getPaidBy().getName())
                        .isTicket(e.getReason() != null && e.getReason().toLowerCase().startsWith("movie ticket"))
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();

        // Build settlements list
        List<OutingSummaryResponseDTO.SettlementRecordDTO> settlementDTOs = settlements.stream()
                .map(s -> OutingSummaryResponseDTO.SettlementRecordDTO.builder()
                        .id(s.getId())
                        .payerId(s.getPayer().getId())
                        .payerName(s.getPayer().getName())
                        .receiverId(s.getReceiver().getId())
                        .receiverName(s.getReceiver().getName())
                        .amount(s.getAmount().setScale(2, RoundingMode.HALF_UP))
                        .settledAt(s.getSettledAt())
                        .build())
                .toList();

        // 6. Run Greedy Debt Minimization to compute who owes whom
        List<OutingSummaryResponseDTO.DebtInstructionDTO> debtInstructions = computeDebtInstructions(members, netBalanceMap);

        boolean isAllSettled = netBalanceMap.values().stream()
                .allMatch(b -> b.abs().compareTo(BigDecimal.valueOf(0.01)) < 0);

        return OutingSummaryResponseDTO.builder()
                .outingId(outing.getId())
                .outingName(outing.getName())
                .bookingId(outing.getBooking().getId())
                .createdByUserId(outing.getCreatedBy().getId())
                .totalMembers(memberCount)
                .totalGroupSpend(totalSpend.setScale(2, RoundingMode.HALF_UP))
                .perPersonShare(perPersonShare)
                .isAllSettled(isAllSettled)
                .memberBalances(balanceDTOs)
                .debtInstructions(debtInstructions)
                .expenses(expenseDTOs)
                .settlements(settlementDTOs)
                .build();
    }

    /**
     * Greedy algorithm matching Debtors (< 0) with Creditors (> 0)
     */
    private List<OutingSummaryResponseDTO.DebtInstructionDTO> computeDebtInstructions(
            List<OutingMember> members, Map<Long, BigDecimal> netBalances) {

        Map<Long, String> nameMap = new HashMap<>();
        for (OutingMember m : members) {
            nameMap.put(m.getUser().getId(), m.getUser().getName());
        }

        @Getter
        @Setter
        @AllArgsConstructor
        class BalanceNode {
            Long userId;
            BigDecimal amount; // positive for creditor, absolute positive for debtor
        }

        List<BalanceNode> debtors = new ArrayList<>();
        List<BalanceNode> creditors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            BigDecimal bal = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (bal.compareTo(BigDecimal.valueOf(-0.01)) < 0) {
                debtors.add(new BalanceNode(entry.getKey(), bal.abs()));
            } else if (bal.compareTo(BigDecimal.valueOf(0.01)) > 0) {
                creditors.add(new BalanceNode(entry.getKey(), bal));
            }
        }

        List<OutingSummaryResponseDTO.DebtInstructionDTO> instructions = new ArrayList<>();

        int d = 0;
        int c = 0;

        while (d < debtors.size() && c < creditors.size()) {
            BalanceNode debtor = debtors.get(d);
            BalanceNode creditor = creditors.get(c);

            BigDecimal settleAmount = debtor.getAmount().min(creditor.getAmount());

            instructions.add(OutingSummaryResponseDTO.DebtInstructionDTO.builder()
                    .fromUserId(debtor.getUserId())
                    .fromUserName(nameMap.get(debtor.getUserId()))
                    .toUserId(creditor.getUserId())
                    .toUserName(nameMap.get(creditor.getUserId()))
                    .amount(settleAmount.setScale(2, RoundingMode.HALF_UP))
                    .build());

            debtor.setAmount(debtor.getAmount().subtract(settleAmount));
            creditor.setAmount(creditor.getAmount().subtract(settleAmount));

            if (debtor.getAmount().compareTo(BigDecimal.valueOf(0.01)) < 0) {
                d++;
            }
            if (creditor.getAmount().compareTo(BigDecimal.valueOf(0.01)) < 0) {
                c++;
            }
        }

        return instructions;
    }

    /**
     * 5. Get all outings that a specific user belongs to
     */
    @Transactional(readOnly = true)
    public List<MyOutingResponseDTO> getMyOutings(Long userId) {
        List<OutingMember> memberships = outingMemberRepository.findByUserId(userId);

        return memberships.stream()
                .map(m -> MyOutingResponseDTO.fromEntity(m.getOuting()))
                .toList();
    }

    /**
     * 6. Add a real registered user to an existing outing by their email
     */
    @Transactional
    public OutingMember addMemberByEmail(Long outingId, String email) {
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new IllegalArgumentException("Outing not found with ID: " + outingId));

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        String cleanEmail = email.trim().toLowerCase();
        User friend = userRepository.findByGmailAndIsDeletedFalse(cleanEmail)
                .or(() -> userRepository.findByGmailIgnoreCase(cleanEmail))
                .orElseThrow(() -> new IllegalArgumentException("No registered CineSync user found with email: " + email.trim()));

        List<OutingMember> existing = outingMemberRepository.findByOutingId(outingId);
        boolean alreadyMember = existing.stream().anyMatch(m -> m.getUser().getId().equals(friend.getId()));
        if (alreadyMember) {
            throw new IllegalArgumentException(friend.getName() + " (" + cleanEmail + ") is already in this movie outing!");
        }

        OutingMember newMember = OutingMember.builder()
                .outing(outing)
                .user(friend)
                .build();
        return outingMemberRepository.save(newMember);
    }

    /**
     * 7. Remove member from outing
     */
    @Transactional
    public void removeMember(Long outingId, Long userId) {
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new IllegalArgumentException("Outing not found with ID: " + outingId));

        if (outing.getCreatedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove the organizer from the movie outing!");
        }

        // Reassign any custom expenses paid by this member to the organizer
        List<Expense> memberExpenses = expenseRepository.findByOutingId(outingId).stream()
                .filter(e -> e.getPaidBy().getId().equals(userId))
                .toList();
        for (Expense exp : memberExpenses) {
            exp.setPaidBy(outing.getCreatedBy());
            expenseRepository.save(exp);
        }

        List<OutingMember> members = outingMemberRepository.findByOutingId(outingId);
        members.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(outingMemberRepository::delete);
    }

    /**
     * 8. Delete a custom snack/drink expense
     */
    @Transactional
    public void removeExpense(Long outingId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + expenseId));

        if (!expense.getOuting().getId().equals(outingId)) {
            throw new IllegalArgumentException("Expense does not belong to this outing!");
        }

        if (expense.getReason() != null && expense.getReason().toLowerCase().startsWith("movie ticket")) {
            throw new IllegalArgumentException("Base ticket booking expense cannot be deleted!");
        }

        expenseRepository.delete(expense);
    }

    /**
     * 9. Find outing by booking ID or Reference
     */
    @Transactional(readOnly = true)
    public Optional<Outing> findByBookingId(Long bookingId) {
        return outingRepository.findByBookingId(bookingId);
    }

    @Transactional(readOnly = true)
    public Optional<Outing> findByBookingIdOrReference(String bookingIdOrRef) {
        if (bookingIdOrRef == null || bookingIdOrRef.isBlank()) return Optional.empty();

        Optional<Booking> bookingOpt = Optional.empty();
        if (bookingIdOrRef.matches("\\d+")) {
            bookingOpt = bookingRepository.findById(Long.parseLong(bookingIdOrRef));
        }
        if (bookingOpt.isEmpty()) {
            bookingOpt = bookingRepository.findByBookingReference(bookingIdOrRef.trim());
        }
        return bookingOpt.flatMap(b -> outingRepository.findByBookingId(b.getId()));
    }
}

package com.cinesync.app.data

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

// ---------- Auth ----------
data class RegisterRequest(val name: String, val gmail: String, val password: String, val otp: String, val role: String = "ROLE_USER")
data class LoginRequest(val gmail: String, val password: String)
data class VerifyOtpRequest(val email: String, val otp: String)
data class ResetPasswordRequest(val email: String, val otp: String, val newPassword: String)
data class MessageResponse(val message: String? = null)
data class ApiError(val status: Int? = null, val error: String? = null, val message: String? = null)
data class AuthResponse(
    val userId: Long = 0, val name: String? = null, val gmail: String? = null, val role: String? = null,
    val token: String? = null, val requires2fa: Boolean = false, val message: String? = null,
)

// ---------- Catalogue ----------
data class Movie(
    val id: Long = 0, val title: String = "", val genre: String? = null, val language: String? = null,
    val duration: Int? = null, val mood: String? = null, val posterUrl: String? = null,
)
data class VibeRequest(val vibe: String)
data class VibeShow(val showId: Long = 0, val theatreName: String? = null, val screenName: String? = null, val startTime: String? = null, val endTime: String? = null)
data class VibeResult(
    val movieId: Long = 0, val movieTitle: String = "", val genre: String? = null, val language: String? = null,
    val mood: String? = null, val matchReason: String? = null, val confidenceScore: Double? = null,
    val upcomingShows: List<VibeShow>? = null,
)
data class Show(
    val showId: Long = 0, val movieId: Long? = null, val movieTitle: String? = null, val theatreName: String? = null,
    val theatreLocation: String? = null, val screenName: String? = null, val startTime: String? = null, val endTime: String? = null,
)
data class SeatInfo(
    val showSeatId: Long = 0, val seatId: Long? = null, val row: String = "", val seatNumber: Int = 0,
    val category: String = "", val price: BigDecimal = BigDecimal.ZERO, val status: String = "AVAILABLE",
)

// ---------- Bookings ----------
data class BookingRequest(val userId: Long, val showId: Long, val showSeatIds: List<Long>, val paymentMethod: String)
data class BookingResponse(
    val bookingId: Long = 0, val bookingReference: String? = null, val movieTitle: String? = null,
    val theatreName: String? = null, val screenName: String? = null, val showTime: String? = null,
    val seatCodes: List<String>? = null, val showSeatIds: List<Long>? = null, val totalAmountPaid: BigDecimal? = null,
    val status: String? = null, val transactionId: String? = null, val bookedAt: String? = null,
)

// ---------- Resale ----------
data class CreateListingRequest(val sellerUserId: Long, val showSeatId: Long, val askingPrice: BigDecimal)
data class ResaleListing(
    @SerializedName(value = "listingId", alternate = ["id"]) val listingId: Long = 0,
    val showSeatId: Long? = null,
    val movieTitle: String? = null, val theatreName: String? = null, val screenName: String? = null,
    @SerializedName(value = "showTime", alternate = ["startTime"]) val showTime: String? = null,
    @SerializedName(value = "seatCode", alternate = ["seatLabel"]) val seatCode: String? = null,
    val bookingReference: String? = null,
    val originalPrice: BigDecimal? = null, val askingPrice: BigDecimal? = null,
    @SerializedName(value = "sellerUserId", alternate = ["sellerId"]) val sellerUserId: Long? = null,
    val sellerName: String? = null, val buyerName: String? = null, val status: String? = null,
)

// ---------- Budget optimizer ----------
data class BudgetRequest(val totalBudget: BigDecimal, val numberOfSeats: Int, val date: String?, val genre: String?, val language: String?)
data class BudgetRecommendation(
    val recommendationType: String? = null, val movieId: Long? = null, val movieTitle: String? = null, val showId: Long = 0,
    val theatreName: String? = null, val screenName: String? = null,
    @SerializedName(value = "startTime", alternate = ["showTime"]) val startTime: String? = null,
    val seatCategory: String? = null, val pricePerSeat: BigDecimal? = null, val numberOfSeats: Int? = null,
    val totalCost: BigDecimal? = null, val remainingBudget: BigDecimal? = null, val availableSeats: Int? = null,
)

// ---------- Outings ----------
data class CreateOutingRequest(val bookingId: Long?, val bookingReference: String?, val name: String, val memberEmails: List<String>)
data class OutingCreated(val outingId: Long = 0, val name: String? = null, val message: String? = null)
data class AddExpenseRequest(val paidByUserId: Long, val amount: BigDecimal, val reason: String)
data class MyOuting(
    val outingId: Long = 0,
    @SerializedName(value = "outingName", alternate = ["name"]) val outingName: String? = null,
    val bookingReference: String? = null, val movieTitle: String? = null, val theatreName: String? = null,
    val showTime: String? = null, val memberCount: Int? = null, val totalSpend: BigDecimal? = null,
    val totalGroupSpend: BigDecimal? = null,
)
data class MemberBalance(
    val userId: Long = 0, val name: String? = null, val email: String? = null,
    val totalPaid: BigDecimal? = null, val netBalance: BigDecimal? = null, val isOrganizer: Boolean? = null,
)
data class DebtInstruction(
    val fromUserId: Long = 0, val fromUserName: String? = null, val toUserId: Long = 0,
    val toUserName: String? = null, val amount: BigDecimal = BigDecimal.ZERO,
)
data class OutingExpense(
    val id: Long = 0, val reason: String? = null, val amount: BigDecimal? = null,
    @SerializedName(value = "paidByName", alternate = ["payerName", "paidByUserName"]) val paidByName: String? = null,
    @SerializedName(value = "isTicket", alternate = ["ticket"]) val isTicket: Boolean? = null,
    val createdAt: String? = null,
)
data class OutingSettlement(
    val id: Long = 0,
    @SerializedName(value = "payerName", alternate = ["fromUserName"]) val payerName: String? = null,
    @SerializedName(value = "receiverName", alternate = ["toUserName"]) val receiverName: String? = null,
    val amount: BigDecimal? = null, val settledAt: String? = null,
)
data class OutingSummary(
    val outingId: Long = 0, val outingName: String? = null, val bookingId: Long? = null, val createdByUserId: Long? = null,
    val totalMembers: Int? = null, val totalGroupSpend: BigDecimal? = null, val perPersonShare: BigDecimal? = null,
    @SerializedName(value = "isAllSettled", alternate = ["allSettled"]) val isAllSettled: Boolean? = null,
    val memberBalances: List<MemberBalance>? = null, val debtInstructions: List<DebtInstruction>? = null,
    val expenses: List<OutingExpense>? = null, val settlements: List<OutingSettlement>? = null,
)

# CineSync Android Client (Kotlin) - Moviegoer App Specification & LLM Prompt

> **Instructions for the LLM generating the Android App:**
> You are an expert Android Engineer building a modern, production-ready Android application using **Kotlin**, **Jetpack Compose**, **Retrofit 2**, **Kotlin Coroutines / Flow**, and **Hilt / ViewModel Clean Architecture**.
> 
> **Scope & Restrictions:**
> - **ONLY Moviegoer / Customer Interface**: Do NOT build any Theatre Owner or Platform Administrator interfaces, screens, or portals.
> - Strictly follow the backend API endpoints, payload contracts, authentication rules, and business logic detailed below.
> - Every API call, JSON field name, and data class must match this specification 100% with no missing parameters or invented endpoints.

---

## 1. Backend Connectivity & Environment Setup

### 1.1 Base URLs
- **Android Studio Emulator:** `http://10.0.2.2:8080/api/`
- **Physical Device (Same Wi-Fi / Local Network):** `http://<YOUR_LOCAL_IP>:8080/api/` (e.g. `http://192.168.1.100:8080/api/`)
- **Remote Production / Cloud:** `https://<YOUR_DOMAIN>/api/`

### 1.2 Android Manifest Requirements
```xml
<!-- In AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="true" <!-- Required for local HTTP testing -->
    android:theme="@style/Theme.CineSync"
    ... >
```

### 1.3 Recommended Gradle Dependencies (`app/build.gradle.kts`)
```kotlin
dependencies {
    // Jetpack Compose BOM & UI
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // QR Code Generation for Digital Passes
    implementation("com.google.zxing:core:3.5.3")

    // Encrypted Storage for JWT and Session
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

---

## 2. Authentication Rules & Specifications

### 2.1 Critical Authentication Rules
1. **Login DOES NOT require OTP**:
   - Moviegoer enters `gmail` and `password`.
   - On success, the backend directly returns `token`, `userId`, `name`, `gmail`, and `role: "ROLE_USER"`.
   - Store the JWT token securely in `EncryptedSharedPreferences`.
2. **Registration REQUIRES Email OTP**:
   - Step 1: Send registration OTP to the moviegoer's email (`POST /api/auth/send-registration-otp?email=...`).
   - Step 2: User enters the 6-digit verification code.
   - Step 3: Complete registration (`POST /api/auth/register`). Role is strictly `"ROLE_USER"`.
   - **Bypass / Dev Testing Code:** In development or local testing, the backend accepts `123456` in addition to newly generated email OTPs.
3. **Forgot & Reset Password**:
   - Step 1: Request reset code (`POST /api/auth/forgot-password?email=...`).
   - Step 2: Reset password (`POST /api/auth/reset-password`).

---

## 3. Complete Kotlin Data Models (DTOs)

```kotlin
package com.cinesync.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

// ======================== AUTH DTOs ========================

data class LoginRequest(
    @SerializedName("gmail") val gmail: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("gmail") val gmail: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "ROLE_USER", // Always ROLE_USER for moviegoers
    @SerializedName("otp") val otp: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("newPassword") val newPassword: String
)

data class AuthResponse(
    @SerializedName("userId") val userId: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("gmail") val gmail: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("requires2fa") val requires2fa: Boolean?,
    @SerializedName("message") val message: String?
)

data class GenericMessageResponse(
    @SerializedName("message") val message: String
)

// ======================== MOVIE & SHOW DTOs ========================

data class MovieResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("genre") val genre: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("rating") val rating: Double? = 8.8,
    @SerializedName("mood") val mood: String?,
    @SerializedName("posterUrl") val posterUrl: String?
)

data class ShowResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("movieId") val movieId: Long,
    @SerializedName("movieTitle") val movieTitle: String,
    @SerializedName("theatreId") val theatreId: Long,
    @SerializedName("theatreName") val theatreName: String,
    @SerializedName("theatreLocation") val theatreLocation: String?,
    @SerializedName("screenId") val screenId: Long,
    @SerializedName("screenName") val screenName: String,
    @SerializedName("startTime") val startTime: String, // ISO-8601 string: e.g. "2026-11-14T19:30:00"
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("availableSeats") val availableSeats: Int?
)

enum class SeatCategory { SILVER, GOLD, PLATINUM, VIP }
enum class SeatStatus { AVAILABLE, LOCKED, BOOKED }

data class SeatLayoutResponse(
    @SerializedName("showSeatId") val showSeatId: Long,
    @SerializedName("seatId") val seatId: Long,
    @SerializedName("row") val row: String,
    @SerializedName("seatNumber") val seatNumber: Int,
    @SerializedName("category") val category: SeatCategory,
    @SerializedName("price") val price: BigDecimal,
    @SerializedName("status") val status: SeatStatus
)

// ======================== BOOKING DTOs ========================

enum class PaymentMethod { MOCK_UPI, MOCK_CARD, MOCK_NET_BANKING }

data class BookingRequest(
    @SerializedName("userId") val userId: Long,
    @SerializedName("showId") val showId: Long,
    @SerializedName("showSeatIds") val showSeatIds: List<Long>,
    @SerializedName("paymentMethod") val paymentMethod: PaymentMethod = PaymentMethod.MOCK_UPI
)

data class BookingResponse(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("bookingReference") val bookingReference: String,
    @SerializedName("movieTitle") val movieTitle: String,
    @SerializedName("theatreName") val theatreName: String,
    @SerializedName("screenName") val screenName: String,
    @SerializedName("showTime") val showTime: String,
    @SerializedName("seatCodes") val seatCodes: List<String>?, // e.g. ["A1", "A2"]
    @SerializedName("showSeatIds") val showSeatIds: List<Long>?,
    @SerializedName("totalAmountPaid") val totalAmountPaid: BigDecimal,
    @SerializedName("status") val status: String, // CONFIRMED, CANCELLED, RESOLD
    @SerializedName("transactionId") val transactionId: String?,
    @SerializedName("bookedAt") val bookedAt: String?
)

// ======================== RESALE MARKETPLACE DTOs ========================

data class CreateResaleListingRequest(
    @SerializedName("sellerUserId") val sellerUserId: Long,
    @SerializedName("showSeatId") val showSeatId: Long,
    @SerializedName("askingPrice") val askingPrice: BigDecimal
)

data class ResaleListingResponse(
    @SerializedName("listingId") val listingId: Long,
    @SerializedName("showSeatId") val showSeatId: Long,
    @SerializedName("movieTitle") val movieTitle: String,
    @SerializedName("theatreName") val theatreName: String,
    @SerializedName("screenName") val screenName: String,
    @SerializedName("showStartTime") val showStartTime: String,
    @SerializedName("rowName") val rowName: String?,
    @SerializedName("seatNumber") val seatNumber: Int?,
    @SerializedName("seatCode") val seatCode: String?,
    @SerializedName("bookingReference") val bookingReference: String?,
    @SerializedName("seatCategory") val seatCategory: String?,
    @SerializedName("originalPrice") val originalPrice: BigDecimal?,
    @SerializedName("askingPrice") val askingPrice: BigDecimal,
    @SerializedName("sellerId") val sellerId: Long,
    @SerializedName("sellerName") val sellerName: String?,
    @SerializedName("buyerId") val buyerId: Long?,
    @SerializedName("buyerName") val buyerName: String?,
    @SerializedName("status") val status: String, // ACTIVE, SOLD, CANCELLED
    @SerializedName("listedAt") val listedAt: String?,
    @SerializedName("soldAt") val soldAt: String?
)

// ======================== BUDGET OPTIMIZER DTOs ========================

data class BudgetOptimizationRequest(
    @SerializedName("totalBudget") val totalBudget: BigDecimal,
    @SerializedName("numberOfSeats") val numberOfSeats: Int
)

data class BudgetRecommendationResponse(
    @SerializedName("recommendationType") val recommendationType: String, // "BEST_EXPERIENCE" or "MAX_SAVINGS"
    @SerializedName("movieId") val movieId: Long,
    @SerializedName("movieTitle") val movieTitle: String,
    @SerializedName("genre") val genre: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("theatreName") val theatreName: String,
    @SerializedName("screenName") val screenName: String,
    @SerializedName("showId") val showId: Long,
    @SerializedName("showStartTime") val showStartTime: String,
    @SerializedName("seatCategory") val seatCategory: String,
    @SerializedName("pricePerSeat") val pricePerSeat: BigDecimal,
    @SerializedName("numberOfSeats") val numberOfSeats: Int,
    @SerializedName("totalCost") val totalCost: BigDecimal,
    @SerializedName("remainingBudget") val remainingBudget: BigDecimal,
    @SerializedName("availableSeats") val availableSeats: Int
)

// ======================== VIBE SEARCH DTOs ========================

data class VibeSearchRequest(
    @SerializedName("vibe") val vibe: String
)

data class VibeSearchResponse(
    @SerializedName("movieId") val movieId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("genre") val genre: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("vibeMatch") val vibeMatch: String?,
    @SerializedName("shows") val shows: List<ShowResponse>?
)

// ======================== GROUP OUTINGS / EXPENSE SPLIT DTOs ========================

data class CreateOutingRequest(
    @SerializedName("bookingId") val bookingId: Long?,
    @SerializedName("bookingReference") val bookingReference: String?,
    @SerializedName("name") val name: String,
    @SerializedName("memberEmails") val memberEmails: List<String>
)

data class AddExpenseRequest(
    @SerializedName("payerUserId") val payerUserId: Long,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("reason") val reason: String
)

data class MyOutingResponse(
    @SerializedName("outingId") val outingId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("bookingReference") val bookingReference: String?,
    @SerializedName("movieTitle") val movieTitle: String?,
    @SerializedName("theatreName") val theatreName: String?,
    @SerializedName("showTime") val showTime: String?,
    @SerializedName("posterUrl") val posterUrl: String?,
    @SerializedName("totalSpend") val totalSpend: BigDecimal,
    @SerializedName("memberCount") val memberCount: Int,
    @SerializedName("isOrganizer") val isOrganizer: Boolean,
    @SerializedName("isAllSettled") val isAllSettled: Boolean
)

data class OutingSummaryResponse(
    @SerializedName("outingId") val outingId: Long,
    @SerializedName("outingName") val outingName: String,
    @SerializedName("bookingId") val bookingId: Long?,
    @SerializedName("createdByUserId") val createdByUserId: Long,
    @SerializedName("totalMembers") val totalMembers: Int,
    @SerializedName("totalGroupSpend") val totalGroupSpend: BigDecimal,
    @SerializedName("perPersonShare") val perPersonShare: BigDecimal,
    @SerializedName("isAllSettled") val isAllSettled: Boolean,
    @SerializedName("memberBalances") val memberBalances: List<MemberBalanceDTO>,
    @SerializedName("debtInstructions") val debtInstructions: List<DebtInstructionDTO>,
    @SerializedName("expenses") val expenses: List<ExpenseItemDTO>,
    @SerializedName("settlements") val settlements: List<SettlementRecordDTO>
)

data class MemberBalanceDTO(
    @SerializedName("userId") val userId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("totalPaid") val totalPaid: BigDecimal,
    @SerializedName("netBalance") val netBalance: BigDecimal, // Positive = owed money (+), Negative = owes money (-)
    @SerializedName("isOrganizer") val isOrganizer: Boolean
)

data class DebtInstructionDTO(
    @SerializedName("fromUserId") val fromUserId: Long,
    @SerializedName("fromUserName") val fromUserName: String,
    @SerializedName("toUserId") val toUserId: Long,
    @SerializedName("toUserName") val toUserName: String,
    @SerializedName("amount") val amount: BigDecimal
)

data class ExpenseItemDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("reason") val reason: String,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("paidByUserId") val paidByUserId: Long,
    @SerializedName("paidByUserName") val paidByUserName: String,
    @SerializedName("isTicket") val isTicket: Boolean,
    @SerializedName("createdAt") val createdAt: String?
)

data class SettlementRecordDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("payerId") val payerId: Long,
    @SerializedName("payerName") val payerName: String,
    @SerializedName("receiverId") val receiverId: Long,
    @SerializedName("receiverName") val receiverName: String,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("settledAt") val settledAt: String?
)
```

---

## 4. Retrofit API Interface (`CineSyncApiService.kt`)

```kotlin
package com.cinesync.data.network

import com.cinesync.data.model.*
import retrofit2.Response
import retrofit2.http.*
import java.math.BigDecimal

interface CineSyncApiService {

    // ======================== AUTH ========================

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/send-registration-otp")
    suspend fun sendRegistrationOtp(@Query("email") email: String): Response<GenericMessageResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Query("email") email: String): Response<GenericMessageResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericMessageResponse>

    // ======================== MOVIES & DISCOVERY ========================

    @GET("movies")
    suspend fun getMovies(
        @Query("genre") genre: String? = null,
        @Query("language") language: String? = null
    ): Response<List<MovieResponse>>

    @GET("movies/{id}")
    suspend fun getMovieById(@Path("id") id: Long): Response<MovieResponse>

    @POST("movies/vibe-search")
    suspend fun vibeSearch(@Body request: VibeSearchRequest): Response<List<VibeSearchResponse>>

    // ======================== SHOWS & SEATS ========================

    @GET("shows/movie/{movieId}")
    suspend fun getShowsByMovie(
        @Path("movieId") movieId: Long,
        @Query("date") date: String? = null // Format: YYYY-MM-DD
    ): Response<List<ShowResponse>>

    @GET("shows/{showId}/seats")
    suspend fun getSeatLayout(@Path("showId") showId: Long): Response<List<SeatLayoutResponse>>

    // ======================== BOOKINGS ========================

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingRequest): Response<BookingResponse>

    @GET("bookings/{reference}")
    suspend fun getBookingByReference(@Path("reference") reference: String): Response<BookingResponse>

    @GET("bookings/user/{userId}")
    suspend fun getUserBookings(@Path("userId") userId: Long): Response<List<BookingResponse>>

    @POST("bookings/{bookingIdOrRef}/cancel")
    suspend fun cancelBooking(
        @Path("bookingIdOrRef") bookingIdOrRef: String,
        @Header("X-User-Email") email: String? = null,
        @Header("X-User-Role") role: String = "ROLE_USER",
        @Header("X-User-Id") userId: Long? = null
    ): Response<BookingResponse>

    // ======================== SPARE TICKET RESALE ========================

    @GET("resale/listings")
    suspend fun getActiveResaleListings(
        @Query("showId") showId: Long? = null
    ): Response<List<ResaleListingResponse>>

    @POST("resale/listings")
    suspend fun createResaleListing(@Body request: CreateResaleListingRequest): Response<ResaleListingResponse>

    @POST("resale/listings/{id}/buy")
    suspend fun buyResaleTicket(
        @Path("id") listingId: Long,
        @Query("buyerUserId") buyerUserId: Long
    ): Response<ResaleListingResponse>

    @DELETE("resale/listings/{id}")
    suspend fun cancelResaleListing(
        @Path("id") listingId: Long,
        @Query("sellerUserId") sellerUserId: Long
    ): Response<ResaleListingResponse>

    @GET("resale/my-listings")
    suspend fun getMyResaleListings(@Query("sellerUserId") sellerUserId: Long): Response<List<ResaleListingResponse>>

    // ======================== BUDGET OPTIMIZER ========================

    @POST("optimizer/movie-night")
    suspend fun optimizeBudget(@Body request: BudgetOptimizationRequest): Response<List<BudgetRecommendationResponse>>

    // ======================== GROUP OUTINGS / EXPENSE SPLIT ========================

    @GET("outings/my-outings")
    suspend fun getMyOutings(@Query("userId") userId: Long): Response<List<MyOutingResponse>>

    @GET("outings/by-booking/{bookingIdOrRef}")
    suspend fun getOutingByBooking(@Path("bookingIdOrRef") bookingIdOrRef: String): Response<OutingSummaryResponse?>

    @POST("outings")
    suspend fun createOuting(
        @Query("creatorUserId") creatorUserId: Long,
        @Body request: CreateOutingRequest
    ): Response<Map<String, Any>>

    @GET("outings/{outingId}/summary")
    suspend fun getOutingSummary(@Path("outingId") outingId: Long): Response<OutingSummaryResponse>

    @POST("outings/{outingId}/expenses")
    suspend fun addExpense(
        @Path("outingId") outingId: Long,
        @Body request: AddExpenseRequest
    ): Response<Map<String, Any>>

    @DELETE("outings/{outingId}/expenses/{expenseId}")
    suspend fun removeExpense(
        @Path("outingId") outingId: Long,
        @Path("expenseId") expenseId: Long
    ): Response<Map<String, Any>>

    @POST("outings/{outingId}/members")
    suspend fun addMember(
        @Path("outingId") outingId: Long,
        @Query("email") email: String
    ): Response<Map<String, Any>>

    @DELETE("outings/{outingId}/members/{userId}")
    suspend fun removeMember(
        @Path("outingId") outingId: Long,
        @Path("userId") userId: Long
    ): Response<Map<String, Any>>

    @POST("outings/{outingId}/settle")
    suspend fun recordSettlement(
        @Path("outingId") outingId: Long,
        @Query("payerId") payerId: Long,
        @Query("receiverId") receiverId: Long,
        @Query("amount") amount: BigDecimal
    ): Response<Map<String, Any>>
}
```

---

## 5. OkHttp Client & Auth Interceptor Setup

```kotlin
package com.cinesync.data.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val token = tokenProvider()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")
        return chain.proceed(requestBuilder.build())
    }
}

object NetworkClient {
    private const val BASE_URL = "http://10.0.2.2:8080/api/" // Or local LAN IP

    fun create(tokenProvider: () -> String?): CineSyncApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CineSyncApiService::class.java)
    }
}
```

---

## 6. Moviegoer UI Screens & Workflows

Build the following screens in Jetpack Compose:

```
                  ┌────────────────────────────────────────┐
                  │          Authentication Flow           │
                  │  (Login / Register with OTP / Forgot)  │
                  └──────────────────┬─────────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    ▼                                 ▼
         ┌─────────────────────┐           ┌─────────────────────┐
         │ 1. Home / Discovery │           │ 4. Digital Wallet   │
         │ - Now Showing Movies│           │ - Turnstile QR Code │
         │ - Vibe AI Search    │           │ - 2-Hr Cancel Guard │
         │ - Hero Showcase     │           │ - Link to Resale    │
         └──────────┬──────────┘           └─────────────────────┘
                    │                                 ▲
                    ▼                                 │
         ┌─────────────────────┐                      │
         │ 2. Show & Theatres  │                      │
         │ - Date Picker       │                      │
         │ - Screen Formats    │                      │
         └──────────┬──────────┘                      │
                    │                                 │
                    ▼                                 │
         ┌─────────────────────┐                      │
         │ 3. Seat Selection   │                      │
         │ - Live Radar Layout │                      │
         │ - Silver/Gold/Plat  │                      │
         │ - 1-Click Pay & Book├──────────────────────┘
         └─────────────────────┘
                    │
         ┌──────────┴──────────┬────────────────────────┐
         ▼                     ▼                        ▼
┌──────────────────┐  ┌──────────────────┐   ┌──────────────────────┐
│5. Spare Resale   │  │6. Budget Planner │   │7. Split Bill / Outing│
│- List extra seat │  │- Total budget    │   │- Add friends by email│
│- Concurrency Buy │  │- Party size      │   │- Net balance & debt  │
│- Fair cost trade │  │- Smart Optimizer │   │- Real-life settle up │
└──────────────────┘  └──────────────────┘   └──────────────────────┘
```

### Screen 1: Home & Movie Discovery
- Display **Now Showing** movies in a modern grid.
- **Hero Banner**: Auto-scrolling carousel of top releases.
- **Poster Fallback Rule**:
  - If a movie title contains `"Jawan"`, use:
    `https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdnT7OTN5iPqcTD9hozOIuIVG8IWbeFLcPMWDFybXLyw&s=10`
  - For other titles or broken links, fallback to:
    `https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80`
- **Vibe Search Modal / Pill**:
  - An AI-powered search bar where moviegoers type how they feel (e.g. *"Mind-bending sci-fi with dread"* or *"High adrenaline mass action"*).
  - Calls `POST /api/movies/vibe-search` and presents recommended movies with matched reasoning.

### Screen 2: Showtimes & Theatres
- Triggered when clicking on any movie card.
- Calls `GET /api/shows/movie/{movieId}?date={YYYY-MM-DD}`.
- Horizontal calendar strip (Today, Tomorrow, +2 Days...).
- Shows grouped by Theatre (e.g. "PVR Director's Cut", Audi 1 IMAX Laser, Audi 2 Dolby Atmos).

### Screen 3: Interactive Seat Selection
- Calls `GET /api/shows/{showId}/seats`.
- Displays seats categorized by:
  - `SILVER` (₹200) - Row A
  - `GOLD` (₹350) - Row B
  - `PLATINUM` (₹500) - Row C
- Seat status visuals:
  - `AVAILABLE` (Clickable, green/slate border)
  - `LOCKED` (Amber / in-progress by another user)
  - `BOOKED` (Dark grey, line-through, unclickable)
- Maximum 6 seats per booking.
- Bottom sheet showing Total Amount and **"Proceed to Book"**.
- Calls `POST /api/bookings` with selected `showSeatIds`. On success, navigates to the Digital Wallet with celebratory confetti!

### Screen 4: Digital Pass Wallet (My Tickets)
- Calls `GET /api/bookings/user/{userId}`.
- Displays realistic movie tickets with:
  - Movie Title, Theatre & Auditorium screen.
  - Show date and time.
  - Big seat badges (e.g. `ROW A - SEAT 4`, `ROW B - SEAT 5`).
  - Total amount paid.
  - **Turnstile QR Code**: Generated dynamically via ZXing: `CINESYNC-TICKET:<bookingReference>:SEATS-<seatCodes>`.
- **2-Hour Cancellation Rule**:
  - If showtime is > 2 hours away: Enable red **"Cancel Booking"** button. Calls `POST /api/bookings/{reference}/cancel`.
  - If showtime is < 2 hours away: Button is disabled with label `"Locked (< Xm to show)"`.
  - If ticket is actively listed on Resale: Cancellation is blocked until delisted.
- **"Sell Seat on Resale" Button**:
  - Opens the Spare Ticket Resale modal targeting this booking.

### Screen 5: Spare Ticket Resale Marketplace
- **Browse Tab**:
  - Calls `GET /api/resale/listings`.
  - Shows verified spare tickets listed by other fans.
  - Shows Seat Number, Theatre, Movie, Showtime, and Custom Asking Price.
  - **"Buy Ticket" Button**: Calls `POST /api/resale/listings/{id}/buy?buyerUserId={myUserId}`.
  - Once purchased, ticket is immediately moved to the buyer's Digital Wallet.
- **Sell Extra Ticket Modal**:
  - Filters user's upcoming confirmed bookings (must be >= 15 mins before showtime).
  - User chooses which seat they want to resell.
  - **Custom Asking Price Input**: User enters any desired price (e.g. ₹350, ₹450). The input must never revert or reset while typing.
  - Calls `POST /api/resale/listings`.

### Screen 6: Budget Planner & Movie Night Optimizer
- Moviegoer inputs:
  1. `Total Budget (₹)` (e.g. ₹800)
  2. `Number of Seats` (e.g. 2 people)
- Calls `POST /api/optimizer/movie-night`.
- Returns two clear recommendation cards:
  - **Best Experience**: Highest seat tier (e.g. Platinum/Gold) within the budget.
  - **Max Savings**: Cheapest available tier (e.g. Silver) saving the maximum amount of cash.
- Shows total cost, leftover savings, and a 1-tap **"Book This Show"** shortcut.

### Screen 7: Group Outing & Split Bill
- Linked to a movie booking:
  - Calls `GET /api/outings/by-booking/{bookingReference}`.
  - If none exists, creates one via `POST /api/outings?creatorUserId={userId}`. The ticket cost is automatically logged as Expense #1.
- **Add Friends**:
  - Enter friend's registered email (`POST /api/outings/{id}/members?email=...`).
- **Log Extra Expenses**:
  - Add popcorn, nachos, drinks, or cab fares (`POST /api/outings/{id}/expenses`).
- **Automated Net Balances Dashboard**:
  - Calls `GET /api/outings/{id}/summary`.
  - Displays: Total Spend, Per-Person Share, and exact Debt Instructions (e.g. *"Akshit owes Satvik ₹450.00"*).
- **1-Tap Settle Up**:
  - Calls `POST /api/outings/{id}/settle?payerId={id}&receiverId={id}&amount={amount}` when paid in cash/UPI.

---

## 7. Crucial Error Handling & Edge Cases

| Scenario | HTTP Code / Response | Android Handling |
| :--- | :--- | :--- |
| **Concurrent Seat Lock Conflict** | `400` or `500` ("A concurrent conflict occurred...") | Show a Toast/Snackbar: *"Seat was just chosen by another moviegoer. Please pick another seat."* Refresh seat layout. |
| **Resale Race Condition** | `409` or `500` ("A concurrent conflict occurred...") | Show notification: *"This ticket was just purchased by another fan."* Refresh listings. |
| **Cancellation < 2 Hours** | `400` ("Cancellations must be requested at least 2 hours prior...") | Block cancellation in UI with countdown message. |
| **Double Resale Prevention** | `400` ("Seat is already actively listed...") | Strike-through already-listed seats in the sell modal. |
| **Network Offline** | `SocketTimeoutException` / `UnknownHostException` | Display an animated "No Connection" state with a "Retry" button. |

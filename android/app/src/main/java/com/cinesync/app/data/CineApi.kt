package com.cinesync.app.data

import retrofit2.http.*
import java.math.BigDecimal

interface CineApi {
    // Auth
    @POST("api/auth/send-registration-otp") suspend fun sendRegistrationOtp(@Query("email") email: String): MessageResponse
    @POST("api/auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
    @POST("api/auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("api/auth/verify-login-otp") suspend fun verifyLoginOtp(@Body body: VerifyOtpRequest): AuthResponse
    @POST("api/auth/forgot-password") suspend fun forgotPassword(@Query("email") email: String): MessageResponse
    @POST("api/auth/reset-password") suspend fun resetPassword(@Body body: ResetPasswordRequest): MessageResponse

    // Movies & shows
    @GET("api/movies") suspend fun movies(): List<Movie>
    @GET("api/movies/{id}") suspend fun movie(@Path("id") id: Long): Movie
    @POST("api/movies/vibe-search") suspend fun vibeSearch(@Body body: VibeRequest): List<VibeResult>
    @GET("api/shows/movie/{id}") suspend fun showsForMovie(@Path("id") id: Long): List<Show>
    @GET("api/shows/{id}/seats") suspend fun seats(@Path("id") showId: Long): List<SeatInfo>

    // Bookings
    @POST("api/bookings") suspend fun book(@Body body: BookingRequest): BookingResponse
    @GET("api/bookings/user/{userId}") suspend fun myBookings(@Path("userId") userId: Long): List<BookingResponse>
    @POST("api/bookings/{ref}/cancel") suspend fun cancelBooking(@Path("ref") bookingIdOrRef: String): BookingResponse

    // Resale
    @POST("api/resale/listings") suspend fun createListing(@Body body: CreateListingRequest): ResaleListing
    @GET("api/resale/listings") suspend fun listings(): List<ResaleListing>
    @POST("api/resale/listings/{id}/buy") suspend fun buyListing(@Path("id") id: Long, @Query("buyerUserId") buyerUserId: Long): ResaleListing
    @DELETE("api/resale/listings/{id}") suspend fun cancelListing(@Path("id") id: Long, @Query("sellerUserId") sellerUserId: Long): ResaleListing
    @GET("api/resale/my-listings") suspend fun myListings(@Query("sellerUserId") sellerUserId: Long): List<ResaleListing>

    // Budget optimizer
    @POST("api/optimizer/movie-night") suspend fun optimize(@Body body: BudgetRequest): List<BudgetRecommendation>

    // Outings
    @POST("api/outings") suspend fun createOuting(@Query("creatorUserId") creatorUserId: Long, @Body body: CreateOutingRequest): OutingCreated
    @GET("api/outings/my-outings") suspend fun myOutings(@Query("userId") userId: Long): List<MyOuting>
    @GET("api/outings/{id}/summary") suspend fun outingSummary(@Path("id") id: Long): OutingSummary
    @POST("api/outings/{id}/expenses") suspend fun addExpense(@Path("id") id: Long, @Body body: AddExpenseRequest): MessageResponse
    @DELETE("api/outings/{id}/expenses/{expenseId}") suspend fun deleteExpense(@Path("id") id: Long, @Path("expenseId") expenseId: Long): MessageResponse
    @POST("api/outings/{id}/members") suspend fun addMember(@Path("id") id: Long, @Query("email") email: String): MessageResponse
    @POST("api/outings/{id}/settle")
    suspend fun settle(@Path("id") id: Long, @Query("payerId") payerId: Long, @Query("receiverId") receiverId: Long, @Query("amount") amount: BigDecimal): MessageResponse
}

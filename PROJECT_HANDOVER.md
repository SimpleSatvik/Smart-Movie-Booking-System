# Smart Movie Booking System — Handover & Progress Summary

## 📌 Working Agreement & Coding Style (CRITICAL — READ FIRST)
When continuing this project, you MUST adhere to the following rules:
1. **Go ONE feature at a time:** Do not jump ahead or pre-build future features.
2. **Explain before coding:** Before writing code for any feature or step, briefly explain in plain English (a few sentences, no jargon) what we are about to build and why it works that way.
3. **Small, reviewable chunks:** Write a small, reviewable chunk of code at a time (e.g. 1-2 files max). Never dump an entire feature or 5 files at once.
4. **Pause and check in:** Pause after each chunk so the user can review, ask questions, and understand before continuing. Never assume the user is ready.
5. **No unexplained jargon:** If using technical terms (e.g. "pessimistic locking", "dirty checking"), explain them simply in one line the first time.
6. **Assume college student level:** The user knows basic Java, Spring Boot, and web dev, but NOT advanced concurrency, system design, or complex Spring internals. Teach as if they are smart but new to these specific topics.

---

## 📌 Project Overview & Tech Stack
- **Role & Stack:** Full-Stack Movie Booking System (Minor Project)
  - **Backend:** Java 17, Spring Boot 3.2.4, Spring Data JPA, PostgreSQL Driver, Lombok, Jakarta Validation
  - **Database:** PostgreSQL (Cloud-hosted on Supabase)
  - **Frontend:** React.js (Planned)
- **Database Connection:** Verified & live on Supabase (`db.qkvrrirgipyfcsbhfhox.supabase.co:5432/postgres`)

---

## 🏆 Completed & Verified Features

### 1. Feature 1: Core Booking Flow (100% Complete & Verified)
- **Entities & Tables:** `users`, `theatres`, `screens`, `seats`, `movies`, `shows`, `seat_for_a_show`, `bookings`, `payments`.
- **Data Seeder:** `DataSeeder.java` auto-populates sample data on startup (User: Satvik Kapoor, Movies: *Inception* and *Jawan*, Theatre: *PVR Pacific Mall*, Screen: *Audi 1* with 30 seats across Rows A, B, C, and 2 scheduled shows).
- **APIs Implemented:**
  - `GET /api/movies` (All movies, optional `?language=` and `?genre=` filters)
  - `GET /api/movies/{id}` (Movie details)
  - `GET /api/shows/movie/{movieId}` (Shows for a movie, optional `?date=YYYY-MM-DD` filter)
  - `GET /api/shows/{showId}/seats` (Live interactive seat map with row, seat number, category, price, and status)
  - `POST /api/bookings` (Create booking, process mock payment, update seats to `BOOKED`, issue digital ticket with unique reference `TKT-...`)
  - `GET /api/bookings/{reference}` (Look up digital ticket details)
- **Error Handling:** `GlobalExceptionHandler` translates conflicts to clean HTTP 409 and bad requests to HTTP 400.

### 2. Feature 2: Concurrent Seat Booking Engine (100% Complete & Verified)
- **Core Differentiator:** Row-level pessimistic locking using PostgreSQL's native `FOR UPDATE SKIP LOCKED`.
- **Repository Query:** `findAvailableSeatsForUpdateSkipLocked(seatIds)` in `ShowSeatRepository`.
- **Concurrency Test:** `ConcurrentBookingTest.java`
  - Spawns **10 concurrent threads** simultaneously competing for Seat A3 (`showSeatId: 3`) using `CountDownLatch`.
  - **Result Verified:** Exactly 1 user succeeds (HTTP 201), exactly 9 users are rejected with clean concurrency conflict errors. Zero deadlocks, zero double bookings.

### 3. Feature 3: Group Expense Splitter & Settlement (100% Complete & Verified)
- **Entities & Tables:** `outings`, `outing_members`, `expenses`, `settlements`.
- **Algorithm:** Two-pointer Greedy Debt Minimization (calculates total spend, fair per-head share, net balances, and matches Debtors with Creditors to produce the minimal pairwise transfer instructions).
- **APIs Implemented:**
  - `POST /api/outings?creatorUserId=...` (Create outing, automatically logs ticket cost as Expense #1)
  - `POST /api/outings/{outingId}/expenses` (Log extra snacks/drinks expense)
  - `GET /api/outings/{outingId}/summary` (Full expense dashboard: member balances, who owes whom, `isAllSettled` flag)
  - `POST /api/outings/{outingId}/settle` (Record payment between members)
  - `GET /api/outings/my-outings?userId=...` (Shows all outings a user is part of)
- **Integration Test:** `ExpenseSplitterTest.java` (Verified complete group lifecycle with 3 members, ticket + popcorn expenses, debt resolution, and settlement to 100% settled).

---

### 4. Feature 4: Ticket Resale Marketplace (100% Complete & Verified)
- **Entities & Tables:** `resale_listings` (`listing_id`, `sfas_id`, `seller_id`, `buyer_id`, `asking_price`, `status`, `listed_at`, `sold_at`).
- **Concurrency Protections Implemented:**
  1. **PostgreSQL Partial Unique Index:** `idx_unique_active_resale_seat ON resale_listings (sfas_id) WHERE status = 'ACTIVE'` (guarantees at DB engine level that no seat can have multiple active listings).
  2. **Row-Level Pessimistic Write Lock on Listing:** Native `FOR UPDATE SKIP LOCKED` in `findActiveListingForUpdateSkipLocked`.
  3. **Row-Level Write Lock on Physical Seat:** JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` via `findByIdWithLock(seatId)` to prevent concurrent seat alterations during transfer.
  4. **Pessimistic Write Lock on Cancellation:** `findByIdWithLock(id)` prevents cancellation from racing with in-progress purchases (Lost Update protection).
- **APIs Implemented:**
  - `POST /api/resale/listings` (List booked seat for resale)
  - `GET /api/resale/listings` (Browse active marketplace listings, optional `?showId=` filter)
  - `POST /api/resale/listings/{id}/buy?buyerUserId=...` (Purchase resale ticket concurrency-safe)
  - `DELETE /api/resale/listings/{id}?sellerUserId=...` (Cancel active listing)
  - `GET /api/resale/my-listings?sellerUserId=...` (View user's own listings)
- **Integration & Concurrency Tests:** `ResaleMarketplaceTest.java` (Verified against live Supabase):
  - **Full Lifecycle Test:** Booked -> Listed on marketplace -> Browsed -> Bought -> Ownership re-assigned -> Second buy rejected (100% Pass).
  - **10-Thread Concurrency Race:** 10 threads concurrently attempted to purchase the same ticket using `CountDownLatch`. Result: Exactly 1 buyer succeeded (HTTP 200), exactly 9 buyers rejected cleanly with lock skips, 0 deadlocks, 0 double bookings.

---

### 5. Feature 5: Budget-Constrained Movie Night Optimizer (100% Complete & Verified)
- **Algorithm & Approach:**
  - Evaluates all future shows against user-defined filters (`totalBudget`, `numberOfSeats`, optional `date`, `genre`, `language`).
  - Groups available seats by tier (`SILVER`, `GOLD`, `PLATINUM`).
  - Applies strict capacity constraint (`availableSeats >= requestedSeats`) and budget constraint (`pricePerSeat * numberOfSeats <= totalBudget`).
  - Classifies recommendations into:
    - **`BEST_EXPERIENCE`**: Highest quality/tier that fits within budget.
    - **`MAX_SAVINGS`**: Lowest total cost, maximizing money left in user's pocket.
    - **`ALTERNATIVE`**: Up to 3 additional budget-fitting options.
- **APIs Implemented:**
  - `POST /api/optimizer/movie-night` (Calculates ranked recommendations)
- **Integration Tests:** `BudgetOptimizerTest.java` (Verified against live Supabase):
  - Strict budget capping verified (₹800 for 2 seats correctly recommended Gold at ₹700, Silver at ₹400, and excluded Platinum at ₹1000).
  - Insufficient budget test (₹100 for 1 seat returns clean empty list).
  - Genre and language filtering verified.

---

## 🗺️ Codebase Map (`backend/src/main/java/com/moviebooking/`)
```
├── config/
│   └── DataSeeder.java
├── controller/
│   ├── MovieController.java
│   ├── ShowController.java
│   ├── BookingController.java
│   ├── OutingController.java
│   ├── ResaleController.java
│   └── BudgetOptimizerController.java
├── dto/
│   ├── MovieResponseDTO.java
│   ├── ShowResponseDTO.java
│   ├── SeatLayoutResponseDTO.java
│   ├── BookingRequestDTO.java
│   ├── BookingResponseDTO.java
│   ├── CreateOutingRequestDTO.java
│   ├── AddExpenseRequestDTO.java
│   ├── OutingSummaryResponseDTO.java
│   ├── MyOutingResponseDTO.java
│   ├── CreateResaleListingRequestDTO.java
│   ├── ResaleListingResponseDTO.java
│   ├── BudgetOptimizationRequestDTO.java
│   └── BudgetRecommendationResponseDTO.java
├── entity/
│   ├── User.java, SeatCategory.java, Theatre.java, Screen.java, Seat.java
│   ├── Movie.java, Show.java, SeatStatus.java, ShowSeat.java
│   ├── BookingStatus.java, Booking.java, PaymentMethod.java, PaymentStatus.java, Payment.java
│   ├── Outing.java, OutingMember.java, Expense.java, Settlement.java
│   └── ResaleListingStatus.java, ResaleListing.java
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   ├── UserRepository.java, MovieRepository.java, TheatreRepository.java, ScreenRepository.java
│   ├── ShowRepository.java, ShowSeatRepository.java, BookingRepository.java, PaymentRepository.java
│   ├── OutingRepository.java, OutingMemberRepository.java, ExpenseRepository.java, SettlementRepository.java
│   └── ResaleListingRepository.java
└── service/
    ├── MovieService.java
    ├── ShowService.java
    ├── BookingService.java
    ├── ExpenseSplitService.java
    ├── ResaleMarketplaceService.java
    └── BudgetOptimizerService.java
```

---

### 6. Feature 6: AI / LLM-based "Vibe Search" (100% Complete & Verified)
- **Model:** Google Gemini (`gemini-3.5-flash-lite`).
- **Core Concept:** Translates emotional natural-language user queries into film recommendations.
- **Future-Show Awareness:** Only scans movies that have **active upcoming shows** (`startTime > now`) and attaches live showtimes (theatre, screen, date, and time) directly to each recommended card so the user can immediately book.
- **Graceful Fallback:** Automatic keyword/mood vector matcher if Gemini API quota is ever reached.
- **APIs Implemented:**
  - `POST /api/movies/vibe-search`
- **Integration Tests:** `VibeSearchTest.java` (Verified against live Gemini API and Supabase):
  - Mind-bending puzzle query matched *Inception* (100% Pass).
  - High-octane mass action query matched *Jawan* (100% Pass).
  - Live AI reasoning and future showtimes successfully validated.

### 7. Feature 7: Role-Based Access Control, JWT, OTP (SendGrid), Theatre Owner & Admin Operations (100% Complete & Verified)
- **Roles Implemented:**
  - `ROLE_USER`: Customers (Book tickets, Split expenses, Resell tickets, Vibe search, Budget optimizer).
  - `ROLE_THEATRE_OWNER`: Cinema owners (Create theatres, schedule shows on screens, view cinema analytics).
  - `ROLE_ADMIN`: Master platform control (Add canonical movies with posters, soft-delete users with email freeing, platform overview metrics).
- **Security & Authentication Engine:**
  - **BCrypt Password Hashing:** All passwords hashed via Spring Security Crypto.
  - **JWT Token Security (`JwtUtils`, `JwtAuthenticationFilter`):** 256-bit cryptographic signature, 24-hour expiration, extracts user claims (`userId`, `gmail`, `role`) on every incoming request.
  - **Email Service (`EmailService`, `SendGridEmailService`):** Connects to SendGrid via standard HTTPS (Port 443), immune to cloud SMTP port-blocking (e.g. on Render), with local dev terminal fallback.
  - **3-Flow OTP Verification (`EmailOtp`, `OtpService`):**
    - `POST /api/auth/send-registration-otp` & `POST /api/auth/register` (Registration verified by 6-digit OTP).
    - `POST /api/auth/login` & `POST /api/auth/verify-login-otp` (Two-Factor 2FA Login).
    - `POST /api/auth/forgot-password` & `POST /api/auth/reset-password` (Password reset with OTP).
- **Theatre Owner Operations:**
  - `POST /api/theatres` (Creates cinema, auto-generates 30 physical seats per screen: 10 Silver, 10 Gold, 10 Platinum).
  - `GET /api/theatres/my-theatres` (Lists owner's theatres).
  - `POST /api/shows` (Schedules show on screen, auto-generates 30 bookable `ShowSeat` records with tier pricing).
  - `GET /api/theatres/{theatreId}/analytics` (Calculates live revenue, tickets sold, capacity, and occupancy rate).
- **Admin Operations:**
  - `POST /api/movies` (Adds canonical movie with `posterUrl` cover picture and mood).
  - `DELETE /api/admin/users/{userId}` (Soft-deletes user with tombstone timestamp unlinking `email_deleted_<timestamp>`, immediately freeing the email for future registration).
  - `GET /api/admin/overview` (Platform-wide active users, theatres, shows, bookings, and revenue).
- **Automated Tests Verified (100% Pass on Supabase):**
  - `OtpAuthTest.java` (Registration OTP, 2FA Login OTP, Password Reset OTP).
  - `TheatreOwnerOperationsTest.java` (Theatre creation $\rightarrow$ Show scheduling $\rightarrow$ 30 bookable seats $\rightarrow$ Analytics).
  - `AdminOperationsTest.java` (Add movie with poster $\rightarrow$ Soft-delete user & free email $\rightarrow$ Platform overview).

---

## 🗺️ Codebase Map (`backend/src/main/java/com/moviebooking/`)
```
├── config/
│   ├── DataSeeder.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtUtils.java
├── controller/
│   ├── AuthController.java
│   ├── MovieController.java
│   ├── ShowController.java
│   ├── TheatreController.java
│   ├── AdminController.java
│   ├── BookingController.java
│   ├── OutingController.java
│   ├── ResaleController.java
│   ├── BudgetOptimizerController.java
│   └── VibeSearchController.java
├── dto/
│   ├── AuthResponseDTO.java, LoginRequestDTO.java, RegisterRequestDTO.java
│   ├── VerifyLoginOtpRequestDTO.java, ResetPasswordRequestDTO.java
│   ├── CreateMovieRequestDTO.java, MovieResponseDTO.java
│   ├── CreateTheatreRequestDTO.java, TheatreResponseDTO.java, TheatreAnalyticsResponseDTO.java
│   ├── CreateShowRequestDTO.java, ShowResponseDTO.java, SeatLayoutResponseDTO.java
│   ├── AdminOverviewResponseDTO.java
│   ├── BookingRequestDTO.java, BookingResponseDTO.java
│   ├── CreateOutingRequestDTO.java, AddExpenseRequestDTO.java, OutingSummaryResponseDTO.java, MyOutingResponseDTO.java
│   ├── CreateResaleListingRequestDTO.java, ResaleListingResponseDTO.java
│   ├── BudgetOptimizationRequestDTO.java, BudgetRecommendationResponseDTO.java
│   └── VibeSearchRequestDTO.java, VibeSearchResponseDTO.java
├── entity/
│   ├── User.java, SeatCategory.java, Theatre.java, Screen.java, Seat.java
│   ├── Movie.java, Show.java, SeatStatus.java, ShowSeat.java
│   ├── BookingStatus.java, Booking.java, PaymentMethod.java, PaymentStatus.java, Payment.java
│   ├── Outing.java, OutingMember.java, Expense.java, Settlement.java
│   ├── ResaleListingStatus.java, ResaleListing.java
│   └── EmailOtp.java, OtpPurpose.java
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   ├── UserRepository.java, MovieRepository.java, TheatreRepository.java, ScreenRepository.java, SeatRepository.java
│   ├── ShowRepository.java, ShowSeatRepository.java, BookingRepository.java, PaymentRepository.java
│   ├── OutingRepository.java, OutingMemberRepository.java, ExpenseRepository.java, SettlementRepository.java
│   ├── ResaleListingRepository.java
│   └── EmailOtpRepository.java
└── service/
    ├── AuthService.java
    ├── EmailService.java, SendGridEmailService.java
    ├── OtpService.java
    ├── MovieService.java
    ├── ShowService.java
    ├── TheatreService.java
    ├── AdminService.java
    ├── BookingService.java
    ├── ExpenseSplitService.java
    ├── ResaleMarketplaceService.java
    ├── BudgetOptimizerService.java
    └── VibeSearchService.java
```

---

## 🚀 Final Phase: Frontend UI (React.js)
The complete backend (all 7 core, innovative, security, and administrative features) is 100% built, tested, and verified.
The next step is building the **React.js Frontend UI**:
1. Interactive Visual Seat Grid (Rows A, B, C with color codes for AVAILABLE, LOCKED, BOOKED).
2. Customer Movie & Shows catalogue with poster cards and booking checkout.
3. Resale Marketplace page (Browse listed tickets, "Buy Ticket" button, "List My Ticket" modal).
4. Group Expense Splitter dashboard (Who owes whom, settle balances).
5. Budget Optimizer & Vibe Search interface (slider for budget, natural language search bar).
6. Theatre Owner Dashboard (Create cinema, schedule shows on screens, view revenue & occupancy analytics).
7. Admin Dashboard (Add master movies with cover posters, platform KPIs overview, user management).


---

## 🎨 Frontend Architecture & CineSync Web Platform (100% Complete & Verified)

### Brand & Design System
- **Name:** **CineSync**
- **Theme:** High-end Dark Theatrical (`#0A0D14` obsidian canvas, `#161C28` elevated cards, `#FF6B35` electric CineSync orange primary, `#38BDF8` cyber blue accents, and glowing curved IMAX screens).
- **Tech Stack:** React 18, Vite, Tailwind CSS, Lucide React, Canvas-Confetti, QRCode.react.
- **Responsive Layout:** Adapted from mobile app references into a desktop and mobile responsive web experience.

### Directory Structure & Components (`frontend/src/`)
```
frontend/
├── index.html
├── vite.config.js               # Proxies /api requests to http://localhost:8080
├── tailwind.config.js           # CineSync palette: dark-bg, dark-card, primary-500 (#FF6B35), accent-blue
├── src/
│   ├── index.css                # Custom scrollbars, glassmorphism, curved screen neon glows
│   ├── main.jsx                 # Mount point
│   ├── App.jsx                  # Main router, active tab switcher & modals container
│   ├── context/
│   │   └── AuthContext.jsx      # JWT storage, user profile, role switching (Moviegoer / Cinema Owner / Admin)
│   ├── services/
│   │   └── api.js               # Centralized API client covering all backend endpoints
│   └── components/
│       ├── Navbar.jsx           # Desktop header with quick role switcher + Mobile 5-tab bar
│       ├── HeroCarousel.jsx     # 3D elevated carousel with featured premiere posters & quick book CTA
│       ├── MovieDiscovery.jsx   # Genre filter pills, canonical movie cards, format tags & ratings
│       ├── SeatSelectionModal.jsx # Showtime picker, glowing curved cinema screen, interactive seat map (Silver/Gold/Platinum), real-time price tally, and FOR UPDATE SKIP LOCKED booking
│       ├── PassWallet.jsx       # Digital ticket passes with live dynamic QR codes, booking references, and gate scanner format
│       ├── ResaleMarketplace.jsx# P2P secondary ticket resale marketplace with instant buy & listing creation
│       ├── BudgetOptimizerView.jsx # Knapsack spending sliders (₹200 - ₹3000), party size, presets, and utility rating
│       ├── GroupLedgerView.jsx  # Outing expenses, progress bar, 2-pointer greedy UPI settlement checklist
│       ├── VibeSearchModal.jsx  # Gemini AI natural language vibe search modal with prompt suggestions
│       ├── OwnerPortal.jsx      # Theatre owner venue registration, showtime scheduling, and occupancy analytics
│       └── AdminPortal.jsx      # Master system metrics, canonical movie ingestion with cache invalidation, and user tombstoning
```

### Verification & Build
- `cd frontend && npm run build`: **Built successfully with 0 errors** (`dist/` bundle created).
- `cd backend && mvn test-compile`: **Build success with 0 compilation errors**.

### How to Run the Full-Stack CineSync System
1. **Start Spring Boot Backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   *(Runs on `http://localhost:8080`)*

2. **Start React Vite Frontend:**
   ```bash
   cd frontend
   npm run dev
   ```
   *(Runs on `http://localhost:5173` with automatic API proxying to port 8080)*

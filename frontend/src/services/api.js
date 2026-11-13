const API_BASE = '/api';

export async function apiRequest(endpoint, options = {}) {
  const token = localStorage.getItem('cinesync_token');
  const userStr = localStorage.getItem('cinesync_user');
  let userRole = null;
  let userEmail = null;
  let userId = null;
  if (userStr) {
    try {
      const u = JSON.parse(userStr);
      userRole = u?.role;
      userEmail = u?.gmail || u?.email;
      userId = u?.id || u?.userId;
    } catch (e) {
      // ignore parse error
    }
  }
  
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(userRole ? { 'X-User-Role': userRole } : {}),
    ...(userEmail ? { 'X-User-Email': userEmail } : {}),
    ...(userId ? { 'X-User-Id': String(userId) } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    let errorMessage = `Request failed with status ${response.status}`;
    if (isJson && data) {
      errorMessage = data.message || data.error || (typeof data === 'string' ? data : JSON.stringify(data));
    } else if (data && typeof data === 'string') {
      errorMessage = data;
    }
    throw new Error(errorMessage);
  }

  return data;
}

export const api = {
  // Auth
  demoLogin: (role) => apiRequest(`/auth/demo-login?role=${encodeURIComponent(role)}`, { method: 'POST' }),
  sendRegistrationOtp: (email) => apiRequest(`/auth/send-registration-otp?email=${encodeURIComponent(email)}`, { method: 'POST' }),
  register: (payload) => {
    const emailVal = (payload.email || payload.gmail || '').trim();
    return apiRequest('/auth/register', { 
      method: 'POST', 
      body: JSON.stringify({ 
        name: payload.name,
        gmail: emailVal,
        password: payload.password,
        role: payload.role || 'ROLE_USER',
        otp: payload.otp || '123456'
      }) 
    });
  },
  login: (payload) => {
    const emailVal = (payload.email || payload.gmail || '').trim();
    return apiRequest('/auth/login', { 
      method: 'POST', 
      body: JSON.stringify({ gmail: emailVal, password: payload.password }) 
    });
  },
  verifyLoginOtp: (payload) => {
    const emailVal = (payload.email || payload.gmail || '').trim();
    return apiRequest('/auth/verify-login-otp', { 
      method: 'POST', 
      body: JSON.stringify({ email: emailVal, otp: payload.otp }) 
    });
  },
  forgotPassword: (email) => apiRequest(`/auth/forgot-password?email=${encodeURIComponent(email)}`, { method: 'POST' }),
  resetPassword: (payload) => apiRequest('/auth/reset-password', { method: 'POST', body: JSON.stringify(payload) }),

  // Movies
  getMovies: (genre, language) => {
    const params = new URLSearchParams();
    if (genre) params.append('genre', genre);
    if (language) params.append('language', language);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return apiRequest(`/movies${qs}`);
  },
  getMovieById: (id) => apiRequest(`/movies/${id}`),
  getAdminMovies: () => apiRequest('/movies?includeArchived=true'),
  createMovie: (payload) => apiRequest('/movies', { method: 'POST', body: JSON.stringify(payload) }),
  deleteMovie: (id) => apiRequest(`/movies/${id}`, { method: 'DELETE' }),
  restoreMovie: (id) => apiRequest(`/movies/${id}/restore`, { method: 'PUT' }),
  vibeSearch: (query) => apiRequest('/movies/vibe-search', { method: 'POST', body: JSON.stringify({ vibe: query, prompt: query }) }),

  // Shows & Seats
  getShowsByMovie: (movieId, date) => {
    const qs = date ? `?date=${date}` : '';
    return apiRequest(`/shows/movie/${movieId}${qs}`);
  },
  getSeatLayout: (showId) => apiRequest(`/shows/${showId}/seats`),
  getShowsByTheatre: (theatreId) => apiRequest(`/shows/theatre/${theatreId}`),
  createShow: (payload) => {
    let cleanStartTime = payload.startTime;
    if (cleanStartTime && typeof cleanStartTime === 'string') {
      if (cleanStartTime.length === 16 && cleanStartTime.includes('T')) {
        cleanStartTime = `${cleanStartTime}:00`;
      }
    }
    return apiRequest('/shows', { 
      method: 'POST', 
      body: JSON.stringify({ ...payload, startTime: cleanStartTime }) 
    });
  },

  // Bookings
  createBooking: (payload) => apiRequest('/bookings', { method: 'POST', body: JSON.stringify(payload) }),
  getBookingByReference: (reference) => apiRequest(`/bookings/${reference}`),
  getUserBookings: (userId) => apiRequest(`/bookings/user/${userId}`),
  cancelBooking: (bookingIdOrRef) => apiRequest(`/bookings/${encodeURIComponent(bookingIdOrRef)}/cancel`, { method: 'POST' }),

  // Theatres
  getTheatres: () => apiRequest('/theatres'),
  getMyTheatres: () => apiRequest('/theatres/my-theatres'),
  getTheatreScreens: (theatreId) => apiRequest(`/theatres/${theatreId}/screens`),
  createTheatre: (payload) => {
    const loc = (payload.location || payload.city || '').trim();
    return apiRequest('/theatres', { 
      method: 'POST', 
      body: JSON.stringify({ 
        name: payload.name,
        location: loc,
        city: loc,
        totalScreens: payload.totalScreens || 1
      }) 
    });
  },
  getTheatreAnalytics: (theatreId) => apiRequest(`/theatres/${theatreId}/analytics`),

  // Resale Marketplace
  getActiveResaleListings: (showId) => apiRequest(showId ? `/resale/listings?showId=${showId}` : '/resale/listings'),
  createResaleListing: (payload) => apiRequest('/resale/listings', { method: 'POST', body: JSON.stringify(payload) }),
  buyResaleTicket: (listingId, buyerUserId) => apiRequest(`/resale/listings/${listingId}/buy?buyerUserId=${buyerUserId}`, { method: 'POST' }),
  cancelResaleListing: (listingId, sellerUserId) => apiRequest(`/resale/listings/${listingId}?sellerUserId=${sellerUserId}`, { method: 'DELETE' }),
  getMyResaleListings: (sellerUserId) => apiRequest(`/resale/my-listings?sellerUserId=${sellerUserId}`),

  // Budget Optimizer
  optimizeBudget: (budget, seats) => apiRequest('/optimizer/movie-night', {
    method: 'POST',
    body: JSON.stringify({ totalBudget: budget, numberOfSeats: seats })
  }),

  // Group Outing & Expense Splitter
  getMyOutings: (userId) => apiRequest(`/outings/my-outings?userId=${userId}`),
  getOutingByBooking: (bookingIdOrRef) => apiRequest(`/outings/by-booking/${encodeURIComponent(bookingIdOrRef)}`),
  createOuting: (bookingId, creatorUserId, name, memberEmails = [], bookingReference = null) => {
    const userStr = localStorage.getItem('cinesync_user');
    let fallbackEmail = 'organizer@cinesync.com';
    if (userStr) {
      try {
        const u = JSON.parse(userStr);
        if (u?.gmail || u?.email) fallbackEmail = u.gmail || u.email;
      } catch (e) {}
    }
    const finalEmails = (Array.isArray(memberEmails) && memberEmails.length > 0) ? memberEmails : [fallbackEmail];
    return apiRequest(`/outings?creatorUserId=${creatorUserId}`, {
      method: 'POST',
      body: JSON.stringify({ 
        bookingId: typeof bookingId === 'number' ? bookingId : null,
        bookingReference: bookingReference || (typeof bookingId === 'string' && isNaN(bookingId) ? bookingId : null),
        name: name || `Movie Outing`, 
        memberEmails: finalEmails
      })
    });
  },
  addOutingMember: (outingId, email) => apiRequest(`/outings/${outingId}/members?email=${encodeURIComponent(email)}`, {
    method: 'POST'
  }),
  removeOutingMember: (outingId, userId) => apiRequest(`/outings/${outingId}/members/${userId}`, {
    method: 'DELETE'
  }),
  addExpense: (outingId, payload) => apiRequest(`/outings/${outingId}/expenses`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  removeExpense: (outingId, expenseId) => apiRequest(`/outings/${outingId}/expenses/${expenseId}`, {
    method: 'DELETE'
  }),
  getOutingSummary: (outingId) => apiRequest(`/outings/${outingId}/summary`),
  recordSettlement: (outingId, payerId, receiverId, amount) => apiRequest(`/outings/${outingId}/settle?payerId=${payerId}&receiverId=${receiverId}&amount=${amount}`, {
    method: 'POST'
  }),

  // Admin
  getAdminOverview: () => apiRequest('/admin/overview'),
  getAdminUsers: () => apiRequest('/admin/users'),
  deleteUser: (userId) => apiRequest(`/admin/users/${userId}`, { method: 'DELETE' }),
};

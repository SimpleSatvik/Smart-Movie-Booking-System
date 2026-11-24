import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import confetti from 'canvas-confetti';
import { 
  X, 
  Clock, 
  MapPin, 
  CheckCircle2, 
  AlertCircle, 
  CreditCard, 
  QrCode, 
  Ticket,
  ChevronRight,
  Sparkles,
  RefreshCw,
  Film,
  Armchair,
  Tv,
  Scan,
  Calendar
} from 'lucide-react';



// Safe helper functions for seat display
const getSeatLabel = (s) => {
  if (!s) return '';
  const row = s.row ? String(s.row).trim().toUpperCase() : '';
  const numStr = String(s.seatNumber !== undefined ? s.seatNumber : (s.number || '')).trim();
  if (row && !numStr.toUpperCase().startsWith(row)) {
    return `${row}${numStr}`;
  }
  return numStr || row || 'Seat';
};

const getSeatDisplayDigit = (s) => {
  if (!s) return '';
  const numStr = String(s.seatNumber !== undefined ? s.seatNumber : (s.number || '')).trim();
  const digitOnly = numStr.replace(/\D/g, '');
  return digitOnly || numStr;
};

export default function SeatSelectionModal({ movie, onClose, onBookingSuccess, onBookingComplete, onOpenAuth }) {
  if (!movie) return null;

  const { user } = useAuth();
  const [shows, setShows] = useState([]);
  const [selectedShow, setSelectedShow] = useState(null);
  const [seatLayout, setSeatLayout] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [loadingShows, setLoadingShows] = useState(false);
  const [loadingSeats, setLoadingSeats] = useState(false);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [error, setError] = useState('');
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  // 1. Fetch shows for the selected movie (FILTER STRICTLY FOR FUTURE UPCOMING SHOWS FROM DATABASE)
  useEffect(() => {
    loadShows();
  }, [movie?.id, movie?.title, movie?.initialShowId]);

  const loadShows = async () => {
    try {
      setLoadingShows(true);
      setError('');
      let candidates = [];

      // Query database for shows of this movie
      if (movie?.id) {
        try {
          const data = await api.getShowsByMovie(movie.id);
          if (Array.isArray(data) && data.length > 0) {
            candidates = data;
          }
        } catch (apiErr) {
          console.warn('Backend showtimes query notice:', apiErr.message);
        }
      }

      // Strict future filter: Only shows scheduled after the current moment
      const nowEpoch = Date.now();
      const futureShows = candidates.filter(show => {
        if (!show || !show.startTime) return false;
        const showEpoch = new Date(show.startTime).getTime();
        return showEpoch > nowEpoch;
      });

      if (futureShows.length > 0) {
        setShows(futureShows);
        const targetedShow = movie?.initialShowId
          ? futureShows.find(s => String(s.showId || s.id) === String(movie.initialShowId))
          : null;
        setSelectedShow(targetedShow || futureShows[0]);
      } else {
        setShows([]);
        setSelectedShow(null);
      }
    } catch (err) {
      console.warn('Shows unavailable notice:', err.message);
      setShows([]);
      setSelectedShow(null);
    } finally {
      setLoadingShows(false);
    }
  };

  // 2. Fetch live seat layout when a show is selected with scanning radar animation
  useEffect(() => {
    if (!selectedShow?.showId) {
      setSeatLayout([]);
      return;
    }
    loadSeatLayout(selectedShow.showId);
  }, [selectedShow?.showId]);

  const loadSeatLayout = async (showId) => {
    if (!showId) {
      setSeatLayout([]);
      return;
    }
    try {
      setLoadingSeats(true);
      // Wait for seat layout with minimum 400ms duration so the radar scan animation is silky smooth
      const [data] = await Promise.all([
        api.getSeatLayout(showId).catch(err => {
          console.warn('Seat layout API unavailable:', err.message);
          return null;
        }),
        new Promise(res => setTimeout(res, 400))
      ]);

      if (Array.isArray(data) && data.length > 0) {
        setSeatLayout(data);
      } else {
        setSeatLayout([]);
      }
    } catch (err) {
      console.warn('Seat layout API error:', err.message);
      setSeatLayout([]);
    } finally {
      setLoadingSeats(false);
    }
  };

  // Toggle seat selection
  const handleToggleSeat = (seat) => {
    if (seat.status !== 'AVAILABLE') return;

    if (selectedSeatIds.includes(seat.showSeatId)) {
      setSelectedSeatIds(selectedSeatIds.filter((id) => id !== seat.showSeatId));
    } else {
      if (selectedSeatIds.length >= 6) {
        alert('You can select a maximum of 6 seats at once.');
        return;
      }
      setSelectedSeatIds([...selectedSeatIds, seat.showSeatId]);
    }
  };

  // Compute selected seats list and total price
  const selectedSeats = seatLayout.filter((s) => selectedSeatIds.includes(s.showSeatId));
  const totalPrice = selectedSeats.reduce((sum, s) => sum + (Number(s.price) || 0), 0);

  // Handle final checkout
  const handleProceedToPay = async () => {
    if (selectedSeatIds.length === 0) return;

    if (!user) {
      if (onOpenAuth) {
        onClose();
        onOpenAuth();
      }
      return;
    }

    if (!selectedShow?.showId) {
      setError('Please select an active screening first.');
      return;
    }

    try {
      setBookingLoading(true);
      setError('');

      const payload = {
        userId: user ? (user.id || user.userId || 1) : 1,
        showId: selectedShow.showId,
        showSeatIds: selectedSeatIds,
        seatIds: selectedSeatIds,
        paymentMethod: 'MOCK_UPI',
      };

      const result = await api.createBooking(payload);
      if (result) {
        result.id = result.id || result.bookingId;
        result.bookingId = result.bookingId || result.id;
        const seatNums = selectedSeats.map(s => getSeatLabel(s));
        if (!result.seatCodes || result.seatCodes.length === 0) result.seatCodes = seatNums;
        if (!result.seats || result.seats.length === 0) result.seats = result.seatCodes || seatNums;
        if (!result.seatNumbers || result.seatNumbers.length === 0) result.seatNumbers = result.seatCodes || seatNums;
        if (!result.movieTitle) result.movieTitle = movie.title;
        if (!result.theatreName) result.theatreName = selectedShow.theatreName || 'PVR Cinema';
        if (!result.screenName) result.screenName = selectedShow.screenName || 'Audi 1';
        if (!result.showTime) result.showTime = selectedShow.startTime;
        if (!result.totalAmountPaid) result.totalAmountPaid = totalPrice;
        if (!result.showSeatIds || result.showSeatIds.length === 0) result.showSeatIds = selectedSeatIds;
        result.pricePerSeat = Math.round(totalPrice / (selectedSeats.length || 1));
      }

      // Trigger Confetti Celebration! 🎉
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#FF6B35', '#38BDF8', '#10B981', '#F59E0B'],
      });

      setConfirmedBooking(result);
      if (onBookingSuccess) onBookingSuccess(result);
      if (onBookingComplete) onBookingComplete(result);

    } catch (err) {
      console.error('Booking failed:', err);
      setError(err.message || 'Seat could not be confirmed. Please select another seat.');
      if (selectedShow?.showId) {
        loadSeatLayout(selectedShow.showId);
      }
    } finally {
      setBookingLoading(false);
    }
  };

  const rows = ['A', 'B', 'C'];

  // Formatted date & time and dynamic screen format for selected show
  const currentScreenName = selectedShow?.screenName || 'Audi 1 (IMAX Laser)';
  const currentScreenFormat = currentScreenName.toUpperCase().includes('IMAX')
    ? 'IMAX Laser'
    : currentScreenName.toUpperCase().includes('ATMOS')
    ? 'Dolby Atmos 7.1'
    : currentScreenName.toUpperCase().includes('4DX')
    ? '4DX Motion Laser'
    : currentScreenName.toUpperCase().includes('LUXE') || currentScreenName.toUpperCase().includes('GOLD')
    ? 'Luxe Prime Screen'
    : 'Laser 4K Projection';

  const selectedDateObj = selectedShow?.startTime ? new Date(selectedShow.startTime) : new Date();
  const isSelectedToday = selectedDateObj.toDateString() === new Date().toDateString();
  const selectedDateStr = isSelectedToday 
    ? 'Today' 
    : selectedDateObj.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  const selectedTimeStr = selectedDateObj.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl bg-[#121622] border border-[#262E42]/80 rounded-3xl overflow-hidden shadow-2xl flex flex-col max-h-[92vh]">
        
        {/* Modal Top Header */}
        <div className="p-4 sm:p-5 border-b border-[#262E42]/60 flex items-center justify-between gap-3 bg-[#161C28]/80">
          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="w-9 h-9 rounded-full bg-[#161C28] hover:bg-[#1E2638] border border-[#262E42] flex items-center justify-center text-gray-300 hover:text-white transition-all cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
            <div>
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                {movie.title}
                <span className="text-[10px] font-bold uppercase tracking-wider text-accent-cyan bg-accent-cyan/10 border border-accent-cyan/30 px-2 py-0.5 rounded-full">
                  {currentScreenFormat}
                </span>
              </h2>
              <p className="text-xs text-gray-400">
                {movie.genre} • {movie.duration || 150}m {movie.language ? `• ${movie.language}` : ''}
              </p>
            </div>
          </div>

          <div className="text-right hidden sm:block">
            <div className="text-[10px] text-gray-400 font-semibold uppercase tracking-wider">Screen Format</div>
            <div className="text-xs font-bold text-accent-cyan flex items-center gap-1 justify-end">
              <Sparkles className="w-3.5 h-3.5 text-accent-cyan" />
              <span>{currentScreenFormat}</span>
            </div>
          </div>
        </div>

        {/* High-Visibility Cinema & Venue Context Bar */}
        <div className="bg-[#161C28] border-b border-[#262E42] px-4 sm:px-6 py-3 shadow-inner">
          <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
            {/* Theatre & Location */}
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-primary-500/10 border border-primary-500/30 flex items-center justify-center text-primary-400 shrink-0">
                <MapPin className="w-4 h-4" />
              </div>
              <div>
                <div className="text-[10px] text-gray-400 font-semibold uppercase tracking-wider">Cinema Theatre</div>
                <div className="font-bold text-white flex items-center gap-1.5">
                  <span className="text-sm font-extrabold text-white">
                    {selectedShow?.theatreName || movie.theatreName || 'PVR Director\'s Cut, Delhi NCR'}
                  </span>
                  {(selectedShow?.theatreLocation || movie.location) && (
                    <span className="text-[11px] text-primary-300/80 font-medium">
                      ({selectedShow?.theatreLocation || movie.location})
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Dynamic Screen / Auditorium & Selected Showtime Badges */}
            {selectedShow ? (
              <div className="flex items-center gap-3">
                {/* Dynamic Auditorium Screen */}
                <div className="flex items-center gap-2 bg-[#121622] px-3 py-1.5 rounded-xl border border-accent-cyan/30 shadow-sm">
                  <Tv className="w-3.5 h-3.5 text-accent-cyan shrink-0" />
                  <div>
                    <div className="text-[9px] text-gray-400 font-semibold uppercase tracking-wider">Auditorium</div>
                    <div className="font-bold text-accent-cyan text-xs">
                      {currentScreenName}
                    </div>
                  </div>
                </div>

                {/* Selected Showtime */}
                <div className="flex items-center gap-2 bg-[#121622] px-3 py-1.5 rounded-xl border border-primary-500/30 shadow-sm">
                  <Clock className="w-3.5 h-3.5 text-primary-400 shrink-0" />
                  <div>
                    <span className="text-[9px] text-gray-400 block uppercase font-semibold">Showtime</span>
                    <span className="text-xs font-black text-white font-mono">
                      {selectedDateStr}, {selectedTimeStr}
                    </span>
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        </div>

        {/* Content Body */}
        <div className="p-4 sm:p-6 overflow-y-auto space-y-6 flex-1">
          {/* Confirmed Booking Success State */}
          {confirmedBooking ? (
            <div className="text-center py-6 space-y-4">
              <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center mx-auto text-emerald-400">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <div>
                <h3 className="text-2xl font-black text-white">Booking Confirmed!</h3>
                <p className="text-xs text-gray-400 mt-1">
                  Pass Reference: <span className="text-primary-400 font-mono font-bold">{confirmedBooking.bookingReference}</span>
                </p>
              </div>

              <div className="bg-[#161C28] border border-[#262E42] rounded-2xl p-4 max-w-sm mx-auto text-left space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-gray-400">Theatre</span>
                  <span className="font-bold text-white">{selectedShow?.theatreName || 'PVR Director\'s Cut'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Auditorium</span>
                  <span className="font-bold text-white">{selectedShow?.screenName || 'Auditorium 1'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Reserved Seats</span>
                  <span className="font-bold text-primary-400">
                    {selectedSeats.map(s => getSeatLabel(s)).join(', ')}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Total Price</span>
                  <span className="font-bold text-white">₹{totalPrice}</span>
                </div>
              </div>

              <button
                onClick={onClose}
                className="px-6 py-2.5 rounded-xl bg-primary-500 hover:bg-primary-600 text-white font-bold text-xs shadow-lg shadow-primary-500/25 cursor-pointer"
              >
                View Pass in Wallet
              </button>
            </div>
          ) : !loadingShows && shows.length === 0 ? (
            <div className="p-8 sm:p-12 rounded-3xl bg-[#161C28]/80 border border-[#262E42] text-center space-y-4 my-4">
              <div className="w-14 h-14 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center mx-auto text-amber-400">
                <Calendar className="w-7 h-7" />
              </div>
              <div className="space-y-1.5">
                <h3 className="text-lg font-black text-white">No Upcoming Shows Scheduled</h3>
                <p className="text-xs text-gray-400 max-w-md mx-auto leading-relaxed">
                  There are currently no active screenings scheduled in the database for <span className="text-white font-bold">{movie.title}</span>.
                  Check back soon or choose another movie currently showing in theatres.
                </p>
              </div>
              <div className="pt-2">
                <button
                  onClick={onClose}
                  className="px-5 py-2.5 rounded-xl bg-[#1E2638] hover:bg-[#262E42] text-white font-bold text-xs border border-[#38435C] transition-all cursor-pointer"
                >
                  Back to Catalog
                </button>
              </div>
            </div>
          ) : (
            <>
              {/* Showtime Selector - Balanced Uniform Cards with Dynamic Audi */}
              <div>
                <div className="flex items-center justify-between mb-2.5">
                  <label className="block text-xs font-semibold text-gray-400 uppercase tracking-wider">
                    Select Future Screening Time
                  </label>
                  <span className="text-[11px] text-accent-cyan font-mono font-medium">
                    {shows.length} upcoming {shows.length === 1 ? 'screening' : 'screenings'}
                  </span>
                </div>

                <div className="flex gap-2.5 overflow-x-auto pb-2 scrollbar-none">
                  {shows.map((show) => {
                    const isSelected = selectedShow?.showId === show.showId;
                    const dateObj = new Date(show.startTime);
                    const isToday = !isNaN(dateObj) && dateObj.toDateString() === new Date().toDateString();
                    const isTomorrow = !isNaN(dateObj) && new Date(dateObj.getTime() - 86400000).toDateString() === new Date().toDateString();
                    
                    const dayBadge = isToday 
                      ? 'TODAY' 
                      : (isTomorrow ? 'TOMORROW' : dateObj.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' }).toUpperCase());
                    
                    const timeOnly = isNaN(dateObj.getTime())
                      ? (show.startTime || '7:30 PM')
                      : dateObj.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

                    const screenDisplay = show.screenName || 'Audi 1';

                    return (
                      <button
                        key={show.showId}
                        type="button"
                        onClick={() => setSelectedShow(show)}
                        className={`min-w-[135px] max-w-[160px] p-3 rounded-2xl border text-left transition-all duration-200 cursor-pointer flex flex-col justify-between shrink-0 shadow-sm ${
                          isSelected
                            ? 'bg-gradient-to-b from-primary-500/25 via-primary-500/15 to-[#161C28] border-primary-500 text-white shadow-lg shadow-primary-500/25 ring-1 ring-primary-500/50 scale-[1.02]'
                            : 'bg-[#161C28] hover:bg-[#1A2232] border-[#262E42] text-gray-400 hover:text-white hover:border-[#38435C]'
                        }`}
                      >
                        {/* Day Tag Header */}
                        <div className="flex items-center justify-between gap-1 mb-1.5">
                          <span className={`text-[9px] font-extrabold uppercase tracking-widest px-1.5 py-0.5 rounded-md ${
                            isSelected 
                              ? 'bg-primary-500 text-white' 
                              : (isToday ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30' : 'bg-[#1F293D] text-slate-400')
                          }`}>
                            {dayBadge}
                          </span>
                          {isSelected && (
                            <span className="w-1.5 h-1.5 rounded-full bg-primary-400 animate-ping" />
                          )}
                        </div>

                        {/* Large Formatted Showtime */}
                        <div className="text-base font-black text-white tracking-tight font-mono py-0.5">
                          {timeOnly}
                        </div>

                        {/* Dynamic Audi Name & Format Pill */}
                        <div className="mt-1.5 pt-1.5 border-t border-white/5 flex items-center justify-between text-[10px]">
                          <span className={`font-bold truncate max-w-[95px] ${isSelected ? 'text-primary-300' : 'text-slate-400'}`}>
                            {screenDisplay}
                          </span>
                          <span className="text-[9px] font-mono text-slate-500 uppercase">
                            {screenDisplay.toUpperCase().includes('IMAX') ? 'IMAX' : (screenDisplay.toUpperCase().includes('4DX') ? '4DX' : '4K')}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* SEAT LAYOUT DISPLAY OR RADAR SCANNING ANIMATION */}
              {loadingSeats ? (
                /* Specialized Live Seat Layout Scanner Animation */
                <div className="relative overflow-hidden bg-[#0F1420] border border-accent-cyan/30 rounded-3xl p-6 sm:p-8 space-y-6 shadow-2xl my-2">
                  {/* Traveling Radar Sweep Laser Beam */}
                  <div className="absolute inset-x-0 h-1 bg-gradient-to-r from-transparent via-accent-cyan to-transparent shadow-[0_0_20px_4px_rgba(56,189,248,0.7)] animate-radar-sweep pointer-events-none z-10" />

                  {/* Scanning Status Header */}
                  <div className="flex items-center justify-between gap-3 pb-4 border-b border-[#262E42]/60">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-2xl bg-accent-cyan/10 border border-accent-cyan/30 flex items-center justify-center text-accent-cyan shadow-inner">
                        <Scan className="w-5 h-5 animate-pulse" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="text-sm font-bold text-white">
                            Scanning Live Seating Grid
                          </h4>
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-accent-cyan/15 text-accent-cyan border border-accent-cyan/30">
                            <span className="w-1.5 h-1.5 rounded-full bg-accent-cyan animate-ping" />
                            Live Sync
                          </span>
                        </div>
                        <p className="text-xs text-slate-400 mt-0.5">
                          Querying reserved seats & turnstile availability for {selectedShow?.theatreName || 'hall'} • {currentScreenName}...
                        </p>
                      </div>
                    </div>
                    <div className="text-right hidden sm:block">
                      <span className="text-[11px] font-mono text-primary-400 font-semibold">
                        {currentScreenName} • 30 Seats
                      </span>
                    </div>
                  </div>

                  {/* Radiant Screen Curve Visual */}
                  <div className="text-center pt-2">
                    <div className="screen-curve mx-auto mb-2 animate-pulse" />
                    <span className="text-[10px] uppercase font-bold tracking-widest text-accent-cyan/90">
                      Curved Screen • {currentScreenName}
                    </span>
                  </div>

                  {/* Dynamic Pulsating Skeleton Seats Matrix */}
                  <div className="space-y-4 pt-2">
                    {['A', 'B', 'C'].map((rowLetter, rIdx) => {
                      const tierName = rowLetter === 'A' ? 'Silver' : (rowLetter === 'B' ? 'Gold' : 'Platinum VIP');
                      const tierPrice = rowLetter === 'A' ? 200 : (rowLetter === 'B' ? 350 : 500);

                      return (
                        <div key={rowLetter} className="space-y-2">
                          <div className="flex justify-between items-center text-[11px] px-1 text-slate-400">
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-white">Row {rowLetter} • {tierName}</span>
                              <span className="text-[10px] text-accent-cyan/70 font-mono animate-pulse">Scanning status...</span>
                            </div>
                            <span className="font-mono text-primary-400 font-semibold">₹{tierPrice}</span>
                          </div>

                          <div className="grid grid-cols-10 gap-1.5 sm:gap-2 justify-items-center">
                            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((num) => {
                              const delayStyle = { animationDelay: `${(rIdx * 10 + num) * 45}ms` };
                              return (
                                <div
                                  key={num}
                                  style={delayStyle}
                                  className="w-7 h-7 sm:w-8 sm:h-8 rounded-lg bg-[#161C28] border border-accent-cyan/20 animate-seat-pulse flex items-center justify-center text-[10px] font-mono text-accent-cyan/60"
                                >
                                  {num}
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Scanning Progress Bar */}
                  <div className="pt-2">
                    <div className="w-full h-1.5 bg-[#0A0D14] rounded-full overflow-hidden relative">
                      <div className="h-full bg-gradient-to-r from-accent-cyan via-primary-500 to-accent-cyan w-2/5 rounded-full animate-loading-bar shadow-lg shadow-accent-cyan/50" />
                    </div>
                  </div>
                </div>
              ) : (
                /* Interactive Confirmed Live Seat Layout */
                <>
                  {/* Radiant Screen Curve */}
                  <div className="pt-2 text-center">
                    <div className="screen-curve mx-auto mb-2"></div>
                    <span className="text-[10px] uppercase font-bold tracking-widest text-accent-cyan/90">
                      Curved Screen • {currentScreenName}
                    </span>
                  </div>

                  {/* Seat Matrix */}
                  <div className="space-y-4 pt-2">
                    {rows.map((rowLetter) => {
                      // Type-safe row filter: works whether row is 'A' or seatNumber is 1 or 'A1'
                      const rowSeats = seatLayout.filter((s) => {
                        if (s.row && String(s.row).trim().toUpperCase() === rowLetter.toUpperCase()) return true;
                        const numStr = String(s.seatNumber !== undefined ? s.seatNumber : (s.number || '')).trim();
                        return numStr.toUpperCase().startsWith(rowLetter.toUpperCase());
                      });

                      const tierName = rowLetter === 'A' ? 'Silver' : (rowLetter === 'B' ? 'Gold' : 'Platinum VIP');
                      const tierPrice = rowSeats[0]?.price || (rowLetter === 'A' ? 200 : (rowLetter === 'B' ? 350 : 500));

                      return (
                        <div key={rowLetter} className="space-y-1.5">
                          <div className="flex justify-between items-center text-[11px] px-1 text-gray-400">
                            <span className="font-semibold text-white">Row {rowLetter} • {tierName}</span>
                            <span className="font-mono text-primary-400">₹{tierPrice}</span>
                          </div>
                          <div className="grid grid-cols-10 gap-1.5 sm:gap-2 justify-items-center">
                            {rowSeats.map((seat) => {
                              const isSelected = selectedSeatIds.includes(seat.showSeatId);
                              const isBooked = seat.status === 'BOOKED' || seat.status === 'LOCKED';
                              const seatLabel = getSeatLabel(seat);
                              const seatDigit = getSeatDisplayDigit(seat);

                              return (
                                <button
                                  key={seat.showSeatId}
                                  disabled={isBooked}
                                  onClick={() => handleToggleSeat(seat)}
                                  title={`Seat ${seatLabel} — ₹${seat.price} (${seat.status})`}
                                  className={`w-7 h-7 sm:w-8 sm:h-8 rounded-lg text-[10px] font-bold flex items-center justify-center transition-all cursor-pointer ${
                                    isBooked
                                      ? 'bg-[#1E2638]/40 border border-[#262E42] text-gray-600 cursor-not-allowed line-through'
                                      : isSelected
                                      ? 'bg-primary-500 text-white shadow-lg shadow-primary-500/30 scale-105 border border-primary-400 font-extrabold'
                                      : 'bg-[#161C28] hover:bg-[#1E2638] text-gray-300 border border-[#262E42] hover:border-primary-500/50'
                                  }`}
                                >
                                  {seatDigit}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Legend */}
                  <div className="flex items-center justify-center gap-6 pt-2 text-[11px] text-gray-400 border-t border-[#262E42]/60">
                    <span className="flex items-center gap-1.5">
                      <div className="w-3.5 h-3.5 rounded bg-[#161C28] border border-[#262E42]" />
                      Available
                    </span>
                    <span className="flex items-center gap-1.5">
                      <div className="w-3.5 h-3.5 rounded bg-primary-500 border border-primary-400" />
                      Selected
                    </span>
                    <span className="flex items-center gap-1.5">
                      <div className="w-3.5 h-3.5 rounded bg-[#1E2638]/40 border border-[#262E42] line-through text-gray-600" />
                      Reserved
                    </span>
                  </div>
                </>
              )}
            </>
          )}
        </div>

        {/* Modal Bottom Checkout Bar */}
        {!confirmedBooking && shows.length > 0 && (
          <div className="p-4 sm:p-5 border-t border-[#262E42]/60 bg-[#161C28]/80 flex items-center justify-between gap-4">
            <div>
              <div className="text-[10px] text-gray-400 uppercase font-semibold">
                {selectedSeatIds.length} {selectedSeatIds.length === 1 ? 'Seat' : 'Seats'} Selected
              </div>
              <div className="text-xl font-black text-white font-mono">
                ₹{totalPrice}
              </div>
            </div>

            <button
              onClick={handleProceedToPay}
              disabled={selectedSeatIds.length === 0 || bookingLoading}
              className="px-6 py-3 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 disabled:opacity-40 disabled:pointer-events-none text-white font-bold text-xs rounded-xl transition-all shadow-lg shadow-primary-500/25 flex items-center gap-2 cursor-pointer"
            >
              {bookingLoading ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  <span>Proceed to Book</span>
                  <ChevronRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

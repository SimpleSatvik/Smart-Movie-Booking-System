import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { 
  Ticket, Calendar, Clock, MapPin, Sparkles, ArrowLeft,
  Share2, RefreshCw, ShieldCheck, Armchair, AlertCircle, CheckCircle2, X, Ban, Tag
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import LoadingBar from './LoadingBar';

export default function PassWallet({ onOpenResaleModal, onNavigateToDiscovery, onNavigateTab }) {
  const { user } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [activeResaleListings, setActiveResaleListings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [cancellingBookingId, setCancellingBookingId] = useState(null);

  // Pop-up Modals
  const [confirmCancelModal, setConfirmCancelModal] = useState(null);
  const [successModal, setSuccessModal] = useState(null);
  const [errorModal, setErrorModal] = useState(null);

  const fetchBookings = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await api.getUserBookings(user.id || user.userId || 1);
      if (Array.isArray(data)) {
        setBookings(data);
      }
    } catch (err) {
      console.warn('Fetch user bookings error:', err.message);
    }

    try {
      const resaleData = await api.getActiveResaleListings();
      if (Array.isArray(resaleData)) {
        setActiveResaleListings(resaleData);
      }
    } catch (err) {
      // Local fallback for resale
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, [user]);

  // Server bookings are canonical source of truth from PostgreSQL database
  const uniqueBookingsMap = new Map();

  (Array.isArray(bookings) ? bookings : []).forEach(b => {
    if (b) {
      const key = b.bookingReference || b.id || b.bookingId;
      if (key) {
        uniqueBookingsMap.set(String(key).toUpperCase(), b);
      }
    }
  });

  const allBookings = Array.from(uniqueBookingsMap.values());

  // Exclude cancelled and resold bookings so sold/cancelled tickets are completely removed from the active wallet
  const activeBookings = allBookings.filter(b => {
    if (!b) return false;
    // 1. Exclude CANCELLED and RESOLD tickets
    if (b.status === 'CANCELLED' || b.status === 'RESOLD') return false;
    // 2. Ensure booking actually has at least 1 active confirmed seat
    const seats = b.seatCodes || b.seats || b.seatNumbers || [];
    if (!Array.isArray(seats) || seats.length === 0) return false;
    return true;
  });

  const displayBookings = activeBookings;

  // Helper: Find active resale listing(s) associated with a given booking
  const getActiveResaleForBooking = (booking) => {
    if (!booking) return [];

    // Filter server active resale listings from database
    const combinedResale = (Array.isArray(activeResaleListings) ? activeResaleListings : [])
      .filter(l => l && (l.status === 'ACTIVE' || !l.status));

    const bookingRef = (booking.bookingReference || '').trim().toUpperCase();
    const rawSeats = booking.seatCodes || booking.seats || booking.seatNumbers || booking.bookedSeats;
    let bSeats = [];
    if (Array.isArray(rawSeats)) {
      bSeats = rawSeats.map(s => {
        if (!s) return '';
        if (typeof s === 'string') return s.trim().toUpperCase();
        if (typeof s === 'object') {
          const row = s.row || s.rowName || '';
          const num = s.seatNumber !== undefined ? s.seatNumber : (s.number !== undefined ? s.number : '');
          return `${row}${num}`.trim().toUpperCase();
        }
        return String(s).trim().toUpperCase();
      }).filter(Boolean);
    } else if (typeof rawSeats === 'string') {
      bSeats = rawSeats.split(',').map(s => s.trim().toUpperCase()).filter(Boolean);
    } else if (booking.seatNumber) {
      bSeats = [String(booking.seatNumber).trim().toUpperCase()];
    }

    const showSeatIds = (booking.showSeatIds || []).map(id => String(id));

    return combinedResale.filter(listing => {
      if (!listing) return false;
      const listingStatus = (listing.status || 'ACTIVE').toUpperCase();
      if (listingStatus !== 'ACTIVE') return false;

      const listingRef = (listing.bookingReference || '').trim().toUpperCase();
      const listingSeat = String(listing.seatNumber || '').trim().toUpperCase();
      const listingShowSeatId = listing.showSeatId ? String(listing.showSeatId) : null;

      // Match 1: direct bookingReference match
      if (bookingRef && listingRef && bookingRef === listingRef) {
        return true;
      }
      // Match 2: showSeatId match
      if (listingShowSeatId && showSeatIds.includes(listingShowSeatId)) {
        return true;
      }
      // Match 3: Seat code + Movie Title match
      if (listingSeat && bSeats.includes(listingSeat)) {
        if (listing.movieTitle && booking.movieTitle) {
          const m1 = listing.movieTitle.trim().toLowerCase();
          const m2 = booking.movieTitle.trim().toLowerCase();
          if (m1 === m2 || m1.includes(m2) || m2.includes(m1)) {
            return true;
          }
        }
      }
      return false;
    });
  };

  // 2-Hour Time Window Rule Check & Resale Marketplace Guard
  const getCancellationInfo = (booking) => {
    // 1. Strict Resale Guard: If actively listed on Resale, cancellation is prohibited!
    const activeResales = getActiveResaleForBooking(booking);
    if (activeResales.length > 0) {
      const seats = activeResales.map(r => r.seatNumber || 'Seat').filter(Boolean);
      const seatsLabel = seats.length > 0 ? Array.from(new Set(seats)).join(', ') : 'Ticket';
      return {
        canCancel: false,
        label: `Listed on Resale (${seatsLabel})`,
        isPast: false,
        hoursRemaining: 99,
        isListedOnResale: true,
        resaleSeats: seatsLabel,
        reason: `Cannot cancel booking: Seat(s) (${seatsLabel}) are actively listed on the Resale Marketplace. Please remove/delist the ticket from Resale first before cancelling.`
      };
    }

    if (booking.status === 'CANCELLED') {
      return { canCancel: false, label: 'Cancelled', isPast: false, hoursRemaining: 0, isListedOnResale: false };
    }
    if (!booking.showTime) {
      return { canCancel: true, label: 'Cancel Booking', isPast: false, hoursRemaining: 99, isListedOnResale: false };
    }
    const showDate = new Date(booking.showTime);
    const now = new Date();
    const diffHours = (showDate.getTime() - now.getTime()) / (1000 * 60 * 60);

    if (diffHours < 0) {
      return { canCancel: false, label: 'Screening Concluded', isPast: true, hoursRemaining: diffHours, isListedOnResale: false };
    }
    if (diffHours < 2) {
      const minsRemaining = Math.max(0, Math.floor(diffHours * 60));
      return { 
        canCancel: false, 
        label: `Locked (< ${minsRemaining}m to show)`, 
        isPast: false, 
        hoursRemaining: diffHours,
        isListedOnResale: false
      };
    }
    return { 
      canCancel: true, 
      label: 'Cancel Booking', 
      isPast: false, 
      hoursRemaining: diffHours,
      isListedOnResale: false
    };
  };

  const handleRequestCancel = (booking, seatsList, cancelInfo) => {
    if (cancelInfo.isListedOnResale) {
      setErrorModal({
        title: 'Cancellation Blocked',
        message: cancelInfo.reason,
        actionTab: 'resale'
      });
      return;
    }
    setConfirmCancelModal({
      booking,
      seatsList,
      hoursRemaining: cancelInfo.hoursRemaining
    });
  };

  const handleExecuteCancel = async () => {
    if (!confirmCancelModal) return;
    const targetBooking = confirmCancelModal.booking;

    // Hard check: Ensure not actively listed on Resale right at execution time
    const activeResales = getActiveResaleForBooking(targetBooking);
    if (activeResales.length > 0) {
      const seats = activeResales.map(r => r.seatNumber || 'Seat').filter(Boolean);
      const seatsLabel = seats.length > 0 ? Array.from(new Set(seats)).join(', ') : 'Ticket';
      setErrorModal({
        title: 'Cancellation Blocked',
        message: `Cannot cancel booking: Seat(s) (${seatsLabel}) are actively listed on the Resale Marketplace. Please remove/delist the ticket from Resale first before cancelling.`,
        actionTab: 'resale'
      });
      setConfirmCancelModal(null);
      return;
    }

    const idOrRef = targetBooking.id || targetBooking.bookingId || targetBooking.bookingReference;

    setCancellingBookingId(idOrRef);
    try {
      // 1. Call backend cancellation endpoint
      if (idOrRef) {
        try {
          await api.cancelBooking(idOrRef);
        } catch (backendErr) {
          const errMsg = backendErr?.response?.data?.message || backendErr?.message || '';
          // If backend denied cancellation due to active resale or security policy, propagate!
          if (errMsg.toLowerCase().includes('resale') || errMsg.toLowerCase().includes('actively listed')) {
            throw new Error(errMsg);
          }
          // Only bypass for mock demo tickets (starting with CS-) that returned 404
          if (String(idOrRef).startsWith('CS-') && (errMsg.includes('404') || errMsg.toLowerCase().includes('not found'))) {
            console.warn('Backend ticket not found, proceeding with local mock ticket cancellation');
          } else {
            throw backendErr;
          }
        }
      }

      // 2. Refresh bookings directly from PostgreSQL database
      fetchBookings();

      // 3. Update memory state (mark cancelled)
      setBookings(prev => prev.map(b => {
        if ((b.id && b.id === targetBooking.id) || b.bookingReference === targetBooking.bookingReference) {
          return { ...b, status: 'CANCELLED' };
        }
        return b;
      }));

      const seatsText = confirmCancelModal.seatsList.join(', ');
      setSuccessModal({
        title: 'Booking Cancelled',
        message: `Booking #${targetBooking.bookingReference || idOrRef} has been cancelled successfully. Seat(s) ${seatsText} have been released back to the seating map and your ticket pass has been removed.`
      });
      setConfirmCancelModal(null);
    } catch (err) {
      setErrorModal({
        title: 'Cancellation Blocked',
        message: err.message || 'Failed to cancel booking. Please try again.',
        actionTab: err.message && err.message.toLowerCase().includes('resale') ? 'resale' : null
      });
      setConfirmCancelModal(null);
    } finally {
      setCancellingBookingId(null);
    }
  };

  const handleBack = () => {
    if (onNavigateTab) onNavigateTab('movies');
    else if (onNavigateToDiscovery) onNavigateToDiscovery();
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {/* Back Button */}
      <button
        onClick={handleBack}
        className="mb-6 inline-flex items-center gap-2 text-xs font-semibold text-gray-400 hover:text-white bg-[#161C28] px-3.5 py-1.5 rounded-xl border border-[#262E42] transition-colors"
      >
        <ArrowLeft className="w-3.5 h-3.5" />
        <span>Back to Now Showing</span>
      </button>

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
        <div>
          <div className="flex items-center gap-2 text-primary-500 font-semibold text-xs tracking-wider uppercase mb-1">
            <Ticket className="w-4 h-4" />
            <span>Digital Ticket Pass</span>
          </div>
          <h1 className="text-3xl md:text-4xl font-extrabold text-white tracking-tight">
            My Movie Tickets
          </h1>
          <p className="text-slate-300 text-sm mt-1">
            Present your QR code at the cinema turnstile gate for contactless entry.
          </p>
        </div>
      </div>

      {/* Loading Indicator with Animated Bar */}
      {loading ? (
        <LoadingBar
          title="Fetching your movie passes & digital tickets..."
          subtitle="Loading your confirmed seats and turnstile QR codes"
        />
      ) : displayBookings.length === 0 ? (
        <div className="text-center py-16 px-4 bg-[#161c28]/40 border border-slate-800 rounded-3xl max-w-lg mx-auto">
          <div className="w-16 h-16 rounded-3xl bg-primary-500/10 text-primary-400 border border-primary-500/20 flex items-center justify-center mx-auto mb-4">
            <Ticket className="w-8 h-8" />
          </div>
          <h3 className="text-lg font-bold text-white mb-1">No Active Digital Passes</h3>
          <p className="text-xs text-slate-400 mb-6">
            You currently have no active movie tickets. Explore our now showing releases to reserve seats!
          </p>
          <button
            onClick={handleBack}
            className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-xs shadow-lg shadow-primary-500/20 transition-all inline-flex items-center gap-2 cursor-pointer"
          >
            <span>Explore Now Showing</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {displayBookings.map((booking) => {
            // Comprehensive seat extraction: checks seatCodes, seats, seatNumbers, bookedSeats
            const rawSeats = booking.seatCodes || booking.seats || booking.seatNumbers || booking.bookedSeats;
            let seatsList = [];

            if (Array.isArray(rawSeats) && rawSeats.length > 0) {
              seatsList = rawSeats.map(s => {
                if (!s) return '';
                if (typeof s === 'string') return s;
                if (typeof s === 'number') return `Seat ${s}`;
                if (typeof s === 'object') {
                  const row = s.row || s.rowName || '';
                  const num = s.seatNumber !== undefined ? s.seatNumber : (s.number !== undefined ? s.number : '');
                  return `${row}${num}`.trim() || `Seat ${num}`;
                }
                return String(s);
              }).filter(Boolean);
            } else if (typeof rawSeats === 'string' && rawSeats.length > 0) {
              seatsList = rawSeats.split(',').map(s => s.trim()).filter(Boolean);
            } else if (booking.seatNumber) {
              seatsList = [String(booking.seatNumber)];
            } else if (booking.seat) {
              seatsList = [String(booking.seat)];
            } else {
              seatsList = ['B5', 'B6'];
            }

            const seatListStr = seatsList.join(', ');

            const dateStr = booking.showTime 
              ? new Date(booking.showTime).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })
              : 'Tonight';

            const timeStr = booking.showTime
              ? new Date(booking.showTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
              : '7:30 PM';

            const cancelInfo = getCancellationInfo(booking);

            const isResold = booking.status === 'RESOLD';

            return (
              <div 
                key={booking.bookingReference || booking.id || Math.random()} 
                className={`relative bg-gradient-to-b from-[#161c28] to-[#0f1420] border rounded-3xl overflow-hidden shadow-2xl transition-all duration-300 group ${
                  isResold ? 'border-purple-500/30 opacity-90' : 'border-slate-800 hover:border-primary-500/40'
                }`}
              >
                {/* Header */}
                <div className={`p-5 border-b border-dark-border/60 flex items-start justify-between ${
                  isResold 
                    ? 'bg-gradient-to-r from-purple-500/20 via-purple-500/10 to-transparent' 
                    : 'bg-gradient-to-r from-primary-500/20 via-primary-500/10 to-transparent'
                }`}>
                  <div>
                    {isResold ? (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30 uppercase tracking-wider mb-1">
                        <CheckCircle2 className="w-3 h-3 text-purple-400" />
                        Resold & Transferred
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold bg-primary-500/20 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-1">
                        <ShieldCheck className="w-3 h-3" />
                        Verified FastPass
                      </span>
                    )}
                    <h2 className="text-xl font-bold text-white group-hover:text-primary-400 transition-colors">
                      {booking.movieTitle || 'CineSync Premiere'}
                    </h2>
                    <p className="text-xs text-slate-400 mt-0.5 flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-primary-500" />
                      {booking.theatreName || 'PVR Director\'s Cut, Delhi NCR'}
                    </p>
                  </div>
                  <span className="text-xs font-mono font-bold text-slate-400 bg-dark-bg/80 px-2.5 py-1 rounded-lg border border-dark-border">
                    #{booking.bookingReference || 'CS-739102'}
                  </span>
                </div>

                {/* Main Body */}
                <div className="p-6">
                  {/* Resold Notice Banner */}
                  {isResold && (
                    <div className="mb-4 p-3.5 rounded-2xl bg-purple-500/10 border border-purple-500/30 flex items-start gap-3 text-xs text-purple-300 shadow-sm animate-in fade-in">
                      <CheckCircle2 className="w-5 h-5 text-purple-400 shrink-0 mt-0.5" />
                      <div>
                        <div className="font-bold text-white mb-0.5">Ticket Successfully Resold on Fan Exchange</div>
                        <p className="leading-relaxed text-purple-300/90 text-[11px]">
                          This ticket was purchased by another moviegoer. Ownership and gate check-in have been transferred to the buyer. This pass is voided for cinema entry.
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Resale Notice Banner if Ticket/Seat is Actively Listed */}
                  {!isResold && cancelInfo.isListedOnResale && (
                    <div className="mb-4 px-3.5 py-2.5 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-between text-xs text-amber-300 shadow-sm animate-in fade-in">
                      <div className="flex items-center gap-2">
                        <Tag className="w-4 h-4 text-amber-400 shrink-0" />
                        <span>
                          Seat <strong className="text-amber-200">{cancelInfo.resaleSeats}</strong> is actively listed on the Resale Marketplace.
                        </span>
                      </div>
                      <button
                        onClick={() => {
                          if (onNavigateTab) onNavigateTab('resale');
                        }}
                        className="text-[11px] font-bold text-amber-400 hover:text-amber-300 bg-amber-500/20 hover:bg-amber-500/30 px-2.5 py-1 rounded-lg border border-amber-500/40 transition-colors inline-flex items-center gap-1 cursor-pointer shrink-0 ml-2"
                      >
                        <span>Manage Resale</span>
                        <span>&rarr;</span>
                      </button>
                    </div>
                  )}

                  {/* Date & Time */}
                  <div className="grid grid-cols-2 gap-3 mb-5 bg-dark-bg/60 p-3.5 rounded-2xl border border-dark-border/40">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-xl bg-primary-500/10 flex items-center justify-center text-primary-400">
                        <Calendar className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-[10px] font-semibold text-slate-400 uppercase">Date</div>
                        <div className="text-xs font-bold text-white">{dateStr}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-xl bg-accent-blue/10 flex items-center justify-center text-accent-blue">
                        <Clock className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-[10px] font-semibold text-slate-400 uppercase">Time</div>
                        <div className="text-xs font-bold text-white">{timeStr}</div>
                      </div>
                    </div>
                  </div>

                  {/* PROMINENT SEATS & AUDITORIUM SECTION */}
                  <div className="mb-6 p-4 rounded-2xl bg-[#121622] border border-[#262E42] space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-lg bg-primary-500/10 flex items-center justify-center text-primary-400">
                          <Armchair className="w-3.5 h-3.5" />
                        </div>
                        <span className="text-xs font-bold text-white uppercase tracking-wider">
                          {isResold ? 'Transferred' : 'Confirmed'} {seatsList.length === 1 ? 'Seat' : 'Seats'} ({seatsList.length})
                        </span>
                      </div>
                      <span className="text-[11px] font-semibold text-accent-cyan">
                        {booking.screenName || 'Audi 1 (IMAX Laser)'}
                      </span>
                    </div>

                    {/* Individual Seat Badges with Row & Number */}
                    <div className="flex flex-wrap gap-2 pt-1">
                      {seatsList.map((st, sIdx) => {
                        const match = String(st).match(/^([A-Za-z]+)(\d+)$/);
                        const rowPart = match ? match[1] : '';
                        const numPart = match ? match[2] : st;

                        return (
                          <div 
                            key={sIdx}
                            className={`px-3.5 py-2 rounded-xl border flex items-center gap-2 shadow-sm ${
                              isResold
                                ? 'bg-purple-500/10 border-purple-500/30'
                                : 'bg-gradient-to-r from-primary-500/20 via-primary-500/10 to-primary-500/5 border-primary-500/40'
                            }`}
                          >
                            {rowPart && (
                              <span className={`text-[10px] font-bold uppercase tracking-widest ${isResold ? 'text-purple-400' : 'text-primary-400'}`}>
                                ROW {rowPart}
                              </span>
                            )}
                            <span className="text-base font-black text-white font-mono tracking-wider">
                              SEAT {numPart}
                            </span>
                          </div>
                        );
                      })}
                    </div>

                    {/* Paid & Status */}
                    <div className="flex items-center justify-between pt-2 border-t border-[#262E42]/60 text-xs">
                      <span className="text-slate-400 font-medium">
                        {isResold ? 'Settlement Amount' : `Total Amount Paid (${seatsList.length} ${seatsList.length === 1 ? 'ticket' : 'tickets'})`}
                      </span>
                      <div className="flex items-center gap-2">
                        <span className={`text-base font-black font-mono ${isResold ? 'text-purple-300' : 'text-primary-400'}`}>
                          ₹{booking.totalAmountPaid || booking.totalPrice || 700}
                        </span>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          isResold
                            ? 'bg-purple-500/15 text-purple-300 border border-purple-500/30'
                            : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        }`}>
                          {isResold ? 'Resold' : 'Confirmed'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Perforated Divider Visual */}
                  <div className="relative -mx-6 my-2 flex items-center justify-between">
                    <div className="w-5 h-8 bg-dark-bg rounded-r-full border-r border-y border-dark-border"></div>
                    <div className="flex-1 border-b-2 border-dashed border-slate-800 mx-2"></div>
                    <div className="w-5 h-8 bg-dark-bg rounded-l-full border-l border-y border-dark-border"></div>
                  </div>

                  {/* QR Code Section */}
                  <div className="pt-4 flex flex-col sm:flex-row items-center gap-6">
                    {isResold ? (
                      <div className="relative p-3 bg-white/40 rounded-2xl shadow-lg flex-shrink-0 overflow-hidden">
                        <div className="blur-sm opacity-20 pointer-events-none">
                          <QRCodeSVG value="VOID-TICKET-RESOLD" size={110} level="L" />
                        </div>
                        <div className="absolute inset-0 flex items-center justify-center bg-black/70 backdrop-blur-[1px]">
                          <span className="text-[10px] font-black tracking-widest text-rose-400 uppercase -rotate-12 border-2 border-rose-500 px-2 py-0.5 rounded shadow-lg bg-black/90">
                            VOID — RESOLD
                          </span>
                        </div>
                      </div>
                    ) : (
                      <div className="p-3 bg-white rounded-2xl shadow-lg shadow-black/50 flex-shrink-0">
                        <QRCodeSVG 
                          value={`CINESYNC-TICKET:${booking.bookingReference || booking.id}:SEATS-${seatListStr}`} 
                          size={110}
                          level="H"
                          includeMargin={false}
                        />
                      </div>
                    )}
                    
                    <div className="flex-1 text-center sm:text-left space-y-2">
                      <div className="text-xs font-bold text-white">
                        {isResold ? 'Gate Pass Deactivated' : 'Contactless Gate Entry'}
                      </div>
                      <p className="text-[11px] text-slate-400 leading-relaxed">
                        {isResold
                          ? 'This pass was transferred to the buyer upon marketplace sale. Turnstiles will reject this pass reference.'
                          : 'Scan this QR pass directly at the cinema turnstile scanner for fast gate check-in.'}
                      </p>
                      
                      {isResold ? (
                        <div className="pt-1">
                          <span className="px-3 py-1.5 rounded-lg bg-purple-500/15 text-purple-300 text-xs font-bold border border-purple-500/30 inline-flex items-center gap-1.5">
                            <CheckCircle2 className="w-3.5 h-3.5 text-purple-400" />
                            <span>Transferred to Buyer — No Further Actions</span>
                          </span>
                        </div>
                      ) : (
                        <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-1">
                          <button
                            onClick={() => onOpenResaleModal && onOpenResaleModal(booking)}
                            className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-primary-400 hover:text-primary-300 text-xs font-semibold border border-primary-500/30 transition-all inline-flex items-center justify-center gap-1.5 cursor-pointer"
                          >
                            <Share2 className="w-3.5 h-3.5" />
                            <span>Sell Seat on Resale</span>
                          </button>

                          {/* Guarded Cancellation Button */}
                          {cancelInfo.isListedOnResale ? (
                            <button
                              onClick={() => {
                                setErrorModal({
                                  title: 'Cancellation Blocked',
                                  message: cancelInfo.reason,
                                  actionTab: 'resale'
                                });
                              }}
                              className="px-3 py-1.5 rounded-lg bg-amber-500/15 hover:bg-amber-500/25 text-amber-400 hover:text-amber-300 text-xs font-bold border border-amber-500/30 transition-all inline-flex items-center justify-center gap-1.5 cursor-pointer shadow-sm"
                              title="Ticket is active on Resale Marketplace. Delist it first before cancelling."
                            >
                              <Ban className="w-3.5 h-3.5 text-amber-400" />
                              <span>Listed on Resale (Delist First)</span>
                            </button>
                          ) : cancelInfo.canCancel ? (
                            <button
                              onClick={() => handleRequestCancel(booking, seatsList, cancelInfo)}
                              disabled={cancellingBookingId === (booking.id || booking.bookingId || booking.bookingReference)}
                              className="px-3 py-1.5 rounded-lg bg-rose-500/15 hover:bg-rose-500/25 text-rose-400 hover:text-rose-300 text-xs font-bold border border-rose-500/30 transition-all inline-flex items-center justify-center gap-1.5 cursor-pointer shadow-sm"
                            >
                              {cancellingBookingId === (booking.id || booking.bookingId || booking.bookingReference) ? (
                                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                              ) : (
                                <Ban className="w-3.5 h-3.5" />
                              )}
                              <span>Cancel Booking</span>
                            </button>
                          ) : (
                            <span 
                              className="px-2.5 py-1.5 rounded-lg bg-slate-800/80 text-slate-400 text-[11px] font-semibold border border-slate-700/80 inline-flex items-center gap-1.5 cursor-not-allowed"
                              title={cancelInfo.isPast ? "This screening has concluded." : "Cancellations must be requested at least 2 hours prior to showtime."}
                            >
                              <Clock className="w-3 h-3 text-slate-400" />
                              <span>{cancelInfo.label}</span>
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Confirmation Modal: Cancel Booking */}
      {confirmCancelModal && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
          onClick={(e) => {
            if (e.target === e.currentTarget) setConfirmCancelModal(null);
          }}
        >
          <div className="w-full max-w-md bg-dark-card border border-rose-500/40 rounded-3xl p-6 shadow-2xl relative overflow-hidden animate-in zoom-in-95 duration-200">
            {/* Ambient background glow */}
            <div className="absolute -top-16 -right-16 w-32 h-32 bg-rose-500/15 rounded-full blur-2xl pointer-events-none" />

            <button
              onClick={() => setConfirmCancelModal(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-white transition-colors p-1.5 rounded-xl hover:bg-white/5"
              aria-label="Close dialog"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="w-12 h-12 rounded-2xl bg-rose-500/15 border border-rose-500/30 flex items-center justify-center text-rose-400 mb-4 shadow-inner">
              <AlertCircle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-white mb-2">
              Cancel Ticket #{confirmCancelModal.booking.bookingReference || confirmCancelModal.booking.id}?
            </h3>
            
            <div className="space-y-3 text-sm text-slate-300 leading-relaxed mb-6">
              <p>
                Are you sure you want to cancel your booking for <span className="font-bold text-white">{confirmCancelModal.booking.movieTitle}</span>?
              </p>
              <div className="p-3 bg-dark-bg/80 rounded-2xl border border-dark-border text-xs space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-400">Seats to Release:</span>
                  <span className="font-bold text-white">{confirmCancelModal.seatsList.join(', ')}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Refund Amount:</span>
                  <span className="font-bold text-emerald-400 font-mono">₹{confirmCancelModal.booking.totalAmountPaid || confirmCancelModal.booking.totalPrice || 0}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Cancellation Policy:</span>
                  <span className="font-semibold text-sky-400">100% Refund (Show in &gt;2 hrs)</span>
                </div>
              </div>
              <p className="text-xs text-slate-400">
                The seat(s) will be immediately marked as available on the live seating map for other moviegoers.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setConfirmCancelModal(null)}
                className="flex-1 py-3 px-4 rounded-xl bg-dark-bg hover:bg-slate-800 text-slate-300 font-bold text-xs uppercase tracking-wider transition-colors cursor-pointer"
              >
                Keep Ticket
              </button>
              <button
                type="button"
                onClick={handleExecuteCancel}
                disabled={cancellingBookingId !== null}
                className="flex-1 py-3 px-4 rounded-xl bg-gradient-to-r from-rose-500 to-rose-600 hover:from-rose-600 hover:to-rose-700 text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-rose-500/20 transition-all flex items-center justify-center gap-2 cursor-pointer"
              >
                {cancellingBookingId ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    <span>Cancelling...</span>
                  </>
                ) : (
                  <span>Confirm Cancel</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Success Pop-up Modal */}
      {successModal && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
          onClick={(e) => {
            if (e.target === e.currentTarget) setSuccessModal(null);
          }}
        >
          <div className="w-full max-w-md bg-dark-card border border-emerald-500/40 rounded-3xl p-6 shadow-2xl relative overflow-hidden animate-in zoom-in-95 duration-200">
            {/* Ambient background glow */}
            <div className="absolute -top-16 -right-16 w-32 h-32 bg-emerald-500/15 rounded-full blur-2xl pointer-events-none" />

            <button
              onClick={() => setSuccessModal(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-white transition-colors p-1.5 rounded-xl hover:bg-white/5"
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="w-12 h-12 rounded-2xl bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mb-4 shadow-inner">
              <CheckCircle2 className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-white mb-2">
              {successModal.title}
            </h3>
            
            <p className="text-sm text-slate-300 leading-relaxed mb-6">
              {successModal.message}
            </p>

            <button
              onClick={() => setSuccessModal(null)}
              className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-emerald-500/20 transition-all flex items-center justify-center cursor-pointer"
            >
              Done
            </button>
          </div>
        </div>
      )}

      {/* Error Alert Pop-up Modal */}
      {errorModal && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
          onClick={(e) => {
            if (e.target === e.currentTarget) setErrorModal(null);
          }}
        >
          <div className="w-full max-w-md bg-dark-card border border-rose-500/40 rounded-3xl p-6 shadow-2xl relative overflow-hidden animate-in zoom-in-95 duration-200">
            {/* Ambient background glow */}
            <div className="absolute -top-16 -right-16 w-32 h-32 bg-rose-500/15 rounded-full blur-2xl pointer-events-none" />

            <button
              onClick={() => setErrorModal(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-white transition-colors p-1.5 rounded-xl hover:bg-white/5"
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="w-12 h-12 rounded-2xl bg-rose-500/15 border border-rose-500/30 flex items-center justify-center text-rose-400 mb-4 shadow-inner">
              <AlertCircle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-white mb-2">
              {errorModal.title}
            </h3>
            
            <p className="text-sm text-slate-300 leading-relaxed mb-6">
              {errorModal.message}
            </p>

            <div className="flex items-center gap-3">
              {errorModal.actionTab && (
                <button
                  type="button"
                  onClick={() => {
                    const tab = errorModal.actionTab;
                    setErrorModal(null);
                    if (onNavigateTab) onNavigateTab(tab);
                  }}
                  className="flex-1 py-3 px-4 rounded-xl bg-gradient-to-r from-amber-500 to-primary-600 hover:from-amber-600 hover:to-primary-700 text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-amber-500/20 transition-all flex items-center justify-center cursor-pointer"
                >
                  Manage in Resale
                </button>
              )}
              <button
                type="button"
                onClick={() => setErrorModal(null)}
                className={`${errorModal.actionTab ? 'flex-1 bg-dark-bg hover:bg-slate-800 text-slate-300' : 'w-full bg-gradient-to-r from-rose-500 to-primary-600 text-white'} py-3 px-4 rounded-xl font-bold text-xs uppercase tracking-wider transition-all flex items-center justify-center cursor-pointer`}
              >
                Understood
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

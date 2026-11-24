import React, { useState, useEffect, useRef } from 'react';
import { 
  ShoppingBag, Tag, Plus, Clock, ArrowRight, ArrowLeft,
  CheckCircle2, RefreshCw, X, Film, Ticket, AlertCircle
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import confetti from 'canvas-confetti';
import LoadingBar from './LoadingBar';

export default function ResaleMarketplace({ onOpenAuth, onNavigateTab, initialBooking }) {
  const { user } = useAuth();
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [purchasingId, setPurchasingId] = useState(null);
  const [notice, setNotice] = useState(null);

  // New Listing Modal state
  const [isListModalOpen, setIsListModalOpen] = useState(false);
  const [myBookings, setMyBookings] = useState([]);
  const [fetchingBookings, setFetchingBookings] = useState(false);
  const [selectedBookingRef, setSelectedBookingRef] = useState('');
  const [selectedSeat, setSelectedSeat] = useState('');
  const [resalePriceInput, setResalePriceInput] = useState('');
  const userHasEditedPriceRef = useRef(false);
  const [listingLoading, setListingLoading] = useState(false);

  // 1. Fetch live active listings directly from PostgreSQL database
  const fetchListings = async () => {
    setLoading(true);
    try {
      const data = await api.getActiveResaleListings();
      if (Array.isArray(data)) {
        // Normalize backend DTO fields to frontend format
        const serverListings = data.map(dto => ({
          id: dto.listingId || dto.id,
          listingId: dto.listingId || dto.id,
          bookingReference: dto.bookingReference,
          movieTitle: dto.movieTitle,
          theatreName: dto.theatreName,
          showTime: dto.showStartTime || dto.showTime,
          seatNumber: dto.seatCode || (dto.rowName && dto.seatNumber ? `${dto.rowName}${dto.seatNumber}` : String(dto.seatNumber || 'A01')),
          originalPrice: dto.originalPrice ? Number(dto.originalPrice) : 350,
          resalePrice: dto.askingPrice ? Number(dto.askingPrice) : (dto.originalPrice ? Number(dto.originalPrice) : 350),
          sellerId: dto.sellerId,
          sellerName: dto.sellerName || 'Verified Fan',
          status: dto.status || 'ACTIVE',
          isBackendSynced: true
        }));
        setListings(serverListings);
      } else {
        setListings([]);
      }
    } catch (err) {
      console.warn('Backend resale listings fetch notice:', err.message);
      setListings([]);
    } finally {
      setLoading(false);
    }
  };

  // 2. Fetch user's real confirmed bookings (UPCOMING FUTURE SHOWS ONLY)
  const loadMyBookings = async () => {
    setFetchingBookings(true);
    let rawBookings = [];
    if (user) {
      try {
        const userId = user.id || user.userId || 1;
        const serverBookings = await api.getUserBookings(userId);
        if (Array.isArray(serverBookings)) {
          rawBookings = serverBookings;
        }
      } catch (err) {
        console.warn('Server bookings fetch notice:', err.message);
      } finally {
        setFetchingBookings(false);
      }
    } else {
      setFetchingBookings(false);
    }

    // STRICT RESALE FILTER:
    // Only CONFIRMED bookings for FUTURE showtimes (must be at least 15 mins before show starts)
    const nowEpoch = Date.now();
    const eligibleBookings = rawBookings.filter(b => {
      if (!b) return false;
      // 1. Cannot be a cancelled or already resold ticket
      if (b.status === 'CANCELLED' || b.status === 'RESOLD') return false;
      // 2. Must have active seats
      const seats = b.seatCodes || b.seats || b.seatNumbers || [];
      if (!Array.isArray(seats) || seats.length === 0) return false;
      // 3. Must be an upcoming future screening
      if (b.showTime) {
        const showEpoch = new Date(b.showTime).getTime();
        return showEpoch > (nowEpoch + 15 * 60 * 1000);
      }
      return false;
    });

    setMyBookings(eligibleBookings);
    if (eligibleBookings.length > 0) {
      // If initialBooking was passed from Wallet, target it
      const target = initialBooking 
        ? (eligibleBookings.find(b => b.bookingReference === initialBooking.bookingReference) || eligibleBookings[0])
        : eligibleBookings[0];

      setSelectedBookingRef(target.bookingReference);
      const seats = target.seatCodes || target.seats || target.seatNumbers || [];
      setSelectedSeat(seats.length > 0 ? seats[0] : '');

      // NEVER overwrite user's input if they have manually typed or edited the price
      if (!userHasEditedPriceRef.current) {
        const price = target.pricePerSeat || Math.round((target.totalAmountPaid || 350) / (seats.length || 1));
        setResalePriceInput(String(price));
      }
    } else {
      setSelectedBookingRef('');
      setSelectedSeat('');
      if (!userHasEditedPriceRef.current) {
        setResalePriceInput('');
      }
    }
  };

  useEffect(() => {
    fetchListings();
    loadMyBookings();
    if (initialBooking) {
      setIsListModalOpen(true);
    }
  }, [user?.id, initialBooking?.bookingReference]);

  const currentSelectedBooking = myBookings.find(b => b.bookingReference === selectedBookingRef) || myBookings[0];
  const allBookingSeats = currentSelectedBooking?.seatCodes || currentSelectedBooking?.seats || currentSelectedBooking?.seatNumbers || ['B5', 'B6'];

  // DOUBLE-SELLING PROTECTION CHECK:
  // Identify which seats from this booking are ALREADY listed on the marketplace
  const alreadyListedSeats = listings
    .filter(l => l.bookingReference === currentSelectedBooking?.bookingReference || l.movieTitle === currentSelectedBooking?.movieTitle)
    .map(l => String(l.seatNumber || l.seat || ''));

  const availableSeatsToSell = allBookingSeats.filter(s => !alreadyListedSeats.includes(String(s)));

  const handleBookingChange = (ref) => {
    setSelectedBookingRef(ref);
    const bk = myBookings.find(b => b.bookingReference === ref);
    if (bk) {
      const seats = bk.seatCodes || bk.seats || bk.seatNumbers || [];
      const listed = listings
        .filter(l => l.bookingReference === bk.bookingReference)
        .map(l => String(l.seatNumber || ''));
      const available = seats.filter(s => !listed.includes(String(s)));

      if (available.length > 0) {
        setSelectedSeat(available[0]);
      } else {
        setSelectedSeat('');
      }

      // If user has not typed a custom price, default to this booking's per-seat cost
      if (!userHasEditedPriceRef.current) {
        const price = bk.pricePerSeat || Math.round((bk.totalAmountPaid || 700) / (seats.length || 1));
        setResalePriceInput(String(price));
      }
    }
  };

  const handleCloseModal = () => {
    setIsListModalOpen(false);
    userHasEditedPriceRef.current = false;
    setResalePriceInput('');
  };

  const handleBuyTicket = async (listing) => {
    if (!user) {
      onOpenAuth();
      return;
    }

    setPurchasingId(listing.id);
    const buyerId = user.id || user.userId || 1;

    try {
      await api.buyResaleTicket(listing.id, buyerId);

      confetti({ particleCount: 80, spread: 60, origin: { y: 0.6 } });
      setNotice({ 
        type: 'success', 
        msg: `Ticket purchased for ${listing.movieTitle} (Seat ${listing.seatNumber})! Digital pass added to your tickets wallet.` 
      });

      // Refresh listings from database
      fetchListings();

      // Navigate buyer directly to PassWallet so their new ticket is immediately visible
      setTimeout(() => {
        if (onNavigateTab) {
          onNavigateTab('wallet');
        }
      }, 1500);
    } catch (err) {
      console.error('Resale purchase error:', err);
      setNotice({
        type: 'error',
        msg: err.message || 'Failed to purchase resale ticket. It may have already been sold or withdrawn.'
      });
      fetchListings();
    } finally {
      setPurchasingId(null);
    }
  };

  const handleCancelListing = async (listing) => {
    try {
      await api.cancelResaleListing(listing.id, user?.id || user?.userId || 1);
      setNotice({
        type: 'success',
        msg: `Listing for Seat ${listing.seatNumber} has been withdrawn.`
      });
      fetchListings();
      loadMyBookings();
    } catch (err) {
      console.error('Cancel listing error:', err);
      setNotice({
        type: 'error',
        msg: err.message || 'Failed to withdraw listing.'
      });
    }
  };

  const handleCreateListing = async (e) => {
    e.preventDefault();
    if (!user) {
      onOpenAuth();
      return;
    }
    if (!selectedBookingRef || !selectedSeat || !resalePriceInput) return;

    const parsedPrice = parseFloat(resalePriceInput);
    if (isNaN(parsedPrice) || parsedPrice <= 0) {
      setNotice({ type: 'error', msg: 'Please enter a valid asking price of at least ₹1.00.' });
      return;
    }

    // Hard check: prevent double-selling
    if (alreadyListedSeats.includes(String(selectedSeat))) {
      setNotice({ type: 'error', msg: `Seat ${selectedSeat} is already actively listed for resale!` });
      return;
    }

    setListingLoading(true);
    const currentBooking = myBookings.find(b => b.bookingReference === selectedBookingRef) || myBookings[0];
    
    // Find the seat's showSeatId if available
    const seatIdx = allBookingSeats.findIndex(s => String(s) === String(selectedSeat));
    const showSeatId = (currentBooking.showSeatIds && currentBooking.showSeatIds[seatIdx]) 
      ? currentBooking.showSeatIds[seatIdx] 
      : null;

    if (!showSeatId) {
      setNotice({
        type: 'error',
        msg: `Could not determine physical seat ID for ${selectedSeat}. Please select another seat.`
      });
      setListingLoading(false);
      return;
    }

    try {
      await api.createResaleListing({
        sellerUserId: user.id || user.userId || 1,
        showSeatId: showSeatId,
        askingPrice: parsedPrice
      });

      setNotice({ 
        type: 'success', 
        msg: `Seat ${selectedSeat} from Booking #${currentBooking.bookingReference} is now listed for resale at ₹${parsedPrice}!` 
      });
      handleCloseModal();
      fetchListings();
      loadMyBookings();
    } catch (err) {
      console.error('Backend resale create listing error:', err);
      setNotice({
        type: 'error',
        msg: err.message || 'Failed to list ticket for resale on the marketplace.'
      });
    } finally {
      setListingLoading(false);
      setTimeout(() => setNotice(null), 6000);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {/* Back Button */}
      {onNavigateTab && (
        <button
          onClick={() => onNavigateTab('movies')}
          className="mb-6 inline-flex items-center gap-2 text-xs font-semibold text-gray-400 hover:text-white bg-[#161C28] px-3.5 py-1.5 rounded-xl border border-[#262E42] transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Now Showing</span>
        </button>
      )}

      {/* Banner / Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-2">
            <ShoppingBag className="w-3.5 h-3.5" />
            <span>Spare Ticket Exchange</span>
          </div>
          <h1 className="text-3xl md:text-4xl font-extrabold text-white tracking-tight">
            Spare Ticket Marketplace
          </h1>
          <p className="text-slate-300 text-sm mt-1 max-w-xl">
            Can’t make it to the movie? Easily resell your extra ticket to another moviegoer at original face value so your money isn’t wasted.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchListings}
            className="p-2.5 rounded-xl bg-dark-card border border-dark-border text-slate-400 hover:text-white transition-colors"
            title="Refresh Listings"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={() => {
              if (!user) {
                onOpenAuth();
              } else {
                userHasEditedPriceRef.current = false;
                setResalePriceInput('');
                loadMyBookings();
                setIsListModalOpen(true);
              }
            }}
            className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-primary-500/25 inline-flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            Sell My Extra Ticket
          </button>
        </div>
      </div>

      {/* Notification banner */}
      {notice && (
        <div className={`mb-6 p-4 rounded-2xl border flex items-center gap-3 ${
          notice.type === 'error' 
            ? 'bg-red-500/10 border-red-500/30 text-red-400' 
            : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
        }`}>
          {notice.type === 'error' ? <AlertCircle className="w-5 h-5 flex-shrink-0" /> : <CheckCircle2 className="w-5 h-5 flex-shrink-0" />}
          <span className="text-sm font-semibold">{notice.msg}</span>
        </div>
      )}

      {/* Listings Grid */}
      {loading ? (
        <LoadingBar 
          title="Fetching spare tickets from the fan exchange..." 
          subtitle="Loading tickets listed at original face value by verified moviegoers" 
        />
      ) : listings.length === 0 ? (
        <div className="bg-dark-card/40 border border-dark-border/60 rounded-3xl p-12 text-center max-w-lg mx-auto">
          <Tag className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white mb-1">No spare tickets available right now</h3>
          <p className="text-slate-400 text-xs mb-6">
            All tickets are currently held by their original bookers. Check back soon or list your own extra ticket.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {listings.map((item) => {
            const price = item.resalePrice || item.price || item.askingPrice || 350;
            const isMyListing = item.sellerName === 'You' || 
              (user && (item.sellerId === (user.id || user.userId) || item.sellerName === user.name));

            return (
              <div 
                key={item.id}
                className="bg-dark-card/90 border border-dark-border hover:border-primary-500/50 rounded-3xl p-5 flex flex-col justify-between transition-all duration-300 hover:shadow-xl group"
              >
                <div>
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-primary-500/10 text-primary-400 border border-primary-500/20 uppercase tracking-wider">
                      Seat {item.seatNumber || 'A04'}
                    </span>
                    {isMyListing ? (
                      <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30">
                        Your Listing
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        Face Value
                      </span>
                    )}
                  </div>

                  <h3 className="text-lg font-bold text-white group-hover:text-primary-400 transition-colors line-clamp-1">
                    {item.movieTitle || 'Live Screening'}
                  </h3>

                  <p className="text-xs text-slate-400 mt-1 line-clamp-1">
                    {item.theatreName || 'PVR Cinema Complex'}
                  </p>

                  <div className="mt-4 pt-4 border-t border-dark-border/60 flex items-center justify-between text-xs text-slate-400">
                    <span className="flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-accent-cyan" />
                      {item.showTime ? new Date(item.showTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'Tonight'}
                    </span>
                    <span className="text-[11px] text-slate-400 font-mono">
                      Ref: #{item.bookingReference || 'CS-847291'}
                    </span>
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-dark-border/60 flex items-center justify-between">
                  <div>
                    <span className="text-2xl font-black text-white">₹{price}</span>
                    <div className="text-[10px] text-emerald-400 font-semibold">
                      {item.originalPrice && Number(item.originalPrice) === Number(price) ? 'Original ticket price' : 'Resale asking price'}
                    </div>
                  </div>

                  {isMyListing ? (
                    <button
                      onClick={() => handleCancelListing(item)}
                      className="px-3.5 py-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 font-semibold text-xs rounded-xl transition-all flex items-center gap-1.5"
                    >
                      <X className="w-3.5 h-3.5" />
                      <span>Cancel Listing</span>
                    </button>
                  ) : (
                    <button
                      onClick={() => handleBuyTicket(item)}
                      disabled={purchasingId === item.id}
                      className="px-4 py-2 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 disabled:opacity-50 text-white font-bold text-xs rounded-xl transition-all shadow-md shadow-primary-500/20 flex items-center gap-1.5"
                    >
                      {purchasingId === item.id ? (
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <>
                          <span>Buy Ticket</span>
                          <ArrowRight className="w-3.5 h-3.5" />
                        </>
                      )}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal: List My Ticket With Direct Booking Reference Attachment & Double-Sell Protection */}
      {isListModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="relative w-full max-w-md bg-dark-card border border-dark-border rounded-3xl p-6 shadow-2xl">
            <button
              onClick={handleCloseModal}
              className="absolute top-5 right-5 text-slate-400 hover:text-white p-1 rounded-lg"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-2.5 mb-2">
              <div className="w-8 h-8 rounded-xl bg-primary-500/10 flex items-center justify-center text-primary-400">
                <Tag className="w-4 h-4" />
              </div>
              <h2 className="text-xl font-bold text-white">Sell Extra Ticket</h2>
            </div>
            <p className="text-xs text-slate-300 mb-6">
              Select which of your confirmed upcoming bookings you want to sell from. Past screenings and cancelled tickets cannot be resold.
            </p>

            {fetchingBookings ? (
              <div className="py-10 px-4 bg-dark-bg/60 rounded-2xl border border-dark-border text-center space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-primary-500/10 text-primary-400 border border-primary-500/20 flex items-center justify-center mx-auto">
                  <RefreshCw className="w-6 h-6 animate-spin" />
                </div>
                <h3 className="text-sm font-bold text-white">Fetching Your Upcoming Tickets...</h3>
                <p className="text-xs text-slate-400 max-w-xs mx-auto leading-relaxed">
                  Checking your confirmed bookings from your account to find eligible shows for resale.
                </p>
              </div>
            ) : myBookings.length === 0 ? (
              <div className="py-8 px-4 bg-dark-bg/60 rounded-2xl border border-dark-border text-center space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center justify-center mx-auto">
                  <Film className="w-6 h-6" />
                </div>
                <h3 className="text-sm font-bold text-white">No Upcoming Eligible Tickets</h3>
                <p className="text-xs text-slate-400 max-w-xs mx-auto leading-relaxed">
                  You do not have any upcoming confirmed bookings. You can only resell tickets for future scheduled shows (at least 15 minutes before showtime).
                </p>
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold cursor-pointer"
                >
                  Close
                </button>
              </div>
            ) : (
              <form onSubmit={handleCreateListing} className="space-y-4">
                {/* 1. Select Which Confirmed Booking */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                    <Film className="w-3.5 h-3.5 text-primary-500" />
                    Select Your Upcoming Show
                  </label>
                  <select
                    required
                    value={selectedBookingRef}
                    onChange={(e) => handleBookingChange(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  >
                    {myBookings.map((b) => {
                      const dStr = b.showTime 
                        ? new Date(b.showTime).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
                        : '';
                      const tStr = b.showTime 
                        ? new Date(b.showTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
                        : '';
                      return (
                        <option key={b.bookingReference} value={b.bookingReference}>
                          {b.movieTitle} — #{b.bookingReference} {dStr ? `(${dStr} at ${tStr})` : ''}
                        </option>
                      );
                    })}
                  </select>
                  <span className="text-[10px] text-slate-400 mt-1 block">
                    {currentSelectedBooking?.theatreName || 'PVR Cinema'}
                  </span>
                </div>

              {/* 2. Select Which Specific Seat to Resell (With Double-Selling Check) */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                  <Ticket className="w-3.5 h-3.5 text-accent-cyan" />
                  Choose Seat to Resell
                </label>

                {allBookingSeats.length === 0 ? (
                  <p className="text-xs text-amber-400">No seats found in this booking.</p>
                ) : availableSeatsToSell.length === 0 ? (
                  <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>All seats for this booking ({allBookingSeats.join(', ')}) are already listed on the marketplace!</span>
                  </div>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {allBookingSeats.map((seat) => {
                      const isListed = alreadyListedSeats.includes(String(seat));
                      const isSelected = selectedSeat === seat;

                      return (
                        <button
                          type="button"
                          key={seat}
                          disabled={isListed}
                          onClick={() => setSelectedSeat(seat)}
                          className={`py-2 px-3.5 rounded-xl border text-xs font-bold transition-all flex items-center gap-1.5 ${
                            isListed
                              ? 'bg-slate-900 border-slate-800 text-gray-500 cursor-not-allowed line-through'
                              : isSelected
                              ? 'bg-primary-500 text-white border-primary-400 shadow-md shadow-primary-500/25'
                              : 'bg-dark-bg border-dark-border text-slate-300 hover:text-white'
                          }`}
                        >
                          <span>Seat {seat}</span>
                          {isListed && <span className="text-[9px] text-amber-400 uppercase font-normal">(Listed)</span>}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* 3. Ticket Price */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Selling Price (₹)
                </label>
                <input
                  type="number"
                  step="any"
                  min="1"
                  required
                  placeholder="e.g. 350"
                  value={resalePriceInput}
                  onChange={(e) => {
                    userHasEditedPriceRef.current = true;
                    setResalePriceInput(e.target.value);
                  }}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm font-semibold"
                />
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Enter your asking price for this ticket.
                </span>
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={listingLoading || availableSeatsToSell.length === 0 || !selectedSeat}
                  className="w-full py-3 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 disabled:opacity-40 disabled:cursor-not-allowed text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2"
                >
                  {listingLoading ? (
                    <RefreshCw className="w-4 h-4 animate-spin" />
                  ) : availableSeatsToSell.length === 0 ? (
                    'All Seats Already Listed'
                  ) : (
                    `Publish Seat ${selectedSeat || ''} for Sale`
                  )}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
      )}
    </div>
  );
}

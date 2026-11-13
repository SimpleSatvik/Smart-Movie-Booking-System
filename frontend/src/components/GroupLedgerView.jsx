import React, { useState, useEffect } from 'react';
import { 
  Users, DollarSign, Plus, CheckCircle2, ArrowRight, ArrowLeft,
  Sparkles, RefreshCw, Calculator, Copy, Check, Receipt, Film,
  MapPin, Clock, Calendar, Ticket, Trash2, UserPlus, X, AlertCircle, ShieldCheck
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import confetti from 'canvas-confetti';
import LoadingBar from './LoadingBar';

import { getMoviePoster, handleImageError } from '../utils/moviePosters';

export default function GroupLedgerView({ onOpenAuth, onNavigateTab }) {
  const { user } = useAuth();
  const [activeMode, setActiveMode] = useState('trip'); // 'trip' or 'quick'
  const [outingItems, setOutingItems] = useState([]);
  const [selectedItem, setSelectedItem] = useState(null);
  const [outingSummary, setOutingSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Quick Calculator State (In-memory helper)
  const [quickTotal, setQuickTotal] = useState(1200);
  const [quickPeople, setQuickPeople] = useState(2);
  const [copiedQuick, setCopiedQuick] = useState(false);

  // Add Expense Modal
  const [isExpenseModalOpen, setIsExpenseModalOpen] = useState(false);
  const [expenseDesc, setExpenseDesc] = useState('');
  const [expenseAmount, setExpenseAmount] = useState('');
  const [expensePayerId, setExpensePayerId] = useState('');

  // Add Friend Modal (Strictly Verified by Database Registered Email)
  const [isAddFriendOpen, setIsAddFriendOpen] = useState(false);
  const [newFriendEmail, setNewFriendEmail] = useState('');
  const [friendError, setFriendError] = useState('');

  const getPosterForMovie = (title = '') => {
    return getMoviePoster(title);
  };

  // Load live summary from backend for an existing Outing
  const loadOutingSummary = async (outingId) => {
    if (!outingId) return;
    try {
      const summary = await api.getOutingSummary(outingId);
      if (summary && summary.outingId) {
        setOutingSummary(summary);
        const organizer = summary.memberBalances?.find(m => m.isOrganizer) || summary.memberBalances?.[0];
        if (organizer) {
          setExpensePayerId(String(organizer.userId));
        }
      } else {
        setOutingSummary(null);
      }
    } catch (err) {
      console.error("Error loading outing summary from DB:", err);
      setErrorMessage(err.message || 'Failed to load outing details from database.');
      setOutingSummary(null);
    }
  };

  // 100% Database-Driven: Load both user's bookings and shared outings
  const loadDataFromDatabase = async () => {
    if (!user) return;
    setLoading(true);
    setErrorMessage('');

    const userId = user.id || user.userId;
    let myOutingsList = [];
    let myBookingsList = [];

    try {
      // 1. Fetch all outings the user is a member of (organized by user OR invited by friends)
      const outingsRes = await api.getMyOutings(userId);
      if (Array.isArray(outingsRes)) {
        myOutingsList = outingsRes;
      }
    } catch (err) {
      console.warn("Could not fetch user outings from backend:", err);
    }

    try {
      // 2. Fetch all tickets booked by this user
      const bookingsRes = await api.getUserBookings(userId);
      if (Array.isArray(bookingsRes)) {
        myBookingsList = bookingsRes.filter(b => b && b.status !== 'CANCELLED');
      }
    } catch (err) {
      console.warn("Could not fetch user bookings from backend:", err);
    }

    const items = [];
    const handledBookingIds = new Set();

    // A. Add all active Outings from PostgreSQL
    myOutingsList.forEach(outing => {
      if (outing.bookingId) handledBookingIds.add(String(outing.bookingId));
      items.push({
        type: 'OUTING',
        key: `outing-${outing.outingId}`,
        outingId: outing.outingId,
        bookingId: outing.bookingId,
        bookingReference: outing.bookingReference,
        movieTitle: outing.movieTitle,
        posterUrl: outing.posterUrl,
        theatreName: outing.theatreName,
        showTime: outing.showTime,
        createdByName: outing.createdByName,
        createdByUserId: outing.createdByUserId,
        isOrganizer: String(outing.createdByUserId) === String(userId),
        totalMembers: outing.totalMembers,
        totalGroupSpend: outing.totalGroupSpend
      });
    });

    // B. Add un-split bookings owned by the user (ready to create an outing)
    myBookingsList.forEach(booking => {
      const bId = String(booking.bookingId || booking.id);
      if (!handledBookingIds.has(bId)) {
        items.push({
          type: 'UNSPLIT_BOOKING',
          key: `booking-${bId}`,
          bookingId: booking.bookingId || booking.id,
          bookingReference: booking.bookingReference,
          movieTitle: booking.movieTitle || 'Movie Booking',
          theatreName: booking.theatreName,
          screenName: booking.screenName,
          showTime: booking.showTime,
          totalAmountPaid: Number(booking.totalAmountPaid || 0),
          isOrganizer: true
        });
      }
    });

    setOutingItems(items);

    if (items.length > 0) {
      const first = items[0];
      setSelectedItem(first);
      if (first.type === 'OUTING') {
        await loadOutingSummary(first.outingId);
      } else {
        setOutingSummary(null);
      }
    } else {
      setSelectedItem(null);
      setOutingSummary(null);
    }

    setLoading(false);
  };

  useEffect(() => {
    loadDataFromDatabase();
  }, [user]);

  // Switch selection handler
  const handleSelectItem = async (itemKey) => {
    const found = outingItems.find(i => i.key === itemKey);
    if (found) {
      setSelectedItem(found);
      if (found.type === 'OUTING') {
        setLoading(true);
        await loadOutingSummary(found.outingId);
        setLoading(false);
      } else {
        setOutingSummary(null);
      }
    }
  };

  // Create Outing on PostgreSQL database
  const handleCreateOuting = async () => {
    if (!selectedItem || selectedItem.type !== 'UNSPLIT_BOOKING' || !user) return;
    setActionLoading(true);
    setErrorMessage('');
    const userId = user.id || user.userId;
    const organizerEmail = user.gmail || user.email || 'organizer@cinesync.com';

    try {
      const outingName = `${selectedItem.movieTitle} Squad Outing`;
      const res = await api.createOuting(
        selectedItem.bookingId,
        userId,
        outingName,
        [organizerEmail],
        selectedItem.bookingReference
      );

      if (res && res.outingId) {
        await loadDataFromDatabase();
        await loadOutingSummary(res.outingId);
      }
    } catch (err) {
      console.error("Error creating outing in DB:", err);
      setErrorMessage(err.message || 'Failed to create movie outing on server.');
    } finally {
      setActionLoading(false);
    }
  };

  // Add Real Registered Friend by Email (Strict Database Verification)
  const handleAddFriendSubmit = async (e) => {
    e.preventDefault();
    setFriendError('');
    if (!newFriendEmail.trim() || !outingSummary) return;

    setActionLoading(true);
    try {
      // Calls backend: userRepository.findByGmailAndIsDeletedFalse(email)
      await api.addOutingMember(outingSummary.outingId, newFriendEmail.trim());
      // Refresh real summary directly from database
      const updated = await api.getOutingSummary(outingSummary.outingId);
      setOutingSummary(updated);
      setIsAddFriendOpen(false);
      setNewFriendEmail('');
    } catch (err) {
      console.error("Error adding member:", err);
      setFriendError(err.message || 'No registered CineSync user found with this email.');
    } finally {
      setActionLoading(false);
    }
  };

  // Remove Member from Outing (Database operation)
  const handleRemoveFriend = async (memberUserId) => {
    if (!outingSummary) return;
    if (!confirm('Are you sure you want to remove this member from the split?')) return;

    setActionLoading(true);
    try {
      await api.removeOutingMember(outingSummary.outingId, memberUserId);
      const updated = await api.getOutingSummary(outingSummary.outingId);
      setOutingSummary(updated);
    } catch (err) {
      alert(err.message || 'Failed to remove member from database.');
    } finally {
      setActionLoading(false);
    }
  };

  // Add Real Expense to Outing (Database operation)
  const handleAddExpenseSubmit = async (e) => {
    e.preventDefault();
    if (!expenseAmount || !outingSummary) return;
    const amountNum = parseFloat(expenseAmount);
    if (isNaN(amountNum) || amountNum <= 0) return;

    setActionLoading(true);
    try {
      const payerId = expensePayerId ? Number(expensePayerId) : (user.id || user.userId);
      await api.addExpense(outingSummary.outingId, {
        paidByUserId: payerId,
        amount: amountNum,
        reason: expenseDesc.trim() || 'Popcorn / Snacks'
      });
      const updated = await api.getOutingSummary(outingSummary.outingId);
      setOutingSummary(updated);
      setIsExpenseModalOpen(false);
      setExpenseAmount('');
      setExpenseDesc('');
    } catch (err) {
      alert(err.message || 'Failed to add expense.');
    } finally {
      setActionLoading(false);
    }
  };

  // Delete Expense (Database operation)
  const handleRemoveExpense = async (expenseId) => {
    if (!outingSummary) return;
    if (!confirm('Delete this expense from the outing?')) return;

    setActionLoading(true);
    try {
      await api.removeExpense(outingSummary.outingId, expenseId);
      const updated = await api.getOutingSummary(outingSummary.outingId);
      setOutingSummary(updated);
    } catch (err) {
      alert(err.message || 'Failed to delete expense.');
    } finally {
      setActionLoading(false);
    }
  };

  // Settle Debt via Backend Settlement API (Database operation)
  const handleSettleDebt = async (debtInstruction) => {
    if (!outingSummary) return;
    setActionLoading(true);
    try {
      await api.recordSettlement(
        outingSummary.outingId,
        debtInstruction.fromUserId,
        debtInstruction.toUserId,
        debtInstruction.amount
      );
      confetti({ particleCount: 60, spread: 60, origin: { y: 0.7 } });
      const updated = await api.getOutingSummary(outingSummary.outingId);
      setOutingSummary(updated);
    } catch (err) {
      alert(err.message || 'Failed to record settlement in database.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCopyQuick = () => {
    const share = Math.round(quickTotal / (quickPeople || 1));
    const msg = `🍿 Movie Bill Split: Total ₹${quickTotal} split among ${quickPeople} friends = ₹${share} each.`;
    navigator.clipboard.writeText(msg);
    setCopiedQuick(true);
    setTimeout(() => setCopiedQuick(false), 2500);
  };

  const currentPoster = selectedItem ? (selectedItem.posterUrl || getPosterForMovie(selectedItem.movieTitle)) : getPosterForMovie('');
  const showTimeStr = selectedItem?.showTime 
    ? new Date(selectedItem.showTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) 
    : '';

  // Get current user's balance
  const currentUserId = user?.id || user?.userId;
  const myBalance = outingSummary?.memberBalances?.find(m => String(m.userId) === String(currentUserId)) || outingSummary?.memberBalances?.[0];
  const myNet = myBalance ? Number(myBalance.netBalance || 0) : 0;
  const isMeOrganizer = outingSummary ? String(outingSummary.createdByUserId) === String(currentUserId) : selectedItem?.isOrganizer;

  if (!user) {
    return (
      <div className="max-w-xl mx-auto px-4 py-24 text-center">
        <div className="w-16 h-16 rounded-2xl bg-primary-500/10 border border-primary-500/20 flex items-center justify-center mx-auto mb-4 text-primary-500">
          <Users className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">Split Movie Bills with Friends</h2>
        <p className="text-slate-300 text-sm mb-6">
          Sign in to access your CineSync outings, invite real friends by registered email, and track snacks.
        </p>
        <button
          onClick={onOpenAuth}
          className="px-6 py-3 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-primary-500/25"
        >
          Sign In to Open Split Bill
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
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

      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-6">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-2">
            <Users className="w-3.5 h-3.5" />
            <span>Social Cinema Ledger • PostgreSQL Real Users</span>
          </div>
          <h1 className="text-3xl md:text-4xl font-extrabold text-white tracking-tight">
            Movie Outings & Bill Split
          </h1>
          <p className="text-slate-300 text-sm mt-1 max-w-xl">
            Linked to your verified CineSync ticket bookings. Invite friends with their CineSync accounts to share costs and settle up.
          </p>
        </div>

        {/* Mode Toggle */}
        <div className="flex items-center bg-[#121622] border border-[#262E42] p-1 rounded-2xl">
          <button
            onClick={() => setActiveMode('trip')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 ${
              activeMode === 'trip'
                ? 'bg-primary-500 text-white shadow-md shadow-primary-500/20'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Film className="w-3.5 h-3.5" />
            <span>Movie Outings ({outingItems.length})</span>
          </button>
          <button
            onClick={() => setActiveMode('quick')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 ${
              activeMode === 'quick'
                ? 'bg-primary-500 text-white shadow-md shadow-primary-500/20'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Calculator className="w-3.5 h-3.5" />
            <span>Quick 10s Split</span>
          </button>
        </div>
      </div>

      {errorMessage && (
        <div className="p-4 mb-6 rounded-2xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-red-400 text-xs">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* ===================== MODE 1: QUICK CALCULATOR ===================== */}
      {activeMode === 'quick' && (
        <div className="bg-dark-card border border-dark-border rounded-3xl p-6 sm:p-8 max-w-2xl mx-auto shadow-2xl space-y-6">
          <div className="text-center space-y-1">
            <h2 className="text-xl font-bold text-white">Instant 10-Second Bill Split</h2>
            <p className="text-xs text-slate-400">Offline calculator to divide cinema expenses with friends.</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="bg-dark-bg/80 border border-dark-border p-4 rounded-2xl">
              <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                Total Bill (₹)
              </label>
              <input
                type="number"
                min="0"
                value={quickTotal}
                onChange={(e) => setQuickTotal(Number(e.target.value) || 0)}
                className="w-full bg-transparent text-2xl font-black text-white font-mono focus:outline-none"
              />
            </div>

            <div className="bg-dark-bg/80 border border-dark-border p-4 rounded-2xl">
              <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                Number of Friends
              </label>
              <input
                type="number"
                min="1"
                max="50"
                value={quickPeople}
                onChange={(e) => setQuickPeople(Math.max(1, Number(e.target.value) || 1))}
                className="w-full bg-transparent text-2xl font-black text-accent-cyan font-mono focus:outline-none"
              />
            </div>
          </div>

          {/* Quick Result Box */}
          <div className="bg-gradient-to-br from-primary-500/15 via-[#161C28] to-primary-500/5 border border-primary-500/30 rounded-2xl p-6 text-center space-y-2">
            <div className="text-xs font-bold text-primary-400 uppercase tracking-wider">
              Each Person Pays
            </div>
            <div className="text-4xl font-black text-white font-mono">
              ₹{Math.round(quickTotal / (quickPeople || 1))}
            </div>
            <p className="text-xs text-slate-400">
              ₹{quickTotal} split evenly across {quickPeople} {quickPeople === 1 ? 'person' : 'friends'}
            </p>
          </div>

          <button
            onClick={handleCopyQuick}
            className="w-full py-3 bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs rounded-xl border border-slate-700 transition-colors flex items-center justify-center gap-2"
          >
            {copiedQuick ? (
              <>
                <Check className="w-4 h-4 text-emerald-400" />
                <span className="text-emerald-400">Copied to Clipboard!</span>
              </>
            ) : (
              <>
                <Copy className="w-4 h-4 text-slate-400" />
                <span>Copy Summary for WhatsApp</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* ===================== MODE 2: REAL POSTGRESQL MOVIE SHOW SPLIT ===================== */}
      {activeMode === 'trip' && (
        <div className="space-y-6">
          {loading && (
            <LoadingBar 
              title="Querying CineSync PostgreSQL Database..." 
              subtitle="Fetching your bookings and shared squad outings" 
            />
          )}

          {!loading && outingItems.length === 0 && (
            <div className="bg-dark-card border border-dark-border rounded-3xl p-8 sm:p-12 text-center max-w-2xl mx-auto shadow-2xl space-y-5">
              <div className="w-16 h-16 rounded-2xl bg-primary-500/10 border border-primary-500/20 text-primary-400 flex items-center justify-center mx-auto text-2xl font-black">
                <Ticket className="w-8 h-8" />
              </div>
              <div>
                <h2 className="text-2xl font-bold text-white mb-2">No Bookings or Outings in Database</h2>
                <p className="text-slate-400 text-sm max-w-md mx-auto">
                  You haven't booked any movie tickets yet, and no friends have invited you to an outing. Book a show to start splitting, or give your email (<strong>{user?.gmail || user?.email}</strong>) to an organizer friend!
                </p>
              </div>

              <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
                <button
                  onClick={() => onNavigateTab ? onNavigateTab('movies') : null}
                  className="w-full sm:w-auto px-6 py-3 rounded-xl bg-primary-500 hover:bg-primary-600 text-white text-xs font-bold shadow-lg shadow-primary-500/25 transition-all flex items-center justify-center gap-2"
                >
                  <Film className="w-4 h-4" />
                  <span>Browse Now Showing</span>
                </button>
                <button
                  onClick={() => setActiveMode('quick')}
                  className="w-full sm:w-auto px-6 py-3 rounded-xl bg-[#161C28] hover:bg-[#1E2638] text-slate-300 border border-[#262E42] text-xs font-bold transition-all flex items-center justify-center gap-2"
                >
                  <Calculator className="w-4 h-4 text-accent-cyan" />
                  <span>Use Quick Calculator</span>
                </button>
              </div>
            </div>
          )}

          {!loading && outingItems.length > 0 && selectedItem && (
            <>
              {/* 1. VISUAL ANCHOR BANNER: ACTIVE OUTING OR BOOKING */}
              <div className="bg-gradient-to-r from-[#161C28] via-[#1A2234] to-[#161C28] border-2 border-primary-500/40 rounded-3xl p-5 sm:p-6 shadow-2xl relative overflow-hidden">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-5">
                  <div className="flex items-center gap-4">
                    <img 
                      src={currentPoster} 
                      alt={selectedItem.movieTitle} 
                      className="w-16 h-24 sm:w-20 sm:h-28 rounded-2xl object-cover border border-[#262E42] shadow-xl flex-shrink-0"
                    />
                    <div>
                      <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-primary-500/20 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-1.5">
                        <Film className="w-3 h-3" />
                        <span>
                          {selectedItem.type === 'OUTING' 
                            ? (isMeOrganizer ? 'Outing Organized by You' : `Shared Outing (Organizer: ${selectedItem.createdByName})`)
                            : 'Unsplit Ticket Booking'}
                        </span>
                      </div>
                      <h2 className="text-xl sm:text-2xl font-black text-white">
                        {selectedItem.movieTitle}
                      </h2>
                      <div className="flex flex-wrap items-center gap-y-1 gap-x-3 text-xs text-slate-300 mt-1.5">
                        {selectedItem.theatreName && (
                          <span className="flex items-center gap-1.5 font-semibold text-white">
                            <MapPin className="w-3.5 h-3.5 text-primary-500" />
                            {selectedItem.theatreName}
                          </span>
                        )}
                        {showTimeStr && (
                          <>
                            <span className="text-slate-500">•</span>
                            <span className="flex items-center gap-1 text-accent-cyan font-medium">
                              <Clock className="w-3.5 h-3.5" />
                              {showTimeStr}
                            </span>
                          </>
                        )}
                        {selectedItem.bookingReference && (
                          <>
                            <span className="text-slate-500">•</span>
                            <span className="text-slate-400 font-mono">
                              Ref #{selectedItem.bookingReference}
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Switch Outing / Booking Dropdown & Action Buttons */}
                  <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                    {outingItems.length > 1 && (
                      <div className="bg-[#121622] p-2 rounded-2xl border border-[#262E42]">
                        <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1 px-1">
                          Select Outing / Booking:
                        </label>
                        <select
                          value={selectedItem.key}
                          onChange={(e) => handleSelectItem(e.target.value)}
                          className="bg-transparent text-white text-xs font-semibold px-2 py-1 focus:outline-none cursor-pointer"
                        >
                          {outingItems.map((item) => (
                            <option key={item.key} value={item.key} className="bg-[#161C28]">
                              {item.type === 'OUTING' ? '🍿 Outing: ' : '🎟️ Ticket: '} 
                              {item.movieTitle} {item.bookingReference ? `(${item.bookingReference})` : ''}
                            </option>
                          ))}
                        </select>
                      </div>
                    )}

                    {outingSummary && (
                      <div className="flex gap-2">
                        <button
                          onClick={() => setIsExpenseModalOpen(true)}
                          className="px-3.5 py-2.5 bg-primary-500 hover:bg-primary-600 text-white font-bold text-xs rounded-xl shadow-md shadow-primary-500/20 transition-all flex items-center justify-center gap-1.5"
                        >
                          <Plus className="w-3.5 h-3.5" />
                          <span>Add Expense</span>
                        </button>
                        {isMeOrganizer && (
                          <button
                            onClick={() => setIsAddFriendOpen(true)}
                            className="px-3.5 py-2.5 bg-[#121622] hover:bg-[#1E2638] text-slate-300 font-semibold text-xs rounded-xl border border-[#262E42] transition-colors flex items-center justify-center gap-1.5"
                          >
                            <UserPlus className="w-3.5 h-3.5 text-accent-cyan" />
                            <span>Invite Friend</span>
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* IF SELECTED ITEM IS AN UNSPLIT BOOKING */}
              {selectedItem.type === 'UNSPLIT_BOOKING' && !outingSummary && (
                <div className="bg-dark-card border border-dark-border rounded-3xl p-8 sm:p-10 text-center shadow-xl space-y-4">
                  <div className="w-14 h-14 rounded-2xl bg-primary-500/10 border border-primary-500/20 text-primary-400 flex items-center justify-center mx-auto text-xl font-bold">
                    <Users className="w-7 h-7" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-white mb-1">
                      Start Movie Outing for "{selectedItem.movieTitle}"
                    </h3>
                    <p className="text-xs text-slate-400 max-w-md mx-auto">
                      Create an official group outing in the PostgreSQL database for this booking. Your ticket cost of ₹{selectedItem.totalAmountPaid} will automatically be logged as Expense #1.
                    </p>
                  </div>
                  <button
                    onClick={handleCreateOuting}
                    disabled={actionLoading}
                    className="px-6 py-3 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-xs shadow-lg shadow-primary-500/25 transition-all inline-flex items-center gap-2"
                  >
                    {actionLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                    <span>Create Database Outing Split</span>
                  </button>
                </div>
              )}

              {/* IF LIVE OUTING SUMMARY LOADED FROM BACKEND */}
              {outingSummary && (
                <>
                  {/* 2. THE 3 SUMMARY CARDS */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {/* 1. Total Group Spend */}
                    <div className="bg-dark-card border border-dark-border p-5 rounded-3xl shadow-lg">
                      <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        Total Outing Spend
                      </div>
                      <div className="text-3xl font-black text-white font-mono mt-1">
                        ₹{outingSummary.totalGroupSpend}
                      </div>
                      <p className="text-[11px] text-slate-400 mt-1">
                        Tickets & snacks across {outingSummary.totalMembers} registered {outingSummary.totalMembers === 1 ? 'member' : 'members'}
                      </p>
                    </div>

                    {/* 2. Fair Share */}
                    <div className="bg-dark-card border border-dark-border p-5 rounded-3xl shadow-lg">
                      <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        Fair Share Per Person
                      </div>
                      <div className="text-3xl font-black text-accent-cyan font-mono mt-1">
                        ₹{outingSummary.perPersonShare}
                      </div>
                      <p className="text-[11px] text-slate-400 mt-1">
                        ₹{outingSummary.totalGroupSpend} ÷ {outingSummary.totalMembers} equal mathematical share
                      </p>
                    </div>

                    {/* 3. Your Personal Balance */}
                    <div className="bg-dark-card border border-dark-border p-5 rounded-3xl shadow-lg">
                      <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        Your Personal Balance
                      </div>
                      <div className="mt-1">
                        {myNet > 0 ? (
                          <div>
                            <div className="text-3xl font-black text-emerald-400 font-mono">+₹{myNet}</div>
                            <p className="text-[11px] text-emerald-400 font-medium mt-1">You are owed money back</p>
                          </div>
                        ) : myNet < 0 ? (
                          <div>
                            <div className="text-3xl font-black text-amber-400 font-mono">-₹{Math.abs(myNet)}</div>
                            <p className="text-[11px] text-amber-400 font-medium mt-1">You need to pay your share</p>
                          </div>
                        ) : (
                          <div>
                            <div className="text-3xl font-black text-emerald-400 font-mono">₹0</div>
                            <p className="text-[11px] text-emerald-400 font-medium mt-1">All settled up!</p>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* PROMPT IF ONLY ORGANIZER IN OUTING */}
                  {outingSummary.totalMembers === 1 && isMeOrganizer && (
                    <div className="bg-primary-500/10 border border-primary-500/20 rounded-2xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3 text-center sm:text-left">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-primary-500/20 text-primary-400 flex items-center justify-center font-bold">
                          <Users className="w-5 h-5" />
                        </div>
                        <div>
                          <div className="text-sm font-bold text-white">Invite Real Friends by CineSync Email</div>
                          <div className="text-xs text-slate-400">
                            Add registered CineSync users (e.g. <strong>varun@gmail.com</strong>, <strong>akshit@gmail.com</strong>). They will see this outing on their own dashboard!
                          </div>
                        </div>
                      </div>
                      <button
                        onClick={() => setIsAddFriendOpen(true)}
                        className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white font-bold text-xs rounded-xl shadow-md transition-all flex items-center gap-1.5"
                      >
                        <UserPlus className="w-3.5 h-3.5" />
                        <span>Invite Friend</span>
                      </button>
                    </div>
                  )}

                  {/* 3. WHO PAYS WHOM (Greedy Debt Minimization from Database) */}
                  <div className="bg-dark-card border border-dark-border rounded-3xl p-6 sm:p-8 space-y-4 shadow-xl">
                    <div className="flex items-center justify-between">
                      <div>
                        <h2 className="text-lg font-bold text-white flex items-center gap-2">
                          <Sparkles className="w-4 h-4 text-primary-500" />
                          Who Pays Whom (Algorithm Settlement)
                        </h2>
                        <p className="text-xs text-slate-400 mt-0.5">
                          Greedy debt minimization computed by PostgreSQL backend for this outing.
                        </p>
                      </div>
                      {outingSummary.isAllSettled && (
                        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>All Debts Settled</span>
                        </div>
                      )}
                    </div>

                    <div className="space-y-3">
                      {outingSummary.debtInstructions?.length === 0 ? (
                        <div className="p-6 text-center text-slate-400 text-xs bg-dark-bg/40 rounded-2xl border border-dark-border/40">
                          {outingSummary.totalMembers === 1 
                            ? 'You are the only member in this outing right now. Click "Invite Friend" above to add registered CineSync users!' 
                            : 'All balances are settled! Nobody owes any money.'}
                        </div>
                      ) : (
                        outingSummary.debtInstructions.map((inst, idx) => (
                          <div
                            key={idx}
                            className="p-4 rounded-2xl border bg-dark-bg/80 border-dark-border/80 hover:border-primary-500/40 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                          >
                            <div className="flex items-center gap-3">
                              <div className="w-8 h-8 rounded-xl bg-primary-500/20 text-primary-400 flex items-center justify-center font-bold text-xs">
                                #{idx + 1}
                              </div>
                              <div>
                                <div className="text-sm font-bold text-white flex items-center gap-2">
                                  <span className="text-white font-semibold">{inst.fromUserName}</span>
                                  <span className="text-xs text-slate-400 font-normal">owes</span>
                                  <span className="text-primary-400 font-bold">{inst.toUserName}</span>
                                </div>
                                <div className="text-[11px] text-slate-400">
                                  Peer-to-peer real settlement
                                </div>
                              </div>
                            </div>

                            <div className="flex items-center justify-between sm:justify-end gap-4">
                              <span className="text-xl font-black text-white font-mono">₹{inst.amount}</span>
                              <button
                                onClick={() => handleSettleDebt(inst)}
                                disabled={actionLoading}
                                className="px-4 py-2 rounded-xl bg-primary-500 hover:bg-primary-600 text-white font-bold text-xs shadow-md shadow-primary-500/20 transition-all flex items-center gap-1.5"
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                <span>Record Paid</span>
                              </button>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>

                  {/* 4. TWO COLUMNS: REAL EXPENSES & REAL REGISTERED MEMBERS */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Column 1: Outing Expenses */}
                    <div className="bg-dark-card border border-dark-border rounded-3xl p-6 space-y-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="text-base font-bold text-white flex items-center gap-2">
                            <Receipt className="w-4 h-4 text-primary-500" />
                            Outing Expenses ({outingSummary.expenses?.length || 0})
                          </h3>
                          <p className="text-[11px] text-slate-400 mt-0.5">
                            Ticket booking and group snacks logged in PostgreSQL.
                          </p>
                        </div>
                        <button
                          onClick={() => setIsExpenseModalOpen(true)}
                          className="px-3 py-1 bg-primary-500/10 hover:bg-primary-500/20 text-primary-400 border border-primary-500/30 rounded-xl text-xs font-bold transition-colors flex items-center gap-1"
                        >
                          <Plus className="w-3 h-3" />
                          <span>Add</span>
                        </button>
                      </div>

                      <div className="space-y-2.5">
                        {outingSummary.expenses?.map((exp) => (
                          <div
                            key={exp.id}
                            className="p-3.5 rounded-2xl bg-dark-bg/60 border border-dark-border/60 flex items-center justify-between group"
                          >
                            <div>
                              <div className="text-sm font-bold text-white flex items-center gap-2">
                                <span>{exp.reason}</span>
                                {exp.isTicket && (
                                  <span className="text-[10px] font-bold bg-primary-500/20 text-primary-400 px-1.5 py-0.5 rounded border border-primary-500/30">
                                    Base Ticket
                                  </span>
                                )}
                              </div>
                              <div className="text-[11px] text-slate-400">
                                Paid by <strong className="text-primary-400">{exp.paidByUserName}</strong>
                              </div>
                            </div>
                            <div className="flex items-center gap-3">
                              <span className="text-base font-black text-white font-mono">
                                ₹{exp.amount}
                              </span>
                              {!exp.isTicket && isMeOrganizer && (
                                <button
                                  onClick={() => handleRemoveExpense(exp.id)}
                                  disabled={actionLoading}
                                  className="text-slate-500 hover:text-red-400 opacity-60 hover:opacity-100 transition-opacity p-1"
                                  title="Delete snack expense"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Column 2: Registered Members */}
                    <div className="bg-dark-card border border-dark-border rounded-3xl p-6 space-y-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="text-base font-bold text-white flex items-center gap-2">
                            <Users className="w-4 h-4 text-accent-cyan" />
                            Registered Members ({outingSummary.totalMembers})
                          </h3>
                          <p className="text-[11px] text-slate-400 mt-0.5">
                            CineSync users linked to this outing in database.
                          </p>
                        </div>
                        {isMeOrganizer && (
                          <button
                            onClick={() => setIsAddFriendOpen(true)}
                            className="px-3 py-1 bg-accent-cyan/10 hover:bg-accent-cyan/20 text-accent-cyan border border-accent-cyan/30 rounded-xl text-xs font-bold transition-colors flex items-center gap-1"
                          >
                            <Plus className="w-3 h-3" />
                            <span>Invite</span>
                          </button>
                        )}
                      </div>

                      <div className="space-y-2.5">
                        {outingSummary.memberBalances?.map((m) => {
                          const isMe = String(m.userId) === String(currentUserId);
                          return (
                            <div
                              key={m.userId}
                              className="p-3.5 rounded-2xl bg-dark-bg/60 border border-dark-border/60 flex items-center justify-between"
                            >
                              <div>
                                <div className="text-sm font-bold text-white flex items-center gap-1.5">
                                  {m.name}
                                  {m.isOrganizer && (
                                    <span className="text-[10px] font-bold text-amber-400 bg-amber-500/10 border border-amber-500/30 px-1.5 py-0.5 rounded">
                                      Organizer
                                    </span>
                                  )}
                                  {isMe && (
                                    <span className="text-[10px] font-bold text-primary-400 bg-primary-500/10 px-1.5 py-0.5 rounded">
                                      You
                                    </span>
                                  )}
                                </div>
                                <div className="text-[11px] text-slate-400">
                                  {m.email} • Paid: <strong className="text-white font-mono">₹{m.totalPaid}</strong>
                                </div>
                              </div>

                              <div className="flex items-center gap-3">
                                <div className="text-right">
                                  {Number(m.netBalance) > 0 ? (
                                    <span className="text-xs font-black text-emerald-400 font-mono">
                                      +₹{m.netBalance} (Gets)
                                    </span>
                                  ) : Number(m.netBalance) < 0 ? (
                                    <span className="text-xs font-black text-amber-400 font-mono">
                                      -₹{Math.abs(Number(m.netBalance))} (Owes)
                                    </span>
                                  ) : (
                                    <span className="text-xs font-bold text-slate-400 font-mono">
                                      Settled ✓
                                    </span>
                                  )}
                                </div>
                                {!m.isOrganizer && isMeOrganizer && (
                                  <button
                                    onClick={() => handleRemoveFriend(m.userId)}
                                    disabled={actionLoading}
                                    className="text-slate-500 hover:text-red-400 opacity-60 hover:opacity-100 transition-opacity p-1"
                                    title="Remove member"
                                  >
                                    <X className="w-3.5 h-3.5" />
                                  </button>
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                </>
              )}
            </>
          )}
        </div>
      )}

      {/* ===================== MODAL: ADD EXPENSE ===================== */}
      {isExpenseModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-dark-card border border-dark-border rounded-3xl p-6 shadow-2xl">
            <h2 className="text-xl font-bold text-white mb-1">Add Cinema Expense</h2>
            <p className="text-xs text-slate-400 mb-6">
              Log snacks, drinks, popcorn combos, or cab rides directly to PostgreSQL database.
            </p>

            <form onSubmit={handleAddExpenseSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Expense Description
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Popcorn Combo & Cold Drinks"
                  value={expenseDesc}
                  onChange={(e) => setExpenseDesc(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Total Cost (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="e.g. 550"
                  value={expenseAmount}
                  onChange={(e) => setExpenseAmount(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Who Paid For This?
                </label>
                <select
                  value={expensePayerId}
                  onChange={(e) => setExpensePayerId(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                >
                  {outingSummary?.memberBalances?.map((m) => (
                    <option key={m.userId} value={m.userId}>
                      {m.name} ({m.email})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsExpenseModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-5 py-2 rounded-xl bg-primary-500 hover:bg-primary-600 text-white text-xs font-bold shadow-lg shadow-primary-500/25 flex items-center gap-1.5"
                >
                  {actionLoading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : 'Log Expense'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ===================== MODAL: ADD REGISTERED FRIEND BY EMAIL ===================== */}
      {isAddFriendOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-dark-card border border-dark-border rounded-3xl p-6 shadow-2xl">
            <h2 className="text-xl font-bold text-white mb-1">Invite Friend to Movie Outing</h2>
            <p className="text-xs text-slate-400 mb-6">
              Enter your friend's registered CineSync email. The system verifies their account in PostgreSQL before adding them.
            </p>

            {friendError && (
              <div className="p-3 mb-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{friendError}</span>
              </div>
            )}

            <form onSubmit={handleAddFriendSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  CineSync Registered Email
                </label>
                <input
                  type="email"
                  required
                  placeholder="e.g. varun@gmail.com, akshit@gmail.com"
                  value={newFriendEmail}
                  onChange={(e) => setNewFriendEmail(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div className="p-3 rounded-xl bg-[#121622] border border-[#262E42] text-[11px] text-slate-400">
                🔒 <span className="font-semibold text-white">Strict Verification:</span> Unregistered emails will be rejected by the server. Your friend must register on CineSync before you can add them.
              </div>

              <div className="flex justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => {
                    setIsAddFriendOpen(false);
                    setFriendError('');
                  }}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-5 py-2 rounded-xl bg-primary-500 hover:bg-primary-600 text-white text-xs font-bold shadow-lg shadow-primary-500/25 flex items-center gap-1.5"
                >
                  {actionLoading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : 'Verify & Invite Member'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

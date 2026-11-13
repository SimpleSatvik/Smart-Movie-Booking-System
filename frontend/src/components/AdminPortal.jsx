import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, Film, Users, Building2, DollarSign, Plus, 
  Trash2, AlertCircle, CheckCircle2, RefreshCw, Search, Clock, RotateCcw, Archive, X
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { getMoviePoster, handleImageError } from '../utils/moviePosters';

export default function AdminPortal() {
  const { user } = useAuth();
  const [overview, setOverview] = useState(null);
  const [movies, setMovies] = useState([]);
  const [movieTab, setMovieTab] = useState('active'); // 'active' | 'archived'
  const [users, setUsers] = useState([]);
  const [userSearch, setUserSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successModal, setSuccessModal] = useState(null); // { title: string, message: string }
  const [confirmModal, setConfirmModal] = useState(null); // { title: string, message: string, onConfirm: () => void, confirmText?: string }

  // Add Movie Form
  const [isAddMovieOpen, setIsAddMovieOpen] = useState(false);
  const [movieForm, setMovieForm] = useState({
    title: '',
    genre: 'Sci-Fi',
    duration: 150,
    language: 'English',
    rating: 8.5,
    posterUrl: '',
    description: ''
  });
  const [submittingMovie, setSubmittingMovie] = useState(false);
  const [deletingMovieId, setDeletingMovieId] = useState(null);
  const [restoringMovieId, setRestoringMovieId] = useState(null);
  const [deletingUserId, setDeletingUserId] = useState(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [overviewData, movieList, userList] = await Promise.all([
        api.getAdminOverview().catch(() => null),
        api.getAdminMovies().catch(() => []),
        api.getAdminUsers().catch(() => [])
      ]);

      setOverview(overviewData || {
        totalUsers: 0,
        totalTheatres: 0,
        totalMovies: 0,
        totalBookings: 0,
        totalPlatformRevenue: 0
      });
      setMovies(Array.isArray(movieList) ? movieList : []);
      setUsers(Array.isArray(userList) ? userList : []);
    } catch (err) {
      console.error('Admin data fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateMovie = async (e) => {
    e.preventDefault();
    setSubmittingMovie(true);
    setError(null);

    const submitPayload = {
      title: movieForm.title,
      genre: movieForm.genre,
      duration: parseInt(movieForm.duration, 10),
      language: movieForm.language,
      rating: parseFloat(movieForm.rating),
      posterUrl: movieForm.posterUrl,
      description: movieForm.description
    };

    try {
      await api.createMovie(submitPayload);
      setSuccessModal({
        title: 'Movie Published',
        message: `"${movieForm.title}" was published successfully! It is now active across the CineSync network.`
      });
      setIsAddMovieOpen(false);
      setMovieForm({
        title: '',
        genre: 'Sci-Fi',
        duration: 150,
        language: 'English',
        rating: 8.5,
        posterUrl: '',
        description: ''
      });
      loadData();
    } catch (err) {
      setError(err.message || 'Failed to create movie.');
    } finally {
      setSubmittingMovie(false);
    }
  };

  const handleDeleteMovie = (movie) => {
    setConfirmModal({
      title: `Archive "${movie.title}"?`,
      message: `Are you sure you want to archive "${movie.title}"? The system will verify that all upcoming screenings have concluded first. Past bookings and receipts will remain intact.`,
      confirmText: 'Archive Movie',
      onConfirm: async () => {
        setConfirmModal(null);
        setDeletingMovieId(movie.id);
        setError(null);
        try {
          await api.deleteMovie(movie.id);
          setSuccessModal({
            title: 'Movie Archived',
            message: `"${movie.title}" has been safely archived. It will no longer appear in new screening schedules.`
          });
          loadData();
        } catch (err) {
          setError(err.message || 'Failed to delete movie.');
        } finally {
          setDeletingMovieId(null);
        }
      }
    });
  };

  const handleRestoreMovie = async (movie) => {
    setRestoringMovieId(movie.id);
    setError(null);
    try {
      await api.restoreMovie(movie.id);
      setSuccessModal({
        title: 'Movie Restored',
        message: `Movie "${movie.title}" has been restored to the active catalog and is now available for new screenings.`
      });
      loadData();
    } catch (err) {
      setError(err.message || 'Failed to restore movie.');
    } finally {
      setRestoringMovieId(null);
    }
  };

  const handleDeleteUser = (targetUser) => {
    const currentEmail = (user?.gmail || user?.email || '').toLowerCase();
    const targetEmail = (targetUser.email || targetUser.gmail || '').toLowerCase();

    if (currentEmail && targetEmail && currentEmail === targetEmail) {
      setError('Safety restriction: You cannot delete your own active Administrator account.');
      return;
    }

    const isOwner = targetUser.role && targetUser.role.toUpperCase().includes('OWNER');

    setConfirmModal({
      title: isOwner ? 'Deactivate Cinema Owner Account?' : 'Deactivate User Account?',
      message: isOwner
        ? `Are you sure you want to deactivate "${targetUser.name}" (${targetUser.email})? The system will strictly check that all active and upcoming shows across their cinema venues have concluded before allowing deactivation.`
        : `Are you sure you want to deactivate "${targetUser.name}" (${targetUser.email})? This will deactivate their login and free their email.`,
      confirmText: 'Deactivate User',
      onConfirm: async () => {
        setConfirmModal(null);
        setDeletingUserId(targetUser.id);
        setError(null);
        try {
          await api.deleteUser(targetUser.id);
          setSuccessModal({
            title: 'Account Deactivated',
            message: isOwner
              ? `Cinema Owner "${targetUser.name}" has been successfully deactivated. System verified 0 upcoming shows across their cinema venues.`
              : `User "${targetUser.name}" (${targetUser.email}) has been deactivated successfully.`
          });
          loadData();
        } catch (err) {
          setError(err.message || 'Failed to delete user.');
        } finally {
          setDeletingUserId(null);
        }
      }
    });
  };

  const activeMovies = movies.filter((m) => !m.isDeleted);
  const archivedMovies = movies.filter((m) => m.isDeleted);
  const displayedMovies = movieTab === 'archived' ? archivedMovies : activeMovies;

  const filteredUsers = users.filter((u) => {
    if (!userSearch) return true;
    const q = userSearch.toLowerCase();
    const nameMatch = (u.name || '').toLowerCase().includes(q);
    const emailMatch = (u.email || u.gmail || '').toLowerCase().includes(q);
    const roleMatch = (u.role || '').toLowerCase().includes(q);
    return nameMatch || emailMatch || roleMatch;
  });

  const getRoleBadge = (role) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return (
          <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-primary-500/15 text-primary-400 border border-primary-500/30">
            Platform Admin
          </span>
        );
      case 'ROLE_THEATRE_OWNER':
        return (
          <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-sky-500/15 text-sky-400 border border-sky-500/30">
            Cinema Owner
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
            Moviegoer
          </span>
        );
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Admin Portal</span>
          </div>
          <h1 className="text-3xl md:text-4xl font-extrabold text-white tracking-tight">
            CineSync Admin Portal
          </h1>
          <p className="text-slate-400 text-sm mt-1 max-w-xl">
            System metrics, movie catalog, and user accounts.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsAddMovieOpen(true)}
            className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-xs rounded-xl transition-all shadow-lg shadow-primary-500/25 inline-flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            Add Movie
          </button>
        </div>
      </div>

      {/* KPI Overview Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Network Revenue</span>
            <DollarSign className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-3xl font-black text-white font-mono">
            ₹{overview?.totalPlatformRevenue?.toLocaleString('en-IN') || '0'}
          </div>
          <div className="text-[11px] text-emerald-400 mt-1 font-medium">Aggregated across all venues</div>
        </div>

        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Registered Users</span>
            <Users className="w-4 h-4 text-primary-500" />
          </div>
          <div className="text-3xl font-black text-white font-mono">
            {overview?.totalUsers || users.length}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Active verified accounts</div>
        </div>

        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Multiplex Venues</span>
            <Building2 className="w-4 h-4 text-accent-cyan" />
          </div>
          <div className="text-3xl font-black text-accent-cyan font-mono">
            {overview?.totalTheatres || '0'}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Auditorium clusters</div>
        </div>

        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Catalog</span>
            <Film className="w-4 h-4 text-primary-400" />
          </div>
          <div className="text-3xl font-black text-white font-mono">
            {activeMovies.length}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">{archivedMovies.length} archived title(s)</div>
        </div>
      </div>

      {/* 1. Database Movie Catalog Management */}
      <div className="bg-dark-card border border-dark-border rounded-3xl p-6 md:p-8 mb-8 shadow-xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-6 border-b border-dark-border/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-primary-500/10 flex items-center justify-center text-primary-400">
              <Film className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Movie Catalog</h3>
              <p className="text-xs text-slate-400">
                Manage titles, view archival status, and safely soft-delete once all screenings conclude.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Filter Toggle: Active vs Archived */}
            <div className="flex items-center bg-dark-bg p-1 rounded-xl border border-dark-border text-xs">
              <button
                onClick={() => setMovieTab('active')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                  movieTab === 'active' 
                    ? 'bg-primary-500 text-white shadow-sm' 
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Active ({activeMovies.length})
              </button>
              <button
                onClick={() => setMovieTab('archived')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                  movieTab === 'archived' 
                    ? 'bg-primary-500 text-white shadow-sm' 
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Archived ({archivedMovies.length})
              </button>
            </div>

            <button
              type="button"
              onClick={() => setIsAddMovieOpen(true)}
              className="px-4 py-2 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-xs rounded-xl transition-all flex items-center gap-1.5 shadow-md shadow-primary-500/20"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Movie
            </button>
          </div>
        </div>

        {displayedMovies.length === 0 ? (
          <div className="text-center py-10 bg-dark-bg/60 rounded-2xl border border-dark-border/60">
            <Film className="w-8 h-8 text-slate-500 mx-auto mb-2 opacity-60" />
            <p className="text-sm font-semibold text-white">
              {movieTab === 'archived' ? 'No Archived Movies' : 'No Active Movies in Catalog'}
            </p>
            <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
              {movieTab === 'archived' 
                ? 'Soft-deleted movies will appear here for historical auditing and one-click restoration.' 
                : 'Click "+ Add Movie" above to publish your first motion picture into the catalog.'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {displayedMovies.map((m) => (
              <div 
                key={m.id}
                className={`bg-dark-bg/80 border rounded-2xl overflow-hidden p-3 flex flex-col justify-between group transition-colors ${
                  m.isDeleted ? 'border-amber-500/30 opacity-75' : 'border-dark-border hover:border-primary-500/40'
                }`}
              >
                <div className="flex gap-3 items-center">
                  <img 
                    src={getMoviePoster(m)} 
                    alt={m.title} 
                    onError={(e) => handleImageError(e, m.title)}
                    className="w-12 h-16 object-cover rounded-xl flex-shrink-0 bg-slate-900" 
                  />
                  <div className="overflow-hidden flex-1">
                    <div className="flex items-center gap-1.5">
                      <h4 className="text-xs font-bold text-white truncate" title={m.title}>{m.title}</h4>
                      {m.isDeleted && (
                        <span className="px-1.5 py-0.2 rounded text-[9px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 flex-shrink-0">
                          Archived
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-slate-400 mt-0.5 truncate">{m.genre}</p>
                    <p className="text-[10px] text-primary-400 font-mono mt-1">ID #{m.id} • {m.duration}m</p>
                  </div>
                </div>

                <div className="mt-3 pt-2.5 border-t border-dark-border/60 flex items-center justify-between">
                  <span className="text-[10px] text-slate-400 font-mono">
                    {m.language || 'English'}
                  </span>

                  {m.isDeleted ? (
                    <button
                      onClick={() => handleRestoreMovie(m)}
                      disabled={restoringMovieId === m.id}
                      className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center gap-1 transition-all"
                      title={`Restore ${m.title} to active catalog`}
                    >
                      {restoringMovieId === m.id ? (
                        <RefreshCw className="w-3 h-3 animate-spin" />
                      ) : (
                        <RotateCcw className="w-3 h-3" />
                      )}
                      <span>Restore</span>
                    </button>
                  ) : (
                    <button
                      onClick={() => handleDeleteMovie(m)}
                      disabled={deletingMovieId === m.id}
                      className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 flex items-center gap-1 transition-all"
                      title={`Safely archive ${m.title}`}
                    >
                      {deletingMovieId === m.id ? (
                        <RefreshCw className="w-3 h-3 animate-spin" />
                      ) : (
                        <Trash2 className="w-3 h-3" />
                      )}
                      <span>Archive</span>
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 2. User Accounts & Access Management */}
      <div className="bg-dark-card border border-dark-border rounded-3xl p-6 md:p-8 mb-8 shadow-xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-6 border-b border-dark-border/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-sky-500/10 flex items-center justify-center text-sky-400">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Registered User Accounts</h3>
              <p className="text-xs text-slate-400">
                View all registered accounts, identify privileges, and deactivate users.
              </p>
            </div>
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search user by name or email..."
              value={userSearch}
              onChange={(e) => setUserSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-xs text-white placeholder-slate-500"
            />
          </div>
        </div>

        {filteredUsers.length === 0 ? (
          <div className="text-center py-10 bg-dark-bg/60 rounded-2xl border border-dark-border/60">
            <Users className="w-8 h-8 text-slate-500 mx-auto mb-2 opacity-60" />
            <p className="text-sm font-semibold text-white">No Users Found</p>
            <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
              {userSearch ? 'No registered accounts match your search filter.' : 'No registered users in the platform.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-dark-border/60 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">User</th>
                  <th className="py-3 px-4">Role</th>
                  <th className="py-3 px-4">Account ID</th>
                  <th className="py-3 px-4">Joined Date</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-dark-border/40 text-xs">
                {filteredUsers.map((u) => {
                  const currentEmail = (user?.gmail || user?.email || '').toLowerCase();
                  const targetEmail = (u.email || u.gmail || '').toLowerCase();
                  const isCurrentAdmin = currentEmail && targetEmail && currentEmail === targetEmail;

                  return (
                    <tr key={u.id} className="hover:bg-dark-bg/40 transition-colors">
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-primary-500 to-amber-400 text-white font-bold flex items-center justify-center text-xs flex-shrink-0 shadow-sm">
                            {u.name ? u.name.charAt(0).toUpperCase() : 'U'}
                          </div>
                          <div>
                            <div className="font-bold text-white flex items-center gap-1.5">
                              <span>{u.name || 'User'}</span>
                              {isCurrentAdmin && (
                                <span className="text-[10px] text-primary-400 font-semibold bg-primary-500/10 px-1.5 py-0.2 rounded">
                                  You
                                </span>
                              )}
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono">{u.email || u.gmail}</div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        {getRoleBadge(u.role)}
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-300">
                        #{u.id}
                      </td>
                      <td className="py-3 px-4 text-slate-400">
                        {u.createdAt ? (
                          <div className="flex items-center gap-1 text-[11px]">
                            <Clock className="w-3 h-3 text-slate-500" />
                            <span>
                              {new Date(u.createdAt).toLocaleDateString('en-IN', {
                                month: 'short',
                                day: 'numeric',
                                year: 'numeric'
                              })}
                            </span>
                          </div>
                        ) : (
                          <span className="text-slate-500">Active</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        {isCurrentAdmin ? (
                          <span className="text-[10px] font-semibold text-slate-500 italic">
                            Active Session
                          </span>
                        ) : (
                          <button
                            onClick={() => handleDeleteUser(u)}
                            disabled={deletingUserId === u.id}
                            className="px-3 py-1.5 rounded-xl bg-red-500/15 hover:bg-red-500/25 border border-red-500/30 text-red-400 font-bold text-xs inline-flex items-center gap-1.5 transition-all shadow-sm"
                          >
                            {deletingUserId === u.id ? (
                              <RefreshCw className="w-3 h-3 animate-spin" />
                            ) : (
                              <Trash2 className="w-3 h-3" />
                            )}
                            <span>Delete User</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal: Add Movie */}
      {isAddMovieOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-lg bg-dark-card border border-dark-border rounded-3xl p-6 shadow-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold text-white mb-2">Publish Movie to Catalog</h2>
            <p className="text-xs text-slate-400 mb-6">
              Enter movie information to register a title across the CineSync network.
            </p>

            <form onSubmit={handleCreateMovie} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Movie Title
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Gladiator II"
                  value={movieForm.title}
                  onChange={(e) => setMovieForm({ ...movieForm, title: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Genre
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Action, Sci-Fi"
                    value={movieForm.genre}
                    onChange={(e) => setMovieForm({ ...movieForm, genre: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Duration (Minutes)
                  </label>
                  <input
                    type="number"
                    required
                    placeholder="150"
                    value={movieForm.duration}
                    onChange={(e) => setMovieForm({ ...movieForm, duration: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Language
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. English, Hindi"
                    value={movieForm.language}
                    onChange={(e) => setMovieForm({ ...movieForm, language: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    IMDb Rating
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    max="10"
                    value={movieForm.rating}
                    onChange={(e) => setMovieForm({ ...movieForm, rating: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Poster Image URL
                </label>
                <input
                  type="url"
                  placeholder="https://images.unsplash.com/..."
                  value={movieForm.posterUrl}
                  onChange={(e) => setMovieForm({ ...movieForm, posterUrl: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Synopsis / Description
                </label>
                <textarea
                  rows={3}
                  placeholder="Brief synopsis..."
                  value={movieForm.description}
                  onChange={(e) => setMovieForm({ ...movieForm, description: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-dark-border">
                <button
                  type="button"
                  onClick={() => setIsAddMovieOpen(false)}
                  className="px-4 py-2.5 rounded-xl bg-dark-bg hover:bg-slate-800 text-slate-300 text-xs font-bold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingMovie}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white text-xs font-bold transition-all flex items-center gap-2 shadow-lg shadow-primary-500/25"
                >
                  {submittingMovie ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      <span>Publishing...</span>
                    </>
                  ) : (
                    <span>Publish Movie</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Pop-up Modal */}
      {confirmModal && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
          onClick={(e) => {
            if (e.target === e.currentTarget) setConfirmModal(null);
          }}
        >
          <div className="w-full max-w-md bg-dark-card border border-dark-border rounded-3xl p-6 shadow-2xl relative overflow-hidden animate-in zoom-in-95 duration-200">
            <button
              onClick={() => setConfirmModal(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-white transition-colors p-1.5 rounded-xl hover:bg-white/5"
              aria-label="Close confirmation dialog"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="w-12 h-12 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 mb-4 shadow-inner">
              <AlertCircle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-white mb-2">
              {confirmModal.title}
            </h3>
            
            <p className="text-sm text-slate-300 leading-relaxed mb-6">
              {confirmModal.message}
            </p>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setConfirmModal(null)}
                className="flex-1 py-3 px-4 rounded-xl bg-dark-bg hover:bg-slate-800 text-slate-300 font-bold text-xs uppercase tracking-wider transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={confirmModal.onConfirm}
                className="flex-1 py-3 px-4 rounded-xl bg-gradient-to-r from-rose-500 to-rose-600 hover:from-rose-600 hover:to-rose-700 text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-rose-500/20 transition-all flex items-center justify-center cursor-pointer"
              >
                {confirmModal.confirmText || 'Confirm'}
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
              aria-label="Close notification"
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
      {error && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
          onClick={(e) => {
            if (e.target === e.currentTarget) setError(null);
          }}
        >
          <div className="w-full max-w-md bg-dark-card border border-rose-500/40 rounded-3xl p-6 shadow-2xl relative overflow-hidden animate-in zoom-in-95 duration-200">
            {/* Ambient background glow */}
            <div className="absolute -top-16 -right-16 w-32 h-32 bg-rose-500/15 rounded-full blur-2xl pointer-events-none" />

            <button
              onClick={() => setError(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-white transition-colors p-1.5 rounded-xl hover:bg-white/5"
              aria-label="Close error dialog"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="w-12 h-12 rounded-2xl bg-rose-500/15 border border-rose-500/30 flex items-center justify-center text-rose-400 mb-4 shadow-inner">
              <AlertCircle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-white mb-2">
              Action Blocked
            </h3>
            
            <p className="text-sm text-slate-300 leading-relaxed mb-6">
              {error}
            </p>

            <button
              onClick={() => setError(null)}
              className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-rose-500 to-primary-600 hover:from-rose-600 hover:to-primary-700 text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-rose-500/20 transition-all flex items-center justify-center cursor-pointer"
            >
              Understood
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

import React, { useState, useEffect } from 'react';
import { 
  Building2, Film, Plus, TrendingUp, DollarSign, Users, 
  Calendar, Clock, ShieldCheck, CheckCircle2, AlertCircle, RefreshCw, BarChart3
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function OwnerPortal() {
  const { user } = useAuth();
  const [theatres, setTheatres] = useState([]);
  const [movies, setMovies] = useState([]);
  const [selectedTheatre, setSelectedTheatre] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  // Forms
  const [isAddTheatreOpen, setIsAddTheatreOpen] = useState(false);
  const [theatreForm, setTheatreForm] = useState({ name: '', city: '', totalScreens: 4 });
  const [submittingTheatre, setSubmittingTheatre] = useState(false);

  const [isScheduleShowOpen, setIsScheduleShowOpen] = useState(false);
  const [theatreScreens, setTheatreScreens] = useState([]);
  const [loadingScreens, setLoadingScreens] = useState(false);
  const [theatreShows, setTheatreShows] = useState([]);
  const [loadingShows, setLoadingShows] = useState(false);
  const [showForm, setShowForm] = useState({
    movieId: '',
    theatreId: '',
    screenId: '',
    startTime: '',
    silverPrice: 200,
    goldPrice: 350,
    platinumPrice: 500
  });
  const [submittingShow, setSubmittingShow] = useState(false);

  const loadShowsForTheatre = async (theatreId) => {
    if (!theatreId) {
      setTheatreShows([]);
      return;
    }
    setLoadingShows(true);
    try {
      const shows = await api.getShowsByTheatre(theatreId);
      setTheatreShows(Array.isArray(shows) ? shows : []);
    } catch (err) {
      console.warn('Could not load shows for theatre:', err.message);
      setTheatreShows([]);
    } finally {
      setLoadingShows(false);
    }
  };

  const loadScreensForTheatre = async (theatreId) => {
    if (!theatreId) {
      setTheatreScreens([]);
      setShowForm(prev => ({ ...prev, screenId: '' }));
      return;
    }
    setLoadingScreens(true);
    try {
      const screens = await api.getTheatreScreens(theatreId);
      const list = Array.isArray(screens) ? screens : [];
      setTheatreScreens(list);
      if (list.length > 0) {
        setShowForm(prev => ({
          ...prev,
          theatreId: String(theatreId),
          screenId: prev.screenId && list.some(s => String(s.screenId) === String(prev.screenId))
                    ? prev.screenId
                    : String(list[0].screenId)
        }));
      } else {
        setShowForm(prev => ({ ...prev, theatreId: String(theatreId), screenId: '' }));
      }
    } catch (err) {
      console.warn('Could not load screens for theatre:', err.message);
      setTheatreScreens([]);
      setShowForm(prev => ({ ...prev, theatreId: String(theatreId), screenId: '' }));
    } finally {
      setLoadingScreens(false);
    }
  };

  const openScheduleShowModal = () => {
    const targetTheatreId = selectedTheatre?.id || (theatres.length > 0 ? theatres[0].id : '');
    const firstMovieId = movies.length > 0 ? String(movies[0].id) : '';
    setShowForm({
      movieId: firstMovieId,
      theatreId: String(targetTheatreId || ''),
      screenId: '',
      startTime: '',
      silverPrice: 200,
      goldPrice: 350,
      platinumPrice: 500
    });
    if (targetTheatreId) {
      loadScreensForTheatre(targetTheatreId);
    }
    setIsScheduleShowOpen(true);
  };

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      // ONLY load theatres belonging to the authenticated owner
      let theatreList = [];
      try {
        theatreList = await api.getMyTheatres();
      } catch (err) {
        console.warn('Could not load owner theatres:', err.message);
        theatreList = [];
      }

      const movieList = await api.getMovies().catch(() => []);

      const tList = Array.isArray(theatreList) ? theatreList : [];
      const mList = Array.isArray(movieList) ? movieList : [];

      setTheatres(tList);
      setMovies(mList);

      if (tList.length > 0) {
        setSelectedTheatre(tList[0]);
        loadAnalytics(tList[0].id);
        loadShowsForTheatre(tList[0].id);
      } else {
        setSelectedTheatre(null);
        setTheatreShows([]);
        setAnalytics({
          theatreName: '',
          totalShowsScheduled: 0,
          totalSeatsBooked: 0,
          occupancyRate: 0,
          totalRevenue: 0
        });
      }
    } catch (err) {
      console.error('Data load error:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadAnalytics = async (theatreId) => {
    if (!theatreId) {
      setAnalytics({
        theatreName: '',
        totalShowsScheduled: 0,
        totalSeatsBooked: 0,
        occupancyRate: 0,
        totalRevenue: 0
      });
      return;
    }
    try {
      const data = await api.getTheatreAnalytics(theatreId);
      setAnalytics(data);
    } catch (err) {
      setAnalytics({
        theatreName: selectedTheatre?.name || '',
        totalShowsScheduled: 0,
        totalSeatsBooked: 0,
        occupancyRate: 0,
        totalRevenue: 0
      });
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateTheatre = async (e) => {
    e.preventDefault();
    setSubmittingTheatre(true);
    setError(null);
    try {
      const cityVal = theatreForm.city?.trim() || '';
      await api.createTheatre({
        name: theatreForm.name?.trim(),
        location: cityVal,
        city: cityVal,
        totalScreens: parseInt(theatreForm.totalScreens, 10) || 1
      });
      setMessage('New theatre multiplex registered successfully!');
      setIsAddTheatreOpen(false);
      setTheatreForm({ name: '', city: '', totalScreens: 4 });
      loadData();
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setError(err.message || 'Failed to create theatre.');
    } finally {
      setSubmittingTheatre(false);
    }
  };

  const handleScheduleShow = async (e) => {
    e.preventDefault();
    if (!showForm.theatreId) {
      setError('Please select a target theatre for this show.');
      return;
    }
    if (!showForm.screenId) {
      setError('Please select an auditorium screen for this show.');
      return;
    }
    setSubmittingShow(true);
    setError(null);
    try {
      const cleanStartTime = showForm.startTime.length === 16 
        ? `${showForm.startTime}:00` 
        : showForm.startTime;

      await api.createShow({
        movieId: parseInt(showForm.movieId, 10),
        theatreId: parseInt(showForm.theatreId, 10),
        screenId: parseInt(showForm.screenId, 10),
        startTime: cleanStartTime,
        silverPrice: parseFloat(showForm.silverPrice),
        goldPrice: parseFloat(showForm.goldPrice),
        platinumPrice: parseFloat(showForm.platinumPrice),
      });
      setMessage('Show screening scheduled and 30 tier seats populated!');
      setIsScheduleShowOpen(false);
      const targetId = showForm.theatreId || selectedTheatre?.id;
      if (targetId) {
        const found = theatres.find(t => String(t.id) === String(targetId));
        if (found) setSelectedTheatre(found);
        loadAnalytics(targetId);
        loadShowsForTheatre(targetId);
      }
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setError(err.message || 'Failed to schedule show.');
    } finally {
      setSubmittingShow(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-2">
            <Building2 className="w-3.5 h-3.5" />
            <span>Exhibitor Ops</span>
          </div>
          <h1 className="text-3xl md:text-4xl font-extrabold text-white tracking-tight">
            Theatre Owner Dashboard
          </h1>
          <p className="text-slate-400 text-sm mt-1 max-w-xl">
            Register cinema venues, configure auditorium layouts, schedule showings with tiered pricing, and track live occupancy.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsAddTheatreOpen(true)}
            className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs rounded-xl border border-slate-700 transition-all inline-flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4 text-primary-500" />
            Add Theatre
          </button>
          <button
            onClick={openScheduleShowModal}
            className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-xs rounded-xl transition-all shadow-lg shadow-primary-500/25 inline-flex items-center gap-1.5"
          >
            <Calendar className="w-4 h-4" />
            Schedule Show
          </button>
        </div>
      </div>

      {message && (
        <div className="mb-6 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
          <span>{message}</span>
        </div>
      )}

      {error && (
        <div className="mb-6 p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-400 text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Theatres Select Bar */}
      {theatres.length > 0 ? (
        <div className="mb-6 flex gap-2 overflow-x-auto pb-2 scrollbar-none">
          {theatres.map((t) => (
            <button
              key={t.id}
              onClick={() => {
                setSelectedTheatre(t);
                loadAnalytics(t.id);
                loadShowsForTheatre(t.id);
              }}
              className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition-all border whitespace-nowrap flex items-center gap-2 ${
                selectedTheatre?.id === t.id
                  ? 'bg-primary-500 text-white border-primary-400 shadow-md shadow-primary-500/20'
                  : 'bg-dark-card border-dark-border text-slate-400 hover:text-white'
              }`}
            >
              <Building2 className="w-3.5 h-3.5" />
              <span>{t.name}</span>
              <span className="text-[10px] opacity-75">({t.city || t.location})</span>
            </button>
          ))}
        </div>
      ) : (
        <div className="bg-dark-card border border-dark-border rounded-3xl p-8 text-center mb-8">
          <Building2 className="w-10 h-10 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-bold text-white mb-1">No Theatres Registered Yet</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto mb-4">
            You haven't registered any cinema multiplexes under this owner account yet. Click "Add Theatre" above to configure your screens and schedule shows.
          </p>
          <button
            onClick={() => setIsAddTheatreOpen(true)}
            className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white font-bold text-xs rounded-xl shadow-lg shadow-primary-500/25 inline-flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            <span>Add Your First Theatre</span>
          </button>
        </div>
      )}

      {/* Analytics KPI Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Gross Box Office</span>
            <DollarSign className="w-4 h-4 text-primary-500" />
          </div>
          <div className="text-3xl font-black text-white font-mono">
            ₹{analytics?.totalRevenue ? analytics.totalRevenue.toLocaleString('en-IN') : '0'}
          </div>
          <div className="text-[11px] text-emerald-400 mt-1 font-medium">Real-time ledger aggregation</div>
        </div>

        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Live Occupancy</span>
            <BarChart3 className="w-4 h-4 text-accent-cyan" />
          </div>
          <div className="text-3xl font-black text-accent-cyan font-mono">
            {analytics?.occupancyRate || '0.0'}%
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Across all auditorium screens</div>
        </div>

        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Tickets Sold</span>
            <Users className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-3xl font-black text-white font-mono">
            {(analytics?.totalTicketsSold ?? analytics?.totalSeatsBooked ?? 0)}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Zero concurrency lock-outs</div>
        </div>

        <div className="bg-dark-card border border-dark-border p-5 rounded-3xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Shows</span>
            <Film className="w-4 h-4 text-primary-400" />
          </div>
          <div className="text-3xl font-black text-white font-mono">
            {(analytics?.totalShows ?? analytics?.totalShowsScheduled ?? 0)}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Scheduled for this week</div>
        </div>
      </div>

      {/* Scheduled Shows Section for Selected Theatre */}
      {selectedTheatre && (
        <div className="mb-12">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <Film className="w-5 h-5 text-primary-400" />
                <span>Scheduled Screenings</span>
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-primary-500/10 text-primary-400 border border-primary-500/30 font-mono">
                  {theatreShows.length} Shows
                </span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Screenings currently active at {selectedTheatre.name} ({selectedTheatre.city || selectedTheatre.location})
              </p>
            </div>
            <button
              onClick={openScheduleShowModal}
              className="px-3.5 py-1.5 rounded-xl bg-primary-500/10 hover:bg-primary-500/20 text-primary-400 border border-primary-500/30 text-xs font-bold transition-all flex items-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add Showtime</span>
            </button>
          </div>

          {loadingShows ? (
            <div className="p-8 text-center bg-dark-card border border-dark-border rounded-3xl">
              <RefreshCw className="w-6 h-6 animate-spin text-primary-500 mx-auto mb-2" />
              <p className="text-xs text-slate-400 font-mono">Loading showtimes...</p>
            </div>
          ) : theatreShows.length === 0 ? (
            <div className="p-8 text-center bg-dark-card border border-dark-border rounded-3xl">
              <Film className="w-8 h-8 text-slate-600 mx-auto mb-2" />
              <p className="text-sm font-semibold text-white">No Showtimes Scheduled Yet</p>
              <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto mb-4">
                You haven't scheduled any screenings at this cinema. Click "Schedule Show" to publish your first showtime.
              </p>
              <button
                onClick={openScheduleShowModal}
                className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white font-bold text-xs rounded-xl shadow-lg shadow-primary-500/25 inline-flex items-center gap-1.5"
              >
                <Plus className="w-4 h-4" />
                <span>Schedule First Show</span>
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {theatreShows.map(show => (
                <div
                  key={show.showId || show.id}
                  className="bg-dark-card border border-dark-border hover:border-primary-500/40 rounded-3xl p-5 transition-all group"
                >
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div>
                      <span className="text-[10px] font-mono uppercase tracking-wider px-2 py-0.5 rounded-md bg-sky-500/10 text-sky-400 border border-sky-500/20">
                        {show.screenName || 'Auditorium'}
                      </span>
                      <h3 className="text-base font-bold text-white mt-1 group-hover:text-primary-400 transition-colors">
                        {show.movieTitle || 'Canonical Movie'}
                      </h3>
                      <p className="text-[11px] text-slate-400">{show.movieGenre} • {show.movieLanguage || 'English'}</p>
                    </div>
                    <span className="px-2 py-1 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-mono font-bold">
                      30 Seats
                    </span>
                  </div>

                  <div className="pt-3 border-t border-dark-border/60 flex items-center justify-between text-xs">
                    <div className="text-slate-300 font-mono">
                      {(() => {
                        try {
                          return new Date(show.startTime).toLocaleString('en-IN', {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                            hour12: true
                          });
                        } catch (e) {
                          return show.startTime;
                        }
                      })()}
                    </div>
                    <div className="text-[11px] text-slate-400">
                      Tier Pricing: <span className="text-white font-semibold font-mono">₹{show.silverPrice || 200}+</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Modal: Add Theatre */}
      {isAddTheatreOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-dark-card border border-dark-border rounded-3xl p-6 shadow-2xl">
            <h2 className="text-xl font-bold text-white mb-2">Register New Multiplex</h2>
            <p className="text-xs text-slate-400 mb-6">
              Add your theatre details to list screenings on the CineSync consumer discovery network.
            </p>

            <form onSubmit={handleCreateTheatre} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Multiplex / Cinema Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. CineSync IMAX Pavilion"
                  value={theatreForm.name}
                  onChange={(e) => setTheatreForm({ ...theatreForm, name: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  City / Location
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Mumbai"
                  value={theatreForm.city}
                  onChange={(e) => setTheatreForm({ ...theatreForm, city: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Total Screens
                </label>
                <input
                  type="number"
                  min="1"
                  max="20"
                  required
                  value={theatreForm.totalScreens}
                  onChange={(e) => setTheatreForm({ ...theatreForm, totalScreens: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsAddTheatreOpen(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingTheatre}
                  className="px-5 py-2 rounded-xl bg-primary-500 hover:bg-primary-600 text-white text-xs font-bold shadow-lg shadow-primary-500/25 flex items-center gap-1.5"
                >
                  {submittingTheatre ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : 'Register Venue'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Schedule Show */}
      {isScheduleShowOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-lg bg-dark-card border border-dark-border rounded-3xl p-6 shadow-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold text-white mb-2">Schedule Showtime</h2>
            <p className="text-xs text-slate-400 mb-6">
              Pick a movie, set the screening timestamp, and customize tier pricing. 30 physical seats (A1-A10 Silver, B1-B10 Gold, C1-C10 Platinum) are provisioned immediately.
            </p>

            <form onSubmit={handleScheduleShow} className="space-y-4">
              {/* Select Target Theatre */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 flex items-center justify-between">
                  <span>Target Multiplex / Theatre</span>
                  {showForm.theatreId && (
                    <span className="text-[10px] text-primary-400 font-mono">
                      Venue ID: #{showForm.theatreId}
                    </span>
                  )}
                </label>
                <select
                  required
                  value={showForm.theatreId}
                  onChange={(e) => {
                    const tId = e.target.value;
                    setShowForm(prev => ({ ...prev, theatreId: tId, screenId: '' }));
                    loadScreensForTheatre(tId);
                  }}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                >
                  <option value="">-- Choose Target Cinema --</option>
                  {theatres.map(t => (
                    <option key={t.id} value={t.id}>
                      {t.name} ({t.city || t.location}) — {t.totalScreens} screens
                    </option>
                  ))}
                </select>
              </div>

              {/* Select Movie */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Select Movie
                </label>
                <select
                  required
                  value={showForm.movieId}
                  onChange={(e) => setShowForm({ ...showForm, movieId: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                >
                  <option value="">-- Choose Canonical Title --</option>
                  {movies.map(m => (
                    <option key={m.id} value={m.id}>{m.title} ({m.genre})</option>
                  ))}
                </select>
              </div>

              {/* Select Exact Auditorium Screen */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 flex items-center justify-between">
                  <span>Auditorium Screen</span>
                  {loadingScreens && (
                    <span className="text-[10px] text-slate-400 flex items-center gap-1">
                      <RefreshCw className="w-3 h-3 animate-spin text-primary-400" /> Fetching screens...
                    </span>
                  )}
                </label>
                <select
                  required
                  disabled={loadingScreens || theatreScreens.length === 0}
                  value={showForm.screenId}
                  onChange={(e) => setShowForm({ ...showForm, screenId: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm disabled:opacity-50"
                >
                  {theatres.length === 0 ? (
                    <option value="">Register a venue first to create screens</option>
                  ) : !showForm.theatreId ? (
                    <option value="">First choose a target multiplex above</option>
                  ) : theatreScreens.length === 0 ? (
                    <option value="">No screens found for this venue</option>
                  ) : (
                    <>
                      <option value="">-- Choose Screen --</option>
                      {theatreScreens.map(s => (
                        <option key={s.screenId} value={s.screenId}>
                          {s.screenName} ({s.totalSeats} seats) — [Screen ID #{s.screenId}]
                        </option>
                      ))}
                    </>
                  )}
                </select>
                {showForm.screenId && theatreScreens.length > 0 && (
                  <p className="text-[11px] text-emerald-400 mt-1 flex items-center gap-1 font-mono">
                    <CheckCircle2 className="w-3 h-3" />
                    Screen ID #{showForm.screenId} selected (30 physical seats: Row A Silver, B Gold, C Platinum)
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Start Date & Time
                </label>
                <input
                  type="datetime-local"
                  required
                  value={showForm.startTime}
                  onChange={(e) => setShowForm({ ...showForm, startTime: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
              </div>

              <div className="grid grid-cols-3 gap-3 pt-2">
                <div>
                  <label className="block text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">
                    Silver (₹)
                  </label>
                  <input
                    type="number"
                    required
                    value={showForm.silverPrice}
                    onChange={(e) => setShowForm({ ...showForm, silverPrice: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 text-white text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">
                    Gold (₹)
                  </label>
                  <input
                    type="number"
                    required
                    value={showForm.goldPrice}
                    onChange={(e) => setShowForm({ ...showForm, goldPrice: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-dark-bg border border-primary-500/50 text-white text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">
                    Platinum (₹)
                  </label>
                  <input
                    type="number"
                    required
                    value={showForm.platinumPrice}
                    onChange={(e) => setShowForm({ ...showForm, platinumPrice: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 text-white text-xs font-mono"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsScheduleShowOpen(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingShow}
                  className="px-5 py-2 rounded-xl bg-primary-500 hover:bg-primary-600 text-white text-xs font-bold shadow-lg shadow-primary-500/25 flex items-center gap-1.5"
                >
                  {submittingShow ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : 'Publish Showtime'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

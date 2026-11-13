import React, { useState, useEffect } from 'react';
import { 
  Sparkles, Ticket, Users, ShoppingBag, 
  Sliders, ArrowRight, Star, CheckCircle2, ChevronRight,
  Flame, Clock, QrCode, Film, ShieldCheck
} from 'lucide-react';
import { api } from '../services/api';
import { getMoviePoster, handleImageError } from '../utils/moviePosters';

export default function HomeScreen({ onNavigateTab, onOpenVibeSearch, onSelectMovieForBooking }) {
  const [liveMovies, setLiveMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    api.getMovies()
      .then(data => {
        if (isMounted) {
          setLiveMovies(Array.isArray(data) ? data : []);
          setLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          setLiveMovies([]);
          setLoading(false);
        }
      });
    return () => { isMounted = false; };
  }, []);

  const genuineFeatures = [
    {
      icon: Sparkles,
      title: 'AI Movie Vibe Search',
      subtitle: 'Mood-Based Recommendations',
      description: 'Not sure what to watch? Tell CineSync your mood in plain words — like "a cozy weekend comedy" or "an intense thriller" — and get the perfect movie recommendation instantly.',
      actionTab: 'vibe',
      actionText: 'Find Your Movie Vibe',
      color: 'text-accent-cyan',
      bgGlow: 'from-accent-cyan/15'
    },
    {
      icon: Sliders,
      title: 'Budget Movie Night Planner',
      subtitle: 'Great Cinema Within Your Limit',
      description: 'Set your spending limit for date night or hanging out with friends. CineSync finds the best auditorium screens and seats without exceeding your budget.',
      actionTab: 'budget',
      actionText: 'Plan Within Budget',
      color: 'text-amber-400',
      bgGlow: 'from-amber-500/15'
    },
    {
      icon: Users,
      title: 'Squad Outing Splitter',
      subtitle: 'Fair Group Expense Tracking',
      description: 'Book cinema tickets and snacks for your whole squad in one go. CineSync automatically divides the bill so everyone knows their fair share without any confusion.',
      actionTab: 'ledger',
      actionText: 'Track & Split Expenses',
      color: 'text-emerald-400',
      bgGlow: 'from-emerald-500/15'
    },
    {
      icon: ShoppingBag,
      title: 'Spare Ticket Resale',
      subtitle: 'Never Waste a Ticket',
      description: 'Friend canceled at the last minute? Don’t let your money go to waste. Easily resell your extra ticket to another moviegoer at the original price.',
      actionTab: 'resale',
      actionText: 'Browse Spare Tickets',
      color: 'text-purple-400',
      bgGlow: 'from-purple-500/15'
    },
    {
      icon: QrCode,
      title: 'Contactless FastPass Wallet',
      subtitle: 'Zero Box Office Queues',
      description: 'All your movie passes stay safe in your digital wallet. Simply scan the dynamic QR code right at the cinema turnstile gate and walk straight to your seat.',
      actionTab: 'wallet',
      actionText: 'Open Ticket Wallet',
      color: 'text-primary-500',
      bgGlow: 'from-primary-500/15'
    }
  ];

  return (
    <div className="space-y-20 pb-20">
      
      {/* 1. HERO SECTION */}
      <section className="relative overflow-hidden pt-12 md:pt-20 px-4">
        {/* Ambient Top Glow Orbs */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[600px] h-[350px] bg-primary-500/15 blur-[120px] rounded-full pointer-events-none -z-10" />
        <div className="absolute top-1/3 left-1/4 w-[350px] h-[250px] bg-accent-cyan/10 blur-[100px] rounded-full pointer-events-none -z-10" />

        <div className="max-w-5xl mx-auto text-center space-y-6">
          {/* Release Badge */}
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-dark-card/90 border border-dark-border text-xs font-semibold shadow-lg shadow-black/40">
            <span className="w-2 h-2 rounded-full bg-primary-500 animate-pulse" />
            <span className="text-primary-400 font-bold">CineSync</span>
            <span className="text-slate-500">•</span>
            <span className="text-slate-300">Smart Theatrical Cinema Experience</span>
          </div>

          {/* Main Headline (Clean solid high-contrast colors) */}
          <h1 className="text-4xl sm:text-6xl md:text-7xl font-black text-white tracking-tight leading-tight max-w-4xl mx-auto">
            Book Movies, <br />
            <span className="text-primary-500">Split Bills & Enjoy Together.</span>
          </h1>

          {/* Business Value Subtitle */}
          <p className="text-base sm:text-lg md:text-xl text-slate-300 max-w-2xl mx-auto font-normal leading-relaxed">
            The smart movie platform built for moviegoers. Discover films matching your vibe, plan shows within your exact budget, split bills with your group, and resell spare tickets easily.
          </p>

          {/* Hero CTAs */}
          <div className="pt-4 flex flex-col sm:flex-row items-center justify-center gap-4">
            <button
              onClick={() => onNavigateTab('movies')}
              className="w-full sm:w-auto px-8 py-4 rounded-2xl bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-sm shadow-xl shadow-primary-500/30 transition-all flex items-center justify-center gap-2 group"
            >
              <Ticket className="w-4 h-4 transition-transform group-hover:rotate-12" />
              <span>Book Movie Tickets</span>
              <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
            </button>

            <button
              onClick={onOpenVibeSearch}
              className="w-full sm:w-auto px-7 py-4 rounded-2xl bg-dark-card hover:bg-dark-card/80 border border-accent-cyan/40 hover:border-accent-cyan text-accent-cyan font-bold text-sm shadow-lg shadow-accent-cyan/5 transition-all flex items-center justify-center gap-2"
            >
              <Sparkles className="w-4 h-4" />
              <span>What Should I Watch? (AI)</span>
            </button>
          </div>

          {/* Moviegoer Quick Shortcuts */}
          <div className="pt-12 grid grid-cols-2 md:grid-cols-4 gap-3 max-w-4xl mx-auto text-left">
            <button
              onClick={onOpenVibeSearch}
              className="bg-dark-card/70 hover:bg-dark-card border border-dark-border/80 hover:border-accent-cyan/50 p-4 rounded-2xl transition-all text-left group shadow-sm hover:shadow-md hover:scale-[1.02]"
            >
              <div className="text-xl font-black text-accent-cyan flex items-center justify-between">
                <span>AI Vibe Match</span>
                <ChevronRight className="w-4 h-4 opacity-50 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </div>
              <div className="text-[11px] font-semibold text-slate-400 uppercase mt-0.5">Find By Mood</div>
              <div className="text-[10px] text-slate-400 mt-1">Discover movies matching your mood</div>
            </button>

            <button
              onClick={() => onNavigateTab('budget')}
              className="bg-dark-card/70 hover:bg-dark-card border border-dark-border/80 hover:border-amber-500/50 p-4 rounded-2xl transition-all text-left group shadow-sm hover:shadow-md hover:scale-[1.02]"
            >
              <div className="text-xl font-black text-amber-400 flex items-center justify-between">
                <span>Budget Planner</span>
                <ChevronRight className="w-4 h-4 opacity-50 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </div>
              <div className="text-[11px] font-semibold text-slate-400 uppercase mt-0.5">Custom Price Limit</div>
              <div className="text-[10px] text-slate-400 mt-1">Optimize seats for your budget</div>
            </button>

            <button
              onClick={() => onNavigateTab('ledger')}
              className="bg-dark-card/70 hover:bg-dark-card border border-dark-border/80 hover:border-emerald-500/50 p-4 rounded-2xl transition-all text-left group shadow-sm hover:shadow-md hover:scale-[1.02]"
            >
              <div className="text-xl font-black text-emerald-400 flex items-center justify-between">
                <span>Split Bill</span>
                <ChevronRight className="w-4 h-4 opacity-50 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </div>
              <div className="text-[11px] font-semibold text-slate-400 uppercase mt-0.5">Squad Outings</div>
              <div className="text-[10px] text-slate-400 mt-1">Split tickets & snacks fairly</div>
            </button>

            <button
              onClick={() => onNavigateTab('resale')}
              className="bg-dark-card/70 hover:bg-dark-card border border-dark-border/80 hover:border-purple-500/50 p-4 rounded-2xl transition-all text-left group shadow-sm hover:shadow-md hover:scale-[1.02]"
            >
              <div className="text-xl font-black text-purple-400 flex items-center justify-between">
                <span>Resell Tickets</span>
                <ChevronRight className="w-4 h-4 opacity-50 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
              </div>
              <div className="text-[11px] font-semibold text-slate-400 uppercase mt-0.5">Fair Cost Resale</div>
              <div className="text-[10px] text-slate-400 mt-1">List spares or grab verified seats</div>
            </button>
          </div>
        </div>
      </section>

      {/* 2. FEATURED MOVIES STRIP */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-end justify-between mb-8 border-b border-dark-border/60 pb-4">
          <div>
            <div className="flex items-center gap-2 text-primary-500 font-semibold text-xs tracking-wider uppercase mb-1">
              <Flame className="w-4 h-4" />
              <span>Now In Theatres</span>
            </div>
            <h2 className="text-2xl md:text-3xl font-extrabold text-white">
              Trending Blockbusters
            </h2>
          </div>
          <button
            onClick={() => onNavigateTab('movies')}
            className="text-xs font-bold text-primary-400 hover:text-primary-300 flex items-center gap-1 transition-colors"
          >
            <span>See All Movies</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

        {loading ? (
          <div className="py-12 text-center text-xs text-slate-400">Loading catalog movies...</div>
        ) : liveMovies.length === 0 ? (
          <div className="bg-[#121622] border border-[#262E42] rounded-3xl p-8 text-center max-w-md mx-auto shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-primary-500/10 border border-primary-500/20 text-primary-400 flex items-center justify-center mx-auto mb-3 shadow-md">
              <Film className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white mb-1">No Screenings Available</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Check back soon for new movie releases and upcoming cinema showtimes.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {liveMovies.slice(0, 3).map((movie) => (
              <div
                key={movie.id}
                className="group relative bg-dark-card border border-dark-border hover:border-primary-500/50 rounded-3xl overflow-hidden transition-all duration-300 flex flex-col justify-between shadow-xl hover:shadow-2xl hover:shadow-primary-500/10"
              >
                {/* Poster Backdrop */}
                <div className="relative aspect-[16/10] overflow-hidden bg-slate-900">
                  <img
                    src={getMoviePoster(movie)}
                    alt={movie.title}
                    onError={(e) => handleImageError(e, movie.title)}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-dark-card via-dark-card/40 to-transparent" />

                  <div className="absolute top-3 left-3">
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-primary-500 text-white uppercase tracking-wider shadow-md">
                      Live
                    </span>
                  </div>

                  <div className="absolute top-3 right-3 flex items-center gap-1 bg-black/80 backdrop-blur-md px-2 py-0.5 rounded-full text-xs font-bold text-amber-400 border border-white/10">
                    <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                    {movie.rating || 8.8}
                  </div>
                </div>

                {/* Movie Details */}
                <div className="p-6 flex-1 flex flex-col justify-between space-y-4">
                  <div>
                    <div className="flex items-center gap-2 text-xs text-slate-400 mb-1">
                      <span>{movie.genre}</span>
                      <span>•</span>
                      <span className="flex items-center gap-1">
                        <Clock className="w-3 h-3 text-accent-cyan" />
                        {movie.duration}m
                      </span>
                    </div>
                    <h3 className="text-xl font-bold text-white group-hover:text-primary-400 transition-colors">
                      {movie.title}
                    </h3>
                    <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                      {movie.description || `${movie.title} is now showing in cinemas.`}
                    </p>
                  </div>

                  <button
                    onClick={() => onSelectMovieForBooking(movie)}
                    className="w-full py-3 rounded-xl bg-dark-bg hover:bg-primary-500 border border-dark-border hover:border-primary-500 text-white text-xs font-bold transition-all flex items-center justify-center gap-2 shadow-sm"
                  >
                    <Ticket className="w-3.5 h-3.5" />
                    <span>Choose Seats & Times</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 3. CORE GENUINE VALUE PILLARS */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-12">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-2">
            <Ticket className="w-3.5 h-3.5" />
            <span>Smart Moviegoing Features</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Features Built For How People Watch Movies
          </h2>
          <p className="text-slate-400 text-sm mt-2">
            No more messy group calculations, budget surprises, or lost money on unused tickets.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {genuineFeatures.map((p, idx) => {
            const Icon = p.icon;
            return (
              <div
                key={idx}
                className="bg-dark-card border border-dark-border hover:border-slate-700 p-6 rounded-3xl transition-all duration-300 flex flex-col justify-between group hover:-translate-y-1 shadow-lg"
              >
                <div>
                  <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${p.bgGlow} to-transparent border border-dark-border flex items-center justify-center ${p.color} mb-4`}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">
                    {p.subtitle}
                  </div>
                  <h3 className="text-xl font-bold text-white mb-2 group-hover:text-primary-400 transition-colors">
                    {p.title}
                  </h3>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    {p.description}
                  </p>
                </div>

                <div className="pt-6 mt-6 border-t border-dark-border/60">
                  <button
                    onClick={() => {
                      if (p.actionTab === 'vibe') onOpenVibeSearch();
                      else onNavigateTab(p.actionTab);
                    }}
                    className={`text-xs font-bold ${p.color} hover:underline flex items-center gap-1.5`}
                  >
                    <span>{p.actionText}</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* 4. HOW IT WORKS 3-STEP JOURNEY */}
      <section className="max-w-6xl mx-auto px-4">
        <div className="bg-gradient-to-b from-dark-card to-dark-bg border border-dark-border rounded-3xl p-8 md:p-12 shadow-2xl">
          <div className="text-center max-w-xl mx-auto mb-10">
            <h3 className="text-2xl md:text-3xl font-extrabold text-white">
              Planning a Movie Night is Finally Simple
            </h3>
            <p className="text-slate-400 text-xs mt-1">
              From finding the movie to walking through the gate in three easy steps.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
            <div className="text-center space-y-3">
              <div className="w-14 h-14 rounded-2xl bg-primary-500/10 border border-primary-500/30 text-primary-400 font-black text-xl flex items-center justify-center mx-auto shadow-lg shadow-primary-500/10">
                1
              </div>
              <h4 className="text-base font-bold text-white">Discover by Mood or Budget</h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Tell AI your vibe or use the Budget Planner to pick the best shows and seats within your price range.
              </p>
            </div>

            <div className="text-center space-y-3">
              <div className="w-14 h-14 rounded-2xl bg-accent-cyan/10 border border-accent-cyan/30 text-accent-cyan font-black text-xl flex items-center justify-center mx-auto shadow-lg shadow-accent-cyan/10">
                2
              </div>
              <h4 className="text-base font-bold text-white">Choose Your Favorite Seats</h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Pick your seats on our curved cinema screen layout across Silver, Gold, or VIP Recliner categories.
              </p>
            </div>

            <div className="text-center space-y-3">
              <div className="w-14 h-14 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-black text-xl flex items-center justify-center mx-auto shadow-lg shadow-emerald-500/10">
                3
              </div>
              <h4 className="text-base font-bold text-white">FastPass Entry & Split Expenses</h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Scan your contactless QR pass at the gate. Track and split ticket or snack costs with your friends easily.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. BOTTOM CTA BANNER */}
      <section className="max-w-5xl mx-auto px-4 text-center">
        <div className="relative p-10 md:p-14 rounded-3xl bg-gradient-to-r from-primary-600 to-primary-500 overflow-hidden shadow-2xl">
          <div className="relative z-10 space-y-4">
            <h3 className="text-3xl md:text-4xl font-black text-white tracking-tight">
              Ready For Tonight’s Movie?
            </h3>
            <p className="text-white/90 text-sm max-w-lg mx-auto">
              Find showtimes, reserve seats, and enjoy cinema the way it was meant to be.
            </p>
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
              <button
                onClick={() => onNavigateTab('movies')}
                className="px-8 py-3.5 rounded-xl bg-white text-slate-900 font-extrabold text-sm shadow-xl hover:bg-slate-100 transition-all"
              >
                Browse Now Showing
              </button>
              <button
                onClick={() => onNavigateTab('budget')}
                className="px-6 py-3.5 rounded-xl bg-black/30 hover:bg-black/40 text-white font-bold text-sm border border-white/20 transition-all"
              >
                Plan Movie Within Budget
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

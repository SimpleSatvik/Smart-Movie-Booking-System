import React, { useState, useEffect } from 'react';
import { 
  Sliders, Sparkles, DollarSign, Users, Award, 
  ArrowRight, ArrowLeft, RefreshCw
} from 'lucide-react';
import { api } from '../services/api';
import LoadingBar from './LoadingBar';

export default function BudgetOptimizerView({ onSelectShowForBooking, onNavigateTab }) {
  const [budget, setBudget] = useState(800);
  const [seats, setSeats] = useState(2);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState(null);

  const presets = [
    { label: 'Solo Movie Trip', budget: 350, seats: 1 },
    { label: 'Couple Date Night', budget: 850, seats: 2 },
    { label: 'Trio Hangout', budget: 1100, seats: 3 },
    { label: 'Squad Night Out', budget: 1600, seats: 4 },
  ];

  const runOfflineStrictOptimizer = (targetBudget, targetSeats) => {
    const tiers = [
      { name: 'SILVER PRIME', pricePerSeat: 200 },
      { name: 'GOLD VIP', pricePerSeat: 350 },
      { name: 'PLATINUM LUXE', pricePerSeat: 500 }
    ];

    const catalog = [
      { id: 1, title: 'Dune: Part Two (IMAX)', theatre: 'PVR Director\'s Cut, Delhi NCR', time: '7:30 PM (Tonight)' },
      { id: 2, title: 'Interstellar', theatre: 'INOX Megaplex Screen 4', time: '9:45 PM' },
      { id: 3, title: 'Spider-Man: Beyond The Spider-Verse', theatre: 'Cinepolis IMAX Laser', time: '6:00 PM' },
      { id: 4, title: 'Oppenheimer (70mm IMAX)', theatre: 'PVR Superplex Audi 2', time: '8:15 PM' }
    ];

    const validShows = [];

    catalog.forEach((movie, mIdx) => {
      tiers.forEach((tier) => {
        const totalCost = tier.pricePerSeat * targetSeats;
        // ABSOLUTE HARD CONSTRAINT: totalCost MUST be <= targetBudget!
        if (totalCost <= targetBudget) {
          validShows.push({
            showId: (mIdx + 1) * 10 + (tier.pricePerSeat / 50),
            movieId: movie.id,
            movieTitle: movie.title,
            theatreName: movie.theatre,
            showTime: movie.time,
            tier: tier.name,
            seatCostPerPerson: tier.pricePerSeat,
            totalCost: totalCost,
            remainingBudget: targetBudget - totalCost
          });
        }
      });
    });

    if (validShows.length === 0) {
      // The budget is strictly too low for targetSeats!
      const minRequired = 200 * targetSeats;
      setResults({
        totalBudget: targetBudget,
        numberOfSeats: targetSeats,
        bestAllocatedCost: 0,
        savings: 0,
        optimizedShows: [],
        noMatch: true,
        minRequiredCost: minRequired
      });
    } else {
      // Sort by best tier within budget, then lowest cost
      validShows.sort((a, b) => b.seatCostPerPerson - a.seatCostPerPerson || a.totalCost - b.totalCost);
      const best = validShows[0];
      setResults({
        totalBudget: targetBudget,
        numberOfSeats: targetSeats,
        bestAllocatedCost: best.totalCost,
        savings: Math.max(0, targetBudget - best.totalCost),
        optimizedShows: validShows,
        noMatch: false
      });
    }
  };

  const handleRunOptimizer = async (targetBudget = budget, targetSeats = seats) => {
    setLoading(true);
    try {
      const data = await api.optimizeBudget(targetBudget, targetSeats);
      
      let candidateList = [];

      if (Array.isArray(data)) {
        candidateList = data.map(d => ({
          showId: d.showId,
          movieId: d.movieId,
          movieTitle: d.movieTitle,
          theatreName: d.theatreName || 'PVR Cinema Complex',
          screenName: d.screenName || 'Screen 1',
          showTime: d.showStartTime ? new Date(d.showStartTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : '7:30 PM',
          tier: d.seatCategory || 'GOLD VIP',
          seatCostPerPerson: Number(d.pricePerSeat),
          totalCost: Number(d.totalCost),
          remainingBudget: Number(d.remainingBudget)
        }));
      }

      // STRICT CONSTRAINT: Absolutely NO show whose totalCost exceeds targetBudget is allowed!
      const strictlyWithinBudget = candidateList.filter(show => show.totalCost <= targetBudget);

      if (strictlyWithinBudget.length > 0) {
        strictlyWithinBudget.sort((a, b) => b.seatCostPerPerson - a.seatCostPerPerson || a.totalCost - b.totalCost);
        const best = strictlyWithinBudget[0];
        setResults({
          totalBudget: targetBudget,
          numberOfSeats: targetSeats,
          bestAllocatedCost: best.totalCost,
          savings: Math.max(0, targetBudget - best.totalCost),
          optimizedShows: strictlyWithinBudget,
          noMatch: false
        });
      } else {
        runOfflineStrictOptimizer(targetBudget, targetSeats);
      }
    } catch (err) {
      runOfflineStrictOptimizer(targetBudget, targetSeats);
    } finally {
      setLoading(false);
    }
  };

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

      {/* Header */}
      <div className="mb-8">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 uppercase tracking-wider mb-2">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Smart Movie Night Planner</span>
        </div>
        <h1 className="text-3xl md:text-4xl font-extrabold text-white tracking-tight">
          Plan Within Your Budget
        </h1>
        <p className="text-slate-300 text-sm mt-1 max-w-2xl">
          Step 1: Set your budget & tickets below. CineSync finds the best auditorium screens and seats without exceeding your budget.
        </p>
      </div>

      {/* Control Card */}
      <div className="bg-dark-card border border-dark-border rounded-3xl p-6 md:p-8 mb-8 shadow-2xl">
        {/* Preset quick buttons */}
        <div className="mb-6">
          <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
            Quick Scenarios
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            {presets.map((p) => {
              const isSelected = budget === p.budget && seats === p.seats;
              return (
                <button
                  key={p.label}
                  onClick={() => {
                    setBudget(p.budget);
                    setSeats(p.seats);
                    handleRunOptimizer(p.budget, p.seats);
                  }}
                  className={`p-3 rounded-2xl border text-left transition-all ${
                    isSelected
                      ? 'bg-primary-500/15 border-primary-500 text-white shadow-lg shadow-primary-500/15'
                      : 'bg-dark-bg/60 border-dark-border text-slate-400 hover:text-white hover:border-slate-700'
                  }`}
                >
                  <div className="text-xs font-bold truncate">{p.label}</div>
                  <div className="text-[11px] text-primary-400 font-mono mt-0.5">₹{p.budget} • {p.seats} {p.seats === 1 ? 'Seat' : 'Seats'}</div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Sliders */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 pt-4 border-t border-dark-border/60">
          {/* Budget Slider */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <DollarSign className="w-3.5 h-3.5 text-primary-500" />
                Target Budget Limit
              </span>
              <span className="text-2xl font-black text-white font-mono">
                ₹{budget}
              </span>
            </div>
            <input
              type="range"
              min="100"
              max="3000"
              step="50"
              value={budget}
              onChange={(e) => setBudget(Number(e.target.value))}
              className="w-full h-2 bg-dark-bg rounded-lg appearance-none cursor-pointer accent-primary-500"
            />
            <div className="flex justify-between text-[10px] text-slate-400 mt-1 font-mono">
              <span>₹100</span>
              <span>₹1500</span>
              <span>₹3000 (VIP Luxe)</span>
            </div>
          </div>

          {/* Seats Selector */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <Users className="w-3.5 h-3.5 text-accent-cyan" />
                Number of Tickets
              </span>
              <span className="text-2xl font-black text-white font-mono">
                {seats} {seats === 1 ? 'Ticket' : 'Tickets'}
              </span>
            </div>
            <input
              type="range"
              min="1"
              max="8"
              step="1"
              value={seats}
              onChange={(e) => setSeats(Number(e.target.value))}
              className="w-full h-2 bg-dark-bg rounded-lg appearance-none cursor-pointer accent-accent-cyan"
            />
            <div className="flex justify-between text-[10px] text-slate-400 mt-1 font-mono">
              <span>1 Person</span>
              <span>4 People</span>
              <span>8 People</span>
            </div>
          </div>
        </div>

        {/* Compute Button */}
        <div className="mt-8 flex justify-end">
          <button
            onClick={() => handleRunOptimizer(budget, seats)}
            disabled={loading}
            className="w-full sm:w-auto px-8 py-3.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-sm rounded-2xl transition-all shadow-xl shadow-primary-500/25 flex items-center justify-center gap-2"
          >
            {loading ? (
              <RefreshCw className="w-4 h-4 animate-spin" />
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                <span>Find Best Matches For ₹{budget}</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Initial Waiting State */}
      {!results && !loading && (
        <div className="bg-dark-card/60 border border-dark-border rounded-3xl p-8 text-center max-w-lg mx-auto shadow-xl space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-primary-500/10 border border-primary-500/20 flex items-center justify-center mx-auto text-primary-400">
            <Sliders className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-white">Ready to plan your movie night?</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Adjust your budget and ticket count above, then click <strong className="text-white">"Find Best Matches For ₹{budget}"</strong> to view screenings that strictly fit within your limit.
          </p>
        </div>
      )}

      {/* Results View */}
      {loading ? (
        <LoadingBar
          title={`Optimizing showtimes & seating for ₹${budget} (${seats} ${seats === 1 ? 'ticket' : 'tickets'})...`}
          subtitle="Evaluating screens, seat tiers, and price combinations within your limit"
        />
      ) : results ? (
        results.noMatch || results.optimizedShows.length === 0 ? (
          <div className="bg-dark-card border border-amber-500/30 rounded-3xl p-8 text-center max-w-xl mx-auto shadow-2xl space-y-4">
            <div className="w-14 h-14 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center mx-auto text-amber-400">
              <DollarSign className="w-7 h-7" />
            </div>
            <h3 className="text-xl font-bold text-white">
              Budget Too Low for {seats} {seats === 1 ? 'Ticket' : 'Tickets'}
            </h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              Your budget limit is <strong className="text-white">₹{budget}</strong> (₹{Math.round(budget / seats)} per ticket). 
              The lowest cinema ticket tier available is <strong className="text-primary-400">Silver Prime at ₹200 per seat</strong>. 
              For a party of {seats}, the minimum total cost required is <strong className="text-emerald-400 font-mono">₹{results.minRequiredCost || (seats * 200)}</strong>.
            </p>
            <p className="text-[11px] text-slate-400">
              We never force-fit tickets that exceed your budget limit.
            </p>
            <div className="pt-2 flex justify-center">
              <button
                onClick={() => {
                  const min = results.minRequiredCost || (seats * 200);
                  setBudget(min);
                  handleRunOptimizer(min, seats);
                }}
                className="px-6 py-3 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-xs rounded-xl shadow-lg shadow-primary-500/20 transition-all flex items-center gap-2"
              >
                <Sparkles className="w-4 h-4" />
                <span>Adjust Budget to ₹{results.minRequiredCost || (seats * 200)}</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Recommended Shows List */}
            <div className="space-y-4">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Award className="w-5 h-5 text-primary-500" />
                Recommended Screenings Under ₹{budget}
              </h3>

              {(results.optimizedShows || []).map((show, idx) => (
                <div 
                  key={show.showId || idx}
                  className="bg-dark-card border border-dark-border hover:border-primary-500/40 rounded-3xl p-6 transition-all shadow-xl group flex flex-col md:flex-row items-start md:items-center justify-between gap-6"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-primary-500/20 text-primary-400 border border-primary-500/30">
                        MATCH #{idx + 1}
                      </span>
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-accent-blue/15 text-accent-blue border border-accent-blue/30">
                        {show.tier || 'GOLD VIP'}
                      </span>
                    </div>

                    <h4 className="text-xl font-bold text-white group-hover:text-primary-400 transition-colors">
                      {show.movieTitle}
                    </h4>
                    <p className="text-xs text-slate-400 mt-1">
                      {show.theatreName} • <span className="text-white">{show.showTime}</span>
                    </p>
                  </div>

                  <div className="flex md:flex-col items-center md:items-end justify-between w-full md:w-auto gap-4 pt-4 md:pt-0 border-t md:border-t-0 border-dark-border/60">
                    <div className="text-left md:text-right">
                      <div className="text-[10px] text-slate-400 uppercase font-semibold">Total for {seats} Tickets</div>
                      <div className="text-2xl font-black text-white font-mono">
                        ₹{show.totalCost}
                      </div>
                      <div className="text-[11px] text-slate-400">₹{show.seatCostPerPerson} per ticket</div>
                    </div>

                    <button
                      onClick={() => onSelectShowForBooking && onSelectShowForBooking(show)}
                      className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-xs rounded-xl transition-all shadow-lg shadow-primary-500/25 flex items-center gap-1.5"
                    >
                      <span>Book This Show</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )
      ) : null}
    </div>
  );
}

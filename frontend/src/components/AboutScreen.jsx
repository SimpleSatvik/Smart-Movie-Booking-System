import React from 'react';
import { 
  Heart, Sparkles, Sliders, Users, ShoppingBag, 
  CheckCircle2, QrCode, Building2, Tag,
  ArrowRight, Award, Film
} from 'lucide-react';

export default function AboutScreen({ onNavigateTab }) {
  const problemsSolved = [
    {
      title: 'Group Outing Expense Splitting',
      subtitle: 'Who paid, who owes, and messy calculations',
      icon: Users,
      color: 'text-emerald-400',
      solution: 'One person books the tickets and snacks for the group. CineSync automatically divides the total so everyone sees their exact share cleanly without mental math or spreadsheets.'
    },
    {
      title: 'Movie Night Budget Planning',
      subtitle: 'Accidentally spending ₹2,000 when you only wanted a ₹800 date',
      icon: Sliders,
      color: 'text-amber-400',
      solution: 'Enter your hard budget limit and party size. CineSync balances ticket categories, prime screen formats (IMAX, Dolby Atmos), and showtimes so you get the best cinema experience within your limit.'
    },
    {
      title: 'Spare Ticket Resale',
      subtitle: 'A friend cancels last-minute and your ticket goes to waste',
      icon: ShoppingBag,
      color: 'text-purple-400',
      solution: 'List your spare ticket in the Resale section in seconds. Another moviegoer claims it at the original face value price, your money is recouped, and the cinema seat doesn\'t stay empty.'
    },
    {
      title: 'Decision Fatigue on What to Watch',
      subtitle: 'Spending 30 minutes scrolling movie posters without deciding',
      icon: Sparkles,
      color: 'text-accent-cyan',
      solution: 'Describe your mood in plain words — like "I want a visually stunning space adventure" or "funny animated movie for a family night". Our AI understands your vibe and matches you to the right film.'
    },
    {
      title: 'Box Office & Turnstile Lines',
      subtitle: 'Rushing to print physical tickets while opening credits roll',
      icon: QrCode,
      color: 'text-primary-500',
      solution: 'Your digital FastPass stays safe in your personal wallet with a secure rotating QR code. Simply scan your phone directly at the turnstile gate and walk straight to your seat.'
    }
  ];

  const businessPillars = [
    {
      title: 'For Moviegoers',
      points: [
        'Find films matching your mood instantly with AI Vibe Search',
        'Plan movie nights around an exact budget limit with no surprises',
        'Effortlessly track and split tickets and snack costs with friends',
        'Resell extra tickets at original prices if someone cancels last minute',
        'Contactless gate entry with digital FastPass QR tickets'
      ]
    },
    {
      title: 'For Cinema Exhibitors & Multiplexes',
      points: [
        'Higher screen occupancy and ticket sales through smart budget discovery',
        'Healthy secondary market: spare tickets re-enter the ecosystem fairly',
        'Reduced box office counter lines with direct mobile turnstile entry',
        'Real-time box office analytics tracking showtime revenues and seat demand',
        'Flexible multi-tier auditorium pricing (Silver, Gold, and VIP Recliners)'
      ]
    }
  ];

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-16">
      
      {/* 1. Header Hero */}
      <div className="text-center max-w-3xl mx-auto space-y-4">
        <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full text-xs font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 uppercase tracking-wider">
          <Heart className="w-3.5 h-3.5" />
          <span>Our Product Vision & Story</span>
        </div>
        <h1 className="text-3xl sm:text-5xl font-black text-white tracking-tight">
          Reinventing The Cinema Experience
        </h1>
        <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
          Watching a movie on the big screen is one of our favorite shared experiences. But planning movie nights with friends, sticking to a budget, and dealing with unused tickets has always been a hassle. CineSync was built to make cinema outings simple, social, and enjoyable.
        </p>
      </div>

      {/* 2. Customer Problems We Solve */}
      <div>
        <div className="text-center max-w-2xl mx-auto mb-10">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white">
            Real Everyday Moviegoing Frustrations We Solved
          </h2>
          <p className="text-slate-400 text-xs sm:text-sm mt-1.5">
            Every feature in CineSync was built to eliminate a real annoyance people face when heading to the movies.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {problemsSolved.map((p, idx) => {
            const Icon = p.icon;
            return (
              <div 
                key={idx}
                className="bg-dark-card border border-dark-border rounded-3xl p-6 flex flex-col justify-between hover:border-slate-700 transition-all shadow-lg"
              >
                <div>
                  <div className={`w-12 h-12 rounded-2xl bg-dark-bg border border-dark-border flex items-center justify-center ${p.color} mb-4`}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                    {p.subtitle}
                  </div>
                  <h3 className="text-lg font-bold text-white mb-2.5">
                    {p.title}
                  </h3>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    {p.solution}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 3. Win-Win for Audiences and Cinemas */}
      <div className="bg-gradient-to-b from-dark-card to-dark-bg border border-dark-border rounded-3xl p-8 md:p-12 shadow-2xl">
        <div className="text-center max-w-xl mx-auto mb-10">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white">
            A Platform That Works For Everyone
          </h2>
          <p className="text-slate-400 text-xs sm:text-sm mt-1">
            Built to delight moviegoers while empowering cinema owners to run full, profitable screenings.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {businessPillars.map((pillar, i) => (
            <div key={i} className="bg-dark-bg/80 border border-dark-border/80 rounded-2xl p-6">
              <h3 className="text-lg font-bold text-primary-400 mb-4 pb-2 border-b border-dark-border flex items-center gap-2">
                <CheckCircle2 className="w-5 h-5 text-primary-500" />
                {pillar.title}
              </h3>
              <ul className="space-y-3">
                {pillar.points.map((pt, j) => (
                  <li key={j} className="text-xs text-slate-300 flex items-start gap-2.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-accent-cyan mt-1.5 flex-shrink-0" />
                    <span className="leading-relaxed">{pt}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>

      {/* 4. Consumer Guarantees */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-dark-card border border-dark-border p-6 rounded-3xl text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-primary-500/10 text-primary-400 flex items-center justify-center mx-auto mb-3">
            <Tag className="w-6 h-6" />
          </div>
          <h4 className="text-base font-bold text-white">Original Price Resale</h4>
          <p className="text-xs text-slate-400">
            Spare tickets are resold at fair face value so fans never lose money and buyers never get overcharged.
          </p>
        </div>

        <div className="bg-dark-card border border-dark-border p-6 rounded-3xl text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-amber-400/10 text-amber-400 flex items-center justify-center mx-auto mb-3">
            <Sliders className="w-6 h-6" />
          </div>
          <h4 className="text-base font-bold text-white">Smart Budget Limits</h4>
          <p className="text-xs text-slate-400">
            Choose your price ceiling and get prime shows and seating matched strictly within your spending limit.
          </p>
        </div>

        <div className="bg-dark-card border border-dark-border p-6 rounded-3xl text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mx-auto mb-3">
            <QrCode className="w-6 h-6" />
          </div>
          <h4 className="text-base font-bold text-white">Contactless FastPass</h4>
          <p className="text-xs text-slate-400">
            Dynamic contactless QR codes stored directly in your digital pass wallet for easy turnstile gate entry.
          </p>
        </div>
      </div>

      {/* 5. Bottom Navigation Shortcut */}
      <div className="bg-gradient-to-r from-primary-600 to-primary-500 rounded-3xl p-8 text-center text-white space-y-3 shadow-xl">
        <h3 className="text-2xl font-black">Experience CineSync Tonight</h3>
        <p className="text-white/90 text-xs sm:text-sm max-w-md mx-auto">
          Pick a movie, plan your budget, or invite your squad for a movie outing.
        </p>
        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
          <button
            onClick={() => onNavigateTab('movies')}
            className="px-6 py-2.5 rounded-xl bg-white text-slate-900 font-extrabold text-xs shadow-lg hover:bg-slate-100 transition-all"
          >
            Explore Premieres
          </button>
          <button
            onClick={() => onNavigateTab('budget')}
            className="px-6 py-2.5 rounded-xl bg-black/30 hover:bg-black/40 text-white font-bold text-xs border border-white/20 transition-all"
          >
            Try Budget Planner
          </button>
        </div>
      </div>

    </div>
  );
}

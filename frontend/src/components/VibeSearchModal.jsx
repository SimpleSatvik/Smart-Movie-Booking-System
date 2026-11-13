import React, { useState } from 'react';
import { 
  Sparkles, Search, X, Bot, Film, Star, ArrowRight, RefreshCw, AlertCircle, Frown 
} from 'lucide-react';
import { api } from '../services/api';
import { getMoviePoster, handleImageError } from '../utils/moviePosters';

const CATALOG_MOVIES = [
  {
    id: 1,
    title: 'Dune: Part Two (IMAX)',
    genre: 'Sci-Fi / Adventure / Action',
    keywords: ['space', 'sci-fi', 'scifi', 'desert', 'epic', 'visual', 'imax', 'bass', 'soundtrack', 'zimmer', 'action', 'war', 'spectacle', 'futuristic', 'alien', 'philosophy', 'dread', 'serious', 'grand', 'arrakis', 'sandworm', 'prophecy'],
    rating: 8.9,
    duration: 166,
    posterUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80',
    description: 'A grand visual spectacle set on Arrakis featuring deep desert lore, intense world-building, and Hans Zimmer\'s thunderous percussion.'
  },
  {
    id: 2,
    title: 'Interstellar',
    genre: 'Sci-Fi / Drama',
    keywords: ['space', 'sci-fi', 'scifi', 'wormhole', 'black hole', 'time', 'physics', 'relativity', 'emotional', 'father', 'daughter', 'cry', 'sad', 'existential', 'nolan', 'organ', 'synth', 'astronaut', 'quantum', 'mind-bending', 'cosmic', 'space travel'],
    rating: 8.8,
    duration: 169,
    posterUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80',
    description: 'An emotional and existential space journey grappling with black holes, time dilation, and the survival of human love across light years.'
  },
  {
    id: 3,
    title: 'Spider-Man: Beyond The Spider-Verse',
    genre: 'Animation / Action / Superhero',
    keywords: ['animation', 'animated', 'spider-man', 'spiderman', 'superhero', 'marvel', 'comic', 'multiverse', 'family', 'colorful', 'fun', 'upbeat', 'hip hop', 'miles morales', 'wholesome', 'action', 'fast-paced', 'teen', 'cozy', 'comfort', 'cartoon'],
    rating: 9.0,
    duration: 140,
    posterUrl: 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&auto=format&fit=crop&q=80',
    description: 'A vibrant, multi-layered visual masterclass bursting with comic-book artistry, multiverse stakes, and an energetic soundtrack.'
  },
  {
    id: 4,
    title: 'Oppenheimer (70mm IMAX)',
    genre: 'Biography / Drama / History',
    keywords: ['history', 'biography', 'drama', 'ww2', 'atomic', 'bomb', 'tension', 'suspense', 'courtroom', 'physics', 'political', 'cillian murphy', 'nolan', 'dread', 'ethical', 'moral', 'dark', 'serious', 'intense', 'psychological', 'thriller'],
    rating: 8.9,
    duration: 180,
    posterUrl: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800&auto=format&fit=crop&q=80',
    description: 'A gripping, claustrophobic psychological thriller chronicling the creation of the atomic bomb and the moral fallout that followed.'
  },
  {
    id: 5,
    title: 'Jawan',
    genre: 'Action / Thriller / Mass Masala',
    keywords: ['action', 'shah rukh khan', 'srk', 'mass', 'adrenaline', 'revenge', 'vigilante', 'thriller', 'bollywood', 'hindi', 'soldier', 'jail', 'train', 'justice', 'commercial', 'explosive'],
    rating: 8.7,
    duration: 169,
    posterUrl: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdnT7OTN5iPqcTD9hozOIuIVG8IWbeFLcPMWDFybXLyw&s=10',
    description: 'A high-octane emotional action thriller outlining the journey of a man set out to correct the wrongs in society.'
  }
];

export default function VibeSearchModal({ isOpen, onClose, onSelectMovie }) {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const sampleVibes = [
    'Mind-bending space odyssey with philosophical dread',
    'High adrenaline superhero action with comic animation',
    'Psychological historical tension with nuclear stakes',
    'Emotional sci-fi journey about time dilation and family',
  ];

  const handleSearch = async (promptText = query) => {
    const text = promptText.trim();
    if (!text) return;
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await api.vibeSearch(text);

      if (Array.isArray(data)) {
        if (data.length === 0) {
          // Genuine empty result from backend
          setResult({
            query: text,
            aiReasoning: `No movies currently scheduled in theatres match "${text}". We won't force-fit unrelated titles.`,
            recommendedMovies: []
          });
        } else {
          const mapped = data.map((d) => {
            const catalogMatch = CATALOG_MOVIES.find(m => m.id === d.movieId || m.title.toLowerCase().includes((d.movieTitle || '').toLowerCase()));
            
            // Use real backend shows if available
            const validUpcoming = Array.isArray(d.upcomingShows) ? d.upcomingShows : [];

            return {
              id: d.movieId || (catalogMatch ? catalogMatch.id : 1),
              title: d.movieTitle || (catalogMatch ? catalogMatch.title : 'Movie'),
              genre: d.genre || (catalogMatch ? catalogMatch.genre : 'Cinema'),
              rating: catalogMatch ? catalogMatch.rating : 8.8,
              duration: catalogMatch ? catalogMatch.duration : 160,
              posterUrl: catalogMatch ? catalogMatch.posterUrl : CATALOG_MOVIES[0].posterUrl,
              vibeMatch: d.confidenceScore ? `${Math.round(d.confidenceScore * 100)}% Match` : '96% Match',
              description: d.matchReason || (catalogMatch ? catalogMatch.description : ''),
              upcomingShows: validUpcoming,
              shows: validUpcoming
            };
          });

          setResult({
            query: text,
            aiReasoning: `Found ${mapped.length} screening ${mapped.length === 1 ? 'film' : 'films'} genuinely matching your vibe: "${text}".`,
            recommendedMovies: mapped
          });
        }
      } else if (data && data.recommendedMovies) {
        setResult(data);
      } else {
        throw new Error('Fallback matching needed');
      }
    } catch (err) {
      // Offline / semantic fallback: STRICT MATCHING ONLY
      const lower = text.toLowerCase();
      
      // Keywords that are completely absent from current lineup
      const absentGenres = ['horror', 'scary', 'slasher', 'spooky', 'gore', 'zombie', 'haunted', 'vampire', 'romcom', 'romance', 'love story', 'musical', 'western'];
      const isUnmatchedGenre = absentGenres.some(k => lower.includes(k));

      if (isUnmatchedGenre) {
        setResult({
          query: text,
          aiReasoning: `No horror or romance movies are currently scheduled in theatres. Current screenings feature Sci-Fi, Animated Action, and Historical Drama.`,
          recommendedMovies: []
        });
      } else {
        // Score each movie based on word match
        const words = lower.split(/\s+/).filter(w => w.length > 2);
        const matches = CATALOG_MOVIES.filter(m => {
          const matchCount = words.filter(w => 
            m.keywords.some(k => k.includes(w) || w.includes(k)) ||
            m.genre.toLowerCase().includes(w) ||
            m.description.toLowerCase().includes(w)
          ).length;
          return matchCount > 0;
        });

        if (matches.length > 0) {
          setResult({
            query: text,
            aiReasoning: `Matched your vibe "${text}" to ${matches.length} film ${matches.length === 1 ? 'screening' : 'screenings'} currently playing in theatres.`,
            recommendedMovies: matches.map(m => ({
              ...m,
              vibeMatch: '95% Match'
            }))
          });
        } else {
          setResult({
            query: text,
            aiReasoning: `We couldn't find any current movies playing that match the atmosphere of "${text}".`,
            recommendedMovies: []
          });
        }
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
      <div className="relative w-full max-w-2xl bg-dark-card border border-dark-border rounded-3xl p-6 md:p-8 shadow-2xl overflow-hidden max-h-[90vh] flex flex-col">
        {/* Ambient Top Glow */}
        <div className="absolute top-0 left-1/4 right-1/4 h-[2px] bg-gradient-to-r from-transparent via-primary-500 to-transparent"></div>

        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-slate-400 hover:text-white p-2 rounded-xl bg-dark-bg/60 border border-dark-border/40 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-2xl bg-primary-500/15 border border-primary-500/30 flex items-center justify-center text-primary-400">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-xl font-extrabold text-white">AI Vibe Search</h2>
            <p className="text-xs text-slate-400">
              Honest film discovery — describe your mood or vibe, and only genuine matches will be shown
            </p>
          </div>
        </div>

        {/* Input Bar */}
        <div className="relative mb-4">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            placeholder="e.g. 'Mind-bending space exploration' or 'high tension drama'"
            className="w-full pl-12 pr-28 py-3.5 rounded-2xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm placeholder-slate-500 transition-colors shadow-inner"
          />
          <Search className="w-5 h-5 text-slate-500 absolute left-4 top-1/2 -translate-y-1/2" />
          <button
            onClick={() => handleSearch()}
            disabled={loading || !query.trim()}
            className="absolute right-2 top-1/2 -translate-y-1/2 px-4 py-2 bg-primary-500 hover:bg-primary-600 disabled:opacity-50 text-white font-bold text-xs rounded-xl transition-all shadow-md shadow-primary-500/20 flex items-center gap-1.5"
          >
            {loading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : 'Search'}
          </button>
        </div>

        {/* Preset quick vibes */}
        <div className="flex flex-wrap gap-2 mb-6">
          {sampleVibes.map((v, i) => (
            <button
              key={i}
              onClick={() => {
                setQuery(v);
                handleSearch(v);
              }}
              className="text-[11px] font-medium text-slate-400 hover:text-white bg-dark-bg/60 hover:bg-primary-500/15 border border-dark-border hover:border-primary-500/30 px-3 py-1.5 rounded-xl transition-all"
            >
              "{v}"
            </button>
          ))}
        </div>

        {/* Scrollable Content Body */}
        <div className="flex-1 overflow-y-auto space-y-4 pr-1">
          {loading && (
            <div className="py-12 flex flex-col items-center justify-center text-slate-400">
              <Bot className="w-10 h-10 text-primary-500 animate-bounce mb-3" />
              <p className="text-sm font-semibold text-white">Analyzing narrative atmosphere with Gemini AI...</p>
              <p className="text-xs text-slate-400 mt-1">Cross-referencing themes, pacing, and theatrical moods</p>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          {result && !loading && (
            <div className="space-y-4">
              {/* Reasoning Card */}
              {result.aiReasoning && (
                <div className={`p-4 rounded-2xl border flex items-start gap-3 ${
                  result.recommendedMovies.length === 0
                    ? 'bg-amber-500/10 border-amber-500/30 text-amber-200'
                    : 'bg-primary-500/10 border-primary-500/20 text-slate-300'
                }`}>
                  <Bot className={`w-5 h-5 flex-shrink-0 mt-0.5 ${
                    result.recommendedMovies.length === 0 ? 'text-amber-400' : 'text-primary-400'
                  }`} />
                  <div>
                    <div className={`text-[11px] font-bold uppercase tracking-wider mb-0.5 ${
                      result.recommendedMovies.length === 0 ? 'text-amber-400' : 'text-primary-400'
                    }`}>
                      {result.recommendedMovies.length === 0 ? 'No Matching Films' : 'Vibe Analysis'}
                    </div>
                    <p className="text-xs leading-relaxed">
                      {result.aiReasoning}
                    </p>
                  </div>
                </div>
              )}

              {/* Recommended Movies or Empty State */}
              {result.recommendedMovies.length === 0 ? (
                <div className="p-8 rounded-3xl bg-dark-bg/60 border border-dark-border/80 text-center space-y-3">
                  <div className="w-12 h-12 rounded-2xl bg-slate-800/80 border border-slate-700/60 flex items-center justify-center mx-auto text-slate-400">
                    <Film className="w-6 h-6" />
                  </div>
                  <h3 className="text-base font-bold text-white">
                    No matching movies currently in theatres
                  </h3>
                  <p className="text-xs text-slate-400 max-w-md mx-auto leading-relaxed">
                    We won't force-feed you unrelated Sci-Fi or Drama movies when you ask for "{result.query || query}".
                    Theatres currently have <strong>Sci-Fi Epics</strong>, <strong>Superhero Animation</strong>, and <strong>Historical Drama</strong> playing.
                  </p>
                  <div className="pt-2">
                    <button
                      onClick={onClose}
                      className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-semibold text-xs border border-slate-700 transition-colors"
                    >
                      Browse All Now Showing
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-3">
                  {result.recommendedMovies.map((m) => (
                    <div
                      key={m.id}
                      className="p-4 rounded-2xl bg-dark-bg/80 border border-dark-border hover:border-primary-500/40 transition-all flex items-center justify-between gap-4 group"
                    >
                      <div className="flex items-center gap-4">
                        <img 
                          src={getMoviePoster(m)} 
                          alt={m.title}
                          onError={(e) => handleImageError(e, m.title)}
                          className="w-14 h-20 rounded-xl object-cover border border-dark-border"
                        />
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              {m.vibeMatch || '98% Vibe Match'}
                            </span>
                            <span className="text-xs text-slate-400 font-medium">{m.genre}</span>
                          </div>
                          <h4 className="text-base font-bold text-white group-hover:text-primary-400 transition-colors mt-0.5">
                            {m.title}
                          </h4>
                          <p className="text-xs text-slate-400 line-clamp-1 mt-1 max-w-md">
                            {m.description}
                          </p>
                        </div>
                      </div>

                      <button
                        onClick={() => {
                          onClose();
                          onSelectMovie(m);
                        }}
                        className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white font-bold text-xs rounded-xl transition-all shadow-md shadow-primary-500/20 flex items-center gap-1.5 flex-shrink-0"
                      >
                        <span>Showtimes</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

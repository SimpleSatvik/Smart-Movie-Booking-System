import React, { useState, useEffect } from 'react';
import HeroCarousel from './HeroCarousel';
import { api } from '../services/api';
import { Star, Clock, Ticket, Sparkles, Film } from 'lucide-react';
import LoadingBar from './LoadingBar';
import { getMoviePoster, handleImageError } from '../utils/moviePosters';

export default function MovieDiscovery({ onSelectMovie, onOpenVibeSearch, onNavigateTab }) {
  const [movies, setMovies] = useState([]);
  const [selectedGenre, setSelectedGenre] = useState('All');
  const [loading, setLoading] = useState(true);

  const genres = ['All', 'Sci-Fi', 'Action', 'Drama', 'Thriller', 'Animation'];

  const loadMovies = async () => {
    try {
      setLoading(true);
      const genreFilter = selectedGenre === 'All' ? null : selectedGenre;
      const data = await api.getMovies(genreFilter);
      setMovies(Array.isArray(data) ? data : []);
    } catch (err) {
      console.warn('Movie catalog fetch notice:', err.message);
      setMovies([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMovies();
  }, [selectedGenre]);

  const filteredMovies = selectedGenre === 'All' 
    ? movies 
    : movies.filter(m => m.genre && m.genre.toLowerCase().includes(selectedGenre.toLowerCase()));

  return (
    <div className="space-y-10 pb-16">
      
      {/* 1. Featured Premiere Banner Carousel (Only if movies exist) */}
      {movies.length > 0 && (
        <HeroCarousel movies={movies} onSelectMovie={onSelectMovie} />
      )}

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        {/* Genre Pill Filter Buttons */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          {genres.map((genre) => (
            <button
              key={genre}
              onClick={() => setSelectedGenre(genre)}
              className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all duration-200 ${
                selectedGenre === genre
                  ? 'bg-primary-500 text-white shadow-lg shadow-primary-500/25 scale-105'
                  : 'bg-[#121622] text-gray-400 hover:text-white hover:bg-[#161C28] border border-[#262E42]'
              }`}
            >
              {genre}
            </button>
          ))}
        </div>

        {/* Movies Grid & Screenings Header */}
        <div>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-white tracking-tight">
                Now Showing in Theatres
              </h2>
              <p className="text-xs text-gray-400 mt-1">
                Explore currently running films and upcoming releases
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-mono bg-[#161C28] px-3 py-1 rounded-full border border-[#262E42]">
                {movies.length} {movies.length === 1 ? 'Movie' : 'Movies'} Available
              </span>
            </div>
          </div>

          {loading ? (
            <LoadingBar
              title="Fetching movie screenings & showtimes from database..."
              subtitle="Querying live PostgreSQL catalog"
            />
          ) : filteredMovies.length === 0 ? (
            <div className="text-center py-16 px-4 max-w-md mx-auto bg-[#121622] border border-[#262E42] rounded-3xl p-8 shadow-xl">
              <div className="w-12 h-12 rounded-2xl bg-primary-500/10 border border-primary-500/20 text-primary-400 flex items-center justify-center mx-auto mb-3 shadow-lg shadow-primary-500/10">
                <Film className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-white mb-1">No Movies Available</h3>
              <p className="text-xs text-slate-400 leading-relaxed mb-4">
                No titles match your active filter. Check back soon for new cinema releases.
              </p>
              <button
                onClick={loadMovies}
                className="px-5 py-2.5 bg-[#161C28] hover:bg-[#1E2638] text-slate-300 font-bold text-xs rounded-xl border border-[#262E42] transition-colors"
              >
                Refresh Catalog
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {filteredMovies.map((movie) => (
                <div
                  key={movie.id}
                  className="group bg-[#121622] hover:bg-[#161C28] border border-[#262E42] hover:border-primary-500/50 rounded-2xl overflow-hidden shadow-lg hover:shadow-2xl transition-all duration-300 flex flex-col justify-between"
                >
                  {/* Poster Image Container */}
                  <div className="relative aspect-[2/3] overflow-hidden bg-[#161C28]">
                    <img
                      src={getMoviePoster(movie)}
                      alt={movie.title}
                      onError={(e) => handleImageError(e, movie.title)}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />

                    {/* Rating Pill */}
                    <div className="absolute top-2.5 right-2.5 flex items-center gap-1 bg-black/80 backdrop-blur-md px-2 py-0.5 rounded-full text-xs font-bold text-amber-400 border border-white/10 shadow-md">
                      <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                      {movie.rating || 8.8}
                    </div>
                  </div>

                  {/* Movie Info */}
                  <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
                    <div>
                      <h3 className="font-bold text-base text-white group-hover:text-primary-400 transition-colors truncate">
                        {movie.title}
                      </h3>
                      <div className="flex items-center justify-between text-xs text-gray-400 mt-1">
                        <span>{movie.genre?.split('/')[0]?.split(',')[0]}</span>
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          {movie.duration || 150}m
                        </span>
                      </div>
                    </div>

                    <button
                      onClick={() => onSelectMovie(movie)}
                      className="w-full bg-[#161C28] hover:bg-primary-500 border border-[#262E42] hover:border-primary-500 text-white text-xs font-bold py-2.5 rounded-xl flex items-center justify-center gap-2 transition-all duration-200 shadow-sm"
                    >
                      <Ticket className="w-3.5 h-3.5" />
                      Book Seats
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

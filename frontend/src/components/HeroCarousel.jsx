import React, { useState, useEffect } from 'react';
import { Star, Clock, Ticket, ChevronLeft, ChevronRight, Sparkles } from 'lucide-react';
import { getMoviePoster, handleImageError } from '../utils/moviePosters';

const DEFAULT_PREMIERES = [
  {
    id: 1,
    title: 'Dune: Part Two (IMAX)',
    genre: 'Sci-Fi / Action',
    duration: 166,
    language: 'English',
    rating: 8.9,
    mood: 'Epic & Grand',
    posterUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80',
    description: 'Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.'
  },
  {
    id: 2,
    title: 'Interstellar: 10th Anniversary',
    genre: 'Sci-Fi / Drama',
    duration: 169,
    language: 'English',
    rating: 8.8,
    mood: 'Mind-Bending',
    posterUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80',
    description: 'A team of explorers travel through a wormhole in space in an attempt to ensure humanity\'s survival.'
  },
  {
    id: 3,
    title: 'Spider-Man: Beyond The Spider-Verse',
    genre: 'Animation / Action',
    duration: 140,
    language: 'English',
    rating: 9.0,
    mood: 'Adrenaline High',
    posterUrl: 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80',
    description: 'Miles Morales catapults across the Multiverse to protect its very existence from cataclysmic destruction.'
  }
];

export default function HeroCarousel({ movies, onSelectMovie, onBookMovie }) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const activeMovies = (Array.isArray(movies) && movies.length > 0) ? movies : DEFAULT_PREMIERES;

  useEffect(() => {
    if (activeMovies.length <= 1) return;
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % activeMovies.length);
    }, 6000);
    return () => clearInterval(timer);
  }, [activeMovies.length]);

  const currentMovie = activeMovies[currentIndex] || activeMovies[0];

  const handlePrev = (e) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev - 1 + activeMovies.length) % activeMovies.length);
  };

  const handleNext = (e) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev + 1) % activeMovies.length);
  };

  const handleSelect = () => {
    if (onSelectMovie) onSelectMovie(currentMovie);
    else if (onBookMovie) onBookMovie(currentMovie);
  };

  return (
    <div className="relative w-full max-w-5xl mx-auto py-6 px-4">
      {/* 3D Elevated Carousel Container */}
      <div className="relative flex flex-col md:flex-row items-center justify-between gap-8 bg-gradient-to-b from-[#121622] to-[#0A0D14] border border-[#262E42]/80 rounded-3xl p-6 md:p-10 shadow-2xl overflow-hidden">
        
        {/* Subtle Ambient Background Poster Blur */}
        <div 
          className="absolute inset-0 opacity-15 filter blur-3xl bg-cover bg-center pointer-events-none transition-all duration-700"
          style={{ backgroundImage: `url(${getMoviePoster(currentMovie)})` }}
        />

        {/* Left: Poster Presentation with Badges */}
        <div className="relative group w-64 md:w-80 shrink-0 mx-auto md:mx-0">
          <div className="relative aspect-[2/3] rounded-2xl overflow-hidden shadow-2xl border border-white/10 group-hover:border-[#FF6B35]/50 transition-all duration-300">
            <img 
              src={getMoviePoster(currentMovie)} 
              alt={currentMovie.title}
              onError={(e) => handleImageError(e, currentMovie.title)}
              className="w-full h-full object-cover transform group-hover:scale-105 transition-transform duration-500"
            />
            
            {/* Top Badges */}
            <div className="absolute top-3 left-3 right-3 flex items-center justify-between gap-2">
              <span className="flex items-center gap-1 bg-black/75 backdrop-blur-md px-2.5 py-1 rounded-full text-xs font-bold text-amber-400 border border-white/10 shadow-sm">
                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                {currentMovie.rating || 8.9}
              </span>
              <span className="bg-[#161C28]/90 backdrop-blur-md px-2.5 py-1 rounded-full text-[10px] font-bold text-accent-cyan border border-accent-cyan/30 shadow-sm">
                IMAX Laser
              </span>
            </div>
          </div>
        </div>

        {/* Right: Movie Details & Actions */}
        <div className="relative z-10 flex-1 text-center md:text-left space-y-4">
          <div className="flex items-center justify-center md:justify-start gap-2">
            <span className="text-accent-cyan text-xs font-bold tracking-widest uppercase">
              Now Screening
            </span>
            <span className="text-gray-500">•</span>
            <span className="text-gray-400 text-xs font-medium">Verified Theatrical Screening</span>
          </div>

          <h1 className="text-3xl md:text-5xl font-extrabold text-white tracking-tight leading-tight">
            {currentMovie.title}
          </h1>

          {/* Badges row */}
          <div className="flex flex-wrap items-center justify-center md:justify-start gap-2.5 pt-1">
            <span className="bg-[#161C28] border border-[#262E42] px-3 py-1 rounded-full text-xs font-semibold text-gray-300">
              {currentMovie.genre}
            </span>
            <span className="bg-[#161C28] border border-[#262E42] px-3 py-1 rounded-full text-xs font-semibold text-gray-300 flex items-center gap-1.5">
              <Clock className="w-3 h-3 text-accent-cyan" />
              {currentMovie.duration || 150} mins
            </span>
            <span className="bg-[#161C28] border border-[#262E42] px-3 py-1 rounded-full text-xs font-semibold text-gray-300">
              {currentMovie.language || 'English'}
            </span>
            {currentMovie.mood && (
              <span className="bg-purple-950/60 border border-purple-500/40 text-purple-300 px-3 py-1 rounded-full text-xs font-semibold flex items-center gap-1">
                <Sparkles className="w-3 h-3" /> {currentMovie.mood}
              </span>
            )}
          </div>

          <p className="text-sm text-gray-400 max-w-lg mx-auto md:mx-0 leading-relaxed">
            {currentMovie.description || 'Experience unparalleled clarity and 12-channel surround sound with reserved prime seating. Book now with zero transaction lock contention.'}
          </p>

          {/* Primary CTA Button */}
          <div className="pt-4 flex items-center justify-center md:justify-start gap-4">
            <button
              onClick={handleSelect}
              className="px-6 py-3.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white font-bold text-sm rounded-2xl transition-all shadow-lg shadow-primary-500/25 flex items-center gap-2 transform active:scale-95"
            >
              <Ticket className="w-4 h-4" />
              <span>Book Prime Seats</span>
            </button>

            {/* Carousel navigation buttons */}
            <div className="flex items-center gap-2">
              <button
                onClick={handlePrev}
                className="w-10 h-10 rounded-full bg-[#161C28] border border-[#262E42] flex items-center justify-center text-gray-400 hover:text-white hover:border-primary-500 transition-colors"
                title="Previous Movie"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                onClick={handleNext}
                className="w-10 h-10 rounded-full bg-[#161C28] border border-[#262E42] flex items-center justify-center text-gray-400 hover:text-white hover:border-primary-500 transition-colors"
                title="Next Movie"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

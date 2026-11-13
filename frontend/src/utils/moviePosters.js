// Centralized movie poster resolution & fallback utility

const KNOWN_POSTERS = {
  'jawan': 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdnT7OTN5iPqcTD9hozOIuIVG8IWbeFLcPMWDFybXLyw&s=10',
  'dune': 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80',
  'interstellar': 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80',
  'spider': 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80',
  'gladiator': 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=800&q=80',
  'oppenheimer': 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=800&q=80',
  'batman': 'https://images.unsplash.com/photo-1509347528160-9a9e33742cdb?auto=format&fit=crop&w=800&q=80',
  'avatar': 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80',
  'animal': 'https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&w=800&q=80',
  'kalki': 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80',
  'stree': 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80'
};

const DEFAULT_CINEMA_POSTER = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80';

export function getMoviePoster(movieOrTitle, fallbackUrl = '') {
  if (!movieOrTitle) return fallbackUrl || DEFAULT_CINEMA_POSTER;

  let title = '';
  let posterUrl = '';

  if (typeof movieOrTitle === 'object') {
    title = movieOrTitle.title || movieOrTitle.movieTitle || '';
    posterUrl = movieOrTitle.posterUrl || '';
  } else {
    title = String(movieOrTitle);
  }

  const cleanTitle = title.toLowerCase().trim();

  // 1. If title matches known movies, always prefer known verified poster if current one is missing or generic
  for (const [key, url] of Object.entries(KNOWN_POSTERS)) {
    if (cleanTitle.includes(key)) {
      if (!posterUrl || posterUrl.includes('placeholder') || cleanTitle.includes('jawan')) {
        return url;
      }
    }
  }

  // 2. Return valid existing poster if present
  if (posterUrl && posterUrl.trim().length > 10) {
    return posterUrl.trim();
  }

  // 3. Fallback based on known keywords or default cinema poster
  for (const [key, url] of Object.entries(KNOWN_POSTERS)) {
    if (cleanTitle.includes(key)) {
      return url;
    }
  }

  return fallbackUrl || DEFAULT_CINEMA_POSTER;
}

export function handleImageError(e, title = '') {
  e.currentTarget.onerror = null;
  const cleanTitle = String(title || '').toLowerCase().trim();
  for (const [key, url] of Object.entries(KNOWN_POSTERS)) {
    if (cleanTitle.includes(key)) {
      e.currentTarget.src = url;
      return;
    }
  }
  e.currentTarget.src = DEFAULT_CINEMA_POSTER;
}

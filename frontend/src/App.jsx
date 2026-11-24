import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import HomeScreen from './components/HomeScreen';
import AboutScreen from './components/AboutScreen';
import MovieDiscovery from './components/MovieDiscovery';
import SeatSelectionModal from './components/SeatSelectionModal';
import PassWallet from './components/PassWallet';
import ResaleMarketplace from './components/ResaleMarketplace';
import BudgetOptimizerView from './components/BudgetOptimizerView';
import GroupLedgerView from './components/GroupLedgerView';
import VibeSearchModal from './components/VibeSearchModal';
import OwnerPortal from './components/OwnerPortal';
import AdminPortal from './components/AdminPortal';
import AuthModal from './components/AuthModal';
import AuthPage from './components/AuthPage';
import { Ticket, ShieldCheck, Building2, Lock } from 'lucide-react';

function AuthGate({ requiredRole, onNavigateTab }) {
  const isOwner = requiredRole === 'ROLE_THEATRE_OWNER';
  const isAdmin = requiredRole === 'ROLE_ADMIN';

  return (
    <div className="max-w-md mx-auto my-16 p-8 bg-[#121622] border border-[#262E42] rounded-3xl text-center shadow-2xl">
      <div className={`w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4 ${
        isAdmin ? 'bg-primary-500/10 text-primary-400 border border-primary-500/20 shadow-lg shadow-primary-500/10' :
        isOwner ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 shadow-lg shadow-sky-500/10' :
        'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-lg shadow-emerald-500/10'
      }`}>
        {isAdmin ? <ShieldCheck className="w-7 h-7" /> : isOwner ? <Building2 className="w-7 h-7" /> : <Lock className="w-7 h-7" />}
      </div>
      <h2 className="text-xl font-bold text-white mb-2">
        {isAdmin ? 'Administrator Access Required' : isOwner ? 'Cinema Owner Access Required' : 'Sign In Required'}
      </h2>
      <p className="text-xs text-slate-400 mb-6 leading-relaxed">
        {isAdmin 
          ? 'Canonical catalog ingestion, platform metrics, and user tombstoning require Administrator privileges.'
          : isOwner
          ? 'Auditorium screen configuration, analytics, and showtime programming require Cinema Owner privileges.'
          : 'Please sign in or create an account to access tickets, budget optimizer, and split bills.'}
      </p>
      <div className="flex gap-3 justify-center">
        <button
          onClick={() => onNavigateTab('login')}
          className="px-6 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white text-xs font-bold rounded-xl shadow-lg shadow-primary-500/25 transition-all"
        >
          Sign In
        </button>
        <button
          onClick={() => onNavigateTab('register')}
          className="px-6 py-2.5 bg-[#161C28] hover:bg-[#1E2638] text-slate-300 text-xs font-bold rounded-xl border border-[#262E42] transition-colors"
        >
          Register
        </button>
      </div>
    </div>
  );
}

// Graceful Error Boundary
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError() {
    return { hasError: true };
  }
  componentDidCatch(error, errorInfo) {
    console.warn('UI notification:', error, errorInfo);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#0A0D14] text-white flex flex-col items-center justify-center p-6 text-center">
          <div className="w-14 h-14 rounded-2xl bg-primary-500/10 border border-primary-500/20 text-primary-400 flex items-center justify-center mb-4 text-2xl font-black">
            CS
          </div>
          <h2 className="text-xl font-bold mb-2">Refreshing Movie Showcase</h2>
          <p className="text-slate-400 text-xs max-w-md mb-6">
            Tap below to reload and continue browsing live cinema showtimes.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2.5 rounded-xl bg-primary-500 hover:bg-primary-600 text-white text-xs font-bold shadow-lg shadow-primary-500/25 transition-all"
          >
            Reload Movies
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

function MainLayout() {
  const { user } = useAuth();
  // Starts on the rich Home Screen!
  const [activeTab, setActiveTab] = useState('home');

  // Modals state
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [isVibeOpen, setIsVibeOpen] = useState(false);
  const [selectedMovieForBooking, setSelectedMovieForBooking] = useState(null);
  const [resaleInitialBooking, setResaleInitialBooking] = useState(null);

  const handleBookingComplete = (bookingResult) => {
    setActiveTab('wallet');
  };

  return (
    <div className="min-h-screen bg-[#0A0D14] text-slate-100 flex flex-col font-sans selection:bg-[#FF6B35] selection:text-white">
      {/* Top Navbar & Mobile Bottom Tabs */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenAuth={() => setIsAuthOpen(true)}
        onOpenVibeSearch={() => setIsVibeOpen(true)}
      />

      {/* Main Container View */}
      <main className="flex-1 pb-24 md:pb-12">
        {activeTab === 'home' && (
          <HomeScreen
            onNavigateTab={setActiveTab}
            onOpenVibeSearch={() => setIsVibeOpen(true)}
            onSelectMovieForBooking={(movie) => setSelectedMovieForBooking(movie)}
          />
        )}

        {activeTab === 'movies' && (
          <MovieDiscovery 
            onSelectMovie={(movie) => setSelectedMovieForBooking(movie)}
            onOpenVibeSearch={() => setIsVibeOpen(true)}
            onNavigateTab={setActiveTab}
          />
        )}

        {activeTab === 'wallet' && (
          user ? (
            <PassWallet 
              onNavigateTab={setActiveTab}
              onNavigateToDiscovery={() => setActiveTab('movies')}
              onOpenResaleModal={(targetBooking) => {
                setResaleInitialBooking(targetBooking || null);
                setActiveTab('resale');
              }}
            />
          ) : (
            <AuthGate onNavigateTab={setActiveTab} />
          )
        )}

        {activeTab === 'resale' && (
          user ? (
            <ResaleMarketplace 
              initialBooking={resaleInitialBooking}
              onOpenAuth={() => setActiveTab('login')} 
              onNavigateTab={setActiveTab}
            />
          ) : (
            <AuthGate onNavigateTab={setActiveTab} />
          )
        )}

        {activeTab === 'budget' && (
          user ? (
            <BudgetOptimizerView 
              onNavigateTab={setActiveTab}
              onSelectShowForBooking={(show) => {
                setSelectedMovieForBooking({
                  id: show.movieId || show.id || 1,
                  title: show.movieTitle,
                  posterUrl: show.posterUrl || 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80',
                  genre: show.genre || 'Sci-Fi / IMAX',
                  initialShowId: show.showId
                });
              }} 
            />
          ) : (
            <AuthGate onNavigateTab={setActiveTab} />
          )
        )}

        {activeTab === 'ledger' && (
          user ? (
            <GroupLedgerView 
              onOpenAuth={() => setActiveTab('login')} 
              onNavigateTab={setActiveTab}
            />
          ) : (
            <AuthGate onNavigateTab={setActiveTab} />
          )
        )}

        {activeTab === 'about' && (
          <AboutScreen onNavigateTab={setActiveTab} />
        )}

        {activeTab === 'owner' && (
          (user?.role === 'ROLE_THEATRE_OWNER' || user?.role === 'ROLE_ADMIN') ? (
            <OwnerPortal />
          ) : (
            <AuthGate requiredRole="ROLE_THEATRE_OWNER" onNavigateTab={setActiveTab} />
          )
        )}

        {activeTab === 'admin' && (
          user?.role === 'ROLE_ADMIN' ? (
            <AdminPortal />
          ) : (
            <AuthGate requiredRole="ROLE_ADMIN" onNavigateTab={setActiveTab} />
          )
        )}

        {(activeTab === 'login' || activeTab === 'register') && (
          <AuthPage 
            initialMode={activeTab === 'register' ? 'register' : 'login'} 
            onNavigateTab={setActiveTab} 
          />
        )}
      </main>

      {/* Modals */}
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
      />

      <VibeSearchModal
        isOpen={isVibeOpen}
        onClose={() => setIsVibeOpen(false)}
        onSelectMovie={(movie) => setSelectedMovieForBooking(movie)}
      />

      <SeatSelectionModal
        movie={selectedMovieForBooking}
        onClose={() => setSelectedMovieForBooking(null)}
        onBookingComplete={handleBookingComplete}
        onBookingSuccess={handleBookingComplete}
        onOpenAuth={() => setActiveTab('login')}
      />

      {/* Simple, Clean Footer */}
      <footer className="border-t border-[#262E42]/40 py-8 text-center text-xs text-slate-500 hidden md:block">
        <div className="max-w-7xl mx-auto px-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span 
              onClick={() => setActiveTab('home')} 
              className="font-extrabold text-white cursor-pointer hover:text-primary-400"
            >
              Cine<span className="text-[#FF6B35]">Sync</span>
            </span>
            <span>• Smart Theatrical Cinema Experience</span>
          </div>
          <div className="flex items-center gap-6 text-[11px] text-slate-400">
            {(!user || user.role === 'ROLE_USER') ? (
              <>
                <button onClick={() => setActiveTab('home')} className="hover:text-primary-400 transition-colors">
                  Home
                </button>
                <button onClick={() => setActiveTab('movies')} className="hover:text-primary-400 transition-colors">
                  Now Showing
                </button>
                <button onClick={() => setActiveTab('budget')} className="hover:text-primary-400 transition-colors">
                  Budget Planner
                </button>
                <button onClick={() => setActiveTab('ledger')} className="hover:text-primary-400 transition-colors">
                  Split Bill
                </button>
                <button onClick={() => setActiveTab('resale')} className="hover:text-primary-400 transition-colors">
                  Spare Tickets
                </button>
                <button onClick={() => setActiveTab('wallet')} className="hover:text-primary-400 transition-colors">
                  My Tickets
                </button>
              </>
            ) : user.role === 'ROLE_THEATRE_OWNER' ? (
              <>
                <button onClick={() => setActiveTab('owner')} className="hover:text-primary-400 transition-colors">
                  Operations Dashboard
                </button>
                <button onClick={() => setActiveTab('movies')} className="hover:text-primary-400 transition-colors">
                  Cinema Releases
                </button>
              </>
            ) : (
              <>
                <button onClick={() => setActiveTab('admin')} className="hover:text-primary-400 transition-colors">
                  Admin Portal
                </button>
                <button onClick={() => setActiveTab('movies')} className="hover:text-primary-400 transition-colors">
                  Movie Catalog
                </button>
              </>
            )}
            <button onClick={() => setActiveTab('about')} className="hover:text-primary-400 transition-colors">
              About
            </button>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <MainLayout />
      </AuthProvider>
    </ErrorBoundary>
  );
}

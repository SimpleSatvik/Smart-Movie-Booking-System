import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Home,
  Clapperboard, 
  Sparkles, 
  Ticket, 
  Repeat, 
  PiggyBank, 
  Receipt, 
  User, 
  LogOut, 
  LogIn,
  UserPlus,
  Building2,
  ShieldCheck,
  Info
} from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, onOpenAuth, onOpenVibeSearch }) {
  const { user, logout } = useAuth();
  const [showProfileMenu, setShowProfileMenu] = useState(false);

  // Dynamic Navigation Items tailored strictly to authenticated role
  const getNavItems = () => {
    // 1. PUBLIC VISITOR (Not logged in)
    if (!user) {
      return [
        { id: 'home', label: 'Home', icon: Home },
        { id: 'movies', label: 'Now Showing', icon: Clapperboard },
        { id: 'about', label: 'About', icon: Info },
      ];
    }

    // 2. CINEMA OWNER
    if (user.role === 'ROLE_THEATRE_OWNER') {
      return [
        { id: 'owner', label: 'Operations Dashboard', icon: Building2 },
        { id: 'movies', label: 'Cinema Releases', icon: Clapperboard },
        { id: 'about', label: 'About', icon: Info },
      ];
    }

    // 3. PLATFORM ADMINISTRATOR
    if (user.role === 'ROLE_ADMIN') {
      return [
        { id: 'admin', label: 'Admin Portal', icon: ShieldCheck },
        { id: 'movies', label: 'Canonical Catalog', icon: Clapperboard },
        { id: 'about', label: 'About', icon: Info },
      ];
    }

    // 4. MOVIEGOER (Regular customer)
    return [
      { id: 'home', label: 'Home', icon: Home },
      { id: 'movies', label: 'Now Showing', icon: Clapperboard },
      { id: 'wallet', label: 'FastPass Wallet', icon: Ticket },
      { id: 'budget', label: 'Budget Planner', icon: PiggyBank },
      { id: 'ledger', label: 'Split Bill', icon: Receipt },
      { id: 'resale', label: 'Spare Tickets', icon: Repeat },
      { id: 'about', label: 'About', icon: Info },
    ];
  };

  const navItems = getNavItems();

  const getRoleBadge = (role) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-primary-500/20 text-primary-400 border border-primary-500/30">
            Platform Admin
          </span>
        );
      case 'ROLE_THEATRE_OWNER':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-sky-500/20 text-sky-400 border border-sky-500/30">
            Cinema Owner
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
            Moviegoer
          </span>
        );
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full glass-panel border-b border-[#262E42]/60">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between gap-4">
        
        {/* Brand Logo - Returns to Default View */}
        <div 
          onClick={() => {
            if (user?.role === 'ROLE_ADMIN') setActiveTab('admin');
            else if (user?.role === 'ROLE_THEATRE_OWNER') setActiveTab('owner');
            else setActiveTab('home');
          }}
          className="flex items-center gap-3 cursor-pointer group"
          title="Go to CineSync"
        >
          <div className="w-10 h-10 rounded-xl bg-[#161C28] border border-[#262E42] flex items-center justify-center text-primary-500 shadow-glow-orange group-hover:scale-105 transition-transform">
            <Clapperboard className="w-5 h-5 text-primary-500" />
          </div>
          <div>
            <span className="text-xl font-bold tracking-tight text-white flex items-center gap-1.5">
              Cine<span className="text-primary-500">Sync</span>
            </span>
            <span className="text-[10px] tracking-wider uppercase text-gray-400 font-semibold block -mt-1">
              Smart Movie Experience
            </span>
          </div>
        </div>

        {/* Primary Navigation Tabs (Desktop) */}
        <nav className="hidden lg:flex items-center gap-1 bg-[#121622]/90 p-1.5 rounded-full border border-[#262E42]">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-semibold tracking-wide transition-all duration-200 ${
                  isActive
                    ? 'bg-primary-500 text-white shadow-md shadow-primary-500/25'
                    : 'text-gray-300 hover:text-white hover:bg-[#161C28]'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                {item.label}
              </button>
            );
          })}
        </nav>

        {/* Right Action Bar */}
        <div className="flex items-center gap-3">
          {/* AI Vibe Search Pill (Available for customers and public) */}
          {(!user || user.role === 'ROLE_USER') && (
            <button 
              onClick={onOpenVibeSearch}
              className="flex items-center gap-2 bg-[#161C28] hover:bg-[#1E2638] border border-accent-cyan/30 hover:border-accent-cyan px-3.5 py-2 rounded-full text-xs text-accent-cyan hover:text-white font-semibold transition-all shadow-sm"
            >
              <Sparkles className="w-3.5 h-3.5 text-accent-cyan" />
              <span className="hidden sm:inline">Ask AI Vibe</span>
            </button>
          )}

          {/* User Account / Profile */}
          <div className="relative">
            {user ? (
              <div 
                onClick={() => setShowProfileMenu(!showProfileMenu)}
                className="flex items-center gap-2.5 bg-[#161C28] hover:bg-[#1E2638] border border-[#262E42] px-3.5 py-1.5 rounded-full cursor-pointer transition-all"
              >
                <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-primary-500 to-amber-400 text-white font-bold flex items-center justify-center text-xs">
                  {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <div className="hidden md:block text-left">
                  <p className="text-xs font-semibold text-white truncate max-w-[100px]">{user.name}</p>
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setActiveTab('login')}
                  className="flex items-center gap-1.5 bg-[#161C28] hover:bg-[#1E2638] border border-[#262E42] text-white text-xs font-bold px-3.5 py-1.5 rounded-full transition-all"
                >
                  <LogIn className="w-3.5 h-3.5 text-primary-400" />
                  Sign In
                </button>
                <button
                  onClick={() => setActiveTab('register')}
                  className="hidden sm:flex items-center gap-1.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 text-white text-xs font-bold px-3.5 py-1.5 rounded-full shadow-lg shadow-primary-500/25 transition-all"
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  Register
                </button>
              </div>
            )}

            {/* Profile Dropdown */}
            {showProfileMenu && user && (
              <div className="absolute right-0 mt-3 w-60 bg-[#121622] border border-[#262E42] rounded-2xl shadow-2xl p-3 z-50 animate-in fade-in duration-150">
                <div className="px-3 py-2 border-b border-[#262E42]/60">
                  <p className="text-sm font-bold text-white">{user.name}</p>
                  <p className="text-xs text-gray-400 truncate mb-2">{user.email || user.gmail}</p>
                  {getRoleBadge(user.role)}
                </div>

                <div className="pt-2">
                  <button
                    onClick={() => { 
                      logout(); 
                      setActiveTab('login'); 
                      setShowProfileMenu(false); 
                    }}
                    className="w-full text-left px-3 py-2 rounded-xl text-xs font-medium text-red-400 hover:bg-red-500/10 flex items-center gap-2 transition-colors"
                  >
                    <LogOut className="w-3.5 h-3.5" /> Sign Out
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Bottom Navigation Bar */}
      <nav className="lg:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#0A0D14]/95 backdrop-blur-lg border-t border-[#262E42] px-2 py-2 flex items-center justify-around shadow-2xl">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`flex flex-col items-center gap-1 py-1 px-2 rounded-xl transition-all ${
                isActive ? 'text-primary-500 scale-105' : 'text-gray-400 hover:text-white'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span className="text-[10px] font-semibold">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </header>
  );
}

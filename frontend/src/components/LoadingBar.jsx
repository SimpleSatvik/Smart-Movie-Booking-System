import React from 'react';
import { Loader2 } from 'lucide-react';

export default function LoadingBar({
  title = 'Fetching data...',
  subtitle = 'Please wait while CineSync syncs with the server',
  compact = false,
  showSkeleton = true
}) {
  if (compact) {
    return (
      <div className="w-full bg-[#161C28]/90 border border-[#262E42] rounded-2xl p-4 my-4 shadow-lg animate-in fade-in">
        <div className="flex items-center justify-between gap-3 mb-2.5">
          <div className="flex items-center gap-2">
            <Loader2 className="w-4 h-4 text-primary-500 animate-spin flex-shrink-0" />
            <span className="text-xs font-bold text-white tracking-wide">{title}</span>
          </div>
          <span className="text-[11px] font-mono text-primary-400 font-semibold uppercase tracking-wider animate-pulse">
            Fetching...
          </span>
        </div>
        
        {/* Animated Bar */}
        <div className="w-full h-1.5 bg-[#0A0D14] rounded-full overflow-hidden relative">
          <div className="h-full bg-gradient-to-r from-primary-500 via-accent-cyan to-primary-500 w-1/3 rounded-full animate-loading-bar" />
        </div>
        {subtitle && (
          <p className="text-[11px] text-slate-400 mt-2 font-medium">{subtitle}</p>
        )}
      </div>
    );
  }

  return (
    <div className="w-full space-y-6 my-6 animate-in fade-in duration-200">
      {/* Top Banner with Loading Bar */}
      <div className="bg-gradient-to-r from-[#121622] via-[#161C28] to-[#121622] border border-[#262E42] rounded-3xl p-6 shadow-xl relative overflow-hidden">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-primary-500/10 border border-primary-500/20 flex items-center justify-center text-primary-500">
              <Loader2 className="w-5 h-5 animate-spin" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
                {title}
                <span className="inline-block w-2 h-2 rounded-full bg-primary-500 animate-ping" />
              </h3>
              {subtitle && <p className="text-xs text-slate-400 mt-0.5">{subtitle}</p>}
            </div>
          </div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-mono font-bold bg-primary-500/10 text-primary-400 border border-primary-500/30 self-start sm:self-auto">
            <span className="w-1.5 h-1.5 rounded-full bg-primary-400 animate-pulse" />
            Fetching Live Data...
          </span>
        </div>

        {/* The Animated Loading Bar */}
        <div className="w-full h-2 bg-[#0A0D14] rounded-full overflow-hidden relative">
          <div className="h-full bg-gradient-to-r from-primary-500 via-accent-cyan to-primary-500 w-2/5 rounded-full animate-loading-bar shadow-lg shadow-primary-500/50" />
        </div>
      </div>

      {/* Structural Skeleton Placeholder so screen is never blank */}
      {showSkeleton && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div 
              key={i} 
              className="bg-[#121622]/60 border border-[#262E42]/60 rounded-3xl p-5 space-y-3 animate-pulse"
            >
              <div className="h-4 bg-[#1F293D] rounded-lg w-1/3" />
              <div className="h-8 bg-[#1F293D] rounded-xl w-3/4" />
              <div className="h-3 bg-[#1F293D]/60 rounded w-1/2" />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

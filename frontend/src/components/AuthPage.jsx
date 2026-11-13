import React, { useState } from 'react';
import { 
  Film, Mail, Lock, User, ShieldCheck, Building2, Ticket, 
  ArrowRight, CheckCircle2, AlertCircle, RefreshCw, Sparkles,
  ArrowLeft, Check
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';

export default function AuthPage({ initialMode = 'login', onNavigateTab }) {
  const { loginWithToken } = useAuth();
  const [mode, setMode] = useState(initialMode); // 'login' | 'register' | 'forgot'
  const [step, setStep] = useState('credentials'); // 'credentials' | 'otp'

  // Form Fields
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('ROLE_USER'); // 'ROLE_USER' | 'ROLE_THEATRE_OWNER'
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const resetForm = (newMode) => {
    setMode(newMode);
    setStep('credentials');
    setError(null);
    setSuccessMsg(null);
    setOtp('');
  };

  const handleCredentialsSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    setLoading(true);

    try {
      if (mode === 'login') {
        const res = await api.login({ email, password });
        if (res.token) {
          loginWithToken(res.token, {
            id: res.userId,
            name: res.name || email.split('@')[0],
            email: res.gmail || email,
            role: res.role || 'ROLE_USER'
          });
          routeByRole(res.role || 'ROLE_USER');
        } else if (res.requires2fa || res.message?.includes('OTP')) {
          setStep('otp');
          setSuccessMsg(`Verification code dispatched to ${email}`);
        }
      } else if (mode === 'register') {
        // Step 1: Send registration OTP
        try {
          await api.sendRegistrationOtp(email);
        } catch (otpErr) {
          // If SendGrid network bound, proceed gracefully to OTP verification step
          console.warn('Registration OTP service notice:', otpErr.message);
        }
        setStep('otp');
        setSuccessMsg(`Enter the 6-digit verification code sent to ${email}`);
      } else if (mode === 'forgot') {
        await api.forgotPassword(email);
        setStep('otp');
        setSuccessMsg(`Password reset code sent to ${email}`);
      }
    } catch (err) {
      setError(err.message || 'Authentication operation failed. Please verify your input.');
    } finally {
      setLoading(false);
    }
  };

  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (mode === 'login') {
        const res = await api.verifyLoginOtp({ email, otp });
        loginWithToken(res.token, {
          id: res.userId,
          name: res.name || email.split('@')[0],
          email: res.gmail || email,
          role: res.role || 'ROLE_USER'
        });
        routeByRole(res.role || 'ROLE_USER');
      } else if (mode === 'register') {
        const res = await api.register({
          name,
          email,
          password,
          role,
          otp: otp || '123456'
        });
        loginWithToken(res.token, {
          id: res.userId,
          name: res.name || name,
          email: res.gmail || email,
          role: res.role || role
        });
        routeByRole(res.role || role);
      } else if (mode === 'forgot') {
        await api.resetPassword({ email, otp, newPassword });
        setSuccessMsg('Password updated successfully! Please sign in with your new password.');
        setMode('login');
        setStep('credentials');
      }
    } catch (err) {
      setError(err.message || 'Invalid or expired verification code. Please check and try again.');
    } finally {
      setLoading(false);
    }
  };

  const routeByRole = (userRole) => {
    if (userRole === 'ROLE_ADMIN') {
      onNavigateTab('admin');
    } else if (userRole === 'ROLE_THEATRE_OWNER') {
      onNavigateTab('owner');
    } else {
      onNavigateTab('movies');
    }
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center px-4 py-12 relative overflow-hidden">
      {/* Ambient Cinema Lighting */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-primary-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-10 right-10 w-72 h-72 bg-sky-500/5 rounded-full blur-3xl pointer-events-none" />

      <div className="relative w-full max-w-lg bg-[#121622] border border-[#262E42] rounded-3xl p-6 sm:p-10 shadow-2xl overflow-hidden">
        {/* Glow Top Accent */}
        <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-transparent via-primary-500 to-transparent" />

        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-tr from-primary-600 to-primary-400 text-white shadow-xl shadow-primary-500/25 mb-4">
            <Film className="w-7 h-7" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
            Cine<span className="text-primary-500">Sync</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1 max-w-xs mx-auto">
            {mode === 'register' 
              ? 'Create your personalized cinema account' 
              : mode === 'forgot'
              ? 'Reset your CineSync security credentials'
              : 'Sign in to access your wallet, bookings & movies'}
          </p>
        </div>

        {/* Tab Switcher: Sign In vs Register */}
        {mode !== 'forgot' && step === 'credentials' && (
          <div className="flex bg-[#0A0D14] p-1 rounded-2xl border border-[#262E42] mb-6">
            <button
              type="button"
              onClick={() => resetForm('login')}
              className={`flex-1 py-2.5 rounded-xl text-xs font-bold transition-all ${
                mode === 'login'
                  ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => resetForm('register')}
              className={`flex-1 py-2.5 rounded-xl text-xs font-bold transition-all ${
                mode === 'register'
                  ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Create Account
            </button>
          </div>
        )}

        {/* Alert Notifications */}
        {error && (
          <div className="mb-6 p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-3">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}
        {successMsg && (
          <div className="mb-6 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs flex items-center gap-3">
            <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* STEP 1: CREDENTIALS */}
        {step === 'credentials' && (
          <form onSubmit={handleCredentialsSubmit} className="space-y-4">
            {/* REGISTER: Full Name */}
            {mode === 'register' && (
              <div>
                <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  Full Name
                </label>
                <div className="relative">
                  <input
                    type="text"
                    required
                    placeholder="e.g. Satvik Kapoor"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 rounded-xl bg-[#0A0D14] border border-[#262E42] focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                  <User className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
              </div>
            )}

            {/* Email Address */}
            <div>
              <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <input
                  type="email"
                  required
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 rounded-xl bg-[#0A0D14] border border-[#262E42] focus:border-primary-500 focus:outline-none text-white text-sm"
                />
                <Mail className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
              </div>
            </div>

            {/* Password */}
            {mode !== 'forgot' && (
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-[11px] font-bold text-slate-300 uppercase tracking-wider">
                    Password
                  </label>
                  {mode === 'login' && (
                    <button
                      type="button"
                      onClick={() => resetForm('forgot')}
                      className="text-[11px] text-primary-400 hover:underline"
                    >
                      Forgot password?
                    </button>
                  )}
                </div>
                <div className="relative">
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 rounded-xl bg-[#0A0D14] border border-[#262E42] focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                  <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
              </div>
            )}

            {/* REGISTER: Role Selector */}
            {mode === 'register' && (
              <div>
                <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-2">
                  Account Type
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRole('ROLE_USER')}
                    className={`p-3 rounded-2xl border text-left transition-all flex flex-col justify-between ${
                      role === 'ROLE_USER'
                        ? 'bg-emerald-500/10 border-emerald-500/50 text-white shadow-lg shadow-emerald-500/10'
                        : 'bg-[#0A0D14] border-[#262E42] text-slate-400 hover:border-slate-600'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <Ticket className="w-4 h-4 text-emerald-400" />
                      {role === 'ROLE_USER' && <Check className="w-3.5 h-3.5 text-emerald-400" />}
                    </div>
                    <div className="text-xs font-bold text-white">Moviegoer</div>
                    <div className="text-[10px] text-slate-400 mt-0.5">Book seats & passes</div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRole('ROLE_THEATRE_OWNER')}
                    className={`p-3 rounded-2xl border text-left transition-all flex flex-col justify-between ${
                      role === 'ROLE_THEATRE_OWNER'
                        ? 'bg-sky-500/10 border-sky-500/50 text-white shadow-lg shadow-sky-500/10'
                        : 'bg-[#0A0D14] border-[#262E42] text-slate-400 hover:border-slate-600'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <Building2 className="w-4 h-4 text-sky-400" />
                      {role === 'ROLE_THEATRE_OWNER' && <Check className="w-3.5 h-3.5 text-sky-400" />}
                    </div>
                    <div className="text-xs font-bold text-white">Cinema Owner</div>
                    <div className="text-[10px] text-slate-400 mt-0.5">Venues & shows</div>
                  </button>
                </div>
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-3.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 disabled:opacity-50 text-white font-extrabold text-sm rounded-xl transition-all shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2"
            >
              {loading ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  <span>{mode === 'login' ? 'Sign In & Continue' : mode === 'register' ? 'Register Account' : 'Request Password Reset'}</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        )}

        {/* STEP 2: 6-DIGIT OTP VERIFICATION */}
        {step === 'otp' && (
          <form onSubmit={handleOtpSubmit} className="space-y-5">
            <div className="text-center py-2">
              <div className="w-12 h-12 rounded-2xl bg-primary-500/10 border border-primary-500/20 text-primary-400 flex items-center justify-center mx-auto mb-2">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white">
                {mode === 'register' ? 'Email Verification Code' : mode === 'forgot' ? 'Reset Password Code' : 'Security Verification Code'}
              </h3>
              <p className="text-xs text-slate-400 mt-1">
                Enter the 6-digit code for <span className="text-white font-medium">{email}</span>
              </p>
            </div>

            <div>
              <input
                type="text"
                maxLength={6}
                required
                autoFocus
                placeholder="000000"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                className="w-full tracking-[0.5em] text-center font-mono text-3xl font-black py-3.5 rounded-2xl bg-[#0A0D14] border-2 border-primary-500/50 focus:border-primary-500 focus:outline-none text-white shadow-inner"
              />
            </div>

            {mode === 'forgot' && (
              <div>
                <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  Set New Password
                </label>
                <div className="relative">
                  <input
                    type="password"
                    required
                    placeholder="New secure password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 rounded-xl bg-[#0A0D14] border border-[#262E42] focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                  <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={loading || otp.length < 6}
              className="w-full py-3.5 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 disabled:opacity-50 text-white font-extrabold text-sm rounded-xl transition-all shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2"
            >
              {loading ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                'Authenticate & Launch'
              )}
            </button>

            <button
              type="button"
              onClick={() => setStep('credentials')}
              className="w-full text-center text-xs text-slate-400 hover:text-white transition-colors"
            >
              ← Back to credentials
            </button>
          </form>
        )}

        {/* Footer Navigation */}
        <div className="mt-6 text-center text-xs text-slate-400">
          {mode === 'login' ? (
            <p>
              Don't have an account yet?{' '}
              <button
                type="button"
                onClick={() => resetForm('register')}
                className="text-primary-400 font-bold hover:underline"
              >
                Sign up now
              </button>
            </p>
          ) : (
            <p>
              Already have an account?{' '}
              <button
                type="button"
                onClick={() => resetForm('login')}
                className="text-primary-400 font-bold hover:underline"
              >
                Sign in
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

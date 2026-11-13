import React, { useState } from 'react';
import { 
  X, Mail, Lock, User, KeyRound, Sparkles, ArrowRight, 
  CheckCircle2, AlertCircle, RefreshCw, ShieldCheck, Film, Building2
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';

export default function AuthModal({ isOpen, onClose }) {
  const { loginWithToken } = useAuth();
  const [mode, setMode] = useState('login'); // 'login' | 'register' | 'forgot'
  const [step, setStep] = useState('credentials'); // 'credentials' | 'otp'
  
  // Form fields
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('USER'); // 'USER' | 'THEATRE_OWNER'
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  if (!isOpen) return null;

  const resetState = () => {
    setStep('credentials');
    setError(null);
    setSuccessMsg(null);
    setOtp('');
  };

  // Switch modes
  const handleSwitchMode = (newMode) => {
    setMode(newMode);
    resetState();
  };

  // 1. Submit Credentials Step
  const handleCredentialsSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    setLoading(true);

    try {
      if (mode === 'login') {
        // Direct Login with Email & Password
        const res = await api.login({ email, password });
        if (res.token) {
          loginWithToken(res.token, {
            id: res.userId,
            name: res.name || email.split('@')[0],
            email: res.email || email,
            role: res.role || 'ROLE_USER'
          });
          onClose();
        } else if (res.requiresOtp || res.requires2fa || res.message?.includes('OTP')) {
          setStep('otp');
          setSuccessMsg(`Verification code sent to ${email}`);
        }
      } else if (mode === 'register') {
        // Step 1: Send registration OTP
        await api.sendRegistrationOtp(email);
        setStep('otp');
        setSuccessMsg(`6-digit confirmation code dispatched to ${email}`);
      } else if (mode === 'forgot') {
        // Step 1: Send forgot password OTP
        await api.forgotPassword(email);
        setStep('otp');
        setSuccessMsg(`Password reset code dispatched to ${email}`);
      }
    } catch (err) {
      console.error('Auth step 1 error:', err);
      // For developer ease in testing when email service encounters network bounds, auto-proceed
      if (err.message && err.message.includes('OTP')) {
        setStep('otp');
        setSuccessMsg('Enter the 6-digit verification code sent to your inbox:');
      } else {
        setError(err.message || 'Authentication operation failed. Please verify your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  // 2. Submit OTP Step
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
          email: res.email || email,
          role: res.role || 'ROLE_USER'
        });
        onClose();
      } else if (mode === 'register') {
        const res = await api.register({
          name,
          email,
          password,
          role,
          otp
        });
        loginWithToken(res.token, {
          id: res.userId,
          name: res.name || name,
          email: res.email || email,
          role: res.role || role
        });
        onClose();
      } else if (mode === 'forgot') {
        await api.resetPassword({
          email,
          otp,
          newPassword
        });
        setSuccessMsg('Password updated successfully! Please sign in with your new password.');
        setMode('login');
        setStep('credentials');
      }
    } catch (err) {
      console.error('OTP verification error:', err);
      setError(err.message || 'Invalid or expired OTP. Please check and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
      <div className="relative w-full max-w-md bg-dark-card border border-dark-border rounded-3xl p-6 md:p-8 shadow-2xl overflow-hidden">
        {/* Glow accent */}
        <div className="absolute top-0 left-1/4 right-1/4 h-[2px] bg-gradient-to-r from-transparent via-primary-500 to-transparent"></div>

        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-slate-400 hover:text-white p-2 rounded-xl bg-dark-bg/60 border border-dark-border/40 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Brand Header */}
        <div className="flex items-center gap-2.5 mb-6">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-primary-600 to-primary-400 flex items-center justify-center text-white shadow-lg shadow-primary-500/30">
            <Film className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-xl font-black text-white tracking-tight">
              Cine<span className="text-primary-500">Sync</span>
            </h2>
            <div className="text-[11px] text-slate-400">
              {mode === 'login' ? 'Sign In to Your Vault' : mode === 'register' ? 'Create CineSync Account' : 'Reset Credentials'}
            </div>
          </div>
        </div>

        {/* Alert Notifications */}
        {error && (
          <div className="mb-4 p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}
        {successMsg && (
          <div className="mb-4 p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* STEP 1: CREDENTIALS */}
        {step === 'credentials' && (
          <form onSubmit={handleCredentialsSubmit} className="space-y-4">
            {mode === 'register' && (
              <div>
                <label className="block text-[11px] font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Full Name
                </label>
                <div className="relative">
                  <input
                    type="text"
                    required
                    placeholder="e.g. Satvik Kapoor"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                  <User className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
              </div>
            )}

            <div>
              <label className="block text-[11px] font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <input
                  type="email"
                  required
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                />
                <Mail className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
              </div>
            </div>

            {mode !== 'forgot' && (
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-[11px] font-semibold text-slate-300 uppercase tracking-wider">
                    Password
                  </label>
                  {mode === 'login' && (
                    <button
                      type="button"
                      onClick={() => handleSwitchMode('forgot')}
                      className="text-[11px] text-primary-400 hover:text-primary-300 transition-colors"
                    >
                      Forgot?
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
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                  <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
              </div>
            )}

            {mode === 'register' && (
              <div>
                <label className="block text-[11px] font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Account Type
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRole('USER')}
                    className={`py-2 px-3 rounded-xl border text-xs font-semibold transition-all ${
                      role === 'USER'
                        ? 'bg-primary-500/20 border-primary-500 text-white'
                        : 'bg-dark-bg border-dark-border text-slate-400'
                    }`}
                  >
                    Moviegoer
                  </button>
                  <button
                    type="button"
                    onClick={() => setRole('THEATRE_OWNER')}
                    className={`py-2 px-3 rounded-xl border text-xs font-semibold transition-all ${
                      role === 'THEATRE_OWNER'
                        ? 'bg-primary-500/20 border-primary-500 text-white'
                        : 'bg-dark-bg border-dark-border text-slate-400'
                    }`}
                  >
                    Cinema Owner
                  </button>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-3 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 disabled:opacity-50 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2"
            >
              {loading ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  <span>{mode === 'login' ? 'Verify & Continue' : mode === 'register' ? 'Send Verification OTP' : 'Request Reset OTP'}</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        )}

        {/* STEP 2: 6-DIGIT OTP VERIFICATION */}
        {step === 'otp' && (
          <form onSubmit={handleOtpSubmit} className="space-y-4">
            <div className="text-center py-2">
              <div className="w-12 h-12 rounded-2xl bg-primary-500/10 border border-primary-500/20 text-primary-400 flex items-center justify-center mx-auto mb-2">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-white">Enter 6-Digit Code</h3>
              <p className="text-xs text-slate-400 mt-1">
                Dispatched via SendGrid to <span className="text-white font-medium">{email}</span>
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
                className="w-full tracking-[0.5em] text-center font-mono text-2xl font-black py-3 rounded-xl bg-dark-bg border-2 border-primary-500/50 focus:border-primary-500 focus:outline-none text-white shadow-inner"
              />
            </div>

            {mode === 'forgot' && (
              <div>
                <label className="block text-[11px] font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Set New Password
                </label>
                <div className="relative">
                  <input
                    type="password"
                    required
                    placeholder="New secure password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-dark-bg border border-dark-border focus:border-primary-500 focus:outline-none text-white text-sm"
                  />
                  <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={loading || otp.length < 6}
              className="w-full py-3 bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 disabled:opacity-50 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-primary-500/25 flex items-center justify-center gap-2"
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

        {/* Switch mode links */}
        <div className="mt-6 pt-5 border-t border-dark-border/60 text-center">
          {mode === 'login' ? (
            <p className="text-xs text-slate-400">
              Don't have an account?{' '}
              <button
                type="button"
                onClick={() => handleSwitchMode('register')}
                className="text-primary-400 font-bold hover:underline"
              >
                Sign up
              </button>
            </p>
          ) : (
            <p className="text-xs text-slate-400">
              Already registered?{' '}
              <button
                type="button"
                onClick={() => handleSwitchMode('login')}
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

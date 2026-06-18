import React, { useState, useEffect } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { getJavaHashCode } from '../lib/utils'
import { Shield, Mail, Lock, User, Chrome, ArrowRight, AlertCircle } from 'lucide-react'

export default function Auth() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const modeParam = searchParams.get('mode')
  
  const [isLogin, setIsLogin] = useState(modeParam !== 'register')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  useEffect(() => {
    setIsLogin(modeParam !== 'register')
    setErrorMsg('')
    setSuccessMsg('')

    // Check for OAuth error in URL hash or query params
    const hashParams = new URLSearchParams(window.location.hash.substring(1))
    const queryParams = new URLSearchParams(window.location.search)
    const error = hashParams.get('error') || queryParams.get('error')
    const errorDesc = hashParams.get('error_description') || queryParams.get('error_description')
    
    if (error || errorDesc) {
      const combined = (errorDesc || error || '').toLowerCase()
      if (combined.includes('provider') || combined.includes('unsupported') || combined.includes('enabled') || combined.includes('validation')) {
        setErrorMsg('Google Sign-In is not enabled yet. Please use email login or enable Google provider in Supabase.')
      } else if (combined.includes('google_login_failed')) {
        setErrorMsg('Google login failed. Please try again.')
      } else if (combined.includes('no_session_found')) {
        setErrorMsg('Could not retrieve session from Google. Please try again.')
      } else {
        setErrorMsg(errorDesc || error || 'OAuth error occurred.')
      }
    }
  }, [modeParam])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setErrorMsg('')
    setSuccessMsg('')

    if (!email || !password) {
      setErrorMsg('Please fill in all required fields.')
      setLoading(false)
      return
    }

    try {
      if (isLogin) {
        // Sign In
        const { data, error } = await supabase.auth.signInWithPassword({
          email,
          password
        })

        if (error) {
          if (error.message.includes('Invalid login credentials')) {
            setErrorMsg('Invalid email or password.')
          } else {
            setErrorMsg(error.message)
          }
        } else {
          // Success: Auth listener in App.jsx will capture session and load profile
          navigate('/dashboard')
        }
      } else {
        // Register / Sign Up
        if (!name) {
          setErrorMsg('Please enter your name.')
          setLoading(false)
          return
        }

        const { data, error } = await supabase.auth.signUp({
          email,
          password,
          options: {
            data: {
              name: name
            }
          }
        })

        if (error) {
          setErrorMsg(error.message)
        } else if (data?.user) {
          // Sync row in users table immediately
          const uId = getJavaHashCode(email)
          
          const { error: dbError } = await supabase
            .from('users')
            .insert({
              user_id: uId,
              name: name,
              email: email,
              age: 20,
              gender: 'Female',
              concerns: '',
              photo_url: ''
            })

          if (dbError) {
            console.error('Failed to create users row in database:', dbError)
          }

          if (data.session) {
            setSuccessMsg('Account created successfully!')
            setTimeout(() => navigate('/dashboard'), 1500)
          } else {
            setSuccessMsg('Registration successful! Please check your email to confirm your account.')
          }
        }
      }
    } catch (err) {
      setErrorMsg('An unexpected error occurred. Please try again.')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = async () => {
    setErrorMsg('')
    console.log('[DentNova] GOOGLE_SIGNIN_CLICKED')

    const { error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: `${window.location.origin}/auth/callback`
      }
    })

    if (error) {
      console.error('[DentNova] GOOGLE_SIGNIN_ERROR', error)

      // Show a friendly message instead of raw JSON
      let parsed = null
      try { parsed = JSON.parse(error.message) } catch (_) { /* not JSON */ }
      const friendly = parsed?.msg || parsed?.message || error.message || 'Google Sign-In failed.'

      if (
        friendly.toLowerCase().includes('provider') ||
        friendly.toLowerCase().includes('unsupported') ||
        friendly.toLowerCase().includes('enabled') ||
        friendly.toLowerCase().includes('validation')
      ) {
        setErrorMsg('Google Sign-In is not enabled yet. Please use email login.')
      } else {
        setErrorMsg(friendly)
      }
    }
    // If no error: Supabase redirects the browser to Google, then back to /auth/callback
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      <div className="absolute top-1/4 left-1/3 w-72 h-72 bg-cyan-400/10 dark:bg-cyan-500/5 rounded-full blur-3xl -z-10"></div>
      <div className="absolute bottom-1/4 right-1/3 w-72 h-72 bg-blue-400/10 dark:bg-blue-500/5 rounded-full blur-3xl -z-10"></div>

      <div className="w-full max-w-md bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800/80 shadow-xl rounded-2xl p-8 transition-colors duration-300">
        <div className="text-center space-y-2 mb-8">
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">
            {isLogin ? 'Welcome Back' : 'Create Account'}
          </h2>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {isLogin ? 'Access your personal AI oral health assistant' : 'Start tracking your habits and scan your teeth'}
          </p>
        </div>

        {errorMsg && (
          <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-lg text-sm flex items-center gap-2 mb-6">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {successMsg && (
          <div className="bg-emerald-50 dark:bg-emerald-900/10 border border-emerald-200 dark:border-emerald-800/30 text-emerald-600 dark:text-emerald-400 p-3 rounded-lg text-sm flex items-center gap-2 mb-6">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isLogin && (
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Full Name</label>
              <div className="relative">
                <User className="absolute left-3 top-3.5 w-4 h-4 text-slate-400" />
                <input
                  type="text"
                  placeholder="Enter your name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 transition-all text-slate-900 dark:text-white"
                />
              </div>
            </div>
          )}

          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Email Address</label>
            <div className="relative">
              <Mail className="absolute left-3 top-3.5 w-4 h-4 text-slate-400" />
              <input
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 transition-all text-slate-900 dark:text-white"
              />
            </div>
          </div>

          <div className="space-y-1">
            <div className="flex justify-between items-center">
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Password</label>
              {isLogin && (
                <Link to="/forgot-password" className="text-xs font-semibold text-cyan-600 dark:text-cyan-400 hover:underline">
                  Forgot Password?
                </Link>
              )}
            </div>
            <div className="relative">
              <Lock className="absolute left-3 top-3.5 w-4 h-4 text-slate-400" />
              <input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 transition-all text-slate-900 dark:text-white"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 hover:shadow-cyan-500/20 transition-all duration-200 flex items-center justify-center gap-2 disabled:opacity-50"
          >
            {loading ? 'Processing...' : isLogin ? 'Log In' : 'Create Account'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="relative my-6">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-200 dark:border-slate-800"></div>
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-white dark:bg-slate-900 px-3 text-slate-400 dark:text-slate-500 font-semibold">Or continue with</span>
          </div>
        </div>

        <button
          onClick={handleGoogleLogin}
          className="w-full py-3 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 transition-all duration-250 text-slate-700 dark:text-slate-200"
        >
          <Chrome className="w-4 h-4 text-red-500" />
          Sign in with Google
        </button>

        <div className="mt-8 text-center text-sm">
          <span className="text-slate-500 dark:text-slate-400">
            {isLogin ? "Don't have an account? " : "Already have an account? "}
          </span>
          <button
            onClick={() => setIsLogin(!isLogin)}
            className="font-bold text-cyan-600 dark:text-cyan-400 hover:underline"
          >
            {isLogin ? 'Sign Up' : 'Log In'}
          </button>
        </div>
      </div>
    </div>
  )
}

import React, { useState, useEffect } from 'react'
import { Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { supabase } from './supabaseClient'
import { getJavaHashCode } from './lib/utils'
import Navbar from './components/Navbar'
import Footer from './components/Footer'

// Import pages
import Landing from './pages/Landing'
import Auth from './pages/Auth'
import AuthCallback from './pages/AuthCallback'
import ForgotPassword from './pages/ForgotPassword'
import Dashboard from './pages/Dashboard'
import BrushingTimer from './pages/BrushingTimer'
import Assessment from './pages/Assessment'
import AssessmentResult from './pages/AssessmentResult'
import ToothScan from './pages/ToothScan'
import Education from './pages/Education'
import EducationDetail from './pages/EducationDetail'
import Reminders from './pages/Reminders'
import VisitReminders from './pages/VisitReminders'
import Profile from './pages/Profile'
import Settings from './pages/Settings'


export default function App() {
  const [user, setUser] = useState(null)
  const [profile, setProfile] = useState(null)
  const [streak, setStreak] = useState(0)
  const [loading, setLoading] = useState(true)
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'light')
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    // Apply theme
    if (theme === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
    localStorage.setItem('theme', theme)
  }, [theme])

  useEffect(() => {
    // Initial Session Check
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session) {
        setUser(session.user)
        fetchProfile(session.user)
      } else {
        setLoading(false)
      }
    })

    // Auth State Listeners
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session) {
        setUser(session.user)
        fetchProfile(session.user)
      } else {
        setUser(null)
        setProfile(null)
        setStreak(0)
        setLoading(false)
      }
    })

    return () => subscription.unsubscribe()
  }, [])

  const fetchProfile = async (currentUser) => {
    try {
      const email = currentUser.email
      console.log('[DentNova] WEB_AUTH_EMAIL:', email)

      // ─── Step 1: Look up user row by EMAIL ───────────────────────────────
      // Querying by email (not by computed hash) guarantees we get the SAME
      // user_id that the Android app stored, even if hash values differ.
      let { data, error } = await supabase
        .from('users')
        .select('*')
        .eq('email', email)
        .maybeSingle()

      if (error) {
        console.warn('[DentNova] users table lookup error:', error.message)
      }

      // ─── Step 2: Create row if none found ────────────────────────────────
      if (!data) {
        // Compute hash using same logic as mobile (positive hash of email)
        const uId = getJavaHashCode(email)
        console.log('[DentNova] WEB_USERS_TABLE_USER_ID (new row, creating):', uId)
        const displayName = currentUser.user_metadata?.name || email.split('@')[0]

        const { data: inserted, error: insertError } = await supabase
          .from('users')
          .insert({
            user_id: uId,
            name: displayName,
            email: email,
            age: 20,
            gender: 'Female',
            concerns: '',
            photo_url: currentUser.user_metadata?.avatar_url || ''
          })
          .select()
          .maybeSingle()

        if (insertError) {
          // Possible duplicate from another session — re-fetch by email
          console.warn('[DentNova] Insert failed (possible duplicate):', insertError.message)
          const { data: retryData } = await supabase
            .from('users')
            .select('*')
            .eq('email', email)
            .maybeSingle()
          if (retryData) data = retryData
        } else {
          data = inserted
        }
      }

      // ─── Step 3: Store the REAL DB user_id in localStorage ───────────────
      if (data) {
        // Sync Google profile image into users.photo_url if empty
        if (!data.photo_url) {
          const googlePhoto = currentUser.user_metadata?.avatar_url || currentUser.user_metadata?.picture || ''
          if (googlePhoto) {
            console.log('[DentNova] Syncing Google profile photo to users table:', googlePhoto)
            const { data: updatedData, error: syncError } = await supabase
              .from('users')
              .update({ photo_url: googlePhoto })
              .eq('user_id', data.user_id)
              .select()
              .maybeSingle()
            
            if (!syncError && updatedData) {
              data = updatedData
            }
          }
        }

        console.log('[DentNova] WEB_USERS_TABLE_USER_ID:', data.user_id)
        // Always use the integer from the database — never a locally-computed value.
        // This makes assessment/scan/reminder queries match the Android app perfectly.
        localStorage.setItem('dentnova_user_id', data.user_id.toString())
        setProfile(data)
        setStreak(data.streak_count || 0)
      }
    } catch (err) {
      console.error('[DentNova] Error fetching profile:', err)
    } finally {
      setLoading(false)
    }
  }

  const toggleTheme = () => {
    setTheme(prev => prev === 'dark' ? 'light' : 'dark')
  }

  // Route guarding
  const ProtectedRoute = ({ children }) => {
    if (loading) {
      return (
        <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-cyan-500"></div>
        </div>
      )
    }
    if (!user) {
      return <Navigate to="/auth" state={{ from: location }} replace />
    }
    return children
  }

  const AuthRoute = ({ children }) => {
    if (loading) return null
    if (user) {
      return <Navigate to="/dashboard" replace />
    }
    return children
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col transition-colors duration-300">
      <Navbar 
        user={user} 
        profile={profile} 
        streak={streak} 
        theme={theme} 
        toggleTheme={toggleTheme} 
      />

      <main className="flex-grow">
        <Routes>
          <Route path="/" element={<Landing user={user} />} />
          
          <Route path="/auth" element={
            <AuthRoute>
              <Auth />
            </AuthRoute>
          } />

          <Route path="/login" element={<Navigate to="/auth" replace />} />

          {/* Public OAuth callback — NOT wrapped in AuthRoute so the session can be established */}
          <Route path="/auth/callback" element={<AuthCallback />} />

          <Route path="/forgot-password" element={
            <AuthRoute>
              <ForgotPassword />
            </AuthRoute>
          } />

          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard profile={profile} streak={streak} setStreak={setStreak} fetchProfile={() => fetchProfile(user)} />
            </ProtectedRoute>
          } />

          <Route path="/brushing-timer" element={
            <ProtectedRoute>
              <BrushingTimer profile={profile} streak={streak} setStreak={setStreak} fetchProfile={() => fetchProfile(user)} />
            </ProtectedRoute>
          } />

          <Route path="/assessment" element={
            <ProtectedRoute>
              <Assessment />
            </ProtectedRoute>
          } />

          <Route path="/assessment-result" element={
            <ProtectedRoute>
              <AssessmentResult />
            </ProtectedRoute>
          } />

          <Route path="/tooth-scan" element={
            <ProtectedRoute>
              <ToothScan />
            </ProtectedRoute>
          } />

          <Route path="/education" element={
            <ProtectedRoute>
              <Education />
            </ProtectedRoute>
          } />

          <Route path="/education/:topicId" element={
            <ProtectedRoute>
              <EducationDetail />
            </ProtectedRoute>
          } />

          <Route path="/reminders" element={
            <ProtectedRoute>
              <Reminders />
            </ProtectedRoute>
          } />

          <Route path="/visit-reminders" element={
            <ProtectedRoute>
              <VisitReminders />
            </ProtectedRoute>
          } />

          <Route path="/profile" element={
            <ProtectedRoute>
              <Profile profile={profile} onProfileUpdate={() => fetchProfile(user)} />
            </ProtectedRoute>
          } />

          <Route path="/settings" element={
            <ProtectedRoute>
              <Settings 
                theme={theme} 
                toggleTheme={toggleTheme} 
                profile={profile} 
                onProfileUpdate={() => fetchProfile(user)} 
              />
            </ProtectedRoute>
          } />

          {/* Catch-all fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <Footer />
    </div>
  )
}

import React, { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { User, AlertCircle, CheckCircle, Camera, Settings, AlertTriangle } from 'lucide-react'

export default function Profile({ profile, onProfileUpdate }) {
  const navigate = useNavigate()
  const mountedRef = useRef(true)   // guard against setting state after unmount

  // Profile form state
  const [name, setName] = useState('')
  const [age, setAge] = useState(20)
  const [gender, setGender] = useState('Female')
  const [concerns, setConcerns] = useState('')
  const [photoBase64, setPhotoBase64] = useState('')
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  // Habit / streak state
  const [appUserId, setAppUserId] = useState(null)
  const [brushDone, setBrushDone] = useState(false)
  const [flossDone, setFlossDone] = useState(false)
  const [streakCount, setStreakCount] = useState(0)
  const [lastStreakDate, setLastStreakDate] = useState('')
  const [authUser, setAuthUser] = useState(null)
  const [imgError, setImgError] = useState(false)

  // Habit UI state — separate booleans so one never blocks the other
  const [brushLoading, setBrushLoading] = useState(false)
  const [flossLoading, setFlossLoading] = useState(false)
  const [habitError, setHabitError] = useState('')   // visible on-screen error

  // ── Component mount log ──────────────────────────────────────────────────
  useEffect(() => {
    console.log('PROFILE_COMPONENT_LOADED')
    mountedRef.current = true
    return () => { mountedRef.current = false }
  }, [])

  // ── Sync profile prop into form fields ───────────────────────────────────
  useEffect(() => {
    if (profile) {
      setName(profile.name || '')
      setAge(profile.age || 20)
      setGender(profile.gender || 'Female')
      setConcerns(profile.concerns || '')
      setPhotoBase64(profile.photo_url || '')
    }
  }, [profile])

  useEffect(() => {
    setImgError(false)
  }, [profile?.photo_url, authUser?.user_metadata?.avatar_url, photoBase64])

  // ── Date helpers ─────────────────────────────────────────────────────────
  const getLocalDateString = () => {
    const tzOffset = new Date().getTimezoneOffset() * 60000
    return new Date(Date.now() - tzOffset).toISOString().split('T')[0]
  }
  const getLocalYesterdayString = () => {
    const tzOffset = new Date().getTimezoneOffset() * 60000
    return new Date(Date.now() - 86400000 - tzOffset).toISOString().split('T')[0]
  }

  // ── Load habits & streak on mount (and when profile prop changes) ─────────
  useEffect(() => {
    const load = async () => {
      try {
        const { data: { session } } = await supabase.auth.getSession()
        if (!session || !mountedRef.current) return

        const email = session.user.email
        setAuthUser(session.user)
        console.log('WEB_AUTH_EMAIL:', email)

        const { data: row, error } = await supabase
          .from('users')
          .select('*')
          .eq('email', email)
          .single()

        if (error || !row || !mountedRef.current) {
          console.error('HABIT_UPDATE_ERROR: failed to load user row', error)
          return
        }

        const uId = row.user_id
        setAppUserId(uId)
        console.log('WEB_APP_USER_ID:', uId)

        const today = getLocalDateString()
        let finalBrush = row.brushing_done || false
        let finalFloss = row.flossing_done || false
        const habitDate = row.habit_date || null

        if (!habitDate || habitDate !== today) {
          // New day → reset habits in DB (do NOT reset streak here)
          finalBrush = false
          finalFloss = false
          await supabase
            .from('users')
            .update({ habit_date: today, brushing_done: false, flossing_done: false })
            .eq('user_id', uId)
        }

        if (mountedRef.current) {
          setBrushDone(finalBrush)
          setFlossDone(finalFloss)
          setStreakCount(row.streak_count || 0)
          setLastStreakDate(row.last_streak_date || '')
        }
      } catch (err) {
        console.error('HABIT_UPDATE_ERROR: load failed', err)
      }
    }
    load()
  }, [profile])

  // ── Habit click handler ───────────────────────────────────────────────────
  const handleHabitClick = async (type) => {
    // Prevent double-click by checking loading state inline
    if (type === 'brushing' && (brushDone || brushLoading)) return
    if (type === 'flossing' && (flossDone || flossLoading)) return

    const uid = appUserId || parseInt(localStorage.getItem('dentnova_user_id') || '0')
    if (!uid) {
      console.warn('HABIT_CLICK_STARTED: user_id not ready yet')
      setHabitError('User not loaded yet. Please wait and try again.')
      return
    }

    console.log('HABIT_CLICK_STARTED:', type)
    console.log('HABIT_UPDATE_BODY:', { type, user_id: uid })
    setHabitError('')

    // Set loading — separate state per type so they don't block each other
    if (type === 'brushing') setBrushLoading(true)
    else setFlossLoading(true)

    try {
      const today = getLocalDateString()
      const updatePayload = type === 'brushing'
        ? { brushing_done: true, habit_date: today }
        : { flossing_done: true, habit_date: today }

      const { error: updateErr } = await supabase
        .from('users')
        .update(updatePayload)
        .eq('user_id', uid)

      if (updateErr) {
        console.error('HABIT_UPDATE_ERROR:', updateErr.message, updateErr)
        setHabitError(`Failed to save ${type}: ${updateErr.message}`)
        return          // finally still runs
      }

      console.log('HABIT_UPDATE_SUCCESS:', type)
      if (mountedRef.current) {
        if (type === 'brushing') setBrushDone(true)
        else setFlossDone(true)
      }

      // ── Streak logic: refetch the LATEST row from DB ─────────────────────
      await checkAndUpdateStreak(uid, type)

      onProfileUpdate()
    } catch (err) {
      console.error('HABIT_UPDATE_ERROR:', err)
      setHabitError(`Unexpected error: ${err?.message || err}`)
    } finally {
      // Always clears — guaranteed even if Supabase hangs, throws, or rejects
      if (mountedRef.current) {
        if (type === 'brushing') setBrushLoading(false)
        else setFlossLoading(false)
      }
    }
  }

  // ── Streak update (refetch DB ground truth) ───────────────────────────────
  const checkAndUpdateStreak = async (userId, justCompletedType) => {
    const today = getLocalDateString()
    const yesterday = getLocalYesterdayString()

    try {
      const { data: row, error } = await supabase
        .from('users')
        .select('brushing_done, flossing_done, streak_count, last_streak_date')
        .eq('user_id', userId)
        .single()

      if (error || !row) {
        console.error('STREAK_UPDATE_ERROR: refetch failed', error)
        return
      }

      const isBrush = row.brushing_done || false
      const isFloss = row.flossing_done || false
      const dbLastStreak = row.last_streak_date || null
      const dbStreakCount = row.streak_count || 0

      console.log('WEB_STREAK_BEFORE:', dbStreakCount, '| brush:', isBrush, '| floss:', isFloss, '| last:', dbLastStreak)

      if (!isBrush || !isFloss) return   // need both to update streak

      if (dbLastStreak === today) {
        console.log('WEB_STREAK_AFTER:', dbStreakCount, '(already done today)')
        return
      }

      let newStreak = 1
      if (dbLastStreak === yesterday) newStreak = dbStreakCount + 1

      const { error: streakErr } = await supabase
        .from('users')
        .update({ streak_count: newStreak, last_streak_date: today })
        .eq('user_id', userId)

      if (streakErr) {
        console.error('STREAK_UPDATE_ERROR:', streakErr.message)
        setHabitError(`Streak not saved: ${streakErr.message}`)
        return
      }

      if (mountedRef.current) {
        setStreakCount(newStreak)
        setLastStreakDate(today)
      }
      console.log('STREAK_UPDATE_SUCCESS:', newStreak)
      console.log('WEB_STREAK_AFTER:', newStreak)
    } catch (err) {
      console.error('STREAK_UPDATE_ERROR:', err)
    }
  }

  // ── Avatar helpers ────────────────────────────────────────────────────────
  const getAvatarSource = () => {
    if (photoBase64) return photoBase64
    if (profile?.photo_url) return profile.photo_url
    return authUser?.user_metadata?.avatar_url || authUser?.user_metadata?.picture || ''
  }
  const avatarSrc = getAvatarSource()
  const hasAvatar = avatarSrc && !imgError
  const getInitials = () => {
    const n = name || profile?.name || authUser?.email || 'U'
    return n.charAt(0).toUpperCase()
  }

  const handlePhotoUpload = (e) => {
    const file = e.target.files[0]
    if (file) {
      const reader = new FileReader()
      reader.onloadend = () => setPhotoBase64(reader.result)
      reader.readAsDataURL(file)
    }
  }

  const handleSaveProfile = async (e) => {
    e.preventDefault()
    setSaving(true)
    setErrorMsg('')
    setSuccessMsg('')
    const userId = parseInt(localStorage.getItem('dentnova_user_id') || '0')
    if (!userId) { setSaving(false); return }
    try {
      const { error } = await supabase
        .from('users')
        .update({ name, age: parseInt(age), gender, concerns, photo_url: photoBase64 })
        .eq('user_id', userId)
      if (error) throw error
      setSuccessMsg('Profile updated successfully.')
      onProfileUpdate()
    } catch (err) {
      setErrorMsg('Failed to update profile.')
      console.error(err)
    } finally {
      setSaving(false)
    }
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="max-w-5xl mx-auto px-4 py-8 grid grid-cols-1 lg:grid-cols-3 gap-8 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">

      {/* ── Left: Edit Profile ───────────────────────────────────────────── */}
      <div className="lg:col-span-2 space-y-6">
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6 relative overflow-hidden transition duration-300">
          <div className="absolute top-0 right-0 w-24 h-24 bg-cyan-500/5 rounded-full blur-xl" />

          {/* Header row with Settings button */}
          <div className="flex justify-between items-start">
            <div className="space-y-1">
              <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
                <User className="w-6 h-6 text-cyan-500" />
                Edit Profile
              </h2>
              <p className="text-xs text-slate-400 font-medium">Update your personal info and display picture.</p>
            </div>

            {/* ── SETTINGS BUTTON — always rendered, clearly visible ── */}
            {console.log('SETTINGS_BUTTON_RENDERED') || (
              <button
                type="button"
                onClick={() => {
                  console.log('WEB_SETTINGS_CLICKED')
                  navigate('/settings')
                }}
                className="flex items-center gap-2 px-4 py-2.5 bg-cyan-500 hover:bg-cyan-600 active:bg-cyan-700 text-white rounded-2xl text-sm font-bold shadow-md shadow-cyan-500/20 transition-all"
                title="Open Settings"
              >
                <Settings className="w-4 h-4" />
                ⚙️ Settings
              </button>
            )}
          </div>

          {errorMsg && (
            <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-xl text-sm flex items-center gap-2">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}
          {successMsg && (
            <div className="bg-emerald-50 dark:bg-emerald-900/10 border border-emerald-200 dark:border-emerald-800/30 text-emerald-600 dark:text-emerald-400 p-3 rounded-xl text-sm flex items-center gap-2">
              <CheckCircle className="w-5 h-5 shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          <form onSubmit={handleSaveProfile} className="space-y-6">
            {/* Avatar */}
            <div className="flex flex-col sm:flex-row items-center gap-6 pb-2 border-b border-slate-100 dark:border-slate-800">
              <div className="relative group">
                {hasAvatar ? (
                  <img
                    src={avatarSrc.startsWith('data:') || avatarSrc.startsWith('http') ? avatarSrc : `data:image/jpeg;base64,${avatarSrc}`}
                    alt="Avatar"
                    className="w-24 h-24 rounded-full object-cover border-4 border-slate-100 dark:border-slate-800 shadow"
                    onError={() => setImgError(true)}
                  />
                ) : (
                  <div className="w-24 h-24 rounded-full bg-cyan-500/10 text-cyan-500 flex items-center justify-center font-bold text-2xl border-4 border-slate-100 dark:border-slate-800 shadow">
                    {getInitials()}
                  </div>
                )}
                <label className="absolute bottom-0 right-0 p-2 bg-cyan-500 hover:bg-cyan-600 text-white rounded-full cursor-pointer shadow-md transition hover:scale-105">
                  <Camera className="w-4 h-4" />
                  <input type="file" accept="image/*" onChange={handlePhotoUpload} className="hidden" />
                </label>
              </div>
              <div className="space-y-1 text-center sm:text-left">
                <h4 className="font-extrabold text-slate-800 dark:text-slate-200 text-sm">Upload Avatar Photo</h4>
                <p className="text-xs text-slate-400">JPG or PNG, max 2 MB.</p>
              </div>
            </div>

            {/* Name + Age */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
              <div className="sm:col-span-2 space-y-1">
                <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Display Name</label>
                <input
                  type="text" value={name} onChange={(e) => setName(e.target.value)} required
                  className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Age</label>
                <input
                  type="number" value={age} onChange={(e) => setAge(e.target.value)} required
                  className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                />
              </div>
            </div>

            {/* Gender */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Gender</label>
              <div className="flex gap-4">
                {['Male', 'Female', 'Other'].map((g) => (
                  <button
                    key={g} type="button" onClick={() => setGender(g)}
                    className={`flex-1 py-3 text-sm font-semibold rounded-xl border transition-all ${
                      gender === g
                        ? 'bg-cyan-500/10 border-cyan-500 text-cyan-600 dark:text-cyan-400'
                        : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-850'
                    }`}
                  >{g}</button>
                ))}
              </div>
            </div>

            {/* Concerns */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Oral Concerns</label>
              <textarea
                value={concerns} onChange={(e) => setConcerns(e.target.value)} rows={4}
                placeholder="Describe any sensitivity, teeth pain, or bleeding concerns..."
                className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
              />
            </div>

            <button
              type="submit" disabled={saving}
              className="w-full sm:w-auto px-6 py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 transition-all disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save Profile Details'}
            </button>
          </form>
        </div>
      </div>

      {/* ── Right: Streak + Habits + Achievements ────────────────────────── */}
      <div className="space-y-6">

        {/* Streak Banner */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-sm flex items-center gap-4 transition duration-300">
          <div className="w-12 h-12 rounded-full bg-amber-500/10 text-amber-500 flex items-center justify-center text-xl shadow">🔥</div>
          <div>
            <h4 className="font-extrabold text-slate-800 dark:text-slate-200 text-base">
              {streakCount <= 0 ? 'No streak yet' : `🔥 ${streakCount} day streak`}
            </h4>
            <p className="text-xs text-slate-400 font-medium">
              {streakCount <= 0 ? 'Complete both habits today to start your streak' : 'Keep completing both habits daily!'}
            </p>
          </div>
        </div>

        {/* Daily Habits */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-sm space-y-4 transition duration-300">
          <h3 className="font-bold text-slate-800 dark:text-slate-200 text-sm border-b border-slate-100 dark:border-slate-800 pb-2">Daily Habits</h3>

          {/* On-screen habit error */}
          {habitError && (
            <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-700/30 text-red-600 dark:text-red-400 p-3 rounded-xl text-xs flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{habitError}</span>
            </div>
          )}

          <div className="flex gap-4">
            {/* Brushing Card — NO disabled attr, click guard is inline */}
            <button
              type="button"
              onClick={() => handleHabitClick('brushing')}
              style={{ cursor: brushDone ? 'default' : 'pointer' }}
              className={`flex-1 p-4 rounded-2xl border text-left transition-all select-none ${
                brushDone
                  ? 'bg-emerald-500/5 border-emerald-500/30 opacity-80'
                  : brushLoading
                    ? 'border-cyan-300 dark:border-cyan-700 bg-cyan-50 dark:bg-cyan-950/20'
                    : 'border-slate-200 dark:border-slate-800 hover:bg-cyan-50/50 dark:hover:bg-cyan-950/10 hover:border-cyan-400 hover:shadow-sm active:scale-95'
              }`}
            >
              <span className="text-xl block mb-1">{brushLoading ? '⏳' : '🪥'}</span>
              <span className="text-xs font-bold block text-slate-700 dark:text-slate-300">Brushing</span>
              <span className={`text-[10px] font-semibold mt-0.5 block ${
                brushDone ? 'text-emerald-500' : brushLoading ? 'text-cyan-500' : 'text-slate-400'
              }`}>
                {brushLoading ? 'Saving...' : brushDone ? 'Done ✓ 🔥' : 'Tap to mark done'}
              </span>
            </button>

            {/* Flossing Card — NO disabled attr, click guard is inline */}
            <button
              type="button"
              onClick={() => handleHabitClick('flossing')}
              style={{ cursor: flossDone ? 'default' : 'pointer' }}
              className={`flex-1 p-4 rounded-2xl border text-left transition-all select-none ${
                flossDone
                  ? 'bg-emerald-500/5 border-emerald-500/30 opacity-80'
                  : flossLoading
                    ? 'border-cyan-300 dark:border-cyan-700 bg-cyan-50 dark:bg-cyan-950/20'
                    : 'border-slate-200 dark:border-slate-800 hover:bg-cyan-50/50 dark:hover:bg-cyan-950/10 hover:border-cyan-400 hover:shadow-sm active:scale-95'
              }`}
            >
              <span className="text-xl block mb-1">{flossLoading ? '⏳' : '🧵'}</span>
              <span className="text-xs font-bold block text-slate-700 dark:text-slate-300">Flossing</span>
              <span className={`text-[10px] font-semibold mt-0.5 block ${
                flossDone ? 'text-emerald-500' : flossLoading ? 'text-cyan-500' : 'text-slate-400'
              }`}>
                {flossLoading ? 'Saving...' : flossDone ? 'Done ✓ 🔥' : 'Tap to mark done'}
              </span>
            </button>
          </div>
        </div>

        {/* Achievements */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-sm space-y-4 transition duration-300">
          <h3 className="font-bold text-slate-800 dark:text-slate-200 text-sm border-b border-slate-100 dark:border-slate-800 pb-2">Achievements</h3>
          <div className="grid grid-cols-2 gap-3">
            {[
              { emoji: '🌱', label: 'Hygiene Star', threshold: 1 },
              { emoji: '🔁', label: 'Consistent',   threshold: 30 },
              { emoji: '🏆', label: 'Oral Care Pro', threshold: 60 },
              { emoji: '📈', label: 'Improver',      threshold: 90 },
            ].map(({ emoji, label, threshold }) => (
              <div key={label} className="p-3 bg-slate-50 dark:bg-slate-950 border border-slate-100 dark:border-slate-800/60 rounded-2xl flex flex-col justify-between min-h-[92px]">
                <span className="text-xl">{emoji}</span>
                <span className="text-xs font-bold text-slate-700 dark:text-slate-300">{label}</span>
                <span className={`text-[10px] font-bold ${streakCount >= threshold ? 'text-emerald-500' : 'text-slate-400'}`}>
                  {streakCount >= threshold ? 'Earned ✓' : streakCount > 0 ? `${streakCount}/${threshold} days` : 'Locked'}
                </span>
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  )
}

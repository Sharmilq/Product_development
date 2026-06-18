import React, { useState, useEffect, useRef } from 'react'
import { supabase } from '../supabaseClient'
import { Play, Pause, RotateCcw, Clock, Sparkles, CheckCircle, Info, Heart } from 'lucide-react'

export default function BrushingTimer({ profile, streak, setStreak, fetchProfile }) {
  const [timeLeft, setTimeLeft] = useState(120) // 2 minutes
  const [isActive, setIsActive] = useState(false)
  const [isCompleted, setIsCompleted] = useState(false)
  const [flossed, setFlossed] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  const intervalRef = useRef(null)

  useEffect(() => {
    if (isActive && timeLeft > 0) {
      intervalRef.current = setInterval(() => {
        setTimeLeft(prev => prev - 1)
      }, 1000)
    } else if (timeLeft === 0) {
      setIsActive(false)
      setIsCompleted(true)
      clearInterval(intervalRef.current)
    } else {
      clearInterval(intervalRef.current)
    }

    return () => clearInterval(intervalRef.current)
  }, [isActive, timeLeft])

  const handleStartPause = () => {
    setIsActive(!isActive)
  }

  const handleReset = () => {
    setIsActive(false)
    setTimeLeft(120)
    setIsCompleted(false)
    setFlossed(false)
    setErrorMsg('')
    clearInterval(intervalRef.current)
  }

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60)
    const s = secs % 60
    return `${m}:${s.toString().padStart(2, '0')}`
  }

  const getBrushingZone = (secs) => {
    // 120s count down splits
    if (secs > 90) return { title: 'Upper Outer Surfaces', instruction: 'Hold the brush at a 45-degree angle. Sweep gently away from the gum line.', emoji: '🦷' }
    if (secs > 60) return { title: 'Upper Inner Surfaces', instruction: 'Brush vertical strokes using the front tip of your toothbrush.', emoji: '😬' }
    if (secs > 30) return { title: 'Lower Outer & Inner', instruction: 'Brush the outer and inner walls of your bottom teeth.', emoji: '🪥' }
    return { title: 'Chewing Surfaces', instruction: 'Use flat back-and-forth scrubbing strokes on chew surfaces.', emoji: '✨' }
  }

  const getLocalDateString = () => {
    const tzOffset = new Date().getTimezoneOffset() * 60000
    return new Date(Date.now() - tzOffset).toISOString().split('T')[0]
  }

  const getLocalYesterdayString = () => {
    const tzOffset = new Date().getTimezoneOffset() * 60000
    const yesterdayMs = Date.now() - 24 * 60 * 60 * 1000
    return new Date(yesterdayMs - tzOffset).toISOString().split('T')[0]
  }

  const handleSaveHabit = async () => {
    setSaving(true)
    setErrorMsg('')

    const userIdStr = localStorage.getItem('dentnova_user_id')
    if (!userIdStr) return
    const userId = parseInt(userIdStr)
    const todayStr = getLocalDateString()

    try {
      // 1. Fetch current user row for logs and calculation
      const { data: userRow, error: fetchError } = await supabase
        .from('users')
        .select('streak_count, last_streak_date')
        .eq('user_id', userId)
        .single()

      if (fetchError || !userRow) {
        console.error('Error re-fetching user row for streak check:', fetchError)
        return
      }

      const dbLastStreak = userRow.last_streak_date || null
      const dbStreakCount = userRow.streak_count || 0
      const yesterdayStr = getLocalYesterdayString()

      let newStreak = dbStreakCount

      const bothDone = true && flossed

      if (bothDone && dbLastStreak !== todayStr) {
        if (!dbLastStreak) {
          newStreak = 1
        } else if (dbLastStreak === yesterdayStr) {
          newStreak = dbStreakCount + 1
        } else {
          newStreak = 1
        }
      }

      // 2. Save habit completion and calculated streak
      const { error: habitError } = await supabase
        .from('users')
        .update({
          brushing_done: true,
          flossing_done: flossed,
          habit_date: todayStr,
          streak_count: newStreak,
          last_streak_date: bothDone && dbLastStreak !== todayStr ? todayStr : dbLastStreak
        })
        .eq('user_id', userId)
      
      if (habitError) throw habitError

      setStreak(newStreak)

      // Required Logs
      console.log('WEB_APP_USER_ID:', userId)
      console.log('WEB_BRUSHING_DONE:', true)
      console.log('WEB_FLOSSING_DONE:', flossed)
      console.log('WEB_STREAK_BEFORE:', dbStreakCount)
      console.log('WEB_STREAK_AFTER:', newStreak)
      console.log('WEB_LAST_STREAK_DATE:', bothDone && dbLastStreak !== todayStr ? todayStr : dbLastStreak)

      fetchProfile()

      // Fire-and-forget notification insert — do NOT await so it never blocks the success flow
      supabase
        .from('notifications')
        .insert({
          user_id: userId,
          title: 'Daily Habits Completed! 🎉',
          body: `Brushing tracked successfully. ${flossed ? 'Flossing checked.' : ''} Keep it up!`
        })
        .then(() => console.log('[DentNova] WEB_NOTIFICATION_SAVED'))
        .catch((e) => console.warn('[DentNova] WEB_NOTIFICATION_SKIPPED (table may not exist):', e.message))

      // Reset and show success
      handleReset()
      alert('Habits recorded successfully! Keep shining! 😁✨')
    } catch (err) {
      setErrorMsg('Failed to record habit completion. Please try again.')
      console.error('[DentNova] WEB_BRUSHING_SAVE_ERROR:', err)
    } finally {
      setSaving(false)
    }
  }

  const progressPercent = ((120 - timeLeft) / 120) * 100
  const zone = getBrushingZone(timeLeft)

  return (
    <div className="max-w-3xl mx-auto px-4 py-12 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8 shadow-md space-y-8 relative overflow-hidden transition duration-300">
        <div className="absolute top-0 right-0 w-24 h-24 bg-cyan-500/5 rounded-full blur-xl"></div>

        <div className="text-center space-y-1">
          <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center justify-center gap-2">
            <Clock className="w-6 h-6 text-cyan-500" />
            2-Minute Brushing Timer
          </h2>
          <p className="text-xs text-slate-400 font-medium">Follow the dental guides for an optimal thorough clean.</p>
        </div>

        {errorMsg && (
          <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-xl text-sm flex items-center justify-center gap-2">
            <Info className="w-5 h-5" />
            <span>{errorMsg}</span>
          </div>
        )}

        {!isCompleted ? (
          /* Running Timer state */
          <div className="flex flex-col items-center space-y-8">
            
            {/* Visual Circular Timer */}
            <div className="relative w-48 h-48 flex items-center justify-center">
              <svg className="w-full h-full transform -rotate-90">
                <circle
                  cx="96"
                  cy="96"
                  r="80"
                  className="stroke-slate-100 dark:stroke-slate-800 fill-none stroke-[8]"
                />
                <circle
                  cx="96"
                  cy="96"
                  r="80"
                  className="stroke-cyan-500 fill-none stroke-[8] transition-all duration-1000"
                  strokeDasharray={502.4}
                  strokeDashoffset={502.4 - (502.4 * progressPercent) / 100}
                />
              </svg>
              <div className="absolute flex flex-col items-center space-y-1">
                <span className="text-4xl font-black text-slate-800 dark:text-white">{formatTime(timeLeft)}</span>
                <span className="text-3xl select-none">{zone.emoji}</span>
              </div>
            </div>

            {/* Instruction zone details */}
            <div className="text-center space-y-2 max-w-sm">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white uppercase tracking-wider">{zone.title}</h3>
              <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed font-semibold">
                {zone.instruction}
              </p>
            </div>

            {/* Play/Pause controls */}
            <div className="flex items-center gap-4">
              <button
                onClick={handleReset}
                className="p-3 bg-slate-50 dark:bg-slate-850 hover:bg-slate-100 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 rounded-2xl transition"
                title="Reset timer"
              >
                <RotateCcw className="w-5 h-5" />
              </button>
              <button
                onClick={handleStartPause}
                className={`p-5 rounded-2xl text-white shadow-lg shadow-cyan-500/15 hover:shadow-cyan-500/20 transition hover:scale-105 ${
                  isActive ? 'bg-amber-500 shadow-amber-500/10' : 'bg-cyan-500'
                }`}
              >
                {isActive ? <Pause className="w-6 h-6 fill-white" /> : <Play className="w-6 h-6 fill-white" />}
              </button>
            </div>
          </div>
        ) : (
          /* Completion State */
          <div className="space-y-6 text-center py-6">
            <div className="w-16 h-16 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center mx-auto animate-bounce">
              <CheckCircle className="w-8 h-8" />
            </div>
            
            <div className="space-y-2">
              <h3 className="text-2xl font-black text-slate-900 dark:text-white">Timer Completed! 🎉</h3>
              <p className="text-sm text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
                Excellent job brushing your teeth for the full 2 minutes! Your gums will thank you.
              </p>
            </div>

            {/* Flossing selection */}
            <div className="max-w-xs mx-auto p-4 bg-slate-50 dark:bg-slate-950 border border-slate-150 dark:border-slate-850 rounded-2xl space-y-4">
              <label className="flex items-center justify-between cursor-pointer select-none">
                <span className="text-sm font-bold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                  <Heart className="w-4 h-4 text-purple-500 fill-purple-500/25" />
                  Did you also floss today?
                </span>
                <input
                  type="checkbox"
                  checked={flossed}
                  onChange={() => setFlossed(!flossed)}
                  className="w-5 h-5 rounded border-slate-300 text-cyan-500 focus:ring-cyan-500"
                />
              </label>
            </div>

            {/* Actions */}
            <div className="flex gap-4 max-w-md mx-auto pt-4">
              <button
                onClick={handleReset}
                className="flex-1 py-3.5 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-300 rounded-xl text-sm font-bold transition"
              >
                Reset
              </button>
              <button
                onClick={handleSaveHabit}
                disabled={saving}
                className="flex-1 py-3.5 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 transition flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Record Completion'}
                <Sparkles className="w-4 h-4 fill-white" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

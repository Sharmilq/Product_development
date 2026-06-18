import React, { useState, useEffect } from 'react'
import { supabase } from '../supabaseClient'
import {
  Bell, Plus, Trash2, Clock, Calendar, AlertCircle, ToggleLeft, ToggleRight
} from 'lucide-react'

/**
 * Resolves the integer user_id from Supabase users table.
 * Priority:
 *   1. localStorage.dentnova_user_id  (set by App.jsx on login)
 *   2. Fresh query to users table by auth email   (fallback)
 * Returns null if user is not logged in.
 */
async function resolveAppUserId() {
  // 1. Try localStorage first (fast path)
  const cached = localStorage.getItem('dentnova_user_id')
  if (cached) {
    const id = parseInt(cached, 10)
    if (!isNaN(id)) return id
  }

  // 2. Fallback — look up by auth email
  const { data: { session } } = await supabase.auth.getSession()
  if (!session) return null

  const email = session.user.email
  console.log('WEB_AUTH_EMAIL', email)

  const { data: userRow } = await supabase
    .from('users')
    .select('user_id')
    .eq('email', email)
    .maybeSingle()

  if (userRow?.user_id) {
    localStorage.setItem('dentnova_user_id', userRow.user_id.toString())
    return userRow.user_id
  }

  // Create users row using mobile logic if it does not exist
  let hash = 0
  for (let i = 0; i < email.length; i++) {
    hash = (31 * hash + email.charCodeAt(i)) | 0
  }
  const uId = Math.abs(hash)

  const displayName = session.user.user_metadata?.name || email.split('@')[0]
  const { data: inserted, error: insertError } = await supabase
    .from('users')
    .insert({
      user_id: uId,
      name: displayName,
      email: email,
      age: 20,
      gender: 'Female',
      concerns: '',
      photo_url: session.user.user_metadata?.avatar_url || ''
    })
    .select('user_id')
    .maybeSingle()

  if (!insertError && inserted?.user_id) {
    localStorage.setItem('dentnova_user_id', inserted.user_id.toString())
    return inserted.user_id
  }

  // Fallback to query again (in case of concurrency race condition)
  const { data: finalQuery } = await supabase
    .from('users')
    .select('user_id')
    .eq('email', email)
    .maybeSingle()

  if (finalQuery?.user_id) {
    localStorage.setItem('dentnova_user_id', finalQuery.user_id.toString())
    return finalQuery.user_id
  }

  return null
}

export default function Reminders() {
  const [reminders, setReminders] = useState([])
  const [loading, setLoading] = useState(true)
  const [appUserId, setAppUserId] = useState(null)

  // Form state
  const [showAddDialog, setShowAddDialog] = useState(false)
  const [title, setTitle] = useState('Brushing Reminders')
  const [time, setTime] = useState('08:00')
  const [selectedDays, setSelectedDays] = useState([])
  const [replaceDate, setReplaceDate] = useState('')
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  const daysOfWeek = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

  // ── Initialise: resolve user_id then fetch ────────────────────────────────
  useEffect(() => {
    const init = async () => {
      const userId = await resolveAppUserId()
      if (!userId) {
        setLoading(false)
        return
      }
      setAppUserId(userId)
      fetchReminders(userId)
    }
    init()
  }, [])

  // ── Fetch reminders from Supabase ─────────────────────────────────────────
  const fetchReminders = async (userId) => {
    const uid = userId ?? appUserId
    if (!uid) return
    setLoading(true)
    console.log('WEB_REMINDER_FETCH_USER_ID:', uid)

    try {
      const { data, error } = await supabase
        .from('reminders')
        .select('*')
        .eq('user_id', uid)
        .order('created_at', { ascending: false })

      if (error) {
        console.error('[DentNova] WEB_REMINDERS_ERROR', error)
      } else {
        console.log('WEB_REMINDER_FETCH_RESULT:', data)
        setReminders(data || [])
      }
    } catch (err) {
      console.error('[DentNova] WEB_REMINDERS_ERROR', err)
    } finally {
      setLoading(false)
    }
  }

  // ── Toggle enabled flag ───────────────────────────────────────────────────
  const handleToggle = async (id, currentVal) => {
    console.log('WEB_REMINDER_TOGGLE_ID:', id)
    try {
      const { error } = await supabase
        .from('reminders')
        .update({ enabled: !currentVal })
        .eq('id', id)

      if (!error) {
        setReminders(prev => prev.map(r => r.id === id ? { ...r, enabled: !currentVal } : r))
      }
    } catch (err) {
      console.error('[DentNova] WEB_REMINDER_TOGGLE_ERROR:', err)
    }
  }

  // ── Delete reminder ───────────────────────────────────────────────────────
  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this reminder?')) return

    console.log('WEB_REMINDER_DELETE_ID:', id)

    try {
      const { error } = await supabase
        .from('reminders')
        .delete()
        .eq('id', id)

      if (!error) {
        setReminders(prev => prev.filter(r => r.id !== id))
      } else {
        console.error('[DentNova] WEB_REMINDER_DELETE_ERROR:', error)
      }
    } catch (err) {
      console.error('[DentNova] WEB_REMINDER_DELETE_EXCEPTION:', err)
    }
  }

  const handleDaySelect = (day) => {
    setSelectedDays(prev => {
      const updated = prev.includes(day) ? prev.filter(d => d !== day) : [...prev, day]
      console.log('WEB_REMINDER_SELECTED_DAYS:', updated)
      return updated
    })
  }

  // ── Save new reminder ─────────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setErrorMsg('')

    if (!appUserId) {
      setErrorMsg('User session not found. Please log in again.')
      setSaving(false)
      return
    }

    let finalTime = time
    let finalDays = 'ONCE'

    if (title === 'Toothbrush Replacement') {
      if (!replaceDate) {
        setErrorMsg('Please select a replacement date.')
        setSaving(false)
        return
      }
      const d = new Date(replaceDate)
      finalTime = d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
      finalDays = 'ONCE'
    } else {
      if (selectedDays.length === 0) {
        setErrorMsg('Please select at least one day.')
        setSaving(false)
        return
      }
      
      // Convert 24h → 12h AM/PM
      const [h, m] = time.split(':')
      const hour = parseInt(h, 10)
      const ampm = hour >= 12 ? 'PM' : 'AM'
      const adjusted = hour % 12 || 12
      finalTime = `${adjusted.toString().padStart(2, '0')}:${m} ${ampm}`

      // Order selected days to match Mon -> Sun
      const dayOrder = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
      const orderedDays = dayOrder.filter(d => selectedDays.includes(d))
      finalDays = orderedDays.join(',')
    }

    const body = {
      user_id: appUserId,
      title,
      time: finalTime,
      days: finalDays,
      enabled: true
    }

    console.log('WEB_REMINDER_SAVE_BODY:', body)

    try {
      const { data, error } = await supabase
        .from('reminders')
        .insert(body)
        .select()

      if (error) throw error

      console.log('[DentNova] WEB_REMINDER_SAVED_ID:', data?.[0]?.id)

      setShowAddDialog(false)
      setTitle('Brushing Reminders')
      setTime('08:00')
      setSelectedDays([])
      setReplaceDate('')
      fetchReminders(appUserId)
    } catch (err) {
      setErrorMsg('Failed to save reminder. Please try again.')
      console.error('[DentNova] WEB_REMINDER_SAVE_ERROR:', err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-8 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">

      {/* Header */}
      <div className="flex justify-between items-center">
        <div className="space-y-1">
          <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
            <Bell className="w-6 h-6 text-cyan-500" />
            Reminders
          </h2>
          <p className="text-xs text-slate-400 font-medium">Configure daily reminders and toothbrush replacement triggers.</p>
        </div>

        <button
          onClick={() => setShowAddDialog(true)}
          className="flex items-center gap-1.5 px-4 py-2.5 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 transition-all"
        >
          <Plus className="w-4 h-4 stroke-[3px]" />
          Add Reminder
        </button>
      </div>

      {/* Add Reminder Dialog */}
      {showAddDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl transition duration-300">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Add Reminder</h3>

            {errorMsg && (
              <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-lg text-xs flex items-center gap-2 mb-4">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Reminder Type</label>
                <select
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                >
                  <option value="Brushing Reminders">Brushing Reminders</option>
                  <option value="Flossing Reminders">Flossing Reminders</option>
                  <option value="Toothbrush Replacement">Toothbrush Replacement</option>
                </select>
              </div>

              {title === 'Toothbrush Replacement' ? (
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Replacement Date</label>
                  <input
                    type="date"
                    value={replaceDate}
                    onChange={(e) => setReplaceDate(e.target.value)}
                    min={new Date().toISOString().split('T')[0]}
                    className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                    required
                  />
                </div>
              ) : (
                <>
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Reminder Time</label>
                    <input
                      type="time"
                      value={time}
                      onChange={(e) => setTime(e.target.value)}
                      className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Select Days</label>
                    <div className="flex gap-2 flex-wrap pt-1">
                      {daysOfWeek.map((day) => {
                        const isSel = selectedDays.includes(day)
                        return (
                          <button
                            key={day}
                            type="button"
                            onClick={() => handleDaySelect(day)}
                            className={`px-3.5 py-2 rounded-xl text-xs font-bold border transition-all ${
                              isSel
                                ? 'bg-cyan-500 border-cyan-500 text-white shadow-md'
                                : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-850'
                            }`}
                          >
                            {day}
                          </button>
                        )
                      })}
                    </div>
                  </div>
                </>
              )}

              <div className="flex gap-4 pt-4 border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => { setShowAddDialog(false); setErrorMsg('') }}
                  className="flex-1 py-3 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-300 rounded-xl text-sm font-bold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 transition disabled:opacity-50"
                >
                  {saving ? 'Saving...' : 'Save'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reminders List */}
      <div className="space-y-4">
        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-cyan-500" />
          </div>
        ) : reminders.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {reminders.map((r) => {
              const isReplacement = r.title?.includes('Replacement')
              return (
                <div
                  key={r.id}
                  className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm hover:shadow-md transition duration-200 flex justify-between items-center"
                >
                  <div className="flex items-center gap-4 min-w-0">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                      isReplacement ? 'bg-amber-500/10 text-amber-500' : 'bg-cyan-500/10 text-cyan-500'
                    }`}>
                      {isReplacement ? <Calendar className="w-5 h-5" /> : <Clock className="w-5 h-5" />}
                    </div>
                    <div className="min-w-0">
                      <p className="font-bold text-slate-800 dark:text-slate-100 text-sm truncate">{r.title}</p>
                      <p className="text-xs text-slate-400 font-semibold mt-0.5">
                        {isReplacement ? '📅' : '⏰'} {r.time} ({r.days})
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <button
                      onClick={() => handleToggle(r.id, r.enabled)}
                      className={`p-1 rounded-full transition ${r.enabled ? 'text-cyan-500' : 'text-slate-400'}`}
                      title={r.enabled ? 'Disable' : 'Enable'}
                    >
                      {r.enabled ? <ToggleRight className="w-8 h-8" /> : <ToggleLeft className="w-8 h-8" />}
                    </button>
                    <button
                      onClick={() => handleDelete(r.id)}
                      className="p-2 text-slate-400 hover:text-red-500 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-850 transition"
                      title="Delete Reminder"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl text-slate-400 text-sm shadow-sm transition duration-300">
            No active reminders found. Let's add one to stay on track!
          </div>
        )}
      </div>
    </div>
  )
}

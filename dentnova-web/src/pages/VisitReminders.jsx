import React, { useState, useEffect } from 'react'
import { supabase } from '../supabaseClient'
import {
  Calendar, Plus, Trash2, MapPin, ClipboardList, Clock, AlertCircle, CheckCircle
} from 'lucide-react'

/**
 * Parses visit_date ("16 Jun 2026") + visit_time ("02:48 PM")
 * into a JS Date — matching Android's SimpleDateFormat("dd MMM yyyy hh:mm a").
 */
function parseVisitDateTime(dateStr, timeStr) {
  try {
    const MONTHS = { Jan:0, Feb:1, Mar:2, Apr:3, May:4, Jun:5,
                     Jul:6, Aug:7, Sep:8, Oct:9, Nov:10, Dec:11 }
    const [dd, mon, yyyy] = (dateStr || '').trim().split(' ')
    const timeTrimmed = (timeStr || '').trim()
    const spaceIdx = timeTrimmed.lastIndexOf(' ')
    const timePart = spaceIdx >= 0 ? timeTrimmed.slice(0, spaceIdx) : timeTrimmed
    const meridiem = spaceIdx >= 0 ? timeTrimmed.slice(spaceIdx + 1).toUpperCase() : ''
    const [hhStr, mmStr] = timePart.split(':')
    let hour = parseInt(hhStr, 10)
    const minute = parseInt(mmStr, 10)
    if (isNaN(hour) || isNaN(minute)) return new Date(NaN)
    if (meridiem === 'PM' && hour !== 12) hour += 12
    if (meridiem === 'AM' && hour === 12) hour = 0
    const monthIndex = MONTHS[mon]
    if (monthIndex === undefined) return new Date(NaN)
    return new Date(parseInt(yyyy, 10), monthIndex, parseInt(dd, 10), hour, minute, 0, 0)
  } catch {
    return new Date(NaN)
  }
}

/**
 * Resolves the integer user_id from Supabase users table.
 * Priority:
 *   1. localStorage.dentnova_user_id  (set by App.jsx on login)
 *   2. Fresh query to users table by auth email   (fallback)
 */
async function resolveAppUserId() {
  const cached = localStorage.getItem('dentnova_user_id')
  if (cached) {
    const id = parseInt(cached, 10)
    if (!isNaN(id)) return id
  }

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

export default function VisitReminders() {
  const [visits, setVisits] = useState([])
  const [loading, setLoading] = useState(true)
  const [appUserId, setAppUserId] = useState(null)

  // Form state
  const [showAddDialog, setShowAddDialog] = useState(false)
  const [clinicName, setClinicName] = useState('')
  const [reason, setReason] = useState('')
  const [date, setDate] = useState('')
  const [time, setTime] = useState('09:00')
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  // ── Initialise: resolve user_id then fetch ────────────────────────────────
  useEffect(() => {
    const init = async () => {
      const userId = await resolveAppUserId()
      if (!userId) {
        setLoading(false)
        return
      }
      setAppUserId(userId)
      fetchVisits(userId)
    }
    init()
  }, [])

  // ── Fetch visits from Supabase ────────────────────────────────────────────
  const fetchVisits = async (userId) => {
    const uid = userId ?? appUserId
    if (!uid) return
    setLoading(true)

    console.log('WEB_VISITS_QUERY', uid)

    try {
      const { data, error } = await supabase
        .from('visits')
        .select('*')
        .eq('user_id', uid)
        .order('created_at', { ascending: false })

      if (error) {
        console.error('WEB_VISITS_ERROR', error)
      } else {
        console.log('WEB_VISITS_RESULT', data)
        setVisits(data || [])
      }
    } catch (err) {
      console.error('WEB_VISITS_ERROR', err)
    } finally {
      setLoading(false)
    }
  }

  // ── Delete visit ──────────────────────────────────────────────────────────
  const handleDelete = async (id) => {
    if (!window.confirm('Remove this appointment reminder?')) return

    console.log('[DentNova] WEB_VISIT_DELETE_ID:', id)

    try {
      const { error } = await supabase
        .from('visits')
        .delete()
        .eq('id', id)

      if (!error) {
        setVisits(prev => prev.filter(v => v.id !== id))
      } else {
        console.error('[DentNova] WEB_VISIT_DELETE_ERROR:', error)
      }
    } catch (err) {
      console.error('[DentNova] WEB_VISIT_DELETE_EXCEPTION:', err)
    }
  }

  // ── Save new visit ────────────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setErrorMsg('')

    if (!clinicName || !reason || !date || !time) {
      setErrorMsg('All fields are required.')
      setSaving(false)
      return
    }

    if (!appUserId) {
      setErrorMsg('User session not found. Please log in again.')
      setSaving(false)
      return
    }

    // Format date → "25 Jun 2026" (matches Android format)
    const dateObj = new Date(date)
    const formattedDate = dateObj.toLocaleDateString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric'
    })

    // Format time → "09:00 AM" (matches Android format)
    const [h, m] = time.split(':')
    const hour = parseInt(h, 10)
    const ampm = hour >= 12 ? 'PM' : 'AM'
    const adjusted = hour % 12 || 12
    const formattedTime = `${adjusted.toString().padStart(2, '0')}:${m} ${ampm}`

    // note = "clinicName - reason" (same as Android)
    const note = `${clinicName} - ${reason}`

    const body = {
      user_id: appUserId,
      visit_date: formattedDate,
      visit_time: formattedTime,
      note
    }

    console.log('[DentNova] WEB_VISIT_SAVE_BODY:', JSON.stringify(body))

    try {
      const { data, error } = await supabase
        .from('visits')
        .insert(body)
        .select()

      if (error) throw error

      console.log('[DentNova] WEB_VISIT_SAVED_ID:', data?.[0]?.id)

      setShowAddDialog(false)
      setClinicName('')
      setReason('')
      setDate('')
      setTime('09:00')
      fetchVisits(appUserId)
    } catch (err) {
      setErrorMsg('Failed to save visit record. Please try again.')
      console.error('[DentNova] WEB_VISIT_SAVE_ERROR:', err)
    } finally {
      setSaving(false)
    }
  }

  // ── Countdown helper (uses full datetime like Android) ───────────────────
  const getCountdown = (dateStr, timeStr) => {
    try {
      const target = parseVisitDateTime(dateStr, timeStr || '12:00 AM')
      if (isNaN(target.getTime())) return ''
      const nowMs = Date.now()
      const diffMs = target.getTime() - nowMs
      const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24))
      if (diffMs < 0) return 'Past Appointment'
      if (diffDays === 0) return 'Today!'
      if (diffDays === 1) return 'Tomorrow'
      return `In ${diffDays} Days`
    } catch {
      return ''
    }
  }

  // ── Segment into upcoming / past ─────────────────────────────────────────
  // Matches Android: diff = visitDate.getTime() - System.currentTimeMillis()
  const nowMs = Date.now()

  const upcomingVisits = visits.filter(v => {
    const dt = parseVisitDateTime(v.visit_date, v.visit_time)
    return !isNaN(dt.getTime()) && dt.getTime() >= nowMs
  })

  const pastVisits = visits.filter(v => {
    const dt = parseVisitDateTime(v.visit_date, v.visit_time)
    return isNaN(dt.getTime()) || dt.getTime() < nowMs
  })

  // Sort upcoming by soonest first (nearest datetime first)
  upcomingVisits.sort((a, b) =>
    parseVisitDateTime(a.visit_date, a.visit_time) -
    parseVisitDateTime(b.visit_date, b.visit_time)
  )

  const nearestUpcoming = upcomingVisits[0]

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-8 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">

      {/* Header */}
      <div className="flex justify-between items-center">
        <div className="space-y-1">
          <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
            <Calendar className="w-6 h-6 text-cyan-500" />
            Dental Visit Reminders
          </h2>
          <p className="text-xs text-slate-400 font-medium">Record doctor schedules and track checking timelines.</p>
        </div>

        <button
          onClick={() => setShowAddDialog(true)}
          className="flex items-center gap-1.5 px-4 py-2.5 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 transition-all"
        >
          <Plus className="w-4 h-4 stroke-[3px]" />
          Schedule Visit
        </button>
      </div>

      {/* Schedule Visit Dialog */}
      {showAddDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl transition duration-300">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Schedule Dentist Visit</h3>

            {errorMsg && (
              <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-lg text-xs flex items-center gap-2 mb-4">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Clinic / Doctor Name</label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    placeholder="e.g. City Dental Care"
                    value={clinicName}
                    onChange={(e) => setClinicName(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                    required
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Reason for Visit</label>
                <div className="relative">
                  <ClipboardList className="absolute left-3 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    placeholder="e.g. Routine scaling / checkup"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Appointment Date</label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    min={new Date().toISOString().split('T')[0]}
                    className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                    required
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Appointment Time</label>
                  <input
                    type="time"
                    value={time}
                    onChange={(e) => setTime(e.target.value)}
                    className="w-full p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 text-slate-800 dark:text-slate-200"
                    required
                  />
                </div>
              </div>

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

      {/* Countdown Banner */}
      {nearestUpcoming && (
        <div className="p-6 bg-gradient-to-r from-cyan-500 to-blue-600 text-white rounded-3xl shadow-md space-y-3 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full blur-2xl" />
          <p className="text-xs font-extrabold uppercase tracking-widest text-cyan-100 flex items-center gap-1.5">
            🔔 Upcoming Dental Appointment
          </p>
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h3 className="text-2xl font-black">{getCountdown(nearestUpcoming.visit_date, nearestUpcoming.visit_time)}</h3>
              <p className="text-sm text-cyan-50 font-semibold mt-1">
                📍 {nearestUpcoming.note?.split(' - ')[0] || 'Clinic'}
              </p>
              <p className="text-xs text-cyan-100/80">
                Reason: {nearestUpcoming.note?.split(' - ')[1] || '—'}
              </p>
            </div>
            <div className="bg-white/10 px-4 py-2 rounded-xl text-center border border-white/10">
              <span className="text-xs block text-cyan-100">Scheduled Time</span>
              <span className="text-sm font-bold">{nearestUpcoming.visit_date} at {nearestUpcoming.visit_time}</span>
            </div>
          </div>
        </div>
      )}

      {/* Lists Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">

        {/* Upcoming visits */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-sm transition duration-300">
          <h3 className="font-bold text-slate-800 dark:text-slate-200 text-sm border-b border-slate-100 dark:border-slate-800 pb-3 mb-4 flex items-center gap-1.5">
            <Clock className="w-4 h-4 text-cyan-500" />
            Upcoming Visits
          </h3>
          <div className="space-y-4 max-h-60 overflow-y-auto pr-1">
            {loading ? (
              <div className="animate-pulse space-y-3">
                <div className="h-10 bg-slate-100 dark:bg-slate-850 rounded" />
                <div className="h-10 bg-slate-100 dark:bg-slate-850 rounded" />
              </div>
            ) : upcomingVisits.length > 0 ? (
              upcomingVisits.map((v) => (
                <div key={v.id} className="p-3 bg-slate-50/50 dark:bg-slate-950/20 border border-slate-100 dark:border-slate-850 rounded-xl flex justify-between items-center">
                  <div className="min-w-0">
                    <p className="text-xs font-extrabold text-slate-800 dark:text-slate-200 truncate">
                      {v.note?.split(' - ')[0]}
                    </p>
                    <p className="text-[10px] text-slate-400 font-semibold truncate">
                      {v.note?.split(' - ')[1]}
                    </p>
                    <span className="text-[10px] text-cyan-600 dark:text-cyan-400 font-semibold block mt-1">
                      {v.visit_date} at {v.visit_time}
                    </span>
                  </div>
                  <button
                    onClick={() => handleDelete(v.id)}
                    className="p-1.5 text-slate-400 hover:text-red-500 rounded hover:bg-slate-100 dark:hover:bg-slate-800 transition shrink-0"
                    title="Cancel appointment"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))
            ) : (
              <p className="text-xs text-slate-400 text-center py-6">No upcoming checkups scheduled.</p>
            )}
          </div>
        </div>

        {/* Past visits */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-sm transition duration-300">
          <h3 className="font-bold text-slate-800 dark:text-slate-200 text-sm border-b border-slate-100 dark:border-slate-800 pb-3 mb-4 flex items-center gap-1.5">
            <CheckCircle className="w-4 h-4 text-emerald-500" />
            Past Visits History
          </h3>
          <div className="space-y-4 max-h-60 overflow-y-auto pr-1">
            {loading ? (
              <div className="animate-pulse space-y-3">
                <div className="h-10 bg-slate-100 dark:bg-slate-850 rounded" />
              </div>
            ) : pastVisits.length > 0 ? (
              pastVisits.map((v) => (
                <div key={v.id} className="p-3 bg-slate-50/50 dark:bg-slate-950/20 border border-slate-100 dark:border-slate-850 rounded-xl flex justify-between items-center opacity-75">
                  <div className="min-w-0">
                    <p className="text-xs font-extrabold text-slate-800 dark:text-slate-200 truncate">
                      {v.note?.split(' - ')[0]}
                    </p>
                    <p className="text-[10px] text-slate-400 font-semibold truncate">
                      {v.note?.split(' - ')[1]}
                    </p>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 font-semibold block mt-1">
                      Completed on {v.visit_date}
                    </span>
                  </div>
                  <button
                    onClick={() => handleDelete(v.id)}
                    className="p-1.5 text-slate-400 hover:text-red-500 rounded hover:bg-slate-100 dark:hover:bg-slate-850 transition shrink-0"
                    title="Delete log"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))
            ) : (
              <p className="text-xs text-slate-400 text-center py-6">No previous checkup records.</p>
            )}
          </div>
        </div>

      </div>
    </div>
  )
}

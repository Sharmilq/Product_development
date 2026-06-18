import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'

// Parse Android-format date "dd MMM yyyy" + time "09:00 AM" into a JS Date.
// Mirrors the same helper in VisitReminders.jsx.
function parseVisitDateTime(dateStr, timeStr) {
  try {
    const MONTHS = { Jan:0, Feb:1, Mar:2, Apr:3, May:4, Jun:5,
                     Jul:6, Aug:7, Sep:8, Oct:9, Nov:10, Dec:11 }
    const [dd, mon, yyyy] = (dateStr || '').trim().split(' ')
    const timeTrimmed = (timeStr || '12:00 AM').trim()
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
  } catch { return new Date(NaN) }
}
import { supabase } from '../supabaseClient'
import { 
  Flame, Award, Calendar, Clock, ArrowRight, Activity, Smile, ShieldAlert,
  ChevronRight, BellRing, Sparkles, CheckCircle2, ScanFace, CheckSquare
} from 'lucide-react'

export default function Dashboard({ profile, streak, setStreak, fetchProfile }) {
  const navigate = useNavigate()
  const [lastAssessment, setLastAssessment] = useState(null)
  const [lastScan, setLastScan] = useState(null)
  const [nextVisit, setNextVisit] = useState(null)
  const [habitStatus, setHabitStatus] = useState({ brushing: false, flossing: false })
  const [loadingHabit, setLoadingHabit] = useState(true)

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    const userIdStr = localStorage.getItem('dentnova_user_id')
    if (!userIdStr) return
    const userId = parseInt(userIdStr)
    console.log('[DentNova] WEB_ASSESSMENT_QUERY_USER_ID:', userId)

    try {
      // 1. Get latest assessment
      const { data: assessments } = await supabase
        .from('assessments')
        .select('*')
        .eq('user_id', userId)
        .order('created_at', { ascending: false })
        .limit(1)
      console.log('[DentNova] WEB_ASSESSMENTS_FETCHED_COUNT:', assessments?.length || 0)
      if (assessments && assessments.length > 0) {
        console.log('[DentNova] WEB_LATEST_ASSESSMENT_SCORE:', assessments[0].score)
        setLastAssessment(assessments[0])
      }

      // 2. Get latest scan
      const { data: scans } = await supabase
        .from('tooth_scans')
        .select('*')
        .eq('user_id', userId)
        .order('created_at', { ascending: false })
        .limit(1)
      if (scans && scans.length > 0) {
        setLastScan(scans[0])
      }

      // 3. Get upcoming visit
      const { data: visits } = await supabase
        .from('visits')
        .select('*')
        .eq('user_id', userId)
        .order('visit_date', { ascending: true })
      
      if (visits && visits.length > 0) {
        const nowMs = Date.now()
        // Filter visits in the future using the Android-format date parser.
        const upcoming = visits
          .filter(v => {
            const dt = parseVisitDateTime(v.visit_date, v.visit_time)
            return !isNaN(dt.getTime()) && dt.getTime() >= nowMs
          })
          .sort((a, b) =>
            parseVisitDateTime(a.visit_date, a.visit_time) -
            parseVisitDateTime(b.visit_date, b.visit_time)
          )
        if (upcoming.length > 0) setNextVisit(upcoming[0])
      }

      // 4. Fetch Habit status
      const { data: habitRows } = await supabase
        .from('users')
        .select('brushing_done, flossing_done, habit_date')
        .eq('user_id', userId)
        .single()
      
      const getLocalDateString = () => {
        const tzOffset = new Date().getTimezoneOffset() * 60000
        return new Date(Date.now() - tzOffset).toISOString().split('T')[0]
      }

      const getLocalYesterdayString = () => {
        const tzOffset = new Date().getTimezoneOffset() * 60000
        const yesterdayMs = Date.now() - 24 * 60 * 60 * 1000
        return new Date(yesterdayMs - tzOffset).toISOString().split('T')[0]
      }

      if (habitRows) {
        const todayStr = getLocalDateString()
        if (habitRows.habit_date === todayStr) {
          setHabitStatus({
            brushing: habitRows.brushing_done || false,
            flossing: habitRows.flossing_done || false
          })
        } else {
          // Reset status in DB and state
          setHabitStatus({ brushing: false, flossing: false })
          await supabase
            .from('users')
            .update({
              brushing_done: false,
              flossing_done: false,
              habit_date: todayStr
            })
            .eq('user_id', userId)
        }
      }
    } catch (err) {
      console.error('Error fetching dashboard statistics:', err)
    } finally {
      setLoadingHabit(false)
    }
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

  const handleToggleHabit = async (type) => {
    const userIdStr = localStorage.getItem('dentnova_user_id')
    if (!userIdStr) return
    const userId = parseInt(userIdStr)
    const todayStr = getLocalDateString()
    const yesterdayStr = getLocalYesterdayString()

    let nextBrush = habitStatus.brushing
    let nextFloss = habitStatus.flossing

    if (type === 'brushing') {
      nextBrush = true
    } else if (type === 'flossing') {
      nextFloss = true
    }

    setHabitStatus({
      brushing: nextBrush,
      flossing: nextFloss
    })

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

      let newStreak = dbStreakCount

      const bothDone = nextBrush && nextFloss

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
      const { error: updateError } = await supabase
        .from('users')
        .update({
          brushing_done: nextBrush,
          flossing_done: nextFloss,
          habit_date: todayStr,
          streak_count: newStreak,
          last_streak_date: bothDone && dbLastStreak !== todayStr ? todayStr : dbLastStreak
        })
        .eq('user_id', userId)

      if (updateError) throw updateError

      setStreak(newStreak)

      // Required Logs
      console.log('WEB_APP_USER_ID:', userId)
      console.log('WEB_BRUSHING_DONE:', nextBrush)
      console.log('WEB_FLOSSING_DONE:', nextFloss)
      console.log('WEB_STREAK_BEFORE:', dbStreakCount)
      console.log('WEB_STREAK_AFTER:', newStreak)
      console.log('WEB_LAST_STREAK_DATE:', bothDone && dbLastStreak !== todayStr ? todayStr : dbLastStreak)

      fetchProfile()
    } catch (err) {
      console.error('Failed to update habits status:', err)
    }
  }

  const getCountdown = (dateStr) => {
    if (!dateStr) return null
    const diff = new Date(dateStr) - new Date()
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
    if (days < 0) return 'Passed'
    if (days === 0) return 'Today!'
    if (days === 1) return 'Tomorrow'
    return `In ${days} Days`
  }

  const quickActions = [
    {
      title: 'Practice Brushing',
      desc: 'Launch the interactive 2-minute timer guide.',
      path: '/brushing-timer',
      icon: Clock,
      color: 'from-cyan-500 to-blue-500 hover:shadow-cyan-500/10'
    },
    {
      title: 'AI Tooth Scan',
      desc: 'Submit a photo to assess gum health and cleanliness.',
      path: '/tooth-scan',
      icon: ScanFace,
      color: 'from-emerald-500 to-teal-500 hover:shadow-emerald-500/10'
    },
    {
      title: 'Check Oral Health',
      desc: 'Start the clinical assessment questionnaire.',
      path: '/assessment',
      icon: Award,
      color: 'from-violet-500 to-purple-500 hover:shadow-violet-500/10'
    },
    {
      title: 'Reminders & Vis',
      desc: 'Configure alarms and dental appointments.',
      path: '/reminders',
      icon: Calendar,
      color: 'from-amber-500 to-orange-500 hover:shadow-amber-500/10'
    }
  ]

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      {/* Welcome & Streak Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-cyan-500/5 rounded-full blur-2xl"></div>
        <div className="space-y-2 relative z-10">
          <div className="flex items-center gap-2 text-cyan-600 dark:text-cyan-400 font-semibold text-sm">
            <Sparkles className="w-4 h-4 fill-cyan-500" />
            Dental Wellness Dashboard
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900 dark:text-white">
            Hello, {profile?.name || 'User'}! 😁
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 max-w-lg leading-relaxed">
            Welcome back to your dental care suite. Keep up your daily brushing routine to maintain a glowing smile.
          </p>
        </div>

        <div className="flex items-center gap-4 shrink-0 bg-slate-50 dark:bg-slate-950 p-4 rounded-xl border border-slate-150 dark:border-slate-850">
          <div className="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-500 flex items-center justify-center">
            <Flame className="w-7 h-7 fill-amber-500" />
          </div>
          <div>
            <p className="text-2xl font-bold text-slate-800 dark:text-slate-100">{streak || 0} Days</p>
            <p className="text-xs text-slate-400 font-medium">Daily Active Streak</p>
          </div>
        </div>
      </div>

      {/* Grid of Main Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        
        {/* Habit Checklist */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-sm text-slate-500 dark:text-slate-400 uppercase tracking-wider">Today's Habits</h3>
              <CheckSquare className="w-5 h-5 text-cyan-500" />
            </div>
            
            <div className="space-y-3">
              <label className="flex items-center gap-3 p-2.5 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-850 border border-slate-100 dark:border-slate-800/80 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={habitStatus.brushing}
                  onChange={() => handleToggleHabit('brushing')}
                  disabled={loadingHabit}
                  className="w-4.5 h-4.5 rounded border-slate-300 text-cyan-500 focus:ring-cyan-500"
                />
                <span className={`text-sm font-semibold ${habitStatus.brushing ? 'line-through text-slate-400' : 'text-slate-700 dark:text-slate-200'}`}>
                  🪥 Brushing Done
                </span>
              </label>
              <label className="flex items-center gap-3 p-2.5 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-850 border border-slate-100 dark:border-slate-800/80 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={habitStatus.flossing}
                  onChange={() => handleToggleHabit('flossing')}
                  disabled={loadingHabit}
                  className="w-4.5 h-4.5 rounded border-slate-300 text-cyan-500 focus:ring-cyan-500"
                />
                <span className={`text-sm font-semibold ${habitStatus.flossing ? 'line-through text-slate-400' : 'text-slate-700 dark:text-slate-200'}`}>
                  🧵 Flossing Done
                </span>
              </label>
            </div>
          </div>
          <span className="text-[10px] text-slate-400 block mt-4">Mark daily habits to sync updates.</span>
        </div>

        {/* Oral health score card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-sm text-slate-500 dark:text-slate-400 uppercase tracking-wider">Oral Health</h3>
              <Award className="w-5 h-5 text-violet-500" />
            </div>

            {lastAssessment ? (
              <div className="space-y-1">
                <p className="text-4xl font-extrabold text-violet-600 dark:text-violet-400">{lastAssessment.score}%</p>
                <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                  Risk Level: <span className="font-bold">{lastAssessment.risk}</span>
                </p>
                <p className="text-[10px] text-slate-450 mt-1">Calculated on {new Date(lastAssessment.created_at).toLocaleDateString()}</p>
              </div>
            ) : (
              <div className="space-y-2">
                <p className="text-sm text-slate-500 dark:text-slate-400 leading-snug">No health assessments taken yet.</p>
                <Link to="/assessment" className="text-xs text-cyan-500 hover:underline font-bold flex items-center gap-1">
                  Start Assessment <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            )}
          </div>
          {lastAssessment && (
            <Link to="/assessment-result" className="text-xs text-violet-500 hover:underline font-bold flex items-center gap-1 mt-4">
              View Assessment Details <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          )}
        </div>

        {/* Tooth Scan Card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-sm text-slate-500 dark:text-slate-400 uppercase tracking-wider">Latest AI Scan</h3>
              <ScanFace className="w-5 h-5 text-emerald-500" />
            </div>

            {lastScan ? (
              <div className="space-y-1">
                <p className="text-lg font-bold text-emerald-600 dark:text-emerald-400 truncate">{lastScan.result_label}</p>
                <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-400 font-semibold pt-1">
                  <div>Cleanliness: {lastScan.cleanliness_score || 0}%</div>
                  <div>Inflam: {lastScan.gum_score || 0}%</div>
                </div>
                <p className="text-[10px] text-slate-450 mt-1">Scanned on {new Date(lastScan.created_at).toLocaleDateString()}</p>
              </div>
            ) : (
              <div className="space-y-2">
                <p className="text-sm text-slate-500 dark:text-slate-400 leading-snug">No AI dental scans recorded yet.</p>
                <Link to="/tooth-scan" className="text-xs text-cyan-500 hover:underline font-bold flex items-center gap-1">
                  Scan Teeth Now <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            )}
          </div>
          {lastScan && (
            <Link to="/tooth-scan" className="text-xs text-emerald-500 hover:underline font-bold flex items-center gap-1 mt-4">
              View Scan Reports <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          )}
        </div>

        {/* Upcoming Dental Visit Card */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-sm text-slate-500 dark:text-slate-400 uppercase tracking-wider">Dental Visit</h3>
              <Calendar className="w-5 h-5 text-amber-500" />
            </div>

            {nextVisit ? (
              <div className="space-y-1">
                <p className="text-lg font-bold text-amber-600 dark:text-amber-400">{getCountdown(nextVisit.visit_date)}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400 font-semibold truncate">
                  📍 {nextVisit.note?.split(' - ')[0] || 'Clinic appointment'}
                </p>
                <p className="text-[10px] text-slate-400">{nextVisit.visit_date} at {nextVisit.visit_time}</p>
              </div>
            ) : (
              <div className="space-y-2">
                <p className="text-sm text-slate-500 dark:text-slate-400 leading-snug">No upcoming appointments booked.</p>
                <Link to="/visit-reminders" className="text-xs text-cyan-500 hover:underline font-bold flex items-center gap-1">
                  Schedule Appointment <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            )}
          </div>
          {nextVisit && (
            <Link to="/visit-reminders" className="text-xs text-amber-500 hover:underline font-bold flex items-center gap-1 mt-4">
              Manage Appointment <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          )}
        </div>
      </div>

      {/* Quick Actions / Navigation Grid */}
      <div className="space-y-4">
        <h2 className="text-xl font-bold text-slate-900 dark:text-white">Quick Actions</h2>
        
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {quickActions.map((action, i) => (
            <div
              key={i}
              onClick={() => navigate(action.path)}
              className={`p-6 rounded-2xl bg-gradient-to-br bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-lg cursor-pointer hover:-translate-y-1 transition-all duration-200 flex flex-col justify-between min-h-40 group`}
            >
              <div className="flex justify-between items-start">
                <div className="p-3 bg-cyan-500/10 dark:bg-cyan-500/20 text-cyan-600 dark:text-cyan-400 rounded-xl">
                  <action.icon className="w-6 h-6" />
                </div>
                <ArrowRight className="w-4 h-4 text-slate-300 dark:text-slate-600 group-hover:text-cyan-500 group-hover:translate-x-1 transition-all" />
              </div>
              <div className="mt-4">
                <h3 className="font-bold text-slate-900 dark:text-white group-hover:text-cyan-500 transition-colors">{action.title}</h3>
                <p className="text-xs text-slate-450 dark:text-slate-400 mt-1 leading-snug">{action.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

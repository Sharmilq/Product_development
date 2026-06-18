import React, { useEffect, useState } from 'react'
import { useLocation, Link, useNavigate } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { Award, RefreshCw, ChevronRight, CheckCircle, ShieldAlert } from 'lucide-react'

export default function AssessmentResult() {
  const location = useLocation()
  const navigate = useNavigate()
  const [score, setScore] = useState(null)
  const [label, setLabel] = useState('Moderate')
  const [breakdown, setBreakdown] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (location.state && location.state.score !== undefined) {
      // Came directly from completing the assessment — use passed state
      setScore(location.state.score)
      setLabel(location.state.label || 'Moderate')
    } else {
      // Navigated directly (e.g. Dashboard "View Assessment Details") —
      // fetch the latest assessment from Supabase using the stored integer user_id.
      const fetchLatest = async () => {
        setLoading(true)
        try {
          const userIdStr = localStorage.getItem('dentnova_user_id')
          if (userIdStr) {
            const userId = parseInt(userIdStr, 10)
            const { data, error } = await supabase
              .from('assessments')
              .select('score, risk, created_at')
              .eq('user_id', userId)
              .order('created_at', { ascending: false })
              .limit(1)
            if (!error && data && data.length > 0) {
              setScore(data[0].score)
              setLabel(data[0].risk || 'Moderate')
              return
            }
          }
          // Fallback: try localStorage cache written by Assessment.jsx
          const cachedScore = localStorage.getItem('last_score')
          const cachedLabel = localStorage.getItem('last_label')
          if (cachedScore) {
            setScore(parseInt(cachedScore, 10))
            setLabel(cachedLabel || 'Moderate')
          } else {
            setScore(0)
          }
        } catch (err) {
          console.error('[DentNova] AssessmentResult fetch error:', err)
          setScore(0)
        } finally {
          setLoading(false)
        }
      }
      fetchLatest()
    }

    const tempAnswers = localStorage.getItem('temp_answers')
    if (tempAnswers) {
      try { setBreakdown(JSON.parse(tempAnswers)) } catch (_) {}
    }
  }, [location])

  const getRiskColor = (risk) => {
    switch ((risk || '').toLowerCase()) {
      case 'low':  return 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20'
      case 'high': return 'text-red-500 bg-red-500/10 border-red-500/20'
      default:     return 'text-amber-500 bg-amber-500/10 border-amber-500/20'
    }
  }

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-cyan-500" />
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-12 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8 shadow-md space-y-8 relative overflow-hidden transition-all duration-300">
        <div className="absolute top-0 right-0 w-24 h-24 bg-cyan-500/5 rounded-full blur-xl" />

        <div className="text-center space-y-3">
          <div className="w-16 h-16 rounded-2xl bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 flex items-center justify-center mx-auto shadow-sm">
            <Award className="w-8 h-8" />
          </div>
          <h2 className="text-sm font-extrabold text-cyan-500 uppercase tracking-[2px]">
            🦷 ORAL HEALTH SCORE
          </h2>
          <div className="space-y-1">
            <p className="text-6xl font-black text-slate-900 dark:text-white">{score ?? 0}%</p>
            <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold border ${getRiskColor(label)}`}>
              {label} Risk
            </span>
          </div>
          <p className="text-slate-600 dark:text-slate-400 max-w-md mx-auto leading-relaxed text-sm">
            😁 Keep taking care of your smile! Let's improve together.
          </p>
        </div>

        {/* Recommendations */}
        <div className="p-6 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-4">
          <h3 className="font-bold text-sm text-slate-500 dark:text-slate-400 uppercase tracking-wider">
            Recommendations
          </h3>
          <ul className="space-y-3 text-sm text-slate-700 dark:text-slate-300">
            <li className="flex items-start gap-2.5">
              <CheckCircle className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
              <span>Brush your teeth at least twice daily for 2 full minutes. Use the brushing timer tool for visual guides.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <CheckCircle className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
              <span>Floss daily to remove food particles and debris between your teeth where manual brushing cannot reach.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <CheckCircle className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
              <span>Rinse your mouth with water immediately after consuming sugary or acidic meals.</span>
            </li>
            {label.toLowerCase() !== 'low' && (
              <li className="flex items-start gap-2.5">
                <ShieldAlert className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
                <span>You have indicated some areas of vulnerability. Scheduling a visit with a professional dentist is recommended.</span>
              </li>
            )}
          </ul>
        </div>

        {/* Survey Breakdown (if exists) */}
        {breakdown && (
          <div className="space-y-4">
            <h3 className="font-bold text-sm text-slate-500 dark:text-slate-400 uppercase tracking-wider">
              Survey Answers Summary
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-h-60 overflow-y-auto pr-2">
              {Object.keys(breakdown).map((key, idx) => (
                <div key={idx} className="p-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-col justify-between text-xs">
                  <span className="text-slate-400 font-semibold mb-1 truncate">Question {idx + 1}</span>
                  <span className="text-slate-800 dark:text-slate-200 font-bold">{breakdown[key]}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center border-t border-slate-100 dark:border-slate-800/80 pt-6">
          <Link
            to="/assessment"
            className="flex items-center gap-2 px-5 py-3 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-300 rounded-xl text-sm font-bold transition-all"
          >
            <RefreshCw className="w-4 h-4" />
            Retake Assessment
          </Link>
          <Link
            to="/dashboard"
            className="flex items-center gap-2 px-6 py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 hover:shadow-cyan-500/20 transition-all"
          >
            Go to Dashboard
            <ChevronRight className="w-4 h-4" />
          </Link>
        </div>
      </div>
    </div>
  )
}

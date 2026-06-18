import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { AlertCircle, ArrowLeft, ArrowRight, Sparkles, Check, Info, AlertTriangle } from 'lucide-react'

// Question Data from AssessmentActivity.java
const EMOJIS = ["🪥","⏱","🧵","🩸","🍬","🍭","🦷","😬","🚬","📅","💊","💧","😴"]
const QUESTIONS = [
  "How often do you brush your teeth?",
  "How long do you brush each time?",
  "Do you floss regularly?",
  "Do your gums bleed when brushing?",
  "How often do you consume sugary foods or drinks?",
  "Do you rinse your mouth after sugary foods?",
  "Do you experience tooth sensitivity?",
  "Do you grind your teeth at night?",
  "Do you smoke or use tobacco?",
  "When was your last dental checkup?",
  "Do you take any medications regularly?",
  "How much water do you drink daily?",
  "Do you experience dry mouth?"
]
const OPTIONS = [
  ["Twice a day","Once a day","Sometimes","Rarely"],
  ["2 minutes or more","About 1 minute","Less than 1 minute"],
  ["Daily","Few times a week","Rarely","Never"],
  ["Never","Sometimes","Often"],
  ["Rarely","Few times a week","Daily","Multiple times a day"],
  ["Always","Sometimes","Rarely","Never"],
  ["Never","Sometimes","Often","Always"],
  ["No","Occasionally","Often","I'm not sure"],
  ["Never","Occasionally","Daily"],
  ["Within 6 months","6–12 months ago","1–2 years ago","Over 2 years ago"],
  ["No","Yes"],
  ["More than 2L","1–2L","Less than 1L","Rarely drink water"],
  ["Never","Sometimes","Often","Always"]
]
const UNHEALTHY = [
  [2,3],[1,2],[2,3],[1,2],[2,3],[2,3],[2,3],[2],[1,2],[2,3],[],[2,3],[2,3]
]
const WARNINGS = [
  "Brushing less than twice a day increases cavity and gum risk.",
  "Dentists recommend brushing for at least 2 minutes each session.",
  "Not flossing significantly increases risk of gum problems.",
  "Frequent bleeding may indicate gingivitis — a dental visit is recommended.",
  "High sugar exposure puts you at high risk for cavities.",
  "Rinsing after sugary foods helps neutralise harmful acids.",
  "Frequent sensitivity may indicate enamel erosion — consult your dentist.",
  "Frequent grinding (bruxism) wears enamel. Ask your dentist about a night guard.",
  "Tobacco use stains teeth, causes gum disease, and raises oral cancer risk.",
  "Dental checkups every 6 months catch issues early. Book one soon!",
  null,
  "Low water intake can cause dry mouth, which increases cavity risk.",
  "Persistent dry mouth raises your risk of tooth decay and gum disease."
]

export default function Assessment() {
  const navigate = useNavigate()
  const [currentIdx, setCurrentIdx] = useState(0)
  const [answers, setAnswers] = useState({})
  const [warningDismissed, setWarningDismissed] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  const handleSelectOption = (optIdx) => {
    setAnswers({
      ...answers,
      [currentIdx]: optIdx
    })
    setWarningDismissed(false)
  }

  const handleNext = () => {
    if (answers[currentIdx] === undefined) return

    if (currentIdx < QUESTIONS.length - 1) {
      setCurrentIdx(currentIdx + 1)
      setWarningDismissed(false)
    } else {
      handleSubmit()
    }
  }

  const handleBack = () => {
    if (currentIdx > 0) {
      setCurrentIdx(currentIdx - 1)
      setWarningDismissed(false)
    }
  }

  const computeScoreLocal = () => {
    let score = 100
    for (let i = 0; i < QUESTIONS.length; i++) {
      const selectedOpt = answers[i]
      if (selectedOpt === undefined) continue
      const unhealthyOpts = UNHEALTHY[i]
      if (unhealthyOpts.includes(selectedOpt)) {
        score -= 8
      }
    }
    return Math.max(0, Math.min(100, score))
  }

  const handleSubmit = async () => {
    setSubmitting(true)
    setErrorMsg('')

    const userIdStr = localStorage.getItem('dentnova_user_id')
    if (!userIdStr) {
      setErrorMsg('User identity not verified. Please log in again.')
      setSubmitting(false)
      return
    }
    const userId = parseInt(userIdStr)
    console.log('[DentNova] WEB_ASSESSMENT_QUERY_USER_ID:', userId)

    // Prepare payload for ML check
    const mlPayload = {
      q1_brushing_frequency: OPTIONS[0][answers[0]],
      q2_brushing_duration: OPTIONS[1][answers[1]],
      q3_flossing: OPTIONS[2][answers[2]],
      q4_gum_bleeding: OPTIONS[3][answers[3]],
      q5_sugary_foods: OPTIONS[4][answers[4]],
      q6_rinse_after_sugar: OPTIONS[5][answers[5]],
      q7_tooth_sensitivity: OPTIONS[6][answers[6]],
      q8_teeth_grinding: OPTIONS[7][answers[7]],
      q9_tobacco: OPTIONS[8][answers[8]],
      q10_last_checkup: OPTIONS[9][answers[9]],
      q11_medications: OPTIONS[10][answers[10]],
      q12_water_intake: OPTIONS[11][answers[11]],
      q13_dry_mouth: OPTIONS[12][answers[12]]
    }

    let finalScore = 0
    let finalLabel = 'Moderate'

    try {
      // Predict via ML API
      const response = await fetch('https://dentnova-ml.onrender.com/predict', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(mlPayload)
      })

      if (response.ok) {
        const result = await response.json()
        finalScore = result.score
        finalLabel = result.risk
      } else {
        throw new Error('ML endpoint returned error status')
      }
    } catch (err) {
      console.warn('ML Prediction failed or timed out. Falling back to local scoring rules.', err)
      // Fallback local calculations
      finalScore = computeScoreLocal()
      finalLabel = finalScore < 30 ? 'High' : finalScore < 60 ? 'Moderate' : 'Low'
    }

    try {
      // Save details to Supabase assessments table
      const { error: dbError } = await supabase
        .from('assessments')
        .insert({
          user_id: userId,
          score: finalScore,
          risk: finalLabel
        })

      if (dbError) throw dbError

      // Save notification to users list
      await supabase
        .from('notifications')
        .insert({
          user_id: userId,
          title: 'Assessment Completed 🦷',
          body: `Your oral health score is ${finalScore}% (${finalLabel} Risk)`
        })

      // Update preferences / dashboard indicator
      localStorage.setItem(`assessment_done_${userId}`, 'true')

      // Save answers temporarily to verify breakdown
      const answersMap = {}
      for (let i = 0; i < QUESTIONS.length; i++) {
        answersMap[`answer_${i}`] = OPTIONS[i][answers[i]]
      }
      localStorage.setItem('temp_answers', JSON.stringify(answersMap))

      navigate('/assessment-result', { state: { score: finalScore, label: finalLabel } })
    } catch (err) {
      setErrorMsg('Failed to submit results. Please try again.')
      console.error(err)
    } finally {
      setSubmitting(false)
    }
  }

  const selectedOption = answers[currentIdx]
  const isUnhealthy = selectedOption !== undefined && UNHEALTHY[currentIdx].includes(selectedOption)
  const showWarning = isUnhealthy && WARNINGS[currentIdx] && !warningDismissed
  const progressPercent = Math.round(((currentIdx + 1) / QUESTIONS.length) * 100)

  return (
    <div className="max-w-3xl mx-auto px-4 py-12 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      
      {/* Progress indicators */}
      <div className="space-y-3 mb-8">
        <div className="flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
          <span>Question {currentIdx + 1} of {QUESTIONS.length}</span>
          <span className="text-cyan-500 font-extrabold">{progressPercent}% Completed</span>
        </div>
        <div className="w-full bg-slate-200 dark:bg-slate-800 h-2.5 rounded-full overflow-hidden">
          <div 
            className="bg-gradient-to-r from-cyan-500 to-blue-600 h-full rounded-full transition-all duration-300"
            style={{ width: `${progressPercent}%` }}
          ></div>
        </div>
      </div>

      {/* Main card */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8 shadow-md space-y-6 relative overflow-hidden transition-all duration-300">
        <div className="absolute top-0 right-0 w-24 h-24 bg-cyan-500/5 rounded-full blur-xl"></div>
        
        {errorMsg && (
          <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-xl text-sm flex items-center gap-2">
            <AlertCircle className="w-5 h-5" />
            <span>{errorMsg}</span>
          </div>
        )}

        <div className="space-y-4 text-center sm:text-left">
          <div className="text-5xl select-none animate-bounce">{EMOJIS[currentIdx]}</div>
          <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white leading-snug">
            {QUESTIONS[currentIdx]}
          </h2>
        </div>

        {/* Options list */}
        <div className="space-y-3.5">
          {OPTIONS[currentIdx].map((opt, i) => {
            const isSelected = selectedOption === i
            return (
              <button
                key={i}
                type="button"
                onClick={() => handleSelectOption(i)}
                className={`w-full flex items-center justify-between p-4 rounded-xl border text-sm font-semibold transition-all duration-200 ${
                  isSelected
                    ? 'border-cyan-500 bg-cyan-500/10 text-cyan-600 dark:text-cyan-400'
                    : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-350'
                }`}
              >
                <span>{opt}</span>
                <div className={`w-5 h-5 rounded-full flex items-center justify-center border transition-all ${
                  isSelected 
                    ? 'bg-cyan-500 border-cyan-500 text-white' 
                    : 'border-slate-300 dark:border-slate-700'
                }`}>
                  {isSelected && <Check className="w-3.5 h-3.5 stroke-[3px]" />}
                </div>
              </button>
            )
          })}
        </div>

        {/* Warning banner */}
        {showWarning && (
          <div className="p-4 bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20 text-amber-800 dark:text-amber-400 rounded-xl text-xs flex gap-3 relative transition-all animate-[fadeIn_0.3s]">
            <Info className="w-5 h-5 shrink-0 text-amber-500" />
            <div className="space-y-1">
              <p className="font-bold">Dental Health Warning</p>
              <p className="leading-relaxed">{WARNINGS[currentIdx]}</p>
            </div>
            <button
              onClick={() => setWarningDismissed(true)}
              className="absolute top-2 right-2 text-slate-400 hover:text-slate-600 text-[10px] font-bold uppercase tracking-wider"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Next & Back controls */}
        <div className="flex justify-between items-center border-t border-slate-100 dark:border-slate-800/80 pt-6">
          {currentIdx > 0 ? (
            <button
              onClick={handleBack}
              className="px-5 py-3 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-300 rounded-xl text-sm font-bold transition flex items-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              Back
            </button>
          ) : (
            <div></div>
          )}

          <button
            onClick={handleNext}
            disabled={selectedOption === undefined || submitting}
            className="px-6 py-3 bg-cyan-500 hover:bg-cyan-600 disabled:opacity-50 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 hover:shadow-cyan-500/20 transition flex items-center gap-2"
          >
            {submitting ? 'Calculating...' : currentIdx < QUESTIONS.length - 1 ? 'Next' : 'See Results'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  )
}

import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { predictToothScan } from '../services/api'
import { 
  ScanFace, UploadCloud, Trash2, ArrowRight, ShieldAlert, CheckCircle, 
  History, Download, AlertTriangle, Play, Info, Eye
} from 'lucide-react'

export default function ToothScan() {
  const [assessmentDone, setAssessmentDone] = useState(false)
  const [checkingAuth, setCheckingAuth] = useState(true)
  
  const [file, setFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [imageBase64, setImageBase64] = useState('')

  const [analyzing, setAnalyzing] = useState(false)
  const [result, setResult] = useState(null)
  const [history, setHistory] = useState([])
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    checkAssessmentStatus()
  }, [])

  const checkAssessmentStatus = async () => {
    try {
      // Prefer auth email lookup for integer user_id (same as Android)
      const { data: { session } } = await supabase.auth.getSession()
      const email = session?.user?.email || ''
      console.log('[DentNova] WEB_SCAN_AUTH_EMAIL:', email)

      // Resolve integer user_id: try DB lookup by email, fall back to localStorage
      let userId = null
      if (email) {
        const { data: userRow } = await supabase
          .from('users')
          .select('user_id')
          .eq('email', email)
          .maybeSingle()
        if (userRow) userId = userRow.user_id
      }
      if (!userId) {
        const stored = localStorage.getItem('dentnova_user_id')
        userId = stored ? parseInt(stored) : null
      }
      if (!userId) { setCheckingAuth(false); return }
      console.log('[DentNova] WEB_SCAN_APP_USER_ID:', userId)

      // Check if assessment completed (localStorage fast-path, then DB)
      const done = localStorage.getItem(`assessment_done_${userId}`) === 'true'
      let hasAssessment = done  // use local var — React state updates are async

      if (!done) {
        const { data } = await supabase
          .from('assessments')
          .select('id')
          .eq('user_id', userId)
          .limit(1)
        if (data && data.length > 0) {
          hasAssessment = true
          setAssessmentDone(true)
          localStorage.setItem(`assessment_done_${userId}`, 'true')
        } else {
          setAssessmentDone(false)
        }
      } else {
        setAssessmentDone(true)
      }

      // Only fetch history when assessment confirmed — using local var NOT state
      if (hasAssessment) {
        fetchHistory(userId)
      }
    } catch (err) {
      console.error('[DentNova] checkAssessmentStatus error:', err)
    } finally {
      setCheckingAuth(false)
    }
  }

  const fetchHistory = async (userId) => {
    console.log('[DentNova] WEB_SCAN_APP_USER_ID (fetchHistory):', userId)
    try {
      const { data, error } = await supabase
        .from('tooth_scans')
        .select('*')
        .eq('user_id', userId)
        .order('created_at', { ascending: false })

      if (error) {
        console.error('[DentNova] WEB_SCAN_QUERY_ERROR:', error.message)
        return
      }
      console.log('[DentNova] WEB_SCAN_QUERY_RESULT:', data?.length ?? 0, 'records')
      if (data) setHistory(data)
    } catch (err) {
      console.error('[DentNova] WEB_SCAN_QUERY_ERROR:', err)
    }
  }

  /**
   * Issue 3 fix: normalise image_base64 to a valid <img src> data URL.
   * Android saves pure base64 → we add the "data:image/jpeg;base64," prefix.
   * Web saves a full data URL  → leave unchanged.
   * Missing / null             → return '' (placeholder shown instead).
   */
  const toDataUrl = (raw) => {
    if (!raw) return ''
    if (raw.startsWith('data:')) return raw          // already a data URL (web-saved)
    return `data:image/jpeg;base64,${raw}`           // pure base64 (Android-saved)
  }

  /**
   * Robust helper to format confidence values.
   * - Accepts strings or numbers.
   * - Handles 0-1 values (e.g. 0.97 -> 97%).
   * - Handles 0-100 values (e.g. 97 -> 97%).
   * - Safely returns "N/A" for null, undefined, or invalid inputs.
   */
  const formatConfidence = (value) => {
    if (value === null || value === undefined || value === '') {
      return 'N/A'
    }
    let num = Number(value)
    if (isNaN(num)) {
      if (typeof value === 'string' && value.includes('%')) {
        num = parseFloat(value)
      }
    }
    if (isNaN(num)) {
      return 'N/A'
    }
    if (num > 0 && num <= 1) {
      return `${Math.round(num * 100)}%`
    }
    return `${Math.round(num)}%`
  }

  /**
   * Helper to derive predicted class and confidence value from historical label.
   */
  const parseResultLabel = (label) => {
    let predClass = 'Healthy'
    let confidence = undefined

    if (!label) return { predClass, confidence }

    const lower = label.toLowerCase()
    if (lower.includes('calculus') || lower.includes('cleanliness')) {
      predClass = 'Calculus'
    } else if (lower.includes('gingival') || lower.includes('inflammation') || lower.includes('gingivitis')) {
      predClass = 'Gingivitis'
    } else if (lower.includes('healthy')) {
      predClass = 'Healthy'
    }

    // Regex to search for a percentage in the label (e.g. "97%", "89")
    const match = label.match(/(\d+)\s*%/ )
    if (match) {
      confidence = parseFloat(match[1]) / 100
    }
    
    return { predClass, confidence }
  }


  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0]
    if (selectedFile) {
      setFile(selectedFile)
      setPreviewUrl(URL.createObjectURL(selectedFile))
      setErrorMsg('')
      
      // Convert to base64 for database storage
      const reader = new FileReader()
      reader.onloadend = () => {
        setImageBase64(reader.result)
      }
      reader.readAsDataURL(selectedFile)
    }
  }

  const handleRemove = () => {
    setFile(null)
    setPreviewUrl('')
    setImageBase64('')
    setResult(null)
    setErrorMsg('')
  }

  const handleAnalyze = async () => {
    if (!file) return
    setAnalyzing(true)
    setErrorMsg('')
    setResult(null)

    const userIdStr = localStorage.getItem('dentnova_user_id')
    if (!userIdStr) {
      setErrorMsg('User session not found. Please log in again.')
      setAnalyzing(false)
      return
    }
    const userId = parseInt(userIdStr)

    try {
      // ── Call the LOCAL Flask backend via the Vite /api proxy ──────────────
      // predictToothScan() → POST /api/predict-tooth → http://127.0.0.1:5000/predict-tooth
      // Identical multipart request as Android ApiService.java predictToothScan()
      const mlData = await predictToothScan(file)

      // mlData fields exactly match the Flask JSON response (and Android display):
      // { class, confidence, inflammation_score, cleanliness_score, overall_score, result_label }
      const predClass      = mlData.class
      const confidence     = mlData.confidence
      const inflammation   = mlData.inflammation_score
      const cleanliness    = mlData.cleanliness_score
      const overall        = mlData.overall_score
      const label          = mlData.result_label

      console.log('[DentNova] Tooth scan result:', { predClass, confidence, inflammation, cleanliness, overall, label })

      // Only save to Supabase if the image is valid (mirrors Android logic)
      if (predClass !== 'Invalid') {
        const { error: dbError } = await supabase
          .from('tooth_scans')
          .insert({
            user_id:          userId,
            plaque_score:     overall,
            gum_score:        inflammation,
            cleanliness_score: cleanliness,
            result_label:     label,
            image_base64:     imageBase64
          })
        if (dbError) {
          console.error('[DentNova] Supabase save error:', dbError)
          // Non-fatal — still show the result
        } else {
          fetchHistory(userId)
        }
      }

      setResult({
        predClass,
        confidence,
        label,
        inflammation,
        cleanliness,
        overall,
        date: new Date().toLocaleString()
      })
    } catch (err) {
      console.error('[DentNova] Tooth scan error:', err)
      if (err.message && err.message.includes('Failed to fetch')) {
        setErrorMsg('Cannot reach the backend server. Ensure Flask is running at http://127.0.0.1:5000.')
      } else {
        setErrorMsg(err.message || 'Failed to process image scan. Please verify file format and try again.')
      }
    } finally {
      setAnalyzing(false)
    }
  }

  const handlePrintReport = () => {
    if (!result) return
    const patientName = profileName()
    const printWindow = window.open('', '_blank')
    printWindow.document.write(`
      <html>
        <head>
          <title>DentNova AI Oral Health Report</title>
          <style>
            body { font-family: system-ui, sans-serif; padding: 40px; color: #1a2332; }
            .header { border-bottom: 2px solid #00bcd4; padding-bottom: 20px; margin-bottom: 30px; }
            .title { font-size: 24px; font-weight: bold; color: #00bcd4; }
            .meta { color: #6b7b8d; font-size: 12px; margin-top: 5px; }
            .score-box { background: #eaf9fc; padding: 20px; border-radius: 12px; margin: 20px 0; }
            .metric { font-size: 16px; margin: 10px 0; font-weight: 500; }
            .footer { margin-top: 50px; font-size: 11px; color: #94a3b8; text-align: center; border-t: 1px solid #e0e8ef; padding-top: 20px; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="title">DentNova AI Oral Health Report</div>
            <div class="meta">Patient: ${patientName} | Date: ${result.date}</div>
          </div>
          <h2>AI Scan Result</h2>
          <div class="score-box">
            <div style="font-size: 20px; font-weight: bold; margin-bottom: 15px;">${result.label}</div>
            <div class="metric">Gum Cleanliness: ${result.cleanliness}%</div>
            <div class="metric">Gingival Inflammation: ${result.inflammation}%</div>
            <div class="metric">Overall Gum Health Score: ${result.overall}%</div>
          </div>
          <h3>Recommendations</h3>
          <ul>
            <li>Brush twice daily for 2 minutes.</li>
            <li>Floss once daily to reduce plaque buildup.</li>
            <li>Rinse after sugary foods or drinks.</li>
            <li>Visit a dentist if pain, bleeding, or swelling continues.</li>
          </ul>
          <div class="footer">
            Disclaimer: This AI-assisted report is for awareness only and is not a medical diagnosis.
            <br>&copy; ${new Date().getFullYear()} DentNova
          </div>
        </body>
      </html>
    `)
    printWindow.document.close()
    printWindow.print()
  }

  const profileName = () => {
    try {
      const email = supabase.auth.user()?.email || 'User'
      return email.split('@')[0]
    } catch {
      return 'DentNova Patient'
    }
  }

  if (checkingAuth) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-cyan-500"></div>
      </div>
    )
  }

  if (!assessmentDone) {
    return (
      <div className="max-w-md mx-auto px-4 py-20 text-center space-y-6">
        <div className="w-16 h-16 rounded-2xl bg-red-500/10 text-red-500 flex items-center justify-center mx-auto">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <div className="space-y-2">
          <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white">Oral Assessment Required</h2>
          <p className="text-sm text-slate-500 dark:text-slate-450 leading-relaxed">
            You must complete the Oral Health Assessment survey before you can access the AI Tooth Scan feature.
          </p>
        </div>
        <Link
          to="/assessment"
          className="inline-flex items-center gap-2 px-6 py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 hover:shadow-cyan-500/20 transition-all duration-200"
        >
          Take Assessment
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 grid grid-cols-1 lg:grid-cols-3 gap-8 bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      
      {/* Upload & Result Panel */}
      <div className="lg:col-span-2 space-y-6">
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6 relative overflow-hidden transition-all duration-300">
          <div className="absolute top-0 right-0 w-24 h-24 bg-cyan-500/5 rounded-full blur-xl"></div>
          
          <div className="space-y-1">
            <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
              <ScanFace className="w-6 h-6 text-cyan-500" />
              AI Tooth Scan
            </h2>
            <p className="text-xs text-slate-400 font-medium">Upload teeth pictures to detect cleanliness & inflammation.</p>
          </div>

          {errorMsg && (
            <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-xl text-sm flex items-center gap-2">
              <AlertTriangle className="w-5 h-5" />
              <span>{errorMsg}</span>
            </div>
          )}

          {!file ? (
            /* Upload Zone */
            <label className="border-2 border-dashed border-slate-200 dark:border-slate-800 hover:border-cyan-500 dark:hover:border-cyan-500/50 rounded-2xl p-10 flex flex-col items-center justify-center text-center cursor-pointer transition bg-slate-50/50 dark:bg-slate-950/20">
              <UploadCloud className="w-12 h-12 text-slate-400 mb-4" />
              <p className="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-1">Drag and drop file here</p>
              <p className="text-xs text-slate-400 mb-3">Supporting JPG, PNG images up to 5MB</p>
              <span className="px-4 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-bold shadow-sm text-slate-600 dark:text-slate-350">
                Browse Files
              </span>
              <input
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="hidden"
              />
            </label>
          ) : (
            /* Preview Zone */
            <div className="space-y-6">
              <div className="relative rounded-2xl overflow-hidden aspect-video bg-slate-100 dark:bg-slate-950 flex items-center justify-center border border-slate-150 dark:border-slate-850">
                <img
                  src={previewUrl}
                  alt="Tooth preview"
                  className="max-h-full max-w-full object-contain"
                />
                
                {!result && !analyzing && (
                  <button
                    onClick={handleRemove}
                    className="absolute top-3 right-3 p-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition"
                    title="Remove image"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>

              {!result && (
                <div className="flex gap-4">
                  <button
                    onClick={handleRemove}
                    disabled={analyzing}
                    className="flex-1 py-3 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-300 rounded-xl text-sm font-bold transition disabled:opacity-50"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleAnalyze}
                    disabled={analyzing}
                    className="flex-1 py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 hover:shadow-cyan-500/20 transition flex items-center justify-center gap-2 disabled:opacity-50"
                  >
                    {analyzing ? 'Scanning...' : 'Analyze with AI'}
                    <Play className="w-4 h-4 fill-white" />
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Results display */}
          {result && (
            <div className="border-t border-slate-100 dark:border-slate-800/80 pt-6 space-y-6 transition duration-300">

              {/* Status banner — colour matches predicted class, same as Android */}
              {result.predClass === 'Invalid' ? (
                <div className="p-4 bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20 text-amber-800 dark:text-amber-400 rounded-xl text-xs flex gap-3">
                  <AlertTriangle className="w-5 h-5 shrink-0 text-amber-500" />
                  <div>
                    <p className="font-bold text-sm">Invalid Image</p>
                    <p className="leading-relaxed mt-0.5">Please upload a clear, well-lit photo of teeth.</p>
                  </div>
                </div>
              ) : (
                <div className="p-4 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 text-emerald-800 dark:text-emerald-400 rounded-xl text-xs flex gap-3">
                  <CheckCircle className="w-5 h-5 shrink-0 text-emerald-500" />
                  <div>
                    <p className="font-bold text-sm">Scan Diagnostics Complete</p>
                    <p className="leading-relaxed mt-0.5">Results successfully synced to your history.</p>
                  </div>
                </div>
              )}

              <div className="space-y-4">
                {/* Predicted class badge + confidence — mirrors Android result header */}
                <div className="flex items-center gap-3 flex-wrap">
                  <h3 className="font-extrabold text-lg text-slate-900 dark:text-white">
                    Scan Summary:
                  </h3>
                  <span className={`px-3 py-1 rounded-full text-sm font-bold ${
                    result.predClass === 'Healthy'   ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400' :
                    result.predClass === 'Gingivitis' ? 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-400' :
                    result.predClass === 'Calculus'   ? 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-400' :
                    'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
                  }`}>
                    {result.predClass || 'Diagnosis'}{(() => { const v = Number(result.confidence); return (!isNaN(v) && v !== 0) ? ` · ${formatConfidence(result.confidence)} confidence` : ''; })()}
                  </span>
                </div>
                <p className="text-sm text-slate-500 dark:text-slate-400">{result.label}</p>

                {result.predClass !== 'Invalid' && (
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="p-4 bg-slate-50 dark:bg-slate-950 border border-slate-150 dark:border-slate-850 rounded-xl">
                      <p className="text-xs text-slate-400 font-semibold mb-1">Gum Cleanliness</p>
                      <p className="text-2xl font-bold text-slate-800 dark:text-slate-200">{result.cleanliness}%</p>
                    </div>
                    <div className="p-4 bg-slate-50 dark:bg-slate-950 border border-slate-150 dark:border-slate-850 rounded-xl">
                      <p className="text-xs text-slate-400 font-semibold mb-1">Gingival Inflammation</p>
                      <p className="text-2xl font-bold text-slate-800 dark:text-slate-200">{result.inflammation}%</p>
                    </div>
                    <div className="p-4 bg-slate-50 dark:bg-slate-950 border border-slate-150 dark:border-slate-850 rounded-xl">
                      <p className="text-xs text-slate-400 font-semibold mb-1">Overall Gum Health</p>
                      <p className="text-2xl font-bold text-slate-800 dark:text-slate-200">{result.overall}%</p>
                    </div>
                  </div>
                )}
              </div>

              <div className="flex gap-4">
                <button
                  onClick={handleRemove}
                  className="flex-1 py-3 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-350 rounded-xl text-sm font-bold transition"
                >
                  Scan Another Tooth
                </button>
                <button
                  onClick={handlePrintReport}
                  className="flex-1 py-3 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-cyan-500/15 hover:shadow-cyan-500/20 transition flex items-center justify-center gap-2"
                >
                  Download Report PDF
                  <Download className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Historical Records list */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-sm flex flex-col min-h-60 transition duration-300">
        <div className="flex items-center gap-2 mb-6 border-b border-slate-100 dark:border-slate-800 pb-3">
          <History className="w-5 h-5 text-slate-400" />
          <h3 className="font-bold text-slate-800 dark:text-slate-200 text-sm">Scan History</h3>
        </div>

        <div className="flex-grow overflow-y-auto space-y-4 max-h-[480px] pr-2">
          {history.length > 0 ? (
            history.map((h, i) => (
              <div
                key={h.id}
                className="p-3.5 bg-slate-50/50 dark:bg-slate-950/20 hover:bg-slate-50 dark:hover:bg-slate-850 border border-slate-100 dark:border-slate-850 rounded-xl flex items-center gap-3 transition cursor-pointer"
                onClick={() => {
                  const { predClass, confidence } = parseResultLabel(h.result_label)
                  setResult({
                    label: h.result_label,
                    predClass,
                    confidence,
                    cleanliness: h.cleanliness_score || 0,
                    inflammation: h.gum_score || 0,
                    overall: h.plaque_score || 0,
                    date: new Date(h.created_at).toLocaleString()
                  })
                  // Issue 3 fix: normalise before using as preview src
                  setPreviewUrl(toDataUrl(h.image_base64))
                  setFile(true) // stub
                }}
              >
                {h.image_base64 ? (
                  <img
                    src={toDataUrl(h.image_base64)}
                    alt="Scan thumbnail"
                    className="w-12 h-12 rounded-lg object-cover border border-slate-200 dark:border-slate-800"
                  />
                ) : (
                  <div className="w-12 h-12 rounded-lg bg-cyan-500/10 text-cyan-500 flex items-center justify-center shrink-0">
                    <ScanFace className="w-5 h-5" />
                  </div>
                )}
                <div className="flex-grow min-w-0">
                  <p className="text-xs font-bold text-slate-800 dark:text-slate-200 truncate">{h.result_label}</p>
                  <p className="text-[10px] text-slate-400 font-semibold">
                    {new Date(h.created_at).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' })}
                    {' '}·{' '}
                    {new Date(h.created_at).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
                <Eye className="w-4 h-4 text-slate-300 dark:text-slate-600 group-hover:text-cyan-500 shrink-0" />
              </div>
            ))
          ) : (
            <div className="h-full flex flex-col items-center justify-center text-center text-slate-400 text-xs py-10">
              No previous tooth scans recorded.
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from '../supabaseClient'
import { 
  ArrowLeft, User, Lock, Moon, Sun, ShieldCheck, 
  MessageSquare, Info, LogOut, CheckCircle, AlertCircle 
} from 'lucide-react'

export default function Settings({ theme, toggleTheme, profile, onProfileUpdate }) {
  const navigate = useNavigate()
  
  // State for forms/modals
  const [showPasswordForm, setShowPasswordForm] = useState(false)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [updatingPass, setUpdatingPass] = useState(false)
  const [passError, setPassError] = useState('')
  const [passSuccess, setPassSuccess] = useState('')

  const [showPrivacyModal, setShowPrivacyModal] = useState(false)

  const [showFeedbackForm, setShowFeedbackForm] = useState(false)
  const [feedbackText, setFeedbackText] = useState('')
  const [submittingFeedback, setSubmittingFeedback] = useState(false)
  const [feedbackError, setFeedbackError] = useState('')
  const [feedbackSuccess, setFeedbackSuccess] = useState('')

  const handleLogout = async () => {
    await supabase.auth.signOut()
    localStorage.clear()
    navigate('/')
    window.location.reload()
  }

  const handleChangePassword = async (e) => {
    e.preventDefault()
    setUpdatingPass(true)
    setPassError('')
    setPassSuccess('')

    if (newPassword !== confirmPassword) {
      setPassError('Passwords do not match.')
      setUpdatingPass(false)
      return
    }

    if (newPassword.length < 8) {
      setPassError('Password must be at least 8 characters long.')
      setUpdatingPass(false)
      return
    }

    try {
      const { error } = await supabase.auth.updateUser({
        password: newPassword
      })

      if (error) throw error

      setPassSuccess('Password updated successfully.')
      setNewPassword('')
      setConfirmPassword('')
      setTimeout(() => setShowPasswordForm(false), 2000)
    } catch (err) {
      setPassError(err.message || 'Failed to update password.')
    } finally {
      setUpdatingPass(false)
    }
  }

  const handleSendFeedback = async (e) => {
    e.preventDefault()
    if (!feedbackText.trim()) return

    setSubmittingFeedback(true)
    setFeedbackError('')
    setFeedbackSuccess('')

    const userIdStr = localStorage.getItem('dentnova_user_id')
    const userId = userIdStr ? parseInt(userIdStr, 10) : null

    try {
      const { error } = await supabase
        .from('feedback')
        .insert({
          user_id: userId,
          content: feedbackText,
          rating: 5, // Default rating
          created_at: new Date().toISOString()
        })

      if (error) throw error

      setFeedbackSuccess('Thank you for your feedback!')
      setFeedbackText('')
      setTimeout(() => setShowFeedbackForm(false), 2000)
    } catch (err) {
      setFeedbackError(err.message || 'Failed to send feedback.')
    } finally {
      setSubmittingFeedback(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 bg-slate-50 dark:bg-slate-950 min-h-screen transition-colors duration-300">
      
      {/* Header */}
      <div className="flex items-center gap-4 mb-8">
        <button
          onClick={() => navigate('/profile')}
          className="p-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white">Settings</h2>
      </div>

      <div className="space-y-6">
        
        {/* SECTION 1: ACCOUNT */}
        <div className="space-y-2">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">Account</h3>
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-2 shadow-sm divide-y divide-slate-100 dark:divide-slate-800 overflow-hidden">
            
            {/* Edit Profile Link */}
            <div 
              onClick={() => navigate('/profile')}
              className="flex items-center justify-between p-4 hover:bg-slate-50 dark:hover:bg-slate-850 cursor-pointer transition"
            >
              <div className="flex items-center gap-3">
                <User className="w-5 h-5 text-slate-400" />
                <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Edit profile</span>
              </div>
            </div>

            {/* Change Password Trigger */}
            <div className="p-4 hover:bg-slate-50 dark:hover:bg-slate-850 transition">
              <div 
                onClick={() => {
                  setShowPasswordForm(!showPasswordForm)
                  setPassError('')
                  setPassSuccess('')
                }}
                className="flex items-center justify-between cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <Lock className="w-5 h-5 text-slate-400" />
                  <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Change password</span>
                </div>
              </div>

              {showPasswordForm && (
                <form onSubmit={handleChangePassword} className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-800/60 space-y-4">
                  {passError && (
                    <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-lg text-xs flex items-center gap-2">
                      <AlertCircle className="w-4 h-4 shrink-0" />
                      <span>{passError}</span>
                    </div>
                  )}
                  {passSuccess && (
                    <div className="bg-emerald-50 dark:bg-emerald-900/10 border border-emerald-200 dark:border-emerald-800/30 text-emerald-600 dark:text-emerald-400 p-3 rounded-lg text-xs flex items-center gap-2">
                      <CheckCircle className="w-4 h-4 shrink-0" />
                      <span>{passSuccess}</span>
                    </div>
                  )}

                  <div className="space-y-1">
                    <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400">New Password</label>
                    <input
                      type="password"
                      placeholder="Min 8 characters"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      className="w-full p-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-850 rounded-xl text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/20"
                      required
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Confirm Password</label>
                    <input
                      type="password"
                      placeholder="Confirm new password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="w-full p-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-850 rounded-xl text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/20"
                      required
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={updatingPass}
                    className="w-full py-2.5 bg-slate-800 hover:bg-slate-900 dark:bg-slate-700 dark:hover:bg-slate-600 text-white rounded-xl text-xs font-bold transition disabled:opacity-50"
                  >
                    {updatingPass ? 'Updating...' : 'Change Password'}
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>

        {/* SECTION 2: PREFERENCES */}
        <div className="space-y-2">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">Preferences</h3>
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-2 shadow-sm divide-y divide-slate-100 dark:divide-slate-800 overflow-hidden">
            
            {/* Theme Toggle */}
            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                {theme === 'dark' ? <Moon className="w-5 h-5 text-slate-400" /> : <Sun className="w-5 h-5 text-slate-400" />}
                <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Theme</span>
              </div>
              <button
                onClick={toggleTheme}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 text-xs font-bold rounded-xl transition"
              >
                Switch to {theme === 'dark' ? 'Light Mode' : 'Dark Mode'}
              </button>
            </div>
          </div>
        </div>

        {/* SECTION 3: ABOUT */}
        <div className="space-y-2">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">About</h3>
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-2 shadow-sm divide-y divide-slate-100 dark:divide-slate-800 overflow-hidden">
            
            {/* Privacy Policy Trigger */}
            <div 
              onClick={() => setShowPrivacyModal(true)}
              className="flex items-center justify-between p-4 hover:bg-slate-50 dark:hover:bg-slate-850 cursor-pointer transition"
            >
              <div className="flex items-center gap-3">
                <ShieldCheck className="w-5 h-5 text-slate-400" />
                <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Privacy policy</span>
              </div>
            </div>

            {/* Send Feedback Trigger */}
            <div className="p-4 hover:bg-slate-50 dark:hover:bg-slate-850 transition">
              <div 
                onClick={() => {
                  setShowFeedbackForm(!showFeedbackForm)
                  setFeedbackError('')
                  setFeedbackSuccess('')
                }}
                className="flex items-center justify-between cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <MessageSquare className="w-5 h-5 text-slate-400" />
                  <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Send feedback</span>
                </div>
              </div>

              {showFeedbackForm && (
                <form onSubmit={handleSendFeedback} className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-800/60 space-y-4">
                  {feedbackError && (
                    <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/30 text-red-600 dark:text-red-400 p-3 rounded-lg text-xs flex items-center gap-2">
                      <AlertCircle className="w-4 h-4 shrink-0" />
                      <span>{feedbackError}</span>
                    </div>
                  )}
                  {feedbackSuccess && (
                    <div className="bg-emerald-50 dark:bg-emerald-900/10 border border-emerald-200 dark:border-emerald-800/30 text-emerald-600 dark:text-emerald-400 p-3 rounded-lg text-xs flex items-center gap-2">
                      <CheckCircle className="w-4 h-4 shrink-0" />
                      <span>{feedbackSuccess}</span>
                    </div>
                  )}

                  <div className="space-y-1">
                    <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Your Feedback</label>
                    <textarea
                      placeholder="Share your experience or report bugs..."
                      rows={3}
                      value={feedbackText}
                      onChange={(e) => setFeedbackText(e.target.value)}
                      className="w-full p-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-850 rounded-xl text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/20"
                      required
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={submittingFeedback || !feedbackText.trim()}
                    className="w-full py-2.5 bg-slate-800 hover:bg-slate-900 dark:bg-slate-700 dark:hover:bg-slate-600 text-white rounded-xl text-xs font-bold transition disabled:opacity-50"
                  >
                    {submittingFeedback ? 'Sending...' : 'Submit Feedback'}
                  </button>
                </form>
              )}
            </div>

            {/* Version */}
            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                <Info className="w-5 h-5 text-slate-400" />
                <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">DentNova Version</span>
              </div>
              <span className="text-xs text-slate-400">v1.0</span>
            </div>

            {/* Logout */}
            <div 
              onClick={handleLogout}
              className="flex items-center gap-3 p-4 hover:bg-red-50 dark:hover:bg-red-950/20 cursor-pointer transition group"
            >
              <LogOut className="w-5 h-5 text-red-500 group-hover:scale-105 transition" />
              <span className="text-sm font-bold text-red-500">Logout</span>
            </div>
          </div>
        </div>

      </div>

      {/* PRIVACY POLICY MODAL */}
      {showPrivacyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/40 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl max-w-lg w-full max-h-[80vh] overflow-y-auto shadow-2xl p-6 relative">
            <h3 className="text-xl font-bold text-slate-900 dark:text-white mb-4">Privacy Policy</h3>
            <div className="space-y-4 text-sm text-slate-600 dark:text-slate-400 overflow-y-auto leading-relaxed">
              <p>At DentNova, we take your privacy very seriously. We only collect details essential to your oral care companion, including:</p>
              <ul className="list-disc pl-5 space-y-2">
                <li>Your display name, age, gender, and oral concern notes.</li>
                <li>Your habit completions (brushing and flossing history).</li>
                <li>Your scan history and assessment scores.</li>
              </ul>
              <p>We do not share your private diagnostic details with third-party advertising companies. All information is secured in encrypted cloud database tables matching modern security principles.</p>
              <p>For more detailed assistance, please contact our support team at support@dentnova.com.</p>
            </div>
            <button
              onClick={() => setShowPrivacyModal(false)}
              className="mt-6 w-full py-2.5 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl text-sm font-bold transition"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

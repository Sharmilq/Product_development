import React from 'react'
import { Link } from 'react-router-dom'
import { 
  ScanFace, Clock, Bell, Award, ArrowRight, ShieldCheck, Heart, Sparkles, Smile
} from 'lucide-react'

export default function Landing({ user }) {
  const features = [
    {
      title: 'AI Tooth Scan',
      desc: 'Upload a photo of your teeth and get instant AI-powered analysis of gum health, cleanliness, and inflammation.',
      icon: ScanFace,
      color: 'text-cyan-500 bg-cyan-500/10'
    },
    {
      title: 'Interactive Brushing Timer',
      desc: 'Follow the 2-minute timer with visual brushing guides to clean all areas of your mouth, and track daily streaks.',
      icon: Clock,
      color: 'text-blue-500 bg-blue-500/10'
    },
    {
      title: 'Oral Health Assessment',
      desc: 'Complete an in-depth dentist-backed oral health survey to compute your personalized dental risk score.',
      icon: Award,
      color: 'text-indigo-500 bg-indigo-500/10'
    },
    {
      title: 'Smart Reminders',
      desc: 'Schedule reminders for brushing, flossing, toothbrush replacement, and dental visits with automated notifications.',
      icon: Bell,
      color: 'text-amber-500 bg-amber-500/10'
    }
  ]

  return (
    <div className="bg-slate-50 dark:bg-slate-950 transition-colors duration-300">
      {/* Hero Section */}
      <section className="relative overflow-hidden py-24 lg:py-32">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div className="space-y-8 text-center lg:text-left">
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 text-xs font-semibold uppercase tracking-wider">
                <Sparkles className="w-4 h-4 fill-cyan-500 animate-spin-slow" />
                AI-Powered Dental Companion
              </div>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-900 dark:text-white font-sans">
                Next-Gen Oral Health <br />
                <span className="bg-gradient-to-r from-cyan-500 to-blue-600 bg-clip-text text-transparent">
                  In the Palm of Your Hand
                </span>
              </h1>
              <p className="text-lg text-slate-600 dark:text-slate-350 max-w-xl mx-auto lg:mx-0">
                DentNova uses state of the art computer vision and interactive habit building to revolutionize your oral hygiene. Scan, track, and protect your smile.
              </p>
              <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
                {user ? (
                  <Link
                    to="/dashboard"
                    className="flex items-center justify-center gap-2 px-8 py-4 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl shadow-lg shadow-cyan-500/20 font-bold transition-all hover:-translate-y-0.5"
                  >
                    Go to Dashboard
                    <ArrowRight className="w-5 h-5" />
                  </Link>
                ) : (
                  <>
                    <Link
                      to="/auth?mode=register"
                      className="flex items-center justify-center gap-2 px-8 py-4 bg-cyan-500 hover:bg-cyan-600 text-white rounded-xl shadow-lg shadow-cyan-500/20 font-bold transition-all hover:-translate-y-0.5"
                    >
                      Get Started Free
                      <ArrowRight className="w-5 h-5" />
                    </Link>
                    <Link
                      to="/auth"
                      className="flex items-center justify-center gap-2 px-8 py-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850 text-slate-700 dark:text-slate-200 rounded-xl font-bold transition-all hover:-translate-y-0.5"
                    >
                      Log In
                    </Link>
                  </>
                )}
              </div>
            </div>

            {/* Premium UI Mockup Presentation */}
            <div className="relative flex justify-center">
              <div className="absolute -inset-4 bg-cyan-500/20 dark:bg-cyan-500/10 rounded-3xl blur-3xl opacity-50 animate-pulse"></div>
              <div className="relative w-full max-w-md bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl p-6 transition-all duration-300">
                <div className="flex justify-between items-center mb-6">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                    <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                    <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                  </div>
                  <span className="text-xs text-slate-400 font-semibold">AI Scan Preview</span>
                </div>
                
                {/* Visual AI Scan simulation */}
                <div className="relative bg-slate-100 dark:bg-slate-950 rounded-xl overflow-hidden aspect-video flex items-center justify-center group mb-6 border border-slate-200/50 dark:border-slate-800/50">
                  <Smile className="w-16 h-16 text-cyan-500/50 animate-bounce" />
                  <div className="absolute inset-0 bg-gradient-to-t from-cyan-500/20 to-transparent"></div>
                  <div className="absolute top-0 left-0 right-0 h-0.5 bg-cyan-500 shadow-[0_0_10px_#00bcd4] animate-[scan_3s_infinite]"></div>
                </div>

                <div className="space-y-4">
                  <div className="h-4 w-2/3 bg-slate-200 dark:bg-slate-800 rounded"></div>
                  <div className="h-3 w-full bg-slate-100 dark:bg-slate-800/50 rounded"></div>
                  <div className="h-3 w-5/6 bg-slate-100 dark:bg-slate-800/50 rounded"></div>
                  <div className="grid grid-cols-3 gap-2 pt-2">
                    <div className="bg-emerald-50 dark:bg-emerald-500/10 p-2.5 rounded-lg text-center border border-emerald-500/20">
                      <span className="text-[10px] block text-slate-400">Gum Health</span>
                      <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400">92%</span>
                    </div>
                    <div className="bg-amber-50 dark:bg-amber-500/10 p-2.5 rounded-lg text-center border border-amber-500/20">
                      <span className="text-[10px] block text-slate-400">Inflam</span>
                      <span className="text-xs font-bold text-amber-600 dark:text-amber-400">14%</span>
                    </div>
                    <div className="bg-cyan-50 dark:bg-cyan-500/10 p-2.5 rounded-lg text-center border border-cyan-500/20">
                      <span className="text-[10px] block text-slate-400">Clean</span>
                      <span className="text-xs font-bold text-cyan-600 dark:text-cyan-400">95%</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="bg-slate-100 dark:bg-slate-900/50 border-y border-slate-200 dark:border-slate-800/50 py-12 transition-colors duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 text-center">
            <div>
              <p className="text-3xl sm:text-4xl font-extrabold text-cyan-500">98%</p>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">AI analysis accuracy</p>
            </div>
            <div>
              <p className="text-3xl sm:text-4xl font-extrabold text-blue-500">15K+</p>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">Brushing Hours Logged</p>
            </div>
            <div>
              <p className="text-3xl sm:text-4xl font-extrabold text-indigo-500">100%</p>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">Secure HIPAA Data Encrypted</p>
            </div>
            <div>
              <p className="text-3xl sm:text-4xl font-extrabold text-amber-500">4.9 ★</p>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">User Satisfaction Rating</p>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center space-y-4 mb-20">
          <h2 className="text-3xl sm:text-4xl font-bold text-slate-900 dark:text-white">
            Everything you need for a healthy smile
          </h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-xl mx-auto">
            DentNova integrates advanced AI technology with daily dental trackers to make dental hygiene motivating, fun, and healthy.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {features.map((f, i) => (
            <div
              key={i}
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 rounded-2xl shadow-sm hover:shadow-md hover:border-cyan-500/30 dark:hover:border-cyan-500/30 hover:-translate-y-1 transition-all duration-300"
            >
              <div className={`w-12 h-12 rounded-xl flex items-center justify-center mb-6 ${f.color}`}>
                <f.icon className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-2">{f.title}</h3>
              <p className="text-sm text-slate-500 dark:text-slate-450 leading-relaxed">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Health Seal Banner */}
      <section className="bg-gradient-to-r from-cyan-500 to-blue-600 text-white py-16 text-center">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
          <ShieldCheck className="w-16 h-16 mx-auto stroke-1" />
          <h2 className="text-3xl font-bold">Trusted by Dental Care Experts</h2>
          <p className="text-cyan-50 max-w-xl mx-auto leading-relaxed">
            All algorithms, recommendations, checklists, and oral assessments are aligned with clinical dentistry research to provide safe guidance for users.
          </p>
        </div>
      </section>
    </div>
  )
}

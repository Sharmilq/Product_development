import React from 'react'

export default function Footer() {
  return (
    <footer className="bg-slate-900 text-slate-400 py-12 mt-auto border-t border-slate-800 transition-colors duration-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <h3 className="text-white text-lg font-bold mb-4">🦷 DentNova</h3>
            <p className="text-sm">
              Your smart personal companion for oral care. Scan teeth, analyze gum health and cleanliness, track brushing streaks, and set custom reminders.
            </p>
          </div>
          <div>
            <h3 className="text-white text-lg font-bold mb-4">Quick Links</h3>
            <ul className="space-y-2 text-sm">
              <li>Check your Oral Health Score</li>
              <li>Practice 2-minute Brushing</li>
              <li>Learn with Dental Articles</li>
              <li>AI Tooth Scan Analysis</li>
            </ul>
          </div>
          <div>
            <h3 className="text-white text-lg font-bold mb-4">Dental Wisdom</h3>
            <p className="text-sm italic">
              "A beautiful smile starts with healthy habits. Remember to brush twice daily, floss every night, and check in with your dentist twice a year."
            </p>
          </div>
        </div>
        <div className="border-t border-slate-800 mt-8 pt-8 text-center text-xs flex flex-col md:flex-row justify-between items-center gap-4">
          <p>© {new Date().getFullYear()} DentNova. All rights reserved.</p>
          <p className="text-slate-500">Disclaimer: AI assessment is for informational purposes only and not a medical diagnosis.</p>
        </div>
      </div>
    </footer>
  )
}

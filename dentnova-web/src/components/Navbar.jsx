import React, { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { 
  Menu, X, Sun, Moon, LogOut, User, Flame, 
  Bell, Award, Calendar, BookOpen, Clock, Activity, ScanFace
} from 'lucide-react'
import { supabase } from '../supabaseClient'

export default function Navbar({ user, profile, streak, theme, toggleTheme }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [isOpen, setIsOpen] = useState(false)
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const [notifOpen, setNotifOpen] = useState(false)
  const [notifs, setNotifs] = useState([])
  const [imgError, setImgError] = useState(false)

  useEffect(() => {
    setImgError(false)
  }, [profile?.photo_url, user?.user_metadata?.avatar_url, user?.user_metadata?.picture])

  const getAvatarUrl = () => {
    if (profile?.photo_url) return profile.photo_url
    if (user?.user_metadata) {
      return user.user_metadata.avatar_url || user.user_metadata.picture || ''
    }
    return ''
  }

  const avatarUrl = getAvatarUrl()
  const hasAvatar = avatarUrl && !imgError

  const getInitials = () => {
    const displayName = profile?.name || user?.email || 'U'
    return displayName.charAt(0).toUpperCase()
  }

  useEffect(() => {
    if (user) {
      fetchNotifications()
    }
  }, [user])

  const fetchNotifications = async () => {
    try {
      const userId = localStorage.getItem('dentnova_user_id')
      if (!userId) return
      const { data, error } = await supabase
        .from('notifications')
        .select('*')
        .eq('user_id', parseInt(userId))
        .order('created_at', { ascending: false })
        .limit(5)
      
      if (data) setNotifs(data)
    } catch (err) {
      console.error('Error fetching notifications:', err)
    }
  }

  const handleLogout = async () => {
    await supabase.auth.signOut()
    localStorage.clear()
    navigate('/')
    window.location.reload()
  }

  const navItems = [
    { name: 'Dashboard', path: '/dashboard', icon: Activity },
    { name: 'Brushing Timer', path: '/brushing-timer', icon: Clock },
    { name: 'Assessment', path: '/assessment', icon: Award },
    { name: 'Tooth Scan', path: '/tooth-scan', icon: ScanFace },
    { name: 'Education', path: '/education', icon: BookOpen },
    { name: 'Reminders', path: '/reminders', icon: Bell },
    { name: 'Visit Reminder', path: '/visit-reminders', icon: Calendar }
  ]

  const isActive = (path) => location.pathname === path

  return (
    <nav className="sticky top-0 z-50 glass border-b border-slate-200/50 dark:border-slate-800/50 transition-colors duration-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-8">
            <Link to={user ? '/dashboard' : '/'} className="flex items-center gap-2">
              <span className="text-2xl font-extrabold bg-gradient-to-r from-cyan-500 to-blue-600 bg-clip-text text-transparent flex items-center gap-2">
                🦷 DentNova
              </span>
            </Link>
            {user && (
              <div className="hidden lg:flex items-center space-x-1">
                {navItems.map((item) => (
                  <Link
                    key={item.name}
                    to={item.path}
                    className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                      isActive(item.path)
                        ? 'bg-cyan-500/10 text-cyan-600 dark:text-cyan-400'
                        : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    <item.icon className="w-4 h-4" />
                    <span>{item.name}</span>
                  </Link>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-center space-x-4">
            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 transition-colors"
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>

            {user ? (
              <>
                {/* Active Streak */}
                <div 
                  className="flex items-center gap-1 bg-amber-500/10 dark:bg-amber-500/20 text-amber-600 dark:text-amber-400 px-3 py-1.5 rounded-full text-sm font-semibold select-none cursor-pointer"
                  title="Your daily active streak"
                  onClick={() => navigate('/brushing-timer')}
                >
                  <Flame className="w-4 h-4 fill-amber-500 animate-pulse" />
                  <span>{streak || 0}d</span>
                </div>

                {/* Notifications */}
                <div className="relative">
                  <button
                    onClick={() => {
                      setNotifOpen(!notifOpen)
                      setDropdownOpen(false)
                    }}
                    className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 relative"
                  >
                    <Bell className="w-5 h-5" />
                    {notifs.length > 0 && (
                      <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 border border-white rounded-full"></span>
                    )}
                  </button>

                  {notifOpen && (
                    <div className="absolute right-0 mt-2 w-80 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-xl py-2 z-50">
                      <div className="px-4 py-2 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                        <span className="font-semibold text-sm text-slate-800 dark:text-slate-200">Notifications</span>
                        <span className="text-xs text-cyan-600 hover:underline cursor-pointer" onClick={fetchNotifications}>Refresh</span>
                      </div>
                      <div className="max-h-60 overflow-y-auto">
                        {notifs.length > 0 ? (
                          notifs.map((n) => (
                            <div key={n.id} className="px-4 py-3 border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50">
                              <p className="text-xs font-semibold text-slate-800 dark:text-slate-200">{n.title}</p>
                              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{n.body}</p>
                              <span className="text-[10px] text-slate-400 dark:text-slate-500 block mt-1">
                                {new Date(n.created_at).toLocaleDateString()}
                              </span>
                            </div>
                          ))
                        ) : (
                          <div className="px-4 py-6 text-center text-xs text-slate-400">
                            No notifications yet
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                {/* Profile dropdown */}
                <div className="relative">
                  <button
                    onClick={() => {
                      setDropdownOpen(!dropdownOpen)
                      setNotifOpen(false)
                    }}
                    className="flex items-center gap-2 focus:outline-none"
                  >
                    <div className="w-8 h-8 rounded-full overflow-hidden flex items-center justify-center border-2 border-cyan-500 bg-slate-100 dark:bg-slate-800 shrink-0 relative">
                      {hasAvatar ? (
                        <img
                          src={avatarUrl.startsWith('data:') || avatarUrl.startsWith('http') ? avatarUrl : `data:image/jpeg;base64,${avatarUrl}`}
                          alt="Profile"
                          className="w-full h-full object-cover rounded-full"
                          onError={() => setImgError(true)}
                        />
                      ) : (
                        <span className="font-extrabold text-xs text-cyan-600 dark:text-cyan-400 select-none">
                          {getInitials()}
                        </span>
                      )}
                    </div>
                  </button>

                  {dropdownOpen && (
                    <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-xl py-1 z-50">
                      <div className="px-4 py-2 border-b border-slate-100 dark:border-slate-800">
                        <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">{profile?.name || 'DentNova User'}</p>
                        <p className="text-[10px] text-slate-500 dark:text-slate-400 truncate">{user.email}</p>
                      </div>
                      <Link
                        to="/profile"
                        onClick={() => setDropdownOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800"
                      >
                        <User className="w-4 h-4" />
                        My Profile
                      </Link>
                      <button
                        onClick={handleLogout}
                        className="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-slate-100 dark:hover:bg-slate-800"
                      >
                        <LogOut className="w-4 h-4" />
                        Logout
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/auth"
                  className="px-4 py-2 text-sm font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition"
                >
                  Log In
                </Link>
                <Link
                  to="/auth?mode=register"
                  className="px-4 py-2 text-sm font-semibold text-white bg-cyan-500 hover:bg-cyan-600 rounded-lg shadow-sm transition"
                >
                  Register
                </Link>
              </div>
            )}

            {/* Mobile menu button */}
            {user && (
              <button
                onClick={() => setIsOpen(!isOpen)}
                className="p-2 lg:hidden rounded-lg text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800"
              >
                {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Menu */}
      {isOpen && user && (
        <div className="lg:hidden border-t border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 py-2 px-4 space-y-1">
          {navItems.map((item) => (
            <Link
              key={item.name}
              to={item.path}
              onClick={() => setIsOpen(false)}
              className={`flex items-center gap-3 px-3 py-3 rounded-lg text-base font-semibold transition ${
                isActive(item.path)
                  ? 'bg-cyan-500/10 text-cyan-600 dark:text-cyan-400'
                  : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <item.icon className="w-5 h-5" />
              <span>{item.name}</span>
            </Link>
          ))}
        </div>
      )}
    </nav>
  )
}

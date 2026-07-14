import { useEffect, useRef, useCallback } from 'react'
import { supabase } from '../supabaseClient'

/**
 * Custom Hook — Browser notifications for DentNova reminders.
 *
 * Uses a ref for the reminders array so the interval callback always sees
 * the latest data (avoids React stale-closure problem with setInterval).
 */
export function useRemindersNotifier(user) {
  const remindersRef = useRef([])
  const tickerRef = useRef(null)
  const fetchIntervalRef = useRef(null)

  // ── Fetch enabled reminders from Supabase ─────────────────────────────────
  const fetchReminders = useCallback(async () => {
    const userIdStr = localStorage.getItem('dentnova_user_id')
    if (!userIdStr) return
    const userId = parseInt(userIdStr, 10)
    if (isNaN(userId)) return

    try {
      const { data, error } = await supabase
        .from('reminders')
        .select('*')
        .eq('user_id', userId)
        .eq('enabled', true)

      if (!error && data) {
        remindersRef.current = data
        console.log('WEB_REMINDER_CHECK:', `Fetched ${data.length} active reminders for user ${userId}`)
      } else if (error) {
        console.error('WEB_REMINDER_CHECK: Error fetching reminders:', error)
      }
    } catch (err) {
      console.error('WEB_REMINDER_CHECK: Fetch exception:', err)
    }
  }, [])

  // ── Request Notification Permission on mount (when user exists) ───────────
  useEffect(() => {
    if (!user) return
    if (!('Notification' in window)) {
      console.log('WEB_NOTIFICATION_PERMISSION: Browser does not support notifications')
      return
    }

    const perm = Notification.permission
    console.log('WEB_NOTIFICATION_PERMISSION: Current status =', perm)

    if (perm === 'default') {
      Notification.requestPermission().then((result) => {
        console.log('WEB_NOTIFICATION_PERMISSION: User responded =', result)
      })
    }
  }, [user])

  // ── Show a browser notification and handle click ──────────────────────────
  const showNotification = useCallback((title, body, reminderId) => {
    if (!('Notification' in window) || Notification.permission !== 'granted') {
      console.log('WEB_NOTIFICATION_SHOWN: BLOCKED — permission is', Notification.permission)
      return
    }

    try {
      const n = new Notification(title, {
        body,
        icon: 'data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🦷</text></svg>',
        tag: `dentnova-${reminderId}-${Date.now()}`
      })

      console.log('WEB_NOTIFICATION_SHOWN:', title, '—', body)

      n.onclick = () => {
        console.log('WEB_NOTIFICATION_CLICKED: reminder id =', reminderId)
        window.focus()
        window.location.href = '/reminders'
      }
    } catch (err) {
      console.error('WEB_NOTIFICATION_SHOWN: Error creating notification:', err)
    }
  }, [])

  // ── Main ticker: check every 30 seconds if any reminder matches now ───────
  useEffect(() => {
    if (!user) {
      // Cleanup
      if (tickerRef.current) clearInterval(tickerRef.current)
      if (fetchIntervalRef.current) clearInterval(fetchIntervalRef.current)
      remindersRef.current = []
      return
    }

    // Fetch immediately, then every 60 seconds
    fetchReminders()
    fetchIntervalRef.current = setInterval(fetchReminders, 60 * 1000)

    // Ticker every 30 seconds
    tickerRef.current = setInterval(() => {
      const list = remindersRef.current
      if (!list || list.length === 0) return

      const now = new Date()
      const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
      const currentDay = dayNames[now.getDay()]

      // Build current time string in "hh:mm AM/PM" format
      let h = now.getHours()
      const m = String(now.getMinutes()).padStart(2, '0')
      const ampm = h >= 12 ? 'PM' : 'AM'
      h = h % 12 || 12
      const currentTime = `${String(h).padStart(2, '0')}:${m} ${ampm}`

      // Build current date in "dd MMM yyyy" format for toothbrush replacements
      const dd = String(now.getDate()).padStart(2, '0')
      const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
      const currentDate = `${dd} ${monthNames[now.getMonth()]} ${now.getFullYear()}`

      // De-duplication map from localStorage
      let triggered = {}
      try {
        triggered = JSON.parse(localStorage.getItem('dentnova_triggered_reminders') || '{}')
      } catch {
        triggered = {}
      }

      // Prune entries older than 24 hours
      const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000
      for (const k of Object.keys(triggered)) {
        if (triggered[k] < oneDayAgo) delete triggered[k]
      }

      const todayKey = now.toISOString().split('T')[0] // "2026-06-18"
      let didUpdate = false

      for (const r of list) {
        const isReplacement = r.days === 'ONCE' || r.title?.toLowerCase().includes('replacement')

        if (isReplacement) {
          // For toothbrush replacement: r.time holds the date string
          if (r.time === currentDate) {
            const key = `replace-${r.id}-${todayKey}`
            if (!triggered[key]) {
              console.log('WEB_REMINDER_MATCH_FOUND:', r.title, '| date =', currentDate)
              showNotification(
                '🦷 DentNova Reminder',
                `Time for: ${r.title}\nToday is your scheduled replacement date.`,
                r.id
              )
              triggered[key] = Date.now()
              didUpdate = true
            }
          }
        } else {
          // Regular reminder: check day match
          const daysStr = r.days || ''
          const daysList = daysStr.split(',').map(s => s.trim())
          const dayMatches = daysList.includes(currentDay)

          // Check time match
          const timeMatches = r.time === currentTime

          if (dayMatches && timeMatches) {
            const key = `reminder-${r.id}-${todayKey}-${currentTime}`
            if (!triggered[key]) {
              console.log('WEB_REMINDER_MATCH_FOUND:', r.title, '| day =', currentDay, '| time =', currentTime)
              showNotification(
                '🦷 DentNova Reminder',
                `Time for: ${r.title}`,
                r.id
              )
              triggered[key] = Date.now()
              didUpdate = true
            }
          }
        }
      }

      if (didUpdate) {
        localStorage.setItem('dentnova_triggered_reminders', JSON.stringify(triggered))
      }
    }, 30 * 1000) // every 30 seconds

    return () => {
      if (tickerRef.current) clearInterval(tickerRef.current)
      if (fetchIntervalRef.current) clearInterval(fetchIntervalRef.current)
    }
  }, [user, fetchReminders, showNotification])
}

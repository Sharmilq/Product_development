import { useEffect, useState, useRef } from 'react'
import { supabase } from '../supabaseClient'

/**
 * Custom Hook to handle browser notifications for DentNova reminders.
 * Runs in the background, checks active reminders, and triggers a Notification when due.
 */
export function useRemindersNotifier(user) {
  const [reminders, setReminders] = useState([])
  const timerRef = useRef(null)
  const lastFetchedUserId = useRef(null)

  // 1. Request Notification Permission
  useEffect(() => {
    if (user && 'Notification' in window) {
      if (Notification.permission === 'default') {
        Notification.requestPermission().then((permission) => {
          console.log('[DentNova Notifications] Permission status:', permission)
        })
      }
    }
  }, [user])

  // 2. Fetch Active Reminders from Supabase
  const fetchReminders = async () => {
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
        setReminders(data)
        lastFetchedUserId.current = userId
        console.log(`[DentNova Notifications] Fetched ${data.length} active reminders.`)
      } else if (error) {
        console.error('[DentNova Notifications] Error fetching reminders:', error)
      }
    } catch (err) {
      console.error('[DentNova Notifications] Fetch exception:', err)
    }
  }

  // Fetch immediately when user/profile becomes available
  useEffect(() => {
    if (user) {
      fetchReminders()
      
      // Setup periodic polling every 2 minutes to get fresh list of reminders
      const fetchInterval = setInterval(fetchReminders, 2 * 60 * 1000)
      return () => clearInterval(fetchInterval)
    } else {
      setReminders([])
      lastFetchedUserId.current = null
    }
  }, [user])

  // Helper to trigger standard browser notification
  const triggerNotification = (title, body) => {
    if (!('Notification' in window) || Notification.permission !== 'granted') return

    try {
      new Notification(title, {
        body,
        icon: 'data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🦷</text></svg>',
        tag: `dentnova-reminder-${Date.now()}`
      })
      console.log(`[DentNova Notifications] Displayed: "${title}" - "${body}"`)
    } catch (err) {
      console.error('[DentNova Notifications] Failed to show Notification:', err)
    }
  }

  // 3. Ticker: runs every 15 seconds to check if a reminder is due
  useEffect(() => {
    if (!user) {
      if (timerRef.current) clearInterval(timerRef.current)
      return
    }

    timerRef.current = setInterval(() => {
      if (reminders.length === 0) return

      const now = new Date()
      const daysOfWeekShort = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
      const currentDayShort = daysOfWeekShort[now.getDay()] // e.g., "Thu"
      const isWeekday = now.getDay() >= 1 && now.getDay() <= 5 // Mon-Fri

      // Format current time to "hh:mm AM/PM" (e.g. "08:00 AM")
      let hours = now.getHours()
      const minutes = String(now.getMinutes()).padStart(2, '0')
      const ampm = hours >= 12 ? 'PM' : 'AM'
      hours = hours % 12 || 12
      const formattedHours = String(hours).padStart(2, '0')
      const currentFormattedTime = `${formattedHours}:${minutes} ${ampm}`

      // Format current date to "dd MMM yyyy" (e.g. "18 Jun 2026")
      const day = String(now.getDate()).padStart(2, '0')
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
      const month = months[now.getMonth()]
      const year = now.getFullYear()
      const currentFormattedDate = `${day} ${month} ${year}`

      // Retrieve already triggered reminders list from localStorage to handle page refreshes/tabs
      let triggeredMap = {}
      try {
        triggeredMap = JSON.parse(localStorage.getItem('dentnova_triggered_reminders') || '{}')
      } catch {
        triggeredMap = {}
      }

      // Cleanup triggeredMap entries older than 2 days to save space
      const twoDaysAgo = Date.now() - 2 * 24 * 60 * 60 * 1000
      Object.keys(triggeredMap).forEach((key) => {
        if (triggeredMap[key] < twoDaysAgo) {
          delete triggeredMap[key]
        }
      })

      let updated = false

      reminders.forEach((r) => {
        const isReplacement = r.title?.toLowerCase().includes('replacement') || r.days === 'ONCE'

        if (isReplacement) {
          // Replacement reminder matches the exact date (r.time is the date)
          if (r.time === currentFormattedDate) {
            // Trigger replacement reminders around morning (e.g., at any time they open the app on that day)
            const triggerKey = `replace-${r.id}-${currentFormattedDate}`
            if (!triggeredMap[triggerKey]) {
              triggerNotification(
                '🦷 ' + r.title,
                `Today is your scheduled toothbrush replacement date (${r.time})! Keep your smile clean.`
              )
              triggeredMap[triggerKey] = Date.now()
              updated = true
            }
          }
        } else {
          // Regular brushing/flossing reminders matching day and time
          let dayMatches = false
          const daysStr = r.days || ''

          if (daysStr === 'Daily') {
            dayMatches = true
          } else if (daysStr === 'Weekdays') {
            dayMatches = isWeekday
          } else {
            // Custom days list e.g., "Mon, Tue"
            const customDaysList = daysStr.split(',').map((s) => s.trim())
            dayMatches = customDaysList.includes(currentDayShort)
          }

          if (dayMatches && r.time === currentFormattedTime) {
            // Key format: reminderId-date-time (unique for that specific minute check)
            const todayStr = now.toISOString().split('T')[0]
            const triggerKey = `reminder-${r.id}-${todayStr}-${currentFormattedTime}`

            if (!triggeredMap[triggerKey]) {
              const emoji = r.title?.toLowerCase().includes('floss') ? '🧵' : '🪥'
              triggerNotification(
                `${emoji} ${r.title}`,
                `It's time for your scheduled oral care: ${r.title} (${r.time})!`
              )
              triggeredMap[triggerKey] = Date.now()
              updated = true
            }
          }
        }
      })

      if (updated) {
        localStorage.setItem('dentnova_triggered_reminders', JSON.stringify(triggeredMap))
      }
    }, 15000) // Ticker runs every 15 seconds

    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [reminders, user])
}

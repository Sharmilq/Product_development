import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

if (!supabaseUrl || !supabaseAnonKey) {
  console.error('[DentNova] MISSING env vars: VITE_SUPABASE_URL or VITE_SUPABASE_ANON_KEY not set in .env')
} else {
  console.log('[DentNova] WEB_SUPABASE_URL:', supabaseUrl)
  console.log('[DentNova] WEB_BACKEND_CONNECTED: true')
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  }
})

// Log session state on init
supabase.auth.getSession().then(({ data: { session } }) => {
  if (session) {
    console.log('[DentNova] WEB_SUPABASE_SESSION_FOUND: true — user:', session.user?.email)
  } else {
    console.log('[DentNova] WEB_SUPABASE_SESSION_FOUND: false — no active session')
  }
})

/**
 * DentNova Web – API Service Layer
 * Wraps Supabase table operations and the Render OTP backend.
 * All functions log connection events per the WEB_BACKEND_CONNECTED spec.
 */

import { supabase } from '../lib/supabase'

const OTP_BACKEND_URL = import.meta.env.VITE_OTP_BACKEND_URL || 'https://dentnova-otp-backend.onrender.com'

console.log('[DentNova] WEB_OTP_BACKEND_URL:', OTP_BACKEND_URL)

// ─── Helper ────────────────────────────────────────────────────────────────
function getStoredUserId() {
  return parseInt(localStorage.getItem('dentnova_user_id') || '0', 10)
}

// ─── Auth ───────────────────────────────────────────────────────────────────

export async function apiLogin(email, password) {
  const { data, error } = await supabase.auth.signInWithPassword({ email, password })
  if (error) throw error
  console.log('[DentNova] WEB_BACKEND_CONNECTED: email login success:', email)
  return data
}

export async function apiRegister(email, password, name) {
  const { data, error } = await supabase.auth.signUp({
    email,
    password,
    options: { data: { name } }
  })
  if (error) throw error
  return data
}

export async function apiLogout() {
  const { error } = await supabase.auth.signOut()
  if (error) throw error
  localStorage.clear()
}

// ─── OTP / Forgot Password (Render backend) ─────────────────────────────────

export async function requestPasswordOTP(email) {
  console.log('[DentNova] WEB_OTP_BACKEND_URL: POST /auth/request-password-otp')
  const res = await fetch(`${OTP_BACKEND_URL}/auth/request-password-otp`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  })
  const json = await res.json()
  if (!res.ok || !json.success) throw new Error(json.message || 'Failed to send OTP.')
  return json
}

export async function verifyPasswordOTP(email, otp) {
  console.log('[DentNova] WEB_OTP_BACKEND_URL: POST /auth/verify-password-otp')
  const res = await fetch(`${OTP_BACKEND_URL}/auth/verify-password-otp`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, otp })
  })
  const json = await res.json()
  if (!res.ok || !json.success) throw new Error(json.message || 'Invalid or expired OTP.')
  return json
}

export async function resetPasswordWithOTP(email, otp, newPassword) {
  console.log('[DentNova] WEB_OTP_BACKEND_URL: POST /auth/reset-password-with-otp')
  const res = await fetch(`${OTP_BACKEND_URL}/auth/reset-password-with-otp`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, otp, newPassword })
  })
  const json = await res.json()
  if (!res.ok || !json.success) throw new Error(json.message || 'Failed to reset password.')
  return json
}

// ─── Users Table ────────────────────────────────────────────────────────────

export async function getProfile(userId) {
  const { data, error } = await supabase
    .from('users')
    .select('*')
    .eq('user_id', userId)
    .single()
  if (error && error.code !== 'PGRST116') throw error
  return data
}

export async function upsertProfile(profileData) {
  const { data, error } = await supabase
    .from('users')
    .upsert(profileData, { onConflict: 'user_id' })
    .select()
    .single()
  if (error) throw error
  return data
}

export async function updateProfile(userId, updates) {
  const { data, error } = await supabase
    .from('users')
    .update(updates)
    .eq('user_id', userId)
    .select()
    .single()
  if (error) throw error
  return data
}

// ─── Assessments ────────────────────────────────────────────────────────────

export async function saveAssessment(payload) {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('assessments')
    .insert({ ...payload, user_id: userId })
    .select()
    .single()
  if (error) throw error
  console.log('[DentNova] WEB_BACKEND_CONNECTED: assessment saved')
  return data
}

export async function getLatestAssessment() {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('assessments')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false })
    .limit(1)
  if (error) throw error
  return data?.[0] || null
}

// ─── Tooth Scans ────────────────────────────────────────────────────────────

export async function saveToothScan(payload) {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('tooth_scans')
    .insert({ ...payload, user_id: userId })
    .select()
    .single()
  if (error) throw error
  console.log('[DentNova] WEB_BACKEND_CONNECTED: tooth scan saved')
  return data
}

export async function getToothScanHistory() {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('tooth_scans')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false })
  if (error) throw error
  return data || []
}

// ─── Reminders ──────────────────────────────────────────────────────────────

export async function getReminders() {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('reminders')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false })
  if (error) throw error
  return data || []
}

export async function saveReminder(payload) {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('reminders')
    .insert({ ...payload, user_id: userId })
    .select()
    .single()
  if (error) throw error
  return data
}

export async function deleteReminder(id) {
  const userId = getStoredUserId()
  const { error } = await supabase
    .from('reminders')
    .delete()
    .eq('id', id)
    .eq('user_id', userId)
  if (error) throw error
}

// ─── Visits ─────────────────────────────────────────────────────────────────

export async function getVisits() {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('visits')
    .select('*')
    .eq('user_id', userId)
    .order('visit_date', { ascending: true })
  if (error) throw error
  return data || []
}

export async function saveVisit(payload) {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('visits')
    .insert({ ...payload, user_id: userId })
    .select()
    .single()
  if (error) throw error
  return data
}

export async function deleteVisit(id) {
  const userId = getStoredUserId()
  const { error } = await supabase
    .from('visits')
    .delete()
    .eq('id', id)
    .eq('user_id', userId)
  if (error) throw error
}

// ─── Feedback ───────────────────────────────────────────────────────────────

export async function saveFeedback(payload) {
  const userId = getStoredUserId()
  const { data, error } = await supabase
    .from('feedback')
    .insert({ ...payload, user_id: userId })
    .select()
    .single()
  if (error) throw error
  console.log('[DentNova] WEB_BACKEND_CONNECTED: feedback saved')
  return data
}

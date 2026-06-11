const express = require('express');
const cors = require('cors');
const nodemailer = require('nodemailer');
const { createClient } = require('@supabase/supabase-js');
const admin = require('firebase-admin');
const crypto = require('crypto');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

// Request/Response logging middleware
app.use((req, res, next) => {
  const originalJson = res.json;
  const originalSend = res.send;
  console.log(`[HTTP_REQUEST] URL: ${req.method} ${req.originalUrl}`);
  if (req.body) {
    const loggedBody = { ...req.body };
    if (loggedBody.newPassword) loggedBody.newPassword = '[HIDDEN SECRET]';
    if (loggedBody.password) loggedBody.password = '[HIDDEN SECRET]';
    console.log(`[HTTP_REQUEST] Body:`, JSON.stringify(loggedBody));
  }
  
  res.json = function (body) {
    console.log(`[HTTP_RESPONSE] URL: ${req.method} ${req.originalUrl} - Code: ${res.statusCode} - Body:`, JSON.stringify(body));
    return originalJson.call(this, body);
  };
  
  res.send = function (body) {
    console.log(`[HTTP_RESPONSE] URL: ${req.method} ${req.originalUrl} - Code: ${res.statusCode} - Body:`, typeof body === 'object' ? JSON.stringify(body) : body);
    return originalSend.call(this, body);
  };
  
  next();
});

const PORT = process.env.PORT || 5000;

// ── SUPABASE CONFIGURATION ────────────────────────────────────────────────
// The backend requires the service_role key to override Row Level Security (RLS)
// and manage user passwords via the admin auth client.
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!supabaseUrl || !supabaseServiceKey) {
  console.error('ERROR: SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set in env.');
  process.exit(1);
}
const supabase = createClient(supabaseUrl, supabaseServiceKey);

// ── FIREBASE ADMIN CONFIGURATION ──────────────────────────────────────────
// Firebase Admin credentials can be loaded either from a file or directly
// from an environment variable containing the service account JSON.
const firebaseServiceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;

try {
  if (firebaseServiceAccountJson) {
    const serviceAccount = JSON.parse(firebaseServiceAccountJson);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    console.log('Firebase Admin SDK initialized from environment variable.');
  } else {
    // Attempt to load from service account file
    const serviceAccount = require('./firebase-service-account.json');
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    console.log('Firebase Admin SDK initialized from firebase-service-account.json file.');
  }
} catch (error) {
  console.warn('WARNING: Firebase Admin SDK could not be initialized:', error.message);
  console.warn('Firebase password resets will be skipped.');
}

// ── NODEMAILER EMAIL CONFIGURATION ────────────────────────────────────────
// Setup SMTP transporter based on environment variables
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.SMTP_PORT || '587'),
  secure: process.env.SMTP_SECURE === 'true', // true for 465, false for other ports
  auth: {
    user: process.env.SMTP_USER, // e.g. your email
    pass: process.env.SMTP_PASS  // e.g. email App Password
  }
});

// Verify email setup
transporter.verify((error, success) => {
  if (error) {
    console.warn('WARNING: SMTP email transporter configuration failed:', error.message);
  } else {
    console.log('SMTP transporter is ready to send emails.');
  }
});

// ── ENDPOINTS ─────────────────────────────────────────────────────────────

/**
 * 1. Send OTP Endpoint
 * Generates a secure 6-digit OTP, stores it in Supabase 'otps' table, and sends it via email.
 */
app.post('/api/otp/send', async (req, res) => {
  const { email } = req.body;

  if (!email) {
    return res.status(400).json({ success: false, message: 'Email is required' });
  }

  try {
    // Generate secure 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString(); // 10 minutes expiry

    // Save/Upsert OTP details in the Supabase 'otps' table
    const { error: dbError } = await supabase
      .from('otps')
      .upsert({ email, otp, expires_at: expiresAt }, { onConflict: 'email' });

    if (dbError) {
      console.error('Database error storing OTP:', dbError);
      return res.status(500).json({ success: false, message: 'Failed to generate verification code in database' });
    }

    // Email content
    const mailOptions = {
      from: `"DentNova" <${process.env.SMTP_USER}>`,
      to: email,
      subject: 'DentNova Verification Code',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E0E8EF; border-radius: 12dp;">
          <h2 style="color: #00BCD4; text-align: center;">DentNova Verification Code</h2>
          <p>Hello,</p>
          <p>We received a request to reset your password. Use the following secure 6-digit OTP to complete your verification. This code is valid for <b>10 minutes</b>:</p>
          <div style="text-align: center; margin: 30px 0;">
            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #1A2332; background-color: #F5F9FA; padding: 10px 24px; border-radius: 8px; border: 1px dashed #00BCD4;">${otp}</span>
          </div>
          <p>If you did not request a password reset, please ignore this email.</p>
          <p>Best regards,<br>The DentNova Team</p>
        </div>
      `
    };

    // Send email
    await transporter.sendMail(mailOptions);
    console.log(`OTP (${otp}) successfully sent to ${email}`);

    res.status(200).json({ success: true, message: 'Verification code sent successfully' });
  } catch (error) {
    console.error('Error sending OTP:', error);
    res.status(500).json({ success: false, message: 'Failed to send verification code. Try again later.' });
  }
});

/**
 * 2. Verify OTP Endpoint
 * Checks if the OTP entered by the user is correct and not expired.
 */
app.post('/api/otp/verify', async (req, res) => {
  const { email, otp } = req.body;

  if (!email || !otp) {
    return res.status(400).json({ success: false, message: 'Email and OTP code are required' });
  }

  try {
    const { data: records, error } = await supabase
      .from('otps')
      .select('*')
      .eq('email', email);

    if (error || !records || records.length === 0) {
      return res.status(400).json({ success: false, message: 'No verification code requested for this email' });
    }

    const record = records[0];

    // Expiry check
    if (new Date() > new Date(record.expires_at)) {
      // Clean up expired OTP
      await supabase.from('otps').delete().eq('email', email);
      return res.status(400).json({ success: false, message: 'Verification code has expired. Please request a new one.' });
    }

    // Match check
    if (record.otp !== otp) {
      return res.status(400).json({ success: false, message: 'Invalid verification code' });
    }

    res.status(200).json({ success: true, message: 'Verification code verified successfully' });
  } catch (error) {
    console.error('Error verifying OTP:', error);
    res.status(500).json({ success: false, message: 'Server error during verification.' });
  }
});

/**
 * 3. Reset Password Endpoint
 * Validates the OTP one final time, updates the password securely in Supabase / Firebase, and deletes the OTP.
 */
app.post('/api/otp/reset-password', async (req, res) => {
  const { email, otp, newPassword } = req.body;

  if (!email || !otp || !newPassword) {
    return res.status(400).json({ success: false, message: 'Missing required parameters' });
  }

  if (newPassword.length < 6) {
    return res.status(400).json({ success: false, message: 'Password must be at least 6 characters' });
  }

  try {
    // 1. Verify OTP first
    const { data: records, error } = await supabase
      .from('otps')
      .select('*')
      .eq('email', email);

    if (error || !records || records.length === 0) {
      return res.status(400).json({ success: false, message: 'OTP verification failed' });
    }

    const record = records[0];

    if (new Date() > new Date(record.expires_at)) {
      await supabase.from('otps').delete().eq('email', email);
      return res.status(400).json({ success: false, message: 'OTP expired. Request a new one.' });
    }

    if (record.otp !== otp) {
      return res.status(400).json({ success: false, message: 'Invalid OTP' });
    }

    // 2. Perform updates in authentication services
    let supabaseSuccess = false;
    let firebaseSuccess = false;

    // A. Update in Supabase
    try {
      const { data: listData, error: listError } = await supabase.auth.admin.listUsers();
      if (listError) throw listError;

      const supabaseUser = listData.users.find(u => u.email === email);
      if (supabaseUser) {
        const { error: updateError } = await supabase.auth.admin.updateUserById(
          supabaseUser.id,
          { password: newPassword }
        );
        if (updateError) throw updateError;
        supabaseSuccess = true;
        console.log(`Supabase password updated for user: ${email}`);
      }
    } catch (sbError) {
      console.warn('Supabase password update failed or skipped:', sbError.message);
    }

    // B. Update in Firebase (if initialised)
    try {
      if (admin.apps.length > 0) {
        const firebaseUser = await admin.auth().getUserByEmail(email);
        if (firebaseUser) {
          await admin.auth().updateUser(firebaseUser.uid, { password: newPassword });
          firebaseSuccess = true;
          console.log(`Firebase password updated for user: ${email}`);
        }
      }
    } catch (fbError) {
      console.warn('Firebase password update failed or skipped:', fbError.message);
    }

    // If both failed to update (user not found in either system)
    if (!supabaseSuccess && !firebaseSuccess) {
      return res.status(404).json({ success: false, message: 'User account not found' });
    }

    // 3. Success, clean up OTP record
    await supabase.from('otps').delete().eq('email', email);

    res.status(200).json({ success: true, message: 'Password updated successfully' });
  } catch (error) {
    console.error('Error resetting password:', error);
    res.status(500).json({ success: false, message: 'Failed to update password.' });
  }
});

// ── NEW: Custom Password-Reset OTP Flow ─────────────────────────────────────
// These endpoints replace the Supabase /auth/v1/recover link flow.
// The Android app ONLY calls these — it never touches service_role APIs.

// Simple in-memory rate limiter: max 3 OTP requests per email per 15 minutes.
const otpRateLimit = new Map(); // email -> { count, windowStart }
function isRateLimited(email) {
  const now = Date.now();
  const WINDOW_MS = 15 * 60 * 1000; // 15 minutes
  const MAX_REQUESTS = 3;
  const entry = otpRateLimit.get(email);
  if (!entry || now - entry.windowStart > WINDOW_MS) {
    otpRateLimit.set(email, { count: 1, windowStart: now });
    return false;
  }
  if (entry.count >= MAX_REQUESTS) return true;
  entry.count++;
  return false;
}

function sha256(text) {
  return crypto.createHash('sha256').update(text).digest('hex');
}

/**
 * POST /auth/request-password-otp
 * Body: { "email": "user@example.com" }
 * Generates a 6-digit OTP, hashes it, stores it with 5-min expiry, sends email.
 */
app.post('/auth/request-password-otp', async (req, res) => {
  const { email } = req.body;
  if (!email) {
    return res.status(400).json({ success: false, message: 'Email is required' });
  }

  // Rate limit check
  if (isRateLimited(email)) {
    return res.status(429).json({ success: false, message: 'Too many requests. Please wait 15 minutes before requesting another OTP.' });
  }

  try {
    // Check user exists in users table
    const { data: userRows } = await supabase
      .from('users')
      .select('email')
      .eq('email', email)
      .limit(1);

    const userExists = userRows && userRows.length > 0;
    console.log(`[OTP] Registered email check result for ${email}: ${userExists ? 'EXISTS' : 'NOT_EXISTS'}`);

    if (!userExists) {
      console.log(`[OTP] Password reset requested for unregistered email: ${email} — no OTP generated.`);
      return res.status(404).json({
        success: false,
        message: "Email is not registered."
      });
    }

    // Generate secure 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const otpHash = sha256(otp);
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString(); // 5 minutes

    // Upsert into password_reset_otps table
    const { error: dbError } = await supabase
      .from('password_reset_otps')
      .upsert(
        { email, otp_hash: otpHash, expires_at: expiresAt, used: false },
        { onConflict: 'email' }
      );

    if (dbError) {
      console.error('[OTP] DB error storing OTP:', dbError);
      return res.status(500).json({ success: false, message: 'Server error. Please try again.' });
    }

    // Send email
    const mailOptions = {
      from: `"DentNova" <${process.env.SMTP_FROM || process.env.SMTP_USER}>`,
      to: email,
      subject: 'DentNova Password Reset OTP',
      text: `Your DentNova password reset OTP is:\n\n${otp}\n\nThis OTP expires in 5 minutes.`,
      html: `
        <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E0E8EF;border-radius:12px;">
          <p>Your DentNova password reset OTP is:</p>
          <h2 style="font-size:32px;font-weight:bold;color:#1A2332;letter-spacing:5px;">${otp}</h2>
          <p>This OTP expires in 5 minutes.</p>
        </div>
      `
    };

    await transporter.sendMail(mailOptions);
    console.log(`[OTP] OTP sent successfully to ${email}`);

    return res.status(200).json({ success: true, message: 'OTP sent successfully.' });
  } catch (err) {
    console.error(`[OTP] OTP failed to send for ${email}:`, err);
    return res.status(500).json({ success: false, message: 'Failed to send OTP. Please try again.' });
  }
});

/**
 * POST /auth/verify-password-otp
 * Body: { "email": "user@example.com", "otp": "123456" }
 * Checks OTP hash match and expiry. Does NOT invalidate OTP yet.
 */
app.post('/auth/verify-password-otp', async (req, res) => {
  const { email, otp } = req.body;
  if (!email || !otp) {
    return res.status(400).json({ success: false, message: 'Email and OTP are required' });
  }

  try {
    const { data: records, error } = await supabase
      .from('password_reset_otps')
      .select('*')
      .eq('email', email)
      .limit(1);

    if (error || !records || records.length === 0) {
      return res.status(400).json({ success: false, message: 'No OTP request found for this email. Please request a new code.' });
    }

    const record = records[0];

    if (record.used) {
      return res.status(400).json({ success: false, message: 'This OTP has already been used. Please request a new one.' });
    }

    if (new Date() > new Date(record.expires_at)) {
      await supabase.from('password_reset_otps').delete().eq('email', email);
      return res.status(400).json({ success: false, message: 'OTP has expired. Please request a new code.' });
    }

    const otpHash = sha256(otp);
    if (record.otp_hash !== otpHash) {
      return res.status(400).json({ success: false, message: 'Invalid OTP code. Please check and try again.' });
    }

    console.log(`[OTP] OTP verified for ${email}`);
    res.status(200).json({ success: true, message: 'OTP verified successfully' });
  } catch (err) {
    console.error('[OTP] Error in verify-password-otp:', err);
    res.status(500).json({ success: false, message: 'Server error during verification.' });
  }
});

/**
 * POST /auth/reset-password-with-otp
 * Body: { "email": "user@example.com", "otp": "123456", "newPassword": "abc123" }
 * Re-verifies OTP, updates password in Supabase auth, marks OTP as used.
 */
app.post('/auth/reset-password-with-otp', async (req, res) => {
  const { email, otp, newPassword } = req.body;
  if (!email || !otp || !newPassword) {
    return res.status(400).json({ success: false, message: 'email, otp and newPassword are required' });
  }
  
  // Password strength rule validation
  const hasUppercase = /[A-Z]/.test(newPassword);
  const hasLowercase = /[a-z]/.test(newPassword);
  const hasNumber = /[0-9]/.test(newPassword);
  const hasSpecial = /[^A-Za-z0-9]/.test(newPassword);
  const isStrong = newPassword.length >= 8 && hasUppercase && hasLowercase && hasNumber && hasSpecial;
  
  if (!isStrong) {
    return res.status(400).json({
      success: false,
      message: "Password must be at least 8 characters and include uppercase, lowercase, number, and special character."
    });
  }

  try {
    // Re-verify OTP
    const { data: records, error } = await supabase
      .from('password_reset_otps')
      .select('*')
      .eq('email', email)
      .limit(1);

    if (error || !records || records.length === 0) {
      return res.status(400).json({ success: false, message: 'OTP not found. Please request a new code.' });
    }

    const record = records[0];

    if (record.used) {
      return res.status(400).json({ success: false, message: 'OTP already used. Please request a new one.' });
    }

    if (new Date() > new Date(record.expires_at)) {
      await supabase.from('password_reset_otps').delete().eq('email', email);
      return res.status(400).json({ success: false, message: 'OTP expired. Please request a new code.' });
    }

    const otpHash = sha256(otp);
    if (record.otp_hash !== otpHash) {
      return res.status(400).json({ success: false, message: 'Invalid OTP.' });
    }

    // Update Supabase auth password using service role (admin)
    const { data: listData, error: listError } = await supabase.auth.admin.listUsers();
    if (listError) {
      console.error(`[OTP] Password reset failed listing users for ${email}:`, listError.message);
      throw listError;
    }

    const supabaseUser = listData.users.find(u => u.email === email);
    if (!supabaseUser) {
      console.log(`[OTP] Password reset failed for ${email}: User account not found in auth system.`);
      return res.status(404).json({ success: false, message: 'User account not found in auth system.' });
    }

    const { error: updateError } = await supabase.auth.admin.updateUserById(
      supabaseUser.id,
      { password: newPassword }
    );
    if (updateError) {
      console.error(`[OTP] Password reset failed updating user password for ${email}:`, updateError.message);
      throw updateError;
    }

    // Mark OTP as used
    await supabase
      .from('password_reset_otps')
      .update({ used: true })
      .eq('email', email);

    // Also update Firebase if initialized
    try {
      if (admin.apps.length > 0) {
        const fbUser = await admin.auth().getUserByEmail(email);
        if (fbUser) {
          await admin.auth().updateUser(fbUser.uid, { password: newPassword });
          console.log(`[OTP] Firebase password updated for ${email}`);
        }
      }
    } catch (fbErr) {
      console.warn('[OTP] Firebase update skipped:', fbErr.message);
    }

    console.log(`[OTP] Password reset result for ${email}: SUCCESS (Supabase Auth password updated)`);
    res.status(200).json({ success: true, message: 'Password updated successfully. Please sign in.' });
  } catch (err) {
    console.error(`[OTP] Password reset result for ${email}: FAILED`, err);
    res.status(500).json({ success: false, message: 'Failed to update password. Please try again.' });
  }
});

// Start Server
app.listen(PORT, () => {
  console.log(`DentNova backend server is running on port ${PORT}`);
});

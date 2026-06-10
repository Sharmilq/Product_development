const express = require('express');
const cors = require('cors');
const nodemailer = require('nodemailer');
const { createClient } = require('@supabase/supabase-js');
const admin = require('firebase-admin');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

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

// Start Server
app.listen(PORT, () => {
  console.log(`DentNova backend server is running on port ${PORT}`);
});

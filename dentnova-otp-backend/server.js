const express = require('express');
const cors = require('cors');
const nodemailer = require('nodemailer');
const { createClient } = require('@supabase/supabase-js');
const crypto = require('crypto');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 5000;

// Initialize Supabase client with Service Role Key (Admin access)
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!supabaseUrl || !supabaseServiceKey) {
  console.error('ERROR: SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set.');
  process.exit(1);
}
const supabase = createClient(supabaseUrl, supabaseServiceKey);

// Setup Nodemailer SMTP
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.SMTP_PORT || '587'),
  secure: process.env.SMTP_SECURE === 'true',
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS
  }
});

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

function sha256(text) {
  return crypto.createHash('sha256').update(text).digest('hex');
}

// 1. GET / Health Check
app.get('/', (req, res) => {
  res.status(200).json({
    success: true,
    message: "DentNova OTP backend is running"
  });
});

// 2. POST /auth/request-password-otp
app.post('/auth/request-password-otp', async (req, res) => {
  const { email } = req.body;
  if (!email) {
    return res.status(400).json({ success: false, message: 'Email is required' });
  }

  try {
    // Check if user exists in Supabase users table
    const { data: userRows, error: userError } = await supabase
      .from('users')
      .select('email')
      .eq('email', email)
      .limit(1);

    if (userError) {
      console.error('[OTP] Supabase error checking user existence:', userError);
      return res.status(500).json({ success: false, message: 'Server database error' });
    }

    const userExists = userRows && userRows.length > 0;
    console.log(`[OTP] Registered email check result for ${email}: ${userExists ? 'PASSED' : 'FAILED'}`);

    if (!userExists) {
      return res.status(404).json({
        success: false,
        message: "Email is not registered."
      });
    }

    // Generate secure 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const otpHash = sha256(otp);
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString(); // 5 minutes

    // Store in password_reset_otps table
    const { error: dbError } = await supabase
      .from('password_reset_otps')
      .upsert(
        { email, otp_hash: otpHash, expires_at: expiresAt, used: false },
        { onConflict: 'email' }
      );

    if (dbError) {
      console.error('[OTP] DB error storing OTP:', dbError);
      return res.status(500).json({ success: false, message: 'Server database error' });
    }

    // Send email with Nodemailer
    const mailOptions = {
      from: `"DentNova" <${process.env.SMTP_FROM || process.env.SMTP_USER}>`,
      to: email,
      subject: 'DentNova Password Reset OTP',
      text: `Your OTP is:\n${otp}\n\nValid for 5 minutes.`,
      html: `
        <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E0E8EF;border-radius:12px;">
          <p>Your OTP is:</p>
          <h2 style="font-size:32px;font-weight:bold;color:#1A2332;letter-spacing:5px;">${otp}</h2>
          <p>Valid for 5 minutes.</p>
        </div>
      `
    };

    await transporter.sendMail(mailOptions);
    console.log(`[OTP] OTP sent successfully to ${email}`);

    return res.status(200).json({ success: true, message: 'OTP sent successfully.' });
  } catch (err) {
    console.error(`[OTP] OTP failed to send for ${email}:`, err);
    return res.status(500).json({ success: false, message: 'Failed to send OTP.' });
  }
});

// 3. POST /auth/verify-password-otp
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
      return res.status(400).json({ success: false, message: 'No OTP request found for this email.' });
    }

    const record = records[0];

    if (record.used) {
      return res.status(400).json({ success: false, message: 'This OTP has already been used.' });
    }

    if (new Date() > new Date(record.expires_at)) {
      return res.status(400).json({ success: false, message: 'OTP has expired.' });
    }

    const otpHash = sha256(otp);
    if (record.otp_hash !== otpHash) {
      return res.status(400).json({ success: false, message: 'Invalid OTP.' });
    }

    return res.status(200).json({ success: true, message: 'OTP verified successfully' });
  } catch (err) {
    console.error('[OTP] Error verifying OTP:', err);
    return res.status(500).json({ success: false, message: 'Server error during verification.' });
  }
});

// 4. POST /auth/reset-password-with-otp
app.post('/auth/reset-password-with-otp', async (req, res) => {
  const { email, otp, newPassword } = req.body;
  if (!email || !otp || !newPassword) {
    return res.status(400).json({ success: false, message: 'email, otp and newPassword are required' });
  }

  // Password requirements check
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
    // Verify OTP first
    const { data: records, error } = await supabase
      .from('password_reset_otps')
      .select('*')
      .eq('email', email)
      .limit(1);

    if (error || !records || records.length === 0) {
      return res.status(400).json({ success: false, message: 'OTP request not found.' });
    }

    const record = records[0];

    if (record.used) {
      return res.status(400).json({ success: false, message: 'OTP already used.' });
    }

    if (new Date() > new Date(record.expires_at)) {
      return res.status(400).json({ success: false, message: 'OTP expired.' });
    }

    const otpHash = sha256(otp);
    if (record.otp_hash !== otpHash) {
      return res.status(400).json({ success: false, message: 'Invalid OTP.' });
    }

    // Retrieve user list via Supabase Admin Auth
    const { data: listData, error: listError } = await supabase.auth.admin.listUsers();
    if (listError) {
      console.error('[OTP] Error fetching auth user list:', listError);
      return res.status(500).json({ success: false, message: 'Auth list error' });
    }

    const authUser = listData.users.find(u => u.email === email);
    if (!authUser) {
      return res.status(404).json({ success: false, message: 'User account not found in auth system.' });
    }

    // Update password in Supabase Auth
    const { error: updateError } = await supabase.auth.admin.updateUserById(
      authUser.id,
      { password: newPassword }
    );

    if (updateError) {
      console.error('[OTP] Error updating password:', updateError);
      return res.status(500).json({ success: false, message: 'Failed to update password.' });
    }

    // Mark OTP as used
    await supabase
      .from('password_reset_otps')
      .update({ used: true })
      .eq('email', email);

    console.log(`[OTP] Password successfully reset and OTP marked as used for ${email}`);
    return res.status(200).json({ success: true, message: 'Password updated successfully' });
  } catch (err) {
    console.error('[OTP] Error in reset-password-with-otp:', err);
    return res.status(500).json({ success: false, message: 'Failed to update password.' });
  }
});

// Start listening
app.listen(PORT, () => {
  console.log(`DentNova OTP backend server is running on port ${PORT}`);
});

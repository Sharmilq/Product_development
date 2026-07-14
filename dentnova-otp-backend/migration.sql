-- SQL Migration Script to create the password_reset_otps table
-- Execute this script in your Supabase SQL Editor

CREATE TABLE IF NOT EXISTS public.password_reset_otps (
    email TEXT PRIMARY KEY,
    otp_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS (Row Level Security)
ALTER TABLE public.password_reset_otps ENABLE ROW LEVEL SECURITY;

-- Create policy to allow the service role key to manage all operations (default bypasses RLS)
-- No public read/write access is exposed directly. Only the Node.js backend using the service_role key can read/write to this table.

-- Migration to support daily active streaks for users
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS streak_count INTEGER DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS last_active_date DATE;

-- ====================================================
-- DentNova: Fix RLS policies for users table
-- Run in: Supabase Dashboard → SQL Editor
-- Project: kxuwskwwmrpoilrxngha
-- ====================================================

-- Step 1: Check current policies (optional diagnostic)
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
FROM pg_policies
WHERE tablename = 'users'
ORDER BY policyname;

-- Step 2: Drop old/conflicting policies (safe: IF EXISTS)
DROP POLICY IF EXISTS "Users can update own profile"           ON public.users;
DROP POLICY IF EXISTS "Users can view own profile"             ON public.users;
DROP POLICY IF EXISTS "Enable update for users based on email" ON public.users;
DROP POLICY IF EXISTS "Enable read access for users"           ON public.users;
DROP POLICY IF EXISTS "Users can update their own row"         ON public.users;
DROP POLICY IF EXISTS "Users can select their own row"         ON public.users;

-- Step 3: Ensure RLS is ON
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- Step 4: SELECT policy — authenticated users can read their own row
CREATE POLICY "Users can view own profile"
ON public.users
FOR SELECT
TO authenticated
USING (
  email = auth.email()
);

-- Step 5: UPDATE policy — authenticated users can update their own row
CREATE POLICY "Users can update own profile"
ON public.users
FOR UPDATE
TO authenticated
USING (
  email = auth.email()
)
WITH CHECK (
  email = auth.email()
);

-- Step 6: INSERT policy — allow app to create a row on first login
CREATE POLICY "Users can insert own profile"
ON public.users
FOR INSERT
TO authenticated
WITH CHECK (
  email = auth.email()
);

-- Step 7: Verify — should show 3 policies
SELECT schemaname, tablename, policyname, cmd, qual
FROM pg_policies
WHERE tablename = 'users'
ORDER BY policyname;

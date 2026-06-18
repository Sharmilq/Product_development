# DentNova Web Application

This is the responsive web application for DentNova, built with React, Vite, and Tailwind CSS. It connects to the same Supabase database and custom Node.js OTP backend used by the mobile application.

## Getting Started

1. Navigate to the `dentnova-web` folder.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Copy environment variables:
   ```bash
   cp .env.example .env
   ```
   Provide values for `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, and `VITE_OTP_BACKEND_URL`.
4. Start development server:
   ```bash
   npm run dev
   ```
5. Compile production bundle:
   ```bash
   npm run build
   ```

---

## Enabling Google Sign-In

Google Sign-In requires manual setup in both Supabase and Google Cloud Console.

### Step 1 — Google Cloud Console
1. Go to [Google Cloud Console](https://console.cloud.google.com) → **APIs & Services** → **Credentials**.
2. Create an **OAuth 2.0 Client ID** (Web application type).
3. Under **Authorized redirect URIs**, add the Supabase callback URL:
   ```
   https://kxuwskwwmrpoilrxngha.supabase.co/auth/v1/callback
   ```
4. Copy the **Client ID** and **Client Secret**.

### Step 2 — Supabase Dashboard
1. Go to your [Supabase Dashboard](https://supabase.com/dashboard/project/kxuwskwwmrpoilrxngha).
2. Navigate to **Authentication** → **Providers** → **Google**.
3. Toggle **Enable** to ON.
4. Paste the **Google Client ID** and **Client Secret** from Step 1.
5. Save changes.

### Step 3 — Supabase URL Configuration
1. In Supabase Dashboard → **Authentication** → **URL Configuration**.
2. Under **Site URL**, add:
   ```
   http://localhost:5173
   ```
3. Under **Redirect URLs**, add:
   ```
   http://localhost:5173/auth/callback
   ```
4. When deploying to production, also add your production URL.

### How It Works
- Clicking "Sign in with Google" calls `supabase.auth.signInWithOAuth({ provider: 'google', redirectTo: '/auth/callback' })`
- After Google authentication, Supabase redirects the user to `/auth/callback`
- The `AuthCallback` page exchanges the session code and redirects to `/dashboard`

> **Note:** Until Google provider is enabled in Supabase, clicking "Sign in with Google" shows the friendly message:
> *"Google Sign-In is not enabled yet. Please use email login."*
> Email/password login always works independently.


---

## Tech Stack

- **React + Vite** — Fast development and production build
- **Tailwind CSS** — Utility-first styling
- **Supabase JS** — Auth, database, real-time
- **React Router v6** — Client-side routing
- **Lucide React** — Icon library

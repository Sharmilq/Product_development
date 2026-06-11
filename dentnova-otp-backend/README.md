# DentNova OTP Backend

Node.js OTP service for handling forgot password requests securely.

## Features
- Health check route (`GET /`)
- Request password reset OTP (`POST /auth/request-password-otp`)
- Verify OTP code (`POST /auth/verify-password-otp`)
- Complete password reset (`POST /auth/reset-password-with-otp`)

## Setup
1. Run `npm install` to install dependencies.
2. Copy `.env.example` to `.env` and fill in the parameters.
3. Start the server using `npm start`.

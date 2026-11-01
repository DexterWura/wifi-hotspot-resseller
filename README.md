# WiFi Hotspot Reseller

Android client for Cloud ERP Wi-Fi Hotspot staff features.

## Setup

1. Copy `local.properties.example` to `local.properties`.
2. Set `sdk.dir` to your Android SDK path.
3. Set `API_BASE_URL` to your Cloud ERP origin (no trailing slash), for example `https://app.example.com` or `http://10.0.2.2` for the emulator talking to XAMPP on the host.
4. Open the project in Android Studio and sync Gradle.
5. Run the `app` configuration.

## Login flow

1. Sign in with a verified ERP user email/password (`POST /api/v1/auth/login`).
2. Pick a workspace (`GET /api/v1/companies`).
3. The app loads `GET /api/v1/wifi/bootstrap` and shows only screens allowed by `wifi.*` permissions.

## Brand

Primary green `#0F6E56` with white surfaces, matching Cloud ERP.

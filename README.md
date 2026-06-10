# DentNova Android — Java + XML

Converted from Flutter (dentnova_new) to Android Studio Java + XML UI.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/dentnova/app/
│   ├── activities/
│   │   ├── SplashActivity.java          ← splash_screen.dart
│   │   ├── OnboardingActivity.java      ← onboarding_screen.dart
│   │   ├── AuthActivity.java            ← auth_screen.dart
│   │   ├── ProfileSetupActivity.java    ← profile_setup_screen.dart
│   │   ├── HomeActivity.java            ← home_screen.dart
│   │   ├── AssessmentActivity.java      ← assessment_screen.dart
│   │   ├── AssessmentResultActivity.java← assessment_result_screen.dart
│   │   ├── ProfileActivity.java         ← profile_screen.dart
│   │   ├── RemindersActivity.java       ← reminders_screen.dart
│   │   ├── VisitReminderActivity.java   ← visit_reminder_screen.dart
│   │   ├── ToothScanActivity.java       ← tooth_scan_screen.dart
│   │   ├── EducationActivity.java       ← education_screen.dart
│   │   ├── HowItWorksActivity.java      ← how_it_works_screen.dart
│   │   └── SendFeedbackActivity.java    ← send_feedback_screen.dart
│   ├── services/
│   │   └── ApiService.java              ← api_service.dart (OkHttp + Gson)
│   └── utils/
│       └── SessionManager.java          ← SharedPreferences session logic
├── res/
│   ├── layout/      ← All XML layouts (1:1 with Flutter screens)
│   ├── values/      ← colors.xml, strings.xml, themes.xml, dimens.xml
│   ├── drawable/    ← Shape backgrounds + icon placeholders
│   ├── font/        ← Inter + Poppins (add .ttf files)
│   └── menu/        ← menu_bottom_nav.xml
```

## Setup Steps

1. Open in Android Studio (Iguana or later)
2. Add font TTF files to res/font/:
   - inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf, inter_bold.ttf, inter_extrabold.ttf
   - poppins_regular.ttf, poppins_extrabold.ttf
   (Download free from fonts.google.com)
3. Replace icon placeholder XMLs in res/drawable/ with real Material icons
   (use Android Studio → New → Vector Asset)
4. Update ApiService.BASE_URL with your server IP
5. Add tooth_logo and tooth_brush_hero images to res/drawable/
6. Sync Gradle → Run on device

## Flutter → Android mapping

| Flutter concept          | Android Java equivalent                  |
|--------------------------|------------------------------------------|
| StatefulWidget           | Activity / Fragment                      |
| setState()               | runOnUiThread() / notifyDataSetChanged() |
| Navigator.push()         | startActivity(new Intent(...))           |
| SharedPreferences        | SessionManager (SharedPreferences)       |
| http package             | OkHttp3                                  |
| jsonDecode/jsonEncode     | Gson                                     |
| image_picker             | ActivityResultLauncher<GetContent>       |
| flutter_local_notifications | WorkManager + NotificationCompat      |
| AnimationController      | ObjectAnimator / ValueAnimator           |
| PageView / ViewPager2    | ViewPager2 + RecyclerView.Adapter        |
| BottomNavigationBar      | BottomNavigationView                     |
| CircularProgressIndicator| CircularProgressIndicator (Material)     |
| Wrap + GestureDetector   | ChipGroup + Chip                         |
| DropdownButton           | Spinner                                  |
| GoogleFonts.inter()      | fontFamily="@font/inter_*"               |

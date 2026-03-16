# Firebase Configuration Guide for "The Infinite Us"

This document provides step-by-step instructions to configure Firebase for the "The Infinite Us" application, including authentication, Realtime Database, and security rules.

## Prerequisites

- A Google account
- Access to Firebase Console (https://console.firebase.google.com)
- Android Studio with the project open

## Step 1: Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project" and follow the setup wizard
3. Name your project (e.g., "TheInfiniteUs")
4. Enable Google Analytics (optional)
5. Click "Create project"

## Step 2: Register Your Android App

1. In the Firebase Console, click "Add app" and select Android
2. Enter the following details:
   - **Package name**: `com.example.infiniteus`
   - **App nickname**: TheInfiniteUs (optional)
   - **Debug signing certificate SHA-1**: Get this from Android Studio
     - In Android Studio: Build → Generate Signed Bundle/APK → Select APK → Next → Create new keystore (or use existing) → Fill in details → Finish
     - Or run: `./gradlew signingReport` and copy the SHA-1 from the debug variant
3. Click "Register app"
4. Download the `google-services.json` file
5. Place it in the `app/` directory of your project

## Step 3: Enable Firebase Authentication

1. In the Firebase Console, go to **Authentication** → **Sign-in method**
2. Enable **Email/Password** authentication
3. Click "Save"

## Step 4: Create Realtime Database

1. In the Firebase Console, go to **Realtime Database**
2. Click "Create Database"
3. Choose a location (select the closest region to your users)
4. Select "Start in test mode" (for development; change to production rules later)
5. Click "Enable"

## Step 5: Apply Security Rules

1. In the Realtime Database section, go to the **Rules** tab
2. Replace the default rules with the content from `firebase_rules.json` in this project
3. Click "Publish"

**Important**: The rules in `firebase_rules.json` ensure that:
- Users can only read and write their own data
- Only paired users can access their shared session
- Data is validated before being written to the database

## Step 6: Enable Firebase Cloud Messaging (Optional for Ghost Heartbeat)

1. In the Firebase Console, go to **Cloud Messaging**
2. Note the **Sender ID** (you'll need this for advanced features)
3. The FCM dependency is already included in `build.gradle.kts`

## Step 7: Build and Run

1. Open Android Studio
2. Sync Gradle files: File → Sync Now
3. Build the project: Build → Make Project
4. Run the app on an Android 13+ device or emulator

## Testing the App

### First User Setup

1. Launch the app
2. Click "Don't have an account? Sign Up"
3. Enter an email and password
4. Click "Sign Up"
5. You will be taken to the pairing screen
6. Note your **Pairing Code** (e.g., "ABC123")

### Second User Setup

1. On a different device or emulator, launch the app
2. Click "Don't have an account? Sign Up"
3. Enter a different email and password
4. Click "Sign Up"
5. You will be taken to the pairing screen
6. Enter the first user's **Pairing Code**
7. Click "Connect"

### Interacting with the Nebula

1. Both users should now see the Nebula screen
2. Drag your finger across the screen to move your orb
3. Watch as your partner's orb moves in real-time
4. When both users pulse within 500ms of each other, the Eclipse effect triggers
5. Feel the haptic feedback and see the golden corona

## Troubleshooting

### App Crashes on Launch

- Ensure `google-services.json` is in the `app/` directory
- Check that the package name in `google-services.json` matches `com.example.infiniteus`
- Verify that Firebase Authentication is enabled

### Can't Find Partner

- Ensure both users have completed the sign-up process
- Double-check the pairing code (it's case-sensitive)
- Verify that both users are connected to the internet

### Real-time Updates Not Working

- Check that the Realtime Database is enabled
- Verify that the security rules have been applied
- Ensure both users are authenticated

### Haptic Feedback Not Triggering

- Confirm that the device supports haptic feedback
- Check that the device has vibration enabled in settings
- Verify that the app has the `VIBRATE` permission

## Production Deployment

Before deploying to production:

1. **Update Security Rules**: Replace test mode rules with production rules from `firebase_rules.json`
2. **Enable Firestore Backups**: In the Firebase Console, enable automated backups
3. **Set Up Monitoring**: Enable Cloud Monitoring to track app performance
4. **Configure Authentication**: Set up additional sign-in methods (Google, Facebook, etc.)
5. **Test Thoroughly**: Test all features on multiple devices and network conditions

## Additional Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firebase Authentication Guide](https://firebase.google.com/docs/auth)
- [Realtime Database Guide](https://firebase.google.com/docs/database)
- [Security Rules Guide](https://firebase.google.com/docs/database/security)

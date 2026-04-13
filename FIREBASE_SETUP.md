# Quick Firebase Setup Guide

## Step-by-Step Instructions

### 1. Create Firebase Project
1. Visit: https://console.firebase.google.com/
2. Click **"Add project"**
3. Name: `gate-pass-sonia` (or any name you prefer)
4. Disable Google Analytics (optional)
5. Click **"Create project"**

### 2. Enable Required Services

#### Firebase Authentication
1. Left menu → **Authentication**
2. Click **"Get started"**
3. Go to **Sign-in method** tab
4. Enable **Email/Password**
5. Click **"Save"**

#### Firestore Database
1. Left menu → **Firestore Database**
2. Click **"Create database"**
3. Select **"Start in test mode"**
4. Choose location (closest to your region)
5. Click **"Enable"**

#### Cloud Messaging
- Already enabled by default (no action needed)

### 3. Add Android App

1. Project Settings (⚙️ icon) → **"Add app"** → **Android**
2. Package name: `com.sonia.gatepass` (MUST match exactly)
3. App nickname: `Gate Pass Android` (optional)
4. **Download** `google-services.json`
5. **IMPORTANT:** Place the file at:
   ```
   C:\Users\User\Downloads\Gatepass\app\google-services.json
   ```

### 4. Create Test Users

#### In Firebase Authentication:
1. Go to **Authentication** → **Users** tab
2. Click **"Add user"**
3. Create users:
   - **Super Admin:**
     - Email: `superadmin@sonia.com`
     - Password: `admin123`
   - **Admin:**
     - Email: `admin@sonia.com`
     - Password: `admin123`
   - **Production User:**
     - Email: `user@sonia.com`
     - Password: `user123`

4. **Copy the User UID** for each user (click on the user to see it)

#### In Firestore Database:
1. Go to **Firestore Database**
2. Click **"Start collection"**
3. Collection ID: `Users`
4. Add documents (use the **same UID** from Authentication):

**Super Admin Document:**
- Document ID: [CQtyPxPveeaDgt3POfM24szXBOP2]
- Fields:
  ```
  userId: [CQtyPxPveeaDgt3POfM24szXBOP2]
  name: "Super Admin"
  role: "SuperAdmin"
  email: "superadmin@sonia.com"
  createdAt: "09/04/2026"
  isActive: true
  ```

**Admin Document:**
- Document ID: [a11a24yfkGgNzZ0mCE0CpnhiBsY2]
- Fields:
  ```
  userId: [a11a24yfkGgNzZ0mCE0CpnhiBsY2]
  name: "Admin Manager"
  role: "Admin"
  email: "admin@sonia.com"
  createdAt: "09/04/2026"
  isActive: true
  ```

**Production User Document:**
- Document ID: [DqOLUeC3eqVwrysCQyp6FSSe7la2]
- Fields:
  ```
  userId: [DqOLUeC3eqVwrysCQyp6FSSe7la2]
  name: "Production Staff"
  role: "User"
  email: "user@sonia.com"
  createdAt: "09/04/2026"
  isActive: true
  ```

### 5. Apply Security Rules

1. Go to **Firestore Database** → **Rules** tab
2. Open the file: `firestore.rules.json` from the project
3. Copy the entire content
4. Paste into the Rules editor
5. Click **"Publish"**

### 6. Build & Run

1. Open Android Studio
2. Open the project: `C:\Users\User\Downloads\Gatepass`
3. Wait for Gradle sync
4. Connect device or start emulator
5. Click **Run** (▶️)

### 7. Test Login

Try logging in with:
- **Email:** `superadmin@sonia.com`
- **Password:** `admin123`

You should see the Dashboard with Super Admin privileges.

---

## Common Issues

### ❌ "google-services.json not found"
- Make sure the file is at: `app/google-services.json`
- Clean & rebuild: `Build → Clean Project`, then `Build → Rebuild Project`

### ❌ "User not found" on login
- Verify user exists in **both** Authentication AND Firestore
- Check `isActive: true` in Firestore document
- Ensure UIDs match between Authentication and Firestore

### ❌ "Permission denied" errors
- Apply the security rules from `firestore.rules.json`
- Or temporarily set rules to test mode:
  ```
  allow read, write: if request.auth != null;
  ```

### ❌ Build errors
- Ensure JDK 17 is configured
- Check `build.gradle.kts` versions match
- Invalidate caches: `File → Invalidate Caches / Restart`

---

**Need help?** Check the main `README.md` for detailed documentation.

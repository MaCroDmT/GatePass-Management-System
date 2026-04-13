# 🔧 Setup Guide for Contributors

## Getting Started

Welcome to the **Gate Pass Management System** for Sonia & Sweaters Limited. Follow this guide to set up the project on your local machine.

---

## Prerequisites

1. **Android Studio** (Arctic Fox or later)
2. **JDK 17**
3. **Android SDK 34** (minSdk 24)
4. A **Firebase account** (free tier is sufficient)

---

## Step 1: Clone the Repository

```bash
git clone <repository-url>
cd Gatepass
```

---

## Step 2: Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Enter a project name (e.g., `gate-pass-management`)
4. Follow the setup wizard (disable Google Analytics if not needed)
5. Click **"Create project"**

---

## Step 3: Enable Firebase Services

### Authentication
1. In Firebase Console → **Authentication** → **Get started**
2. Enable **Email/Password** sign-in method
3. Click **Save**

### Firestore Database
1. Go to **Firestore Database** → **Create database**
2. Select **"Start in test mode"**
3. Choose a location closest to you
4. Click **Enable**

### Cloud Messaging
- Enabled by default — no action needed

### Storage (optional, for PDFs)
1. Go to **Storage** → **Get started**
2. Start in **test mode**
3. Click **Done**

---

## Step 4: Add Android App to Firebase

1. In Firebase Console → **Project Settings** (⚙️) → **Add app** → **Android**
2. Enter package name: `com.sonia.gatepass`
3. App nickname: `Gate Pass Android` (optional)
4. **Download** the `google-services.json` file
5. **Place** the file at: `app/google-services.json`

> ⚠️ **IMPORTANT:** The `google-services.json` file is git-ignored and should NEVER be committed to the repository. Each contributor must create their own Firebase project and download their own config file.

---

## Step 5: Create Test Users

### In Firebase Authentication:
1. Go to **Authentication** → **Users** tab
2. Click **"Add user"**
3. Create test users:

| Email | Password | Role |
|-------|----------|------|
| `superadmin@sonia.com` | `admin123` | SuperAdmin |
| `admin@sonia.com` | `admin123` | Admin |
| `user@sonia.com` | `user123` | User |

4. **Copy the User UID** for each user (click on the user to see it)

### In Firestore Database:
1. Go to **Firestore Database**
2. Click **"Start collection"** → Collection ID: `Users`
3. Add documents using the **same UID** from Authentication:

**Super Admin:**
```
Document ID: [paste UID here]
- userId: [same UID]
- name: "Super Admin"
- role: "SuperAdmin"
- email: "superadmin@sonia.com"
- createdAt: "12/04/2026"
- isActive: true
```

**Admin:**
```
Document ID: [paste UID here]
- userId: [same UID]
- name: "Admin Manager"
- role: "Admin"
- email: "admin@sonia.com"
- createdAt: "12/04/2026"
- isActive: true
```

**Production User:**
```
Document ID: [paste UID here]
- userId: [same UID]
- name: "Production Staff"
- role: "User"
- email: "user@sonia.com"
- createdAt: "12/04/2026"
- isActive: true
```

---

## Step 6: Apply Firestore Security Rules

1. Go to **Firestore Database** → **Rules** tab
2. Copy the contents of `firestore.rules` from this repository
3. Paste into the Rules editor
4. Click **Publish**

---

## Step 7: Build and Run

1. Open the project in **Android Studio**
2. Wait for Gradle sync to complete
3. Connect an Android device or start an emulator
4. Click **Run** (▶️) or `Shift+F10`
5. Login with one of the test users above

---

## 📁 Project Structure

```
Gatepass/
├── app/
│   ├── src/main/
│   │   ├── java/com/sonia/gatepass/
│   │   │   ├── data/
│   │   │   │   ├── model/          # Data classes
│   │   │   │   ├── repository/     # Firebase data access layer
│   │   │   │   └── service/        # FCM Service
│   │   │   ├── ui/
│   │   │   │   ├── adapter/        # RecyclerView adapters
│   │   │   │   ├── admin/          # Admin panels
│   │   │   │   ├── auth/           # Login screen
│   │   │   │   ├── gatepass/       # Gate pass screens
│   │   │   │   ├── main/           # Main activity & fragments
│   │   │   │   ├── notifications/  # Notifications screen
│   │   │   │   ├── reports/        # Reports screen
│   │   │   │   └── viewmodel/      # MVVM ViewModels
│   │   │   ├── util/               # Utilities
│   │   │   └── GatePassApplication.kt
│   │   ├── res/                    # Layouts, colors, menus, etc.
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json        # ⚠️ NOT in git — create your own!
├── firestore.rules                 # Firestore security rules
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🔐 Security Notes

- **Never commit `google-services.json`** — it contains your Firebase API keys and project secrets
- **Never commit `local.properties`** — it contains your machine's SDK paths
- Test credentials in this guide are for development only — change them for production

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| `google-services.json not found` | Follow Step 4 to create your own Firebase project and download the config |
| Login fails | Verify user exists in both Authentication AND Firestore with matching UIDs |
| Permission denied errors | Apply the Firestore rules from `firestore.rules` (Step 6) |
| Build errors | `Build → Clean Project`, then `Build → Rebuild Project` |
| Gradle sync fails | Check that JDK 17 is configured in Android Studio |

---

## 📞 Support

For issues or questions, contact the development team.

---

**Version:** 1.0  
**Last Updated:** April 2026

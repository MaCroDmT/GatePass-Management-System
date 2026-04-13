# 🚀 Quick Start Guide - Gate Pass Management System

## Get Started in 5 Minutes

### Step 1: Open Project (1 minute)
1. Launch **Android Studio**
2. Click **File → Open**
3. Navigate to: `C:\Users\User\Downloads\Gatepass`
4. Click **OK**
5. Wait for Gradle sync to complete

### Step 2: Create Firebase Project (2 minutes)
1. Go to: https://console.firebase.google.com/
2. Click **"Add project"**
3. Name: `gate-pass-sonia`
4. Disable Google Analytics (optional)
5. Click **"Create project"**

### Step 3: Enable Services (1 minute)

**In Firebase Console:**

1. **Authentication:**
   - Click Authentication → Get started
   - Enable Email/Password → Save

2. **Firestore Database:**
   - Click Firestore Database → Create database
   - Start in test mode → Enable

3. **Download Config:**
   - Project Settings → Add Android app
   - Package name: `com.sonia.gatepass`
   - Download `google-services.json`
   - **Place it in:** `C:\Users\User\Downloads\Gatepass\app\`

### Step 4: Create Test User (1 minute)

**In Authentication:**
1. Add user:
   - Email: `admin@sonia.com`
   - Password: `admin123`
   - Copy the **User UID**

**In Firestore:**
1. Create collection: `Users`
2. Add document (use same UID):
   ```
   Document ID: [paste UID here]
   userId: [same as Document ID]
   name: "Super Admin"
   role: "SuperAdmin"
   email: "admin@sonia.com"
   createdAt: "09/04/2026"
   isActive: true
   ```

### Step 5: Run App (30 seconds)
1. Connect Android device or start emulator
2. Click **Run** (▶️) in Android Studio
3. Login with:
   - Email: `admin@sonia.com`
   - Password: `admin123`

**🎉 You're in!**

---

## What's Next?

- Try creating a gate pass
- Approve it (login as Admin)
- Record movements
- Generate PDF
- Check reports

---

## Need Help?

| Issue | Solution |
|-------|----------|
| Build errors | `Build → Clean Project`, then Rebuild |
| Login fails | Check user exists in Auth AND Firestore |
| Firestore errors | Apply rules from `firestore.rules.json` |
| App crashes | Check Logcat for errors |

**Full docs:** See `README.md` and `FIREBASE_SETUP.md`

---

**That's it! You're ready to use the Gate Pass Management System.** 🎊

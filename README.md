# Gate Pass Management System
**Sonia & Sweaters Limited**

An Android-based Gate Pass Management System designed to digitize and automate the manual gate pass process used for tracking outward and inward movement of garments.

## 📱 Features

- ✅ **Role-based Access Control** (Super Admin, Admin, Production User)
- ✅ **Gate Pass Lifecycle Management** (Pending → Approved → In Progress → Completed)
- ✅ **Partial Return & Re-dispatch Tracking** under RGP
- ✅ **Real-time Updates** via Firebase Firestore
- ✅ **PDF Generation** with company branding
- ✅ **Style-based Tracking** with aggregate reporting
- ✅ **Movement History & Audit Logs**
- ✅ **Push Notifications** via Firebase Cloud Messaging
- ✅ **Gate Pass Reopening** (Super Admin only)

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** XML-based Android UI with Material Design
- **Architecture:** MVVM (Model-View-ViewModel)
- **Backend:** Firebase (Authentication, Firestore, Cloud Messaging, Storage)
- **PDF Generation:** iText7
- **Async:** Kotlin Coroutines & Flow

## 📋 Prerequisites

1. **Android Studio** (Arctic Fox or later)
2. **JDK 17**
3. **Firebase Account** (Free tier is sufficient)
4. **Android SDK 34** (minSdk 24)

## 🚀 Setup Instructions

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Enter project name: `gate-pass-management`
4. Follow the setup wizard (disable Google Analytics if not needed)
5. Click **"Create project"**

### Step 2: Enable Firebase Services

#### Authentication
1. In Firebase Console, go to **Authentication**
2. Click **"Get started"**
3. Enable **Email/Password** sign-in method
4. Click **"Save"**

#### Firestore Database
1. Go to **Firestore Database**
2. Click **"Create database"**
3. Select **"Start in test mode"** (we'll add security rules later)
4. Choose a location closest to you
5. Click **"Enable"**

#### Cloud Messaging
1. Already enabled by default
2. No additional setup required

#### Storage (for PDFs)
1. Go to **Storage**
2. Click **"Get started"**
3. Start in **test mode**
4. Click **"Done"**

### Step 3: Add Android App to Firebase

1. In Firebase Console, click the **Android icon** to add an app
2. Enter package name: `com.sonia.gatepass`
3. App nickname: `Gate Pass Android`
4. **Download** the `google-services.json` file
5. **Place** the file in: `app/google-services.json`

### Step 4: Create Initial Users in Firestore

1. Go to **Firestore Database** in Firebase Console
2. Click **"Start collection"**
3. Collection ID: `Users`
4. Add documents with the following structure:

**Super Admin:**
```
Document ID: [auto-generated]
- email: "admin@soniasweaters.com"
- name: "Super Admin"
- role: "SuperAdmin"
- userId: [copy from document ID]
- createdAt: "09/04/2026"
- isActive: true
```

**Admin:**
```
Document ID: [auto-generated]
- email: "manager@soniasweaters.com"
- name: "Production Manager"
- role: "Admin"
- userId: [copy from document ID]
- createdAt: "09/04/2026"
- isActive: true
```

**Production User:**
```
Document ID: [auto-generated]
- email: "user@soniasweaters.com"
- name: "Production Staff"
- role: "User"
- userId: [copy from document ID]
- createdAt: "09/04/2026"
- isActive: true
```

### Step 5: Create Users in Firebase Authentication

1. Go to **Authentication** → **Users** tab
2. Click **"Add user"**
3. Add users with the **same email** as in Firestore
4. Use temporary passwords (users can change later)

**Important:** The `userId` in Firestore must match the UID in Firebase Authentication.

**To get the UID:**
1. After creating a user in Authentication, click on it
2. Copy the **User UID**
3. Update the Firestore document with this UID

### Step 6: Open Project in Android Studio

1. Open **Android Studio**
2. Click **"Open"** and select the project folder
3. Wait for Gradle sync to complete
4. If prompted to update Gradle or AGP, accept

### Step 7: Build and Run

1. Connect an Android device or start an emulator
2. Click **Run** (green play button) or `Shift+F10`
3. The app will install and launch

## 📱 User Workflows

### Production User
1. Login with credentials
2. Create new gate pass
3. Record movements (Outward/Inward/Re-dispatch)
4. View gate pass details and history
5. Check reports by style number

### Admin
1. Login with credentials
2. View pending approvals
3. Approve or reject gate passes
4. Mark completed gate passes
5. View all gate passes and reports

### Super Admin
1. All Admin capabilities
2. Reopen completed gate passes
3. Access to audit logs

## 🔐 Security Rules

The Firestore security rules are provided in `firestore.rules.json`. To apply them:

1. Go to **Firestore Database** → **Rules** tab
2. Copy the contents of `firestore.rules.json`
3. Click **"Publish"**

## 📁 Project Structure

```
app/src/main/java/com/sonia/gatepass/
├── data/
│   ├── model/          # Data classes (User, GatePass, Movement, Notification)
│   ├── repository/     # Firebase data access layer
│   └── service/        # Firebase Cloud Messaging
├── ui/
│   ├── adapter/        # RecyclerView adapters
│   ├── auth/           # Login screen
│   ├── main/           # Main activity & dashboard
│   ├── gatepass/       # Gate pass CRUD operations
│   ├── admin/          # Admin approval panel
│   ├── notifications/  # Notifications screen
│   ├── reports/        # Reports & analytics
│   └── viewmodel/      # MVVM ViewModels
├── util/               # Utilities (Constants, Date, Validation, SharedPrefs)
└── GatePassApplication.kt

app/src/main/res/
├── layout/             # XML layouts
├── values/             # Strings, colors, themes
├── drawable/           # Images and shapes
└── menu/               # Bottom navigation menu
```

## 🎯 Key Features Explained

### Partial Return & Re-dispatch

When items are sent outward (e.g., 1000 items):
- **Partial Return:** 200 items returned
- **Re-dispatch:** 130 items sent again
- **Balance Calculation:** `Total Sent - Total Returned + Re-dispatched`

The system tracks all movements with timestamps and user attribution.

### Gate Pass Status Flow

```
PENDING → APPROVED → IN_PROGRESS → PARTIALLY_RETURNED → COMPLETED
           ↓
        REJECTED
        
COMPLETED → REOPENED (Super Admin only) → IN_PROGRESS
```

### Auto GPID Generation

Gate Pass IDs are auto-incremented: `GP1`, `GP2`, `GP3`, etc.

## 🐛 Troubleshooting

### Build Errors
- Ensure JDK 17 is configured
- Sync Gradle files
- Clean and rebuild: `Build → Clean Project`, then `Build → Rebuild Project`

### Firebase Connection Issues
- Verify `google-services.json` is in `app/` folder
- Check package name matches: `com.sonia.gatepass`
- Ensure Firebase services are enabled

### Login Fails
- Verify user exists in both Authentication and Firestore
- Check `isActive` field is `true` in Firestore
- Ensure email/password match exactly

### Permissions
- The app requires Internet access (already in manifest)
- Storage permissions for PDF download (Android 12 and below)

## 📊 Firestore Collections

### Users
```
userId, name, role, email, createdAt, isActive
```

### GatePass
```
gpid, styleNo, goodsName, concernedPeopleEmail, destination,
purpose, totalSent, totalReturned, totalRedispatched,
balanceQuantity, returnableDate, status, createdBy,
createdByName, approvedBy, approvedByName, createdAt,
updatedAt, completedAt, reopeningCount, auditLog[]
```

### Movements
```
movementId, gpid, type (OUTWARD/INWARD/RE_DISPATCH),
quantity, date, recordedBy, recordedByName, remarks, createdAt
```

### Notifications
```
notificationId, userId, title, message, gpid, type, status, createdAt
```

## 🚀 Future Enhancements

- [ ] Barcode/QR scanning for items
- [ ] Excel export functionality
- [ ] AI-based anomaly detection
- [ ] Web dashboard for management
- [ ] GPS tracking for deliveries
- [ ] Digital signature integration

## 📄 License

This project is proprietary software for Sonia & Sweaters Limited.

## 👥 Support

For issues or questions, contact the development team.

---

**Version:** 1.0  
**Last Updated:** April 2026

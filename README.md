# Behavior-Based Authentication System — SURAKSHAK

### 🛠️ Built with the tools and technologies:

![Express](https://img.shields.io/badge/Express-black?style=for-the-badge&logo=express)
![Flask](https://img.shields.io/badge/Flask-white?style=for-the-badge&logo=flask)
![JSON](https://img.shields.io/badge/JSON-5E5C5C?style=for-the-badge&logo=json&logoColor=white)
![Markdown](https://img.shields.io/badge/Markdown-000000?style=for-the-badge&logo=markdown)
![npm](https://img.shields.io/badge/npm-CB3837?style=for-the-badge&logo=npm&logoColor=white)
![Mongoose](https://img.shields.io/badge/Mongoose-880000?style=for-the-badge&logo=mongoose)
![TensorFlow](https://img.shields.io/badge/TensorFlow-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)
![scikit-learn](https://img.shields.io/badge/scikit--learn-F7931E?style=for-the-badge&logo=scikit-learn)
![.ENV](https://img.shields.io/badge/.ENV-black?style=for-the-badge)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript)

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![NumPy](https://img.shields.io/badge/NumPy-013243?style=for-the-badge&logo=numpy)
![XML](https://img.shields.io/badge/XML-EF652A?style=for-the-badge&logo=xml&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Google](https://img.shields.io/badge/Google-4285F4?style=for-the-badge&logo=google&logoColor=white)
![.bat](https://img.shields.io/badge/.bat-4D4D4D?style=for-the-badge)
![Pandas](https://img.shields.io/badge/Pandas-150458?style=for-the-badge&logo=pandas)



#OVERVIEW

**Surakshak** is a secure mobile authentication system that extends beyond conventional security approaches by integrating behavioral biometrics, panic gestures, and simulated (fake) environments. These elements work together to detect unauthorized access and silently safeguard sensitive user data.


This project aims to provide developers with a robust framework for safeguarding sensitive user data and ensuring secure interactions. The core features include:

- ✳️ 🔍 **Behavioral Risk Analysis**: Continuously evaluates user behavior to detect anomalies and potential threats.
- 🧠 🤖 **Behavioral Biometrics**: Integrates touch, keystroke, and sensor data for dynamic user profiling.
- 🔑 🔐 **Adaptive Reauthentication**: Implements seamless, context-aware security prompts based on risk levels.
- 🧮 🛡️ **Secure User Management**: Supports OTP verification, passcode setup, and account security workflows.
- ⚙️ 🧩 **Modular Architecture**: Facilitates maintainable, scalable development with clear build configurations.
- 🚨 🛑 **Emergency & Security Triggers**: Enables quick activation of panic modes and app lock features for user safety.

---⚙️ Setup Instructions

### Prerequisites

* Android Studio (latest)
* JDK 8+
* MongoDB Atlas
* Python 3.8+ with Flask
* Android Device/API 24+

1. **Clone the repository**

   ```bash
   git clone https://github.com/ananyadhagat/surakshak-Hackorbit.git
   ```

2. **Backend Setup**
#### python flask (Risk evaluation)
   * Navigate to `backend/`
   * Install dependencies:

     ```bash
     pip install -r requirements.txt
     ```
   * Run the backend server:

     ```bash
     python app.py
     ```
    #### 🔹 Node.js Server (User Authentication via MongoDB)

   * Navigate to `backend/` 
   * Install Node dependencies:
     ```bash
     npm install
     ```
   * Run the Node server:
     ```bash
     node server.js

3. **Android App Setup (Frontend)**

   * Open `app/` in Android Studio
   * Ensure Gradle sync completes
   * Build and run the application on an emulator or device

 4. **MongoDB Configuration**

   * Create a free cluster on [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
   * Create a Database (e.g., `surakshak`)
   * Inside your  backend folder (where `package.json` is), create a `.env` file with:

     ```env
     MONGO_URI=your_mongodb_connection_uri
     PORT=5000
     TWILIO_ACCOUNT_SID=your_twilio_sid
     TWILIO_AUTH_TOKEN=your_twilio_token
     ```

   * Make sure your cluster allows connections from all IPs (`0.0.0.0/0`) or your current IP.
   * Then run:
     ```bash
     npm install
     npm start
     ```

   ✅ You’re now connected to MongoDB Atlas and ready to handle user data securely!

###IMPORTANT NOTE : 🛠️ The backend API calls depend on your system’s IP address. You must update the IP address before running the Android app.

Go to:
---app\src\main\java\com\example\suraksha\utils\Constants.java

Update the following line with your system’s current IP address (the one your backend is running on): 
public static final String BASE_IP = "http://<your-ip-address>:<port>";


## 🗂️ Folder Structure

```
Surakshak-Hackorbit/
├── app/               # Android Application
│   └── src/
│       └── main/java/com/example/suraksha
├── backend/           # Python Flask backend
│   ├── app.py
│   └── requirements.txt
├── README.md
└── ...
```

---

## Application Flow

### 1. Signup Screen

* User enters name and mobile number.
* OTP is sent via backend. (currently otp is displayed on the screen as any paid app is not used)
* Upon OTP verification:
  * A unique `userID` (UUID) is generated.
  * Stored in `SharedPreferences`.
  * Passed to the next activity.

### 2. Set Passcode

* User creates a six-digit passcode.
* Confirmed with second OTP.
* Stored securely in MongoDB.
* Triggers transition to behavior training.

### 3. Set panic gesture
* User has been given with 2 options for stopage of app and transactions in case of panic or emergency like situations.

### 3. Behavioral Training Session (New)

* Launches `TrainingSessionActivity`.
* Displays 4 training sentences for the user to type.
* Collects behavioral traits:
  * Typing speed
  * Dwell time
  * Flight time
  * Tap pressure
  * Inter-key delay
  * Touch size
  * Screen hold time
  * Accelerometer and gyroscope patterns
* Sends data via `/upload-training-batch`.
* Triggers training with `/train-hybrid-model`.
* Calculates average behavior vector.
* Stores baseline in MongoDB.

### 4. Login Screen

* Login via:
  * Passcode
  * Fingerprint (BiometricPrompt)
  * OTP (Forgot Passcode)
* After login:
  * Retrieves mobile number and `userID`.
  * Redirects to home screen.

### 5. Home Screen

* Displays user details, logout, and feature grid.
* Panic gesture detection triggers fake home.
* Behavior monitoring starts automatically.

#### Real-Time Monitoring (New)

* Integrated via `BehaviorMonitor.java`.
* Monitors:
  * Typing patterns
  * Touch metrics
  * Motion sensors
  * Screen hold time
* Every 20 seconds:
  * Constructs a behavior vector.
  * Sends to `/evaluate-risk`.
  * Logs or reacts based on returned risk score.
 
#### Reauthenticating user
*In case of a medium risk score the app functionalities stop and it shows a dialog box appears for choosing the reauthentication option of biometric or passcode of the original user. Once correctly reauthenticated the app gets started.

*in case of high risk score while continous comparing behavioral data user automatically logs out to avoid fraud

---

## New Files & Features Added

### ✅ `BehaviorMonitor.java`

* Standalone class for collecting sensor and touch-based metrics continuously.
* Runs inside `Homescreen.java` and other key screens.
* Sends vector to backend at 20s intervals.

### ✅ `RiskAnalyzer.java`

* Handles sending behavioral vectors and logging risk scores inside Android app.

### ✅ Updated `Homescreen.java`

* Integrated `BehaviorMonitor`.
* Fetches `userID` from `SharedPreferences`.
* Handles panic gestures and logout.
* Sends behavior vectors to backend API.

### ✅ Updated `TrainingSessionActivity.java`

* Collects behavioral training data.
* Sends to `/upload-training-batch`.
* Triggers `/train-hybrid-model`.

### ✅ Backend — Flask `app.py` Updates

* **New Route:** `/evaluate-risk`:
  * Accepts incoming vector.
  * Fetches stored baseline.
  * Calculates risk score.
  * Returns normalized risk between `0–100`.

* Improved:
  * `upload-training-batch`
  * `train-hybrid-model` logic

* MongoDB stores:
  * `userID`, `average_vector`, `risk_score`, `timestamp`

---

## Features Implemented

* User signup and secure passcode storage
* Panic gesture configuration and decoy home screen
* OTP-based TPIN generation and biometric login
* **✅ Training Module for behavioral learning**
* **✅ Real-Time Continuous Monitoring**
* **✅ ML-based Risk Score Evaluation**
* Track Your Behavior screen
* Customize Risk Behavior screen
* Passcode change with OTP

---

## Upcoming Features

* Anomaly-based reauthentication logic (in progress)
* Risk-based session locking
* Dynamic UI adaptation based on score
* Admin portal to visualize analytics

---




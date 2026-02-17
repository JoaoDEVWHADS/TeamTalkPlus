# TeamTalk Plus (Android)

> **Based on TeamTalk 5 Official**  
> *A powerful, feature-rich fork of the TeamTalk 5 Android Client.*

---

## ⛔ LIABILITY & SECURITY DISCLAIMER ⛔

**PLEASE READ THIS SECTION CAREFULLY BEFORE USING THIS SOFTWARE.**

### 1. The `release.keystore` Incident
This repository contains a file named `release.keystore`. **We (the developers of this fork) did NOT publicly share this key.** It was leaked and distributed by third parties without our consent. 

Because this signing key is now public knowledge:
*   **ANYONE** can sign an APK that looks like an official update.
*   **ANYONE** can inject malicious code and sign it with this key.
*   We cannot guarantee that an APK you find on the internet signed with this key is safe, unless it comes directly from us.

### 2. No Warranty or Liability
**This software is provided "AS IS", without warranty of any kind, express or implied.**

*   **We are NOT responsible** for any damage, data loss, ban, or security compromise that may occur from using this software.
*   **We are NOT responsible** for any malicious modifications made by third parties using the leaked keystore.
*   By downloading, compiling, or installing this software, you acknowledge that you do so **AT YOUR OWN RISK**.

### 3. The ONLY Trusted Source
To ensure you are using a clean, safe, and unmodified version of TeamTalk Plus, **ALWAYS** download releases from our official channel:

👉 **[Official Telegram Channel: @joaoprojects](https://t.me/joaoprojects)** 👈

**DO NOT** trust APKs sent by users in private messages or found on other websites.

---

## 📚 THE GRAND USER MANUAL & GUIDE

Welcome to the complete guide for TeamTalk Plus. This section covers everything from installation to advanced administration.

### 🐧 Part 1: Preparation (Linux Environment)

To compile this project yourself, you need a Linux machine (Ubuntu, Debian, or similar). We verify our builds on Ubuntu 22.04 LTS.

**1. Install Essential Tools:**
Open your terminal and run the following command to install Java, Git, and other necessary utilities:

```bash
sudo apt update
sudo apt install openjdk-17-jdk git git-lfs unzip zip curl
```

**2. Verify Installations:**
*   `java -version` should show OpenJDK 17.
*   `git --version` should show Git installed.
*   `git lfs --version` should confirm Large File Storage is ready.

### 🛠️ Part 2: Compilation (Step-by-Step)

**⚠️ CRITICAL WARNING:** Do NOT use Android Studio unless you are an expert. Use the command line as shown below to avoid headaches!

**1. Clone the Repository:**
```bash
git clone https://github.com/YourUsername/TeamTalkPlus.git
cd TeamTalkPlus
```

**2. Extract Native Libraries (Crucial Step):**
The app will CRASH if you skip this. The native audio/video libraries are zipped to save space.
```bash
cd app/src/main
unzip jniLibs.zip -d . 
# You should now see a 'jniLibs' folder here.
cd ../../..
```

**3. Set SDK Location:**
Tell the build system where your Android SDK is.
*   If you successfully installed the SDK to `~/Android/Sdk`, run:
    ```bash
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    ```

**4. Build the APK:**
```bash
chmod +x gradlew
./gradlew assembleRelease
```
*Wait for the "BUILD SUCCESSFUL" message.*

**5. Locate your file:**
The new APK is at: `app/build/outputs/apk/release/app-release.apk`

---

### 📱 Part 3: Using the App (User Guide)

Once you install and open the app, here is how to master it.

#### 1. The Main Screen (Tabs)
The interface is divided into 3 main tabs. Swipe left/right to switch between them:

*   **CHANNELS (Left Tab):** This is the main view. It shows the channel tree, users in your channel, and incoming chat messages.
*   **FILES (Middle Tab):** Displays files uploaded to the current channel.
*   **MEDIA (Right Tab):** Controls for streaming music or video files to the channel.

#### 2. Connecting to a Server
1.  On the startup screen, you see a list of servers.
2.  **Add Server:** Tap "New Server" (or the `+` button depending on version/layout) to add your own.
3.  **Preferences:** Tap "Preferences" to see connection options.
4.  **Connect:** Tap a server to join.

#### 3. In-Channel Actions
Once connected:
*   **Talk:** Press and hold the big **"TX" (Transmit)** button at the bottom to speak (Push-to-Talk).
*   **Chat:** Tap the message box at the bottom, type your text, and tap "Send".
*   **Join a Channel:** Tap a channel name in the tree -> Tap **"Join"**.
*   **View User Info:** Tap a user's name to see their details (IP, Client, Latency).

#### 4. File Transfer (Files Tab)
*   **Download:** Tap a file in the list -> Tap **"Download"**.
*   **Upload:** Tap the **Options Menu (⋮)** -> **"Upload File"** -> Pick a file from your phone.
*   **Delete:** Long press a file -> **"Delete"**.

#### 5. Streaming Music (Media Tab)
Want to play music for everyone?
1.  Go to the **Media** tab.
2.  Tap **"Stream Media File"** (or use Menu -> **"Stream"**).
3.  Select a music file (`.mp3`, `.ogg`, etc.) from your device.
4.  It will start playing. Use the slider to adjust volume. **Everyone in the channel will hear it.**

#### 6. Settings Deep Dive
Tap **Menu (⋮)** -> **"settings"** (or "Preferences").

*   **General:**
    *   **Nickname:** Change the name people see.
    *   **Gender:** Set Male/Female (affects some TTS engines).
    *   **Status Mode:** Set yourself to "Away" or "Question".

*   **Sound System:**
    *   **Master Volume:** How loud TeamTalk is.
    *   **Microphone Gain:** Boost your mic volume if people say you are quiet.
    *   **Voice Activation:** Check this to transmit automatically when you speak (no PTT needed). Slider adjusts sensitivity.
    *   **Echo Cancellation:** Turn this ON if you are not using headphones.

*   **Text-to-Speech (Accessibility):**
    *   **Events:** Choose what the app reads aloud (e.g., "User joined channel", "New message").
    *   **Engine:** Select your preferred TTS engine (Google, Samsung, etc.).

---

### 👑 Part 4: Administration Guide (For Admins)

If you are a Server Administrator, TeamTalk Plus gives you superpowers.

#### 1. User Accounts (`UserAccountsActivity`)
**Menu (⋮) -> "User Accounts"**
*   **Create:** Tap `+`. Fill in Username/Password. Check rights (Admin, Upload, etc.). Save.
*   **Edit:** Long-press a user -> "Edit".
*   **Search:** Type in the top bar to find users fast.

#### 2. Server Properties (`ServerPropActivity`)
**Menu (⋮) -> "Server Properties"**
*   **Rename Server:** Tab 1 -> Edit "Server Name".
*   **MOTD:** Tab 1 -> Edit "Message of the Day".
*   **Bandwidth:** Tab 2 -> specific limits for Voice/Video/Desktop.
*   **Save:** Don't forget to tap "Save" at the bottom!

#### 3. Moving Users (`MoveUsersActivity`)
**Menu (⋮) -> "Move Users"**
*   Great for moving everyone to a "Meeting Room".
*   **Action:** Select "All Users" or specific people -> Tap "Move" -> Select the destination channel.

#### 4. Bans
*   **Server Ban:** Menu -> "Server Banned Users". (Bans from whole server).
*   **Channel Ban:** Long-press Channel -> "Channel Banned Users". (Bans from specific room).
*   **Unban:** Check the box next to the name -> Tap "Unban".

---

## 🌍 Translations

**Want to use TeamTalk Plus in your native language?**

If you want to help translate the project, please leave a comment on **any post** in our [Telegram Channel](https://t.me/joaoprojects). We would love your help!

If you spot a missing string or a bad translation, let us know there as well.

---

## 📥 Installation

1.  **Download:** Get the APK from the **[Telegram Channel](https://t.me/joaoprojects)**.
2.  **Install:** Open the file on your Android device.
3.  **Permissions:** Allow "Install from Unknown Sources" if prompted.
4.  **Login:** Enter your server details and login.

---

**TeamTalk Plus** - Empowering Server Admins.  
*Disclaimer: This project is a fork and is not directly affiliated with BearWare.dk.*

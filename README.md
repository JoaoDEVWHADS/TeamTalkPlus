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

### 📱 Part 3: Accessibility & Screen Readers (TalkBack)

**Is the menu vertical?**
No. The main navigation uses a **Horizontal Tab System** (ViewPager).
*   **To change tabs:** Use a **two-finger swipe left or right**, or **double-tap the Tab Headers** at the top of the screen.
*   **Vertical Lists:** While the tabs are arranged horizontally, the *content* inside them (like the Channel List, File List, and User List) is a standard **Vertical List**. You can swipe up/down to browse these items normally.

**TalkBack Features:**
*   **Labeled Buttons:** All major buttons (Transmit, Speaker, Voice Activation) have text labels (`contentDescription`) readable by screen readers.
*   **Accessibility Assistant:** The app includes a smart `AccessibilityAssistant` that prevents the interface from refreshing too aggressively while you are focusing on a specific item, making it easier to read logs without being interrupted.
*   **Volume Keys:** Creating an account and logging in is fully accessible.

**Tips for Visually Impaired Users:**
1.  **Channel Tree:** Provide a "Long Press" (Double-tap and hold) on a channel to see "Join", "Admin Options", or "Channel Info".
2.  **Transmit Button:** The large red "TX" button is at the bottom right. It's easy to find by sliding your finger to the bottom corner.
3.  **Status:** Use the "Manage Status" tab (Tab 12) to quickly set yourself as "Away" or "Question" without diving into deep menus.

---

### 🗂️ Part 4: The 12 Tabs of TeamTalk Plus

After connecting to a server, user interface is divided into **12 precise tabs**. You can swipe left/right to navigate them.

#### 1. FILES
*   **Function:** Shows files uploaded to the current channel.
*   **Actions:**
    *   Tap a file to **Download**.
    *   Tap Menu -> **Upload File** to share a file.

#### 2. CHANNELS
*   **Function:** The main view showing the server channel tree and users.
*   **Actions:** long-press on channels or users for options (Join, Move, Ban).

#### 3. MEDIA STREAMS
*   **Function:** Control center for streaming media files (Music/Video) to the channel.
*   **Actions:** Tap **Stream Media File** to broadcast music to everyone in the channel.

#### 4. SERVER MANAGEMENT (Admin Only)
*   **Function:** Administrative tools. Visible only if you have admin rights.
*   **Buttons:**
    *   **Server Information:** View/Edit server name and MOTD (`ServerPropActivity`).
    *   **User Accounts:** Create/Edit/Delete users (`UserAccountsActivity`).
    *   **Server Banned Users:** Manage server-wide bans (`ServerBannedUsersActivity`).
    *   **Server Statistics:** View uptime and traffic data (`ServerStatisticsActivity`).

#### 5. GLOBAL
*   **Function:** Global chat (Broadcast messages).
*   **Interface:**
    *   **List:** Shows messages sent to "Everyone".
    *   **Input Box:** Type message here.
    *   **Send:** Broadcasts your message to the entire server.

#### 6. EVENT HISTORY
*   **Function:** A log of server events.
*   **Content:** Shows "User logged in", "User joined channel", "Use left channel" events with timestamps.

#### 7. CHANNEL MESSAGES
*   **Function:** Chat specific to your current channel.
*   **Interface:** Standard chat interface. Only people in your room see these messages.

#### 8. SETTINGS
*   **Function:** Quick access to App Preferences.
*   **Sections:**
    *   **General:** Nickname, Gender, Sound events.
    *   **Sound System:** Volume, Gain, Echo Cancel.
    *   **Text-to-Speech:** Configure what the app reads aloud.

#### 9. PRIVATE
*   **Function:** Manages private conversations (PMs).
*   **Interface:** Lists users you are currently chatting with privately. Tap a user to open the chat window.

#### 10. CONNECTION STATUS
*   **Function:** Technical connection details.
*   **Data:**
    *   **Connection:** Online/Offline status.
    *   **Ping:** Your latency in milliseconds (ms).
    *   **RX/TX:** Total data received and transmitted.

#### 11. ONLINE USERS
*   **Function:** A simple list of everyone currently on the server.
*   **Actions:** Easier to browse than the tree view if the server is huge.

#### 12. MANAGE STATUS
*   **Function:** Quickly update your user status.
*   **Interface:**
    *   **Status Mode:** Dropdown to select **Online**, **Away**, or **Question**.
    *   **Status Message:** Text box to type a custom note (e.g., "Eating lunch").
    *   **Save:** Updates your status immediately for all users to see.

---

### 👑 Part 5: Administration Guide (Deep Dive)

If you are a Server Administrator, TeamTalk Plus gives you superpowers.

#### 1. User Accounts
Found in the **Server Management** tab -> **User Accounts** button.
*   **Create:** Tap `+`. Fill in Username/Password. Check rights (Admin, Upload, etc.). Save.
*   **Edit:** Long-press a user -> "Edit".
*   **Search:** Type in the top bar to find users fast.

#### 2. Server Properties
Found in the **Server Management** tab -> **Server Information** button.
*   **Rename Server:** Tab 1 -> Edit "Server Name".
*   **MOTD:** Tab 1 -> Edit "Message of the Day".
*   **Bandwidth:** Tab 2 -> specific limits for Voice/Video/Desktop.
*   **Save:** Don't forget to tap "Save" at the bottom!

#### 3. Moving Users
**Menu (⋮) -> "Move Users"**
*   **Action:** Select "All Users" or specific people -> Tap "Move" -> Select the destination channel.

#### 4. Bans
*   **Server Ban:** Management Tab -> "Server Banned Users".
*   **Channel Ban:** Long-press Channel -> "Channel Banned Users".
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

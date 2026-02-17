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

## ♿ ACCESSIBILITY GUIDE (TalkBack & Screen Readers)

This application is designed to be usable with Android's accessibility services (TalkBack). Here is a specific guide for visually impaired users.

### 1. Navigation Concepts
*   **Horizontal Tabs, Vertical Lists:** The main interface uses a "Page" system. To switch between views (Files, Channels, Chat, etc.), perform a **two-finger swipe left or right**.
*   **Inside a Tab:** Once you are on a page (e.g., "Channels"), the content is a standard **vertical list**. Swipe up/down with two fingers to scroll.
*   **Focus Assistant:** The app includes a special features that prevents the screen from refreshing too aggressively when you are reading logs. If TalkBack suddenly stops reading, touch the screen again to regain focus.

### 2. Button Locations
*   **Transmit (PTT):** The large button at the **bottom-right** of the screen is the "Transmit" button. Double-tap and hold to talk.
*   **Voice Activation:** The button above the Transmit button toggles Voice Activation (Hands-free) on/off.
*   **Microphone Gain:** The slider in the middle controls your microphone volume. Use volume keys to adjust when focused.

### 3. "Long Press" (Context Menu)
Many features are hidden behind a **Long Press**. In TalkBack, this gesture is performed as:
👉 **Double-tap and hold the second tap.**

*   **To Join a Channel:** Find the channel name -> Double-tap and hold the second tap -> Select **"Join"**.
*   **To Ban a User:** Find the user name -> Double-tap and hold the second tap -> Select **"Ban from server"** (Admin only).

---

## 📚 THE 12 TABS OF TEAMTALK PLUS

After connecting to a server, the interface is divided into **12 tabs**. You can swipe left/right to navigate them.

#### 1. FILES
*   **Function:** Shows files uploaded to the current channel.
*   **Actions:**
    *   Tap a file to **Download**.
    *   Tap Menu -> **Upload File** to share a file.

#### 2. CHANNELS
*   **Function:** The main view showing the server channel tree and users.
*   **Actions:** This is where you spend most of your time. See "Context Menu Reference" below for more.

#### 3. MEDIA STREAMS
*   **Function:** Control center for streaming media files (Music/Video) to the channel.
*   **Actions:** Tap **"Stream Media File"** to broadcast music to everyone in the channel. Note: Requires admin rights on some servers.

#### 4. SERVER MANAGEMENT (Admin Only)
*   **Function:** Administrative tools. Visible only if you have admin rights.
*   **Buttons:**
    *   **Server Information:** View/Edit server name and MOTD.
    *   **User Accounts:** Create/Edit/Delete users.
    *   **Server Banned Users:** Manage server-wide bans.
    *   **Server Statistics:** View uptime and traffic data.

#### 5. GLOBAL
*   **Function:** Global chat (Broadcast messages).
*   **Interface:**
    *   **List:** Shows messages sent to "Everyone".
    *   **Input Box:** Type message here.
    *   **Send:** Broadcasts your message to the entire server.

#### 6. EVENT HISTORY
*   **Function:** A log of server events.
*   **Content:** Shows "User logged in", "User joined channel", "Use left channel" events. Useful for admins to track activity.

#### 7. CHANNEL MESSAGES
*   **Function:** Chat specific to your current channel.
*   **Interface:** Standard chat interface. Only people in your room see these messages.

#### 8. SETTINGS
*   **Function:** Quick access to App Preferences. (See "Ultimate Settings Guide" below).

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
*   **Function:** A simple list of everyone currently on the server, sorted alphabetically.
*   **Actions:** Easier to browse than the tree view if the server is huge.

#### 12. MANAGE STATUS
*   **Function:** Quickly update your user status.
*   **Interface:**
    *   **Status Mode:** Dropdown to select **Online**, **Away**, or **Question**.
    *   **Status Message:** Text box to type a custom note (e.g., "Eating lunch").
    *   **Save:** Updates your status immediately for all users to see.

---

## 👆 CONTEXT MENU REFERENCE (Double-tap and hold)

Detailed guide on what happens when you perform the **TalkBack Long Press** (Double-tap and hold the second tap).

### On a USER
*   **Copy Name/ID/IP:** Copies information to your clipboard.
*   **User Properties:** (Admin) Edit the user's account name, password, or rights.
*   **Message:** Opens a private chat with this user.
*   **Make/Revoke Operator:** Gives or removes Channel Operator status (if you have permission).
*   **Kick from Channel/Server:** Forces the user to leave.
*   **Ban from Channel/Server:** Bans the user permanently (or until unbanned).

### On a CHANNEL
*   **Join:** Enters the channel.
*   **Channel Properties:** (Admin) Edit Name, Password, Topic, Audio Codec.
*   **New Channel:** (Admin) Create a sub-channel under this one.
*   **Move Users:** (Admin) Move selected users into this channel.
*   **Channel Banned Users:** View list of people banned specifically from this channel.

### On a FILE
*   **Download:** Saves file to your device.
*   **Delete:** (Admin/Op) Removes the file from the server.

---

## ⚙️ ULTIMATE SETTINGS GUIDE

Detailed explanation of every setting in the app.

### General
*   **Nickname:** Change your display name.
*   **Gender:** Set icon (Man/Woman/Neutral).
*   **BearWare Logins:** Link your BearWare account (optional).
*   **Move Talking Users:** If checked, people who are talking jump to the top of the list (Useful for large channels).
*   **Show Usernames:** Displays login names (e.g., "john_doe") instead of nicknames (e.g., "John D").
*   **Vibrate:** Vibrate phone when you press the PTT button.
*   **Proximity Sensor:** Turns screen off when you hold phone to ear (saves battery).

### Connection
*   **Auto Join Root:** Automatically enter the main channel upon connection.
*   **Subscriptions:** Choose what data you receive (Text, Voice, Video, Desktop). Unchecking these can save data usage.

### Sound System
*   **Master Volume:** Overall app volume.
*   **Microphone Gain:** Input sensitivity.
*   **Voice Processing:** Enable Echo Cancellation (Recommended).
*   **Speakerphone:** Force sound to main speaker instead of earpiece.

### Text-to-Speech (TTS)
*   **Events:** Configure what the app reads aloud:
    *   User Login/Logout
    *   Channel Join/Leave
    *   Private/Channel Messages
    *   Mistakes/Errors
*   **Voice Engine:** Select which Android TTS engine to use (activity dependent).

---

## 🐧 PREPARATION (Linux)

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

---

## 🛠️ COMPILATION (Step-by-Step)

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

## ❓ TROUBLESHOOTING

*   **"Connection Failed":** Check your internet. Verify the server IP and TCP/UDP ports are correct.
*   **"Authentication Failed":** Check your Username and Password.
*   **"No Audio":** Go to Settings -> Sound System -> Ensure "Speakerphone" is checked if you want loud audio. Check "Microphone Gain".
*   **"App Crashes on Start":** If you compiled it yourself, did you unzip `jniLibs.zip`? That is the #1 cause of crashes.

---

## 🌍 TRANSLATIONS

**Want to use TeamTalk Plus in your native language?**

If you want to help translate the project, please leave a comment on **any post** in our [Telegram Channel](https://t.me/joaoprojects). We would love your help!

If you spot a missing string or a bad translation, let us know there as well.

---

**TeamTalk Plus** - Empowering Server Admins.  
*Disclaimer: This project is a fork and is not directly affiliated with BearWare.dk.*

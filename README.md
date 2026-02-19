# TeamTalk Plus (Android)

> **Based on TeamTalk 5 Official**  
> *A powerful, feature-rich fork of the TeamTalk 5 Android Client.*

---

## 🏆 THE "PC EXPERIENCE" ON ANDROID

**TeamTalk Plus was built with a single philosophy: Why should mobile status mean fewer features?**

The official TeamTalk 5 Android client is designed as a "lightweight" companion apps perfect for listening and basic chatting. However, for server administrators, channel operators, and power users, it often feels limiting. You cannot ban users by IP, you cannot manage user accounts, and you cannot configure complex server properties.

**TeamTalk Plus changes that.**

We have painstakingly ported the deep, granular control of the **TeamTalk 5 Windows Client** to the Android platform. If you are used to managing your server from your desktop PC, you will feel right at home here. This is not just a client; it is a full-featured administration tool in your pocket.

### 🆚 Feature Comparison: The Ultimate Breakdown

| Feature | 🤖 Official Android | 💠 TeamTalk Plus | 💻 TeamTalk Windows |
| :--- | :---: | :---: | :---: |
| **User Account Management** | ❌ No | ✅ **Full Admin** | ✅ Full Admin |
| **Create/Delete User Accounts** | ❌ No | ✅ **Yes** | ✅ Yes |
| **Ban Management** | ⚠️ Basic (Kick) | ✅ **Advanced** | ✅ Advanced |
| **Ban Types Supported** | Kick Only | IP / Name / Channel | IP / Name / Channel |
| **Server Properties Access** | ❌ No | ✅ **Edit MOTD/Name** | ✅ Full Access |
| **Channel Admin** | ⚠️ Limited | ✅ **Full Control** | ✅ Full Control |
| **Create Static Channels** | ❌ No | ✅ **Yes** | ✅ Yes |
| **Audio Processing** | ⚠️ Standard | ✅ **Pro (AEC/NS)** | ✅ Pro (AEC/NS) |
| **Microphone Gain Control** | ⚠️ Auto Only | ✅ **Manual Slider** | ✅ Manual Slider |
| **Genders & Status Icons** | ❌ Generic | ✅ **Male/Female/Neutral** | ✅ Male/Female/Neutral |
| **Text-to-Speech Events** | ⚠️ Minimal | ✅ **User Configurable** | ✅ User Configurable |
| **Blind Accessibility** | ⚠️ Standard | ✅ **TalkBack Optimized** | ✅ NVDA/JAWS Optimized |
| **Navigation Structure** | 3 Basic Tabs | **12 Pro Tabs** | Multiple Windows |
| **Bluetooth SCO Support** | ❌ Limited | ✅ **Enhanced** | N/A |
| **File Transfer Logic** | ⚠️ Download Only | ✅ **Up/Down/Delete** | ✅ Up/Down/Delete |
| **Language Support** | ⚠️ English Only | ✅ **Multi-Language** | ✅ Multi-Language |
| **Media Streaming** | ❌ No | ✅ **Stream Music Files** | ✅ Stream Music |

### 🚀 Why "Plus" Matters (Windows Heritage)

**1. The "12-Tab" Dashboard**
On Windows, you have different windows for Chat, Files, and Server Stats. On a small phone screen, cramming this all into one view is impossible. TeamTalk Plus solves this by implementing a **12-Page Swipe Interface**. This mimics the "Multi-Window" multitasking of the desktop client. You can monitor the "Event History" log on Page 6 while keeping an eye on "Online Users" on Page 11, just like alt-tabbing on your PC.

**2. True Server Administration**
On the official client, if a troll enters your server, your options are limited. On TeamTalk Plus, you have the **Server Management (Page 4)** tab. This gives you direct access to the `USERRIGHT_BAN_USERS` and `USERRIGHT_KICK_USERS` commands in their full capacity. You don't just "kick" them; you can ban their specific IP address from the server entirely, or lock them out of a specific channel, exactly as you would on Windows.

**3. Audio Engineering for Mobile**
The Windows client is famous for its crisp audio configuration. TeamTalk Plus brings **Voice Processing** options to Android. You can manually toggle Echo Cancellation (AEC), Noise Suppression (NS), and Automatic Gain Control (AGC) in the settings, giving you that "Broadcast Quality" sound even on a mobile device.

**4. 100% Syntax Compatibility**
TeamTalk Plus uses the exact same `strings.xml` structure and error codes as the Windows Client. When you see an error like `MAX_USERS_EXCEEDED` or a permission like `USERRIGHT_OPERATOR_ENABLE`, it is identical to the documentation you have been reading for years on the PC. You don't need to learn a new "mobile app language"; you already know how to use this app.

---

## ♿ ACCESSIBILITY GUIDE (TalkBack & Screen Readers)

This application is designed from the ground up to be "100% Accessible" with Android's accessibility services (TalkBack). We have worked closely with visually impaired users to ensure that every button, label, and state is properly announced.

### 1. Navigation Concepts
*   **Horizontal Tabs, Vertical Lists:** The main interface uses a "Page" system. To switch between views (Files, Channels, Chat, etc.), perform a **two-finger swipe left or right**. You can also explore by touch near the top to find the tab headers.
*   **Inside a Tab:** Once you are on a page (e.g., "Channels"), the content is a standard **vertical list**. Swipe up/down using **two fingers** to scroll through the list of channels and users.
*   **Focus Assistant:** The app includes a special features that prevents the screen from refreshing too aggressively when you are reading logs. If TalkBack suddenly stops reading, touch the screen again to regain focus.

### 2. How to Join a Channel
There are two distinct ways to enter a channel using accessibility gestures:
1.  **Swipe Navigation:** Swipe right until you hear the Channel Name. Swipe right *again* to find the **"Join Button"** located immediately next to it. Double-tap to join.
    *   *Right-to-Left Swipe:* You will hear **"Channel Name" -> "Join Button"**.
    *   *Left-to-Right Swipe:* You will hear **"Join Button" -> "Channel Name"**.
2.  **Long Press Shortcut:** Locate the Channel Name -> **Double-tap and hold the second tap** to open the Context Menu -> Select **"Join"**.

### 3. The "Long Press" Gesture
Many features are hidden behind a context menu. In TalkBack, the "Long Press" gesture is performed specifically as:
👉 **Double-tap and hold the second tap.**

### 4. Important Button Locations
*   **Transmit (PTT):** The large button at the **bottom-right** of the screen is the "Transmit" button. **Double-tap and hold** this button to talk.
*   **Voice Activation:** The button immediately above the Transmit button toggles Voice Activation (Hands-free) on/off.
*   **Microphone Gain:** The slider in the middle controls your microphone volume. *Accessibility Tip:* When this slider is focused, use the **Volume Up / Volume Down keys** on your phone to adjust the value precisely.

---

## 📚 THE 12 TABS OF TEAMTALK PLUS

After connecting to a server, the interface is divided into **12 tabs**. You can swipe left/right to navigate them.

#### 1. FILES (Page 1)
*   **Function:** Shows files uploaded to the current channel.
*   **Actions:**
    *   Tap a file to **Download**.
    *   Long Press to **Delete** (Requires `USERRIGHT_DELETE_FILE` or Admin).
    *   Tap Menu -> **Upload File** to share a file.

#### 2. CHANNELS (Page 2)
*   **Function:** The main view showing the server channel tree and users.
*   **Actions:** This is where you spend most of your time. See "Context Menu Reference" below for more.

#### 3. MEDIA STREAMS (Page 3)
*   **Function:** Control center for streaming media files (Music/Video) to the channel.
*   **Actions:** Tap **"Stream Media File"** to broadcast music to everyone in the channel. Note: Requires `USERRIGHT_TRANSMIT_MEDIAFILE` permission.

#### 4. SERVER MANAGEMENT (Page 4)
*   **Function:** Administrative tools. Buttons appear dynamically based on your rights.
*   **Buttons:**
    *   **Server Information:** View/Edit server name and MOTD (`USERRIGHT_UPDATE_SERVERPROPERTIES`).
    *   **User Accounts:** Create/Edit/Delete users. (**Admin Only**).
    *   **Server Banned Users:** Manage server-wide bans (`USERRIGHT_BAN_USERS`).
    *   **Server Statistics:** View uptime and traffic data.

#### 5. GLOBAL (Page 5)
*   **Function:** Global chat (Broadcast messages).
*   **Interface:** Type a message here to send it to **Everyone** on the server. Requires `USERRIGHT_TEXTMESSAGE_BROADCAST`.

#### 6. EVENT HISTORY (Page 6)
*   **Function:** A log of server events.
*   **Content:** Shows "User logged in", "User joined channel", "Use left channel" events. Useful for admins to track activity.

#### 7. CHANNEL MESSAGES (Page 7)
*   **Function:** Chat specific to your current channel.
*   **Interface:** Standard chat interface. Only people in your room see these messages.

#### 8. SETTINGS (Page 8)
*   **Function:** Quick access to App Preferences. (See "Ultimate Settings Guide" below).

#### 9. PRIVATE (Page 9)
*   **Function:** Manages private conversations (PMs).
*   **Interface:** Lists users you are currently chatting with privately. Tap a user to open the chat window.

#### 10. CONNECTION STATUS (Page 10)
*   **Function:** Technical connection details.
*   **Data:**
    *   **Connection:** Online/Offline status.
    *   **Ping:** Your latency in milliseconds (ms).
    *   **RX/TX:** Total data received and transmitted.

#### 11. ONLINE USERS (Page 11)
*   **Function:** A simple list of everyone currently on the server, sorted alphabetically.
*   **Actions:** Easier to browse than the tree view if the server is huge.

#### 12. MANAGE STATUS (Page 12)
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
*   **Make/Revoke Operator:** Gives or removes Channel Operator status.
    *   *If you have `USERRIGHT_OPERATOR_ENABLE` or are Admin:* Toggles immediately.
    *   *If you do NOT have rights:* You will be prompted to enter the **Channel Operator Password**.
*   **Kick from Channel:** Kicks the user out of the channel (`USERRIGHT_KICK_USERS`).
*   **Kick from Server:** Kicks the user off the server (`USERRIGHT_KICK_USERS`).
*   **Ban from Channel:** Bans the user from this channel (`USERRIGHT_BAN_USERS`).
*   **Ban from Server:** Bans the user from the entire server (`USERRIGHT_BAN_USERS`).

### On a CHANNEL
*   **Join:** Enters the channel.
*   **Channel Properties:** (Admin/Op) Edit Name, Password, Topic, Audio Codec.
*   **New Channel:** (Admin/Op) Create a sub-channel under this one.
*   **Move Users:** (Admin) Drag selected users into this channel (`USERRIGHT_MOVE_USERS`).
*   **Channel Banned Users:** View list of people banned specifically from this channel.

### On a FILE
*   **Download:** Saves file to your device (`USERRIGHT_DOWNLOAD_FILE`).
*   **Delete:** (Admin/Op) Removes the file from the server (`USERRIGHT_DELETE_FILE`).

---

## 🔒 ADMIN REFERENCE: USER RIGHTS

This section details every single permission you can assign to a user account.
*Found in `UserAccountEditActivity` -> "User Rights" Tab.*

### General Rights
*   **Multi Login (`USERRIGHT_MULTI_LOGIN`):** Allows the same username to log in multiple times simultaneously.
*   **View All Users (`USERRIGHT_VIEW_ALL_USERS`):** Can see users in other channels even if they are not in the same channel.
*   **Create Temporary Channels (`USERRIGHT_CREATE_TEMPORARY_CHANNEL`):** Can create channels that disappear when empty.
*   **Modify Channels (`USERRIGHT_MODIFY_CHANNELS`):** Can edit names/topics of existing channels.
*   **Change Nickname (`USERRIGHT_LOCKED_NICKNAME` is Off):** Can change their display name.

### Administrative Rights
*   **Kick Users (`USERRIGHT_KICK_USERS`):** Can kick users from channel/server.
*   **Ban Users (`USERRIGHT_BAN_USERS`):** Can ban users from channel/server.
*   **Move Users (`USERRIGHT_MOVE_USERS`):** Can drag and drop users between channels.
*   **Make Operator (`USERRIGHT_OPERATOR_ENABLE`):** Can grant "Channel Operator" status to others without knowing the password.
*   **Upload Files (`USERRIGHT_UPLOAD_FILES`):** Can upload files to channels.
*   **Download Files (`USERRIGHT_DOWNLOAD_FILES`):** Can download files from channels.
*   **Update Server Properties (`USERRIGHT_UPDATE_SERVERPROPERTIES`):** Can edit the Server Name and MOTD.

### Transmission Rights
*   **Transmit Voice (`USERRIGHT_TRANSMIT_VOICE`):** Can use the microphone.
*   **Transmit Video (`USERRIGHT_TRANSMIT_VIDEOCAPTURE`):** Can use the camera.
*   **Transmit Desktop (`USERRIGHT_TRANSMIT_DESKTOP`):** Can share their screen.
*   **Transmit Media Files (`USERRIGHT_TRANSMIT_MEDIAFILE`):** Can stream music files.

---

## ⚙️ ULTIMATE SETTINGS GUIDE

A comprehensive breakdown of every option in the app, divided into 7 categories.

### 1. General (`pref_general`)
*   **Nickname:** Name shown to others. (Default: "NoName").
*   **Language:** Force app user interface language (e.g., Portuguese, English).
*   **Gender:** Icon displayed next to your name (Male/Female/Neutral).
*   **BearWare Logins:** Link your BearWare account (optional).
*   **Move Talking Users:** *Accessibility Tip:* Moves whoever is speaking to the top of the list so you can find them easily.
*   **Show Usernames:** Show login username instead of nickname.
*   **Vibrate:** Tactile feedback when pressing PTT.
*   **Proximity Sensor:** Screens off when holding phone to ear to prevent accidental touches.
*   **Keep Screen On:** Prevents phone from locking while connected.
*   **Show Log Messages:** Display server events (joins/leaves) in the chat view.

### 2. Audio Icons (`pref_soundevents`)
*   **Function:** Toggle sound effects for specific events.
*   **User Join/Left/Logged In/Logged Off:** Tracking user movement.
*   **Server Lost:** Warning beep when connection drops.
*   **TX/RX:** Toggle chirp sounds for transmitting/receiving.
*   **Voice Act Triggered:** Sound when your voice activates the mic.
*   **Intercept:** Sounds when an admin is intercepting your audio/text.
*   **Private/Channel/Broadcast Message:** Notification sounds for chats.
*   **Files Updated:** Sound when a new file is uploaded.

### 3. Text-to-Speech (`pref_tts`)
*   **Speech Engine:** Selects the synthetic voice engine (Google TTS, Samsung TTS, etc.).
*   **Accessibility Volume:** Lowers other audio streams when TTS is reading.
*   **Use Announcements:** Master switch for app announcements.
*   **Events to Announce:**
    *   **Server Login/Logout:** "User X has logged in."
    *   **Channel Join/Leave:** "User Y has joined the channel."
    *   **User Channel Movement:** "User Z left Channel A and joined Channel B."
    *   **Private/Channel/Broadcast Messages:** Reads incoming chat messages aloud.
*   **Transmission Announcements:** Announces "Voice Activated" or "Transmitting".
*   **Subscription Announcements:** Announces "Subscribed to Voice" when entering a channel.

### 4. Server List (`pref_serverlist`)
*   **Show Public Servers:** Displays the list of public servers from the official BearWare directory.
*   **Show Unofficial Servers:** Displays community-run public servers that are not officially verified.

### 5. Connection (`pref_connection`)
*   **Auto Join Root:** Automatically enter the default/root channel immediately after login.
*   **Subscriptions:** Checkboxes to subscribe/unsubscribe from data types. Unchecking these saves bandwidth.
    *   **User Messages:** Receive private chats.
    *   **Channel Messages:** Receive room chats.
    *   **Broadcast Messages:** Receive global alerts.
    *   **Voice/Video/Desktop:** Receive audio/video streams.
    *   **Media Files:** Receive streamed music.

### 6. Sound System (`pref_soundsystem`)
*   **Mute Speakers on TX:** Mutes incoming audio while you are pressing PTT (prevents echoes).
*   **Speakerphone:** Forces audio to the loud speaker instead of the earpiece.
*   **Voice Processing:** Enables Echo Cancellation (AEC) and Noise Suppression (NS). **Highly Recommended**.
*   **Bluetooth Headset:** Experimental support for turning on Bluetooth SCO for high-quality voice.
*   **Media Volume:** Set the default volume for music files relative to voice volume.

### 7. About (`pref_about`)
*   **Version:** Displays current app version (e.g., "TeamTalk Plus v5.x").
*   **Libraries:** Legal notices for open source libraries used (Opus, Speex, OpenSSL).
*   **Translators:** Credits to the translation team.

---

## 📀 TECHNICAL DEEP DIVE

### Audio Codecs
TeamTalk supports multiple audio codecs. You can change these in **Channel Properties**.
*   **OPUS:** The modern standard. High quality, low latency. Recommended for music and voice.
    *   *Variables:* Bitrate (kbps), Application (VoIP users vs Music).
*   **Speex:** Older codec. Lower quality but lower bandwidth usage.
    *   *Variables:* Quality (0-10).
*   **Speex VBR:** Variable Bitrate version of Speex.

### Connection States
When detailed connection info is shown:
*   **Connecting:** Handshaking with the server.
*   **Online:** Fully authenticated and ready.
*   **Unauthorized:** Connected but login failed (Wrong password?).
*   **Offline:** Disconnected.

### Error Codes
Common errors you might see:
*   **MAX_USERS_EXCEEDED:** Server is full.
*   **BANNED:** Your IP or Username is banned.
*   **BAD_LOGIN:** Incorrect username/password.
*   **SERVER_TIMEOUT:** Connection dropped due to lag.

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
*   **"Encryption Error":** The server might require specific SSL certificates that are missing.

---

## 🌍 TRANSLATIONS

**Want to use TeamTalk Plus in your native language?**

If you want to help translate the project, please leave a comment on **any post** in our [Telegram Channel](https://t.me/joaoprojects). We would love your help!

If you spot a missing string or a bad translation, let us know there as well.

---

**TeamTalk Plus** - Empowering Server Admins.  
*Disclaimer: This project is a fork and is not directly affiliated with BearWare.dk.*

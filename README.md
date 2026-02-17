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
*   **Horizontal Tabs (Pages):** The main interface is divided into pages (Files, Channels, Chat, etc.). To switch tabs, perform a **two-finger swipe left or right**. You can also explore by touch near the top to find the tab headers.
*   **Vertical Lists:** Inside a tab, the content (like the channel tree) is a standard list. Scroll up/down using **two fingers**.

### 2. How to Join a Channel
There are two ways to enter a channel:
1.  **Swipe Navigation:** Swipe right until you hear the Channel Name. Swipe right *again* to find the **"Join"** button located next to it. Double-tap to join.
    *   *Note:* If you swipe Left-to-Right, you hear **Name -> Join Button**.
    *   *Note:* If you swipe Right-to-Left, you hear **Join Button -> Name**.
2.  **Long Press Shortcut:** Locate the Channel Name -> **Double-tap and hold the second tap** -> Select **"Join"** from the menu.

### 3. "Long Press" Gesture
Many features are hidden behind a context menu. In TalkBack, the "Long Press" gesture is:
👉 **Double-tap and hold the second tap.**

### 4. Important Buttons
*   **Transmit (PTT):** Bottom-right corner. **Double-tap and hold** to talk.
*   **Voice Activation:** Above the PTT button. Toggles hands-free mode.
*   **Microphone Gain:** Slider in the middle. When focused, use **Volume Up/Down keys** on your phone to adjust the slider precisely.

---

## 📚 THE 12 TABS OF TEAMTALK PLUS

After connecting, swipe left/right to navigate these 12 tabs:

#### 1. FILES
*   **Function:** Shows files in the current channel.
*   **Actions:** Tap to download. Long Press (Double-tap hold) to Delete (requires `USERRIGHT_DELETE_FILE` or Admin).

#### 2. CHANNELS
*   **Function:** The main server tree.
*   **Actions:** See "Context Menu Reference" below.

#### 3. MEDIA STREAMS
*   **Function:** Broadcast music/video to the channel.
*   **Requirements:** Requires `USERRIGHT_UPLOAD_FILE` usually.

#### 4. SERVER MANAGEMENT
*   **Visibility:** Buttons appear based on your specific rights.
*   **Server Properties:** Visible if you have `USERRIGHT_UPDATE_SERVERPROPERTIES`.
*   **User Accounts:** Visible **ONLY** if you are a `USERTYPE_ADMIN`.
*   **Server Banned Users:** Visible if you have `USERRIGHT_BAN_USERS`.
*   **Server Statistics:** Visible to everyone, but the server may refuse to send data if you are not an Admin.

#### 5. GLOBAL
*   **Function:** Send text messages to "Everyone" on the server.

#### 6. EVENT HISTORY
*   **Function:** Logs logins, logouts, and channel joins.

#### 7. CHANNEL MESSAGES
*   **Function:** Text chat for your current channel only.

#### 8. SETTINGS
*   **Function:** Opens the Preferences menu.

#### 9. PRIVATE
*   **Function:** List of active private conversations.

#### 10. CONNECTION STATUS
*   **Function:** Shows Ping, Packet Loss, and UDP connection state.

#### 11. ONLINE USERS
*   **Function:** A flat list of all users. Useful for finding someone quickly without expanding channel trees.

#### 12. MANAGE STATUS
*   **Function:** Set "Away", "Question", or "Online" status and custom status messages.

---

## 👆 CONTEXT MENU REFERENCE (Double-tap and hold)

### On a CHANNEL
*   **Join:** Enter the channel.
*   **New Channel:** Create a sub-channel. *Requires `USERRIGHT_CREATE_TEMPORARY_CHANNEL` or `USERRIGHT_MODIFY_CHANNELS`.*
*   **Channel Properties:** Edit Name, Topic, Password. *Requires `USERRIGHT_MODIFY_CHANNELS` or Channel Operator status.*
*   **Move Users:** Drag users here. *Requires `USERRIGHT_MOVE_USERS`.*

### On a USER
*   **Message:** Private Chat.
*   **Make/Revoke Operator:** Grants Channel Operator.
    *   *If you are Admin or have `USERRIGHT_OPERATOR_ENABLE`:* Toggles immediately.
    *   *If not:* Prompts for the **Channel Operator Password**.
*   **Kick:** Kick from Channel/Server. *Requires `USERRIGHT_KICK_USERS`.*
*   **Ban:** Ban from Channel/Server. *Requires `USERRIGHT_BAN_USERS`.*

---

## ⚙️ ULTIMATE SETTINGS GUIDE

A complete breakdown of every option in the app.

### 1. General (`pref_general`)
*   **Nickname:** Name shown to others.
*   **Language:** Force app language (e.g., Portuguese, English).
*   **Gender:** Icon displayed (Male/Female).
*   **BearWare Logins:** Use a stored BearWare account.
*   **Move Talking Users:** *Accessibility Tip:* Moves whoever is speaking to the top of the list so you can find them easily.
*   **Show Usernames:** Show login username instead of nickname.
*   **Vibrate:** Tactile feedback when pressing PTT.
*   **Proximity Sensor:** Screens off when holding phone to ear.
*   **Keep Screen On:** Prevents phone from locking while connected.
*   **Show Log Messages:** Display server events (joins/leaves) in the chat view.

### 2. Connection (`pref_connection`)
*   **Auto Join Root:** Automatically enter the default channel on login.
*   **Subscriptions:** Checkboxes to subscribe/unsubscribe from data types (Text, Voice, Video, Desktop, Media Files). Turning these off saves bandwidth.

### 3. Sound System (`pref_soundsystem`)
*   **Mute Speakers on TX:** Mutes incoming audio while you are browsing (avoids feedback loops).
*   **Speakerphone:** Forces audio to the loud speaker instead of the earpiece.
*   **Voice Processing:** Enables Echo Cancellation and Noise Suppression. **Highly Recommended**.
*   **Bluetooth Headset:** Experimental support for turning on Bluetooth SCO.
*   **Media Volume:** Set the default volume for music files.

### 4. Text-to-Speech (`pref_tts`)
*   **Speech Engine:** Pick your favorite synthetic voice.
*   **Accessibility Volume:** Lowers other audio when TTS is speaking.
*   **Use Announcements:** Master switch for app announcements.
*   **Events to Announce:**
    *   Server Login/Logout
    *   Channel Join/Leave
    *   User Channel Movement (Tip: Turn off on large servers to avoid noise)
    *   Private/Channel/Broadcast Messages
*   **Transmission Announcements:** Announces when you start/stop transmitting Voice, Video, or Desktop.
*   **Subscription Announcements:** Announces when you subscribe to a stream.

---

## 📥 INSTALLATION

1.  **Download:** Get the APK from the **[Telegram Channel](https://t.me/joaoprojects)**.
2.  **Install:** Open the file on your Android device.
3.  **Permissions:** Allow "Install from Unknown Sources".
4.  **Login:** Enter your server IP, TCP Port, UDP Port, Username, and Password.

---

**TeamTalk Plus** - Empowering Server Admins.  
*Disclaimer: This project is a fork and is not directly affiliated with BearWare.dk.*

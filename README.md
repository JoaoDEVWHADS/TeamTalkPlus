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

## 📱 Project Overview

**TeamTalk Plus** is a specialized, advanced fork of the official [TeamTalk 5 Android Client](https://github.com/BearWare/TeamTalk5). 

The official TeamTalk client is fantastic for voice and video communication, but it lacks the tools needed for **server owners** to manage their communities from a mobile device. If you needed to ban a user, create an account, or change server bandwidth limits, you previously had to rush to a PC.

**TeamTalk Plus solves this problem.** It bridges the gap between the mobile and desktop experience, putting full server administration power in the palm of your hand.

---

## 📚 Detailed Feature Guide

This section explains how to use the advanced features unique to TeamTalk Plus.

### 1. User Account Management (`UserAccountsActivity`)

Stop relying on the desktop client to create accounts for your users. TeamTalk Plus includes a fully functional User Account Manager.

**How to access:**
1.  Connect to your server as an **Administrator**.
2.  Open the drawer menu (hamburger menu).
3.  Select **"User Accounts"**.

**Capabilities:**

*   **List Accounts:** View all registered users on the server.
    *   **Search**: Use the search bar to filter users by username or note.
    *   **Sort**: Toggle between Ascending (A-Z) and Descending (Z-A) order.
    *   **Filter**: The list updates in real-time as you type.

*   **Create New Account:**
    1.  Tap the **"+" (Add)** button.
    2.  **Username:** Enter the login name (e.g., `john_doe`).
    3.  **Password:** Set a secure password.
    4.  **User Type:** Choose between `Default` (Normal user) or `Admin` (Server Administrator).
    5.  **User Rights:** Check specific permissions (e.g., `Can Create Channels`, `Can Upload Files`).
    6.  **Note:** Add a reference note (e.g., "Added on 2024-05-20").
    7.  Tap **Save**.

*   **Edit Existing Account:**
    1.  Long-press on any user in the list.
    2.  Select **Edit**.
    3.  Modify any field (Password, Rights, Notes).
    4.  Tap **Save**.

*   **Delete Account:**
    1.  Long-press on a user.
    2.  Select **Delete**.
    3.  Confirm the action to permanently remove the account.

### 2. Server Properties Management (`ServerPropActivity`)

Need to change the MOTD or adjust bandwidth limits on the fly? You can do that now.

**How to access:**
1.  Connect as Admin.
2.  Menu -> **"Server Properties"**.

**Tabs & Features:**

*   **General Settings:**
    *   **Server Name:** Rename your server instantly.
    *   **MOTD:** Update the "Message of the Day" displayed to users upon connection.
    *   **Max Users:** Set the limit for concurrent connections.
    *   **Max Logins per IP:** Prevent spam by limiting connections from a single IP address.
    *   **Auto-Save:** Toggle whether the server saves changes to the database automatically.

*   **Bandwidth Limits:**
    *   **Voice TX:** Limit voice data usage (Bytes/sec).
    *   **Video TX:** Limit video steam quality.
    *   **Media File TX:** Control how fast files can be streamed.
    *   **Desktop TX:** Limit desktop sharing bandwidth.
    *   **Total TX:** Set a hard cap for the entire server's outgoing traffic.

*   **Abuse Prevention:**
    *   **Max Login Attempts:** Kick users who fail password entry too many times.
    *   **Login Delay:** Enforce a wait time between login attempts to stop brute-force attacks.
    *   **User Timeout:** Automatically kick inactive (ghost) users after X seconds.

*   **Logging:**
    *   Configure what the server logs to its file/console (User logins, Kicks, Channel creation, File transfers, etc.).

**Note:** Changes usually take effect immediately, but some settings may restart the server service depending on your hosting setup.

### 3. Move Users (`MoveUsersActivity`)

Moving users one by one is tedious. The **Move Users** tool allows you to mass-migrate people between channels.

**How to access:**
1.  Connect as Admin/Operator.
2.  Menu -> **"Move Users"**.

**Workflow:**

1.  **Select Users:**
    *   **Filter by Server:** Shows everyone online.
    *   **Filter by Current Channel:** Shows only people in your channel.
    *   **Pick Channel:** Select a specific source channel to grab users from.
    *   **Multi-Select:** Tap users to select them individually, or use "Select All" / "Select None".

2.  **Choose Destination:**
    *   Tap the **"Move Users"** button.
    *   A channel browser will appear.
    *   Navigate through the channel tree (sub-channels supported).
    *   Tap the desired destination channel.

3.  **Execute:**
    *   Confirm the move. All selected users will be instantly teleported to the target channel.

This is incredibly useful for event management, moving AFK users to a lobby, or gathering teams.

### 4. Banned Users Management

TeamTalk Plus splits bans into two categories for easier management.

#### Server Bans (`ServerBannedUsersActivity`)
**Menu -> "Server Bans"**
*   This list shows users banned from the **entire server**.
*   **Information Displayed:**
    *   Nickname & IP Address.
    *   Username (if registered).
    *   Ban Time & Duration.
    *   Banned By (Admin name).
*   **Unban:** Select one or multiple bans and tap "Unban" to restore access.

#### Channel Bans (`ChannelBannedUsersActivity`)
**Context Menu on Channel -> "Channel Bans"**
*   Shows users banned specifically from entering that channel.
*   Useful for private rooms or VIP areas.
*   Admins can view and revoke these bans without needing to be Channel Operators.

### 5. Server Statistics (`ServerStatisticsActivity`)

Get a real-time health check of your server.

**How to access:**
Menu -> **"Server Statistics"**

**Metrics:**
*   **Uptime:** How long the server has been running.
*   **Users:** Current online count / Peak users.
*   **Bandwidth:** current Upload/Download rates.
*   **Audio/Video:** Total audio/video streams active.
*   **Files:** Total file transfers active.

---

## 🐙 Git LFS & Compilation Guide (Read Carefully!)

This project uses **Git Large File Storage (LFS)** to manage big files, specifically the native libraries (`jniLibs.zip`) required for compilation. If you just do a normal `git clone`, you might end up with a small pointer file instead of the actual ZIP file, causing compilation errors.

### 📦 Part 1: Installing Git LFS

You must install Git LFS on your system **BEFORE** cloning the repository.

#### 🪟 Windows Users:

1.  **Download Git:** Make sure you have Git for Windows installed.
2.  **Download LFS:** Go to [git-lfs.github.com](https://git-lfs.github.com/) and download the Windows installer.
3.  **Install:** Run the `.exe` file and follow the wizard.
4.  **Activate:** Open Command Prompt (cmd) or PowerShell and run:
    ```powershell
    git lfs install
    ```
    *If it says "Git LFS initialized", you are ready!*

#### 🐧 Linux Users (Ubuntu/Debian):

1.  **Open Terminal.**
2.  **Install LFS:**
    ```bash
    sudo apt update
    sudo apt install git-lfs
    ```
3.  **Activate:**
    ```bash
    git lfs install
    ```

#### 🍎 macOS Users:

1.  **Use Homebrew:**
    ```bash
    brew install git-lfs
    ```
2.  **Activate:**
    ```bash
    git lfs install
    ```

---

### 📥 Part 2: Cloning & Extracting Libraries

Once Git LFS is installed, follow these steps to get the code and prepare for compilation.

#### Step 1: Clone the Repository
Because you installed LFS, Git will automatically download the large `jniLibs.zip` file along with the source code.

```bash
git clone https://github.com/YourUsername/TeamTalkPlus.git
cd TeamTalkPlus
```

> **Check:** Go to the `libs` or `src/main` folder (depending on where the zip is located). The file `jniLibs.zip` (or similar) should be **several megabytes**, not just a few bytes.

#### Step 2: Extract the Libraries
The Android native libraries (`.so` files) are compressed to save space. You **MUST** unzip them before compiling.

**Where to unzip?**
Look for the `jniLibs.zip` in the root or `app/src/main/` folder. Extract its contents into:
`app/src/main/jniLibs/`

Typically, the structure should look like this after extraction:
```
TeamTalkPlus/
  └── app/
      └── src/
          └── main/
              └── jniLibs/
                  ├── arm64-v8a/
                  │   └── libTeamTalk5-jni.so
                  ├── armeabi-v7a/
                  │   └── libTeamTalk5-jni.so
                  ├── x86/
                  │   └── libTeamTalk5-jni.so
                  └── x86_64/
                      └── libTeamTalk5-jni.so
```

If these files are missing, the app will compile but **crash immediately** on startup.

---

### 🛠️ Part 3: Compiling the APK

Now that you have the source code and the libraries in place, you can compile the APK.

1.  **Open Terminal / Command Prompt** inside the `TeamTalkPlus` folder.
    
2.  **Set up SDK Location (local.properties):**
    Create a file named `local.properties` in the root folder (if it doesn't exist) and add the path to your Android SDK.
    
    *   **Linux:** `sdk.dir=/home/username/Android/Sdk`
    *   **Windows:** `sdk.dir=C:\\Users\\Username\\AppData\\Local\\Android\\Sdk`

3.  **Build Release APK:**
    
    **Linux/Mac:**
    ```bash
    chmod +x gradlew
    ./gradlew assembleRelease
    ```
    
    **Windows:**
    ```powershell
    gradlew.bat assembleRelease
    ```

4.  **Find the APK:**
    Success! Your APK is ready at:
    `app/build/outputs/apk/release/app-release.apk`

---

## 📥 Installation

1.  **Download:** Get the APK from the **[Telegram Channel](https://t.me/joaoprojects)**.
2.  **Install:** Open the file on your Android device.
3.  **Permissions:** Allow "Install from Unknown Sources" if prompted.
4.  **Login:** Enter your server details and login.

---

**TeamTalk Plus** - Empowering Server Admins.  
*Disclaimer: This project is a fork and is not directly affiliated with BearWare.dk.*

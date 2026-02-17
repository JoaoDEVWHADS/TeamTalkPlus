# TeamTalk Plus (Android)

> **Based on TeamTalk 5 Official**  
> *A powerful, feature-rich fork of the TeamTalk 5 Android Client.*

---

## ⚠️ CRITICAL SECURITY WARNING ⚠️

**PLEASE READ BEFORE CLONING OR COMPILING:**

This repository contains a `release.keystore` file. **THIS IS HIGHLY DANGEROUS FOR DISTRIBUTION.**
*   **DO NOT** use this keystore to sign your own modifications if you intend to distribute them.
*   **DO NOT** trust random forks or builds found on the internet that might use this key.
*   **ALWAYS** download the official TeamTalk Plus releases from the only trusted source:

👉 **[Official Telegram Channel: @joaoprojects](https://t.me/joaoprojects)** 👈

Any APK distributed outside this channel using the included key could potentially contain malicious code injected by third parties.

---

## 📱 Project Overview

**TeamTalk Plus** is a specialized fork of the official [TeamTalk 5 Android Client](https://github.com/BearWare/TeamTalk5). While the official client focuses on standard user functionality, TeamTalk Plus bridges the gap between mobile and desktop clients by introducing **advanced server administration features** directly to your Android device.

The goal of this project is to allow server owners and administrators to manage their TeamTalk servers on the go, without needing a PC.

### 🌟 Key Differences & Features

Unlike the standard client, TeamTalk Plus includes comprehensive management tools:

#### 1. User Account Management (`UserAccountsActivity`)
*   **Create & Edit Accounts:** Complete control over server user accounts directly from the app.
*   **Manage Rights:** Grant or revoke user permissions/flags without a desktop client.

#### 2. Advanced Server Administration
*   **Server Properties (`ServerPropActivity`):** View and modify server configurations.
*   **Server Statistics (`ServerStatisticsActivity`):** Monitor server performance, bandwidth usage, and user counts in real-time.
*   **Banned Users Management:** 
    *   **Server Bans (`ServerBannedUsersActivity`):** View, add, or remove bans at the server level.
    *   **Channel Bans (`ChannelBannedUsersActivity`):** Manage bans specific to channels.

#### 3. Enhanced User Management
*   **Move Users (`MoveUsersActivity`):** A dedicated interface to move users between channels easily, a feature often cumbersome in standard mobile interfaces.
*   **Online Users List (`OnlineUsersActivity`):** Enhanced view of connected users.

#### 4. Extended Configuration
*   **Custom Application ID:** Runs as `com.teamtalk.plus`, allowing it to be installed alongside the official TeamTalk app.
*   **Targeted Optimization:** Optimized for Android SDK 34 for a balance of modern features and compatibility.

---

## 🔍 Code Analysis: Plus vs. Official

For developers interested in the changes, TeamTalk Plus modifies the core `dk.bearware.gui` package extensively:

| Feature | Official TeamTalk 5 | TeamTalk Plus |
| :--- | :--- | :--- |
| **Package ID** | `dk.bearware.gui` | `com.teamtalk.plus` |
| **User Admin** | ❌ (View only/Limited) | ✅ **Full Create/Edit/Delete** |
| **Server Admin** | ❌ None | ✅ **Server Props, Stats, Bans** |
| **User Moving** | ⚠️ Basic (Context Menu) | ✅ **Dedicated Activity** |
| **Manifest** | Standard Permissions | Added `VIEW` intent for `tt://` links |

Key file additions in source:
*   `UserAccountsActivity.java` & `UserAccountEditActivity.java`
*   `ServerBannedUsersActivity.java` & `ChannelBannedUsersActivity.java`
*   `MoveUsersActivity.java`
*   `ServerStatisticsActivity.java`

---

## 🛠️ Compilation Instructions (Linux)

Follow these steps to compile TeamTalk Plus on a Linux machine (e.g., Ubuntu/Debian).

### Prerequisites

1.  **Java Development Kit (JDK) 17**:
    ```bash
    sudo apt update
    sudo apt install openjdk-17-jdk
    ```

2.  **Android SDK Command Line Tools**:
    *   Download the latest "commandlinetools-linux" from [Android Studio website](https://developer.android.com/studio#command-tools).
    *   Extract to `~/Android/Sdk`.

3.  **Git**:
    ```bash
    sudo apt install git
    ```

### Build Steps

1.  **Clone the Repository:**
    ```bash
    git clone <repository_url> TeamTalkPlus
    cd TeamTalkPlus
    ```

2.  **Configure `local.properties`:**
    Create a `local.properties` file in the root directory to point to your Android SDK:
    ```bash
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    ```

3.  **Make Gradle Executable:**
    ```bash
    chmod +x gradlew
    ```

4.  **Download Dependencies & Libraries:**
    Ensure you have the `TeamTalk5.jar` and relevant native libraries in the `libs` folder. If they are missing, you may need to copy them from an official TeamTalk SDK distribution or the official repo.

5.  **Compile the APK:**
    Run the following command to build the release version:
    ```bash
    ./gradlew assembleRelease
    ```

6.  **Locate the APK:**
    Once the build completes successfully, your APK will be located at:
    `app/build/outputs/apk/release/app-release.apk`

---

## 📥 Installation

1.  Copy the generated `app-release.apk` to your Android device.
2.  Enable "Install from Unknown Sources" in your device settings.
3.  Tap the APK to install.

> **Note:** TeamTalk Plus can be installed alongside the official TeamTalk client without conflict.

---

*Disclaimer: TeamTalk Plus is an independent fork and is not directly affiliated with BearWare.dk.*

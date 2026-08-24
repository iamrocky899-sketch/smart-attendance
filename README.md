# 📱 SMART ATTENDANCE

> 🚀 A secure, offline-first Android Attendance Management System with real-time synchronization, GPS-based attendance, role-based access control, and Google Sheets backend integration.

---

## 🌟 Overview

**SMART ATTENDANCE** is a modern Android-based attendance management system designed for schools, institutions, and organizations.

The application supports both **Workers** and **Administrators**, with an offline-first architecture that allows attendance to be recorded even without an internet connection.

When internet connectivity becomes available, pending attendance records are automatically synchronized with the cloud backend.

### 🏗️ Architecture

```text
┌───────────────────────────────┐
│       📱 Android App          │
│                               │
│  👨‍💼 Worker       👨‍💻 Admin    │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│       🗄️ Room Database        │
│       Offline-First           │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│     ☁️ Google Apps Script     │
│          REST API             │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│      📊 Google Sheets         │
│                               │
│ Workers                       │
│ Attendance                    │
│ Admins                        │
│ Settings                      │
│ SyncLog                       │
└───────────────────────────────┘

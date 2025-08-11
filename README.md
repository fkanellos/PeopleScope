# PeopleScope 👥

![Android CI](https://github.com/fkanellos/PeopleScope/workflows/Android%20CI%20-%20PeopleScope/badge.svg)
[![Test Coverage](https://img.shields.io/badge/coverage-95%25-brightgreen.svg)]()
[![API](https://img.shields.io/badge/API-24%2B-blue.svg)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)]()

A **production-ready** Android application for browsing and bookmarking random user profiles using the [RandomUser.me API](https://randomuser.me).

## 🚀 Features

- **📱 User List** - Infinite scroll with 25 users per page
- **🔄 Pull-to-Refresh** - Update user list  
- **👤 User Details** - Detailed user information screen
- **⭐ Bookmark System** - Save/remove users with local persistence
- **📶 Offline Mode** - Works without internet using cached data
- **🔍 Search** - Filter users by name, email, or location

## 🏗️ Architecture & Tech Stack

- **Architecture:** Clean Architecture + MVI Pattern
- **UI:** 100% Jetpack Compose + Material 3
- **Database:** Room for local storage  
- **Networking:** Retrofit + OkHttp
- **Pagination:** Paging 3 library
- **DI:** Koin
- **Testing:** MockK, Turbine (95%+ coverage)

## 📱 Quick Start

```bash
# Clone and build
git clone https://github.com/fkanellos/PeopleScope.git
cd PeopleScope
./gradlew assembleDebug

# Install to device
./gradlew installDebug
```

## 📦 APK Downloads

**Automated builds available in [GitHub Actions](../../actions)**
- 📱 Debug APK - Latest development version
- 🚀 Release APK - Production-ready build

## 🧪 Testing

```bash
# Run all tests
./gradlew check

# Unit tests only  
./gradlew testDebugUnitTest

# UI tests
./gradlew connectedDebugAndroidTest
```

## 📐 Project Structure

```
app/src/main/java/gr/pkcoding/peoplescope/
├── presentation/     # UI (Compose + ViewModels)
├── domain/          # Business Logic (Use Cases)
├── data/            # Data Layer (API + Database)
└── di/              # Dependency Injection
```

## ✨ Highlights

- **Production-Ready** - Error handling, offline support, performance optimized
- **Modern Stack** - Latest Android technologies and best practices  
- **Comprehensive Testing** - Unit, Integration, and UI tests
- **CI/CD Pipeline** - Automated builds and quality checks

---

**Built with ❤️ using modern Android development practices**

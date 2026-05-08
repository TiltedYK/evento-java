# ⚡ Evento Java

A full-stack JavaFX desktop application for music event management, featuring a consumer-facing platform and an admin dashboard.

## 👥 Team Members
- Member 1 — Authentication & User Management (meriem lkhedher)
- Member 2 — Events Module (yassine khedher)
- Member 3 — Merch / Shop Module (malek abdel aziz atiya)
- Member 4 — Promos & Partnerships (tassnime rabie)
- Member 5 — Blog (karim amira)

## 🚀 Technologies
- Java 21
- JavaFX
- Maven
- MySQL
- IntelliJ IDEA

## ▶️ How to Run
1. Clone the repository:
```
   git clone https://github.com/malek-scrippy/evento-java
```
2. Set up the database:
   - MySQL running on `localhost:3306`
   - Database name: `6core_cinema`
   - Root user with no password
3. Open the project in IntelliJ IDEA
4. Run the main class

## 📁 Project Structure
```
src/main/java/
├── controllers/     — JavaFX controllers (Login, Dashboard, Events...)
├── models/          — Entity classes (User, Event, Reservation...)
├── services/        — Business logic layer
└── utils/           — Helpers and utilities
```

## ✨ Features

**Consumer Side:**
- Login / Sign-up with Google OAuth 2.0
- Browse and reserve tickets for music events
- Merchandise shop with multi-currency support
- Blog, promotions, and partnership applications
- Built-in music player (Jamendo + iTunes)
- AI chat assistant (Ollama)
- Dynamic themes: Metal / Chill / K-Pop

**Admin Side:**
- Full CRUD for events, products, users, reservations
- Collaboration and partnership management
- Referral analytics with charts
- AI-powered partnership evaluation
- SMTP and OAuth configuration

## 🌐 External APIs Used
- Jamendo — music streaming
- Open-Meteo — weather forecasts
- Exchange Rate API — currency conversion
- Google OAuth — social login
- Ollama — local AI assistant
- Wikipedia — artist biographies

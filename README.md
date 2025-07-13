# 🌍 RoamSafe

**RoamSafe** is a travel intelligence platform designed specifically for **solo female travelers**. We provide real-time safety data, neighborhood ratings, and community-reported safety insights to help women make informed travel decisions.

Built with `Spring Boot`, `Thymeleaf`, and `PostgreSQL`, RoamSafe evolved from a course project into a comprehensive safety platform addressing the growing need for reliable travel safety information.

---

## 🚀 Features

- 🛡️ **Real-time safety intelligence** by city and neighborhood
- 📍 **Auto-detect user location** and pre-fill city search
- 🟢🟡🔴 **Safety zone ratings** (green/yellow/red areas)
- 📝 **Community safety reports** (scams, harassment, positive experiences)
- ✅ **Admin review system** for quality control
- 📊 **Analytics dashboard** (in development)
- 🔄 **Data pipeline** that scrapes Reddit, uses Gemini API, and stores structured safety data
- 🔐 **Upcoming**: Premium subscriptions, 24/7 monitoring, hotel certifications

---

## 🛠️ Technologies Used

- **Java + Spring Boot** – Backend logic and routing
- **Thymeleaf** – Server-side rendering
- **PostgreSQL** – Relational database
- **Reddit + Gemini API** – External data ingestion
- **Browser Geolocation API** – Auto-detect user location
- **OpenStreetMap Nominatim** – Reverse geocoding for city detection
- **Chart.js (planned)** – Scam trend visualization

---

## 🎓 Learning Outcomes

This project supports my learning by helping me:

- Build RESTful web apps using Spring Boot
- Use Thymeleaf for interactive forms and templating
- Design and connect to PostgreSQL databases
- Automate data collection and structuring with Python
- Think critically about usability, safety, and ethics

---

## 🧰 Prerequisites

- Java 11 or later
- Maven
- PostgreSQL (running locally or via Docker)

---

## 📦 Installation

1. Clone this repository:

```bash
git clone https://github.com/yourusername/roamsafe.git
cd roamsafe

2. Configure your database in src/main/resources/application.properties:

```bash
spring.datasource.url=jdbc:postgresql://localhost:5432/yourdbname
spring.datasource.username=yourusername
spring.datasource.password=yourpassword
```

3. Install dependencies and run the project:
```bash
mvn clean install
mvn spring-boot:run
Then visit: http://localhost:8080
```

##  🌱 Roadmap
 Basic form + scam submission flow

 Server-side validations and success feedback

 Reddit scraping → Gemini AI → PostgreSQL pipeline

 Add user authentication (Spring Security)

 Global city filtering and search

 Public dashboard with charts

 Deploy on Fly.io or Railway

## 🧠 Want to Contribute?
Open an issue or send a pull request! All ideas welcome — especially if you're interested in:

Data scraping

NLP structuring

UI/UX

Mapping tools (Leaflet.js, Mapbox, etc.)


“Travel far. Stay safe. Help others do the same.”
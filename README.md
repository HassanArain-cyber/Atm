# VaultBank — Java ATM Web App

Professional responsive ATM demo with a real Java backend. Based on the original ATMSystem.java (included for reference). **Educational only; not a real banking application.**

## Run locally (Java 17+)

```bash
javac ATMWebServer.java
java ATMWebServer
```
Open http://localhost:8080. Demo account: **1001**, PIN: **1234**.

## Share a link

Deploy the repository as a **Docker web service** on a Java/Docker-compatible host (e.g. Render). Use the included Dockerfile. The host will provide a public URL. The frontend and Java backend are served together. A public URL is **not** created by downloading this ZIP; deployment is required.

## OOP topics

Loops (array search and history shifting), operators (balance arithmetic), if-else (validation), methods (deposit/withdraw), arrays (accounts/history), encapsulation (private fields), inheritance (Savings/Current extend base), polymorphism (`type()` override), abstraction (abstract account), constructors (account initialization).

## Limitations

- Server memory only: all accounts and sessions reset after server restart; instances do not share data.
- This is a classroom demo: do not use real names, banking credentials or PINs.
- Public deployment is for testing with fake data only. For production, add persistent database, authentication rate limiting, HTTPS enforcement, CSRF protection, proper authorization and security review.
- Shared demo accounts are accessible to anyone with their credentials.

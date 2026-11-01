# Concurrent Seat Reservation System

A Spring Boot + MySQL backend for booking seats (movies, events) that stays correct when
**hundreds of users try to book the same seat at the same moment**.

Inspired by the problem every ticketing app faces: two people click the same seat,
and only one of them may get it.

## Features

| Feature | What it does |
|---|---|
| **No double booking** | Uses database locking so two users can never both get the same seat |
| **Two locking strategies** | Optimistic (version check) and pessimistic (row lock), selectable per request |
| **Automatic retry** | If an optimistic conflict happens, the booking is retried before failing |
| **Seat holds with expiry** | A seat can be held for 5 minutes during checkout; a scheduled job releases expired holds |
| **Group booking (all or nothing)** | Book several seats in one request; if any seat is taken, none are booked |
| **Duplicate-click protection** | Each checkout sends a `requestId`; sending it again returns the original booking instead of booking twice |
| **Concurrency tests** | 500 simultaneous users try to book the same seat; exactly one succeeds |

## Seat life cycle

```
AVAILABLE --hold--> HELD --book--> BOOKED
    ^                 |
    +---- expires ----+
```

## Tech stack

Java 17, Spring Boot 3, Spring Data JPA (Hibernate), MySQL, JUnit 5, H2 (tests only)

## Project structure

```
src/main/java/com/reshma/seatbooking
├── controller   SeatController, BookingController  (REST endpoints)
├── service      BookingService                     (duplicate check + retry)
│                BookingTransactionService          (one booking attempt in one transaction)
│                SeatService                        (list + hold seats)
│                HoldExpiryScheduler                (releases expired holds)
├── repository   SeatRepository, BookingRepository  (database access + locking queries)
├── entity       Seat, Booking, SeatStatus
├── dto          BookingRequest, BookingResponse, LockStrategy
└── exception    GlobalExceptionHandler + custom exceptions
```

## Running it

1. Install Java 17 and MySQL 8.
2. Set your MySQL username and password (defaults are `root` / `root`):
   ```
   set DB_USERNAME=root
   set DB_PASSWORD=yourpassword
   ```
3. Run `SeatBookingApplication` from IntelliJ, or `mvn spring-boot:run`.
   The database `seat_booking` and 10 sample seats are created automatically.

## API

| Method | URL | Description |
|---|---|---|
| GET | `/seats?show=Inception` | List seats of a show |
| GET | `/seats/{id}` | Get one seat |
| POST | `/seats/{id}/hold?user=alice` | Hold a seat for 5 minutes |
| POST | `/bookings` | Book one or more seats |

Booking request:

```json
{
  "requestId": "b3f1c2e0-checkout-1",
  "user": "alice",
  "seatIds": [1, 2],
  "strategy": "PESSIMISTIC"
}
```

Responses: `201` booked, `200` same requestId already booked (returns the original booking),
`409` seat not available, `404` seat not found, `400` invalid request.

## Running the tests

```
mvn test
```

Tests use an in-memory H2 database, so MySQL is not needed.
`ConcurrentBookingTest` fires 500 parallel booking requests at one seat for each locking
strategy and checks that exactly one booking is created. It prints a line like:

```
OPTIMISTIC: 500 users -> 1 booked, 499 rejected, ... ms
PESSIMISTIC: 500 users -> 1 booked, 499 rejected, ... ms
```

## Design decisions

**Optimistic vs pessimistic locking**
- *Optimistic*: everyone reads freely; on save, Hibernate checks the `version` column.
  If someone else saved first, the update fails and is retried. Fast when conflicts are rare.
- *Pessimistic*: the seat rows are locked while reading (`SELECT ... FOR UPDATE`), so other
  requests wait in line. Better when many users fight for the same few seats.

**Why retry lives in a separate class**
`@Transactional` works through a Spring proxy, so calling a transactional method from inside
the same class skips the transaction. `BookingService` (retry loop) calls
`BookingTransactionService` (one transactional attempt), so every retry gets a fresh transaction.

**Avoiding deadlocks in group bookings**
Seats are always locked in ascending id order. Two users booking `[1, 2]` and `[2, 1]`
lock seat 1 first, so they can never wait on each other forever.

**Why an expired hold is also checked at booking time**
The cleanup job runs every 30 seconds. Treating an expired hold as available during booking
means a seat is never stuck in the gap between two runs.

# Concurrent Seat Booking System

Prevents double booking when many users try to book the same seat at the same time.

## Approach
- Reproduce the race condition (no locking)
- Fix using Optimistic Locking (JPA @Version)
- Fix using Pessimistic Locking (SELECT ... FOR UPDATE)
- Verify with a multithreaded JUnit test (100 parallel requests on one seat)

## Tech Stack
Java 17, Spring Boot, Spring Data JPA, MySQL, JUnit

## Progress
- [ ] Seat entity, repository, REST endpoints
- [ ] No-lock version (race condition demo)
- [ ] Optimistic & pessimistic locking
- [ ] 100-thread concurrency test
- [ ] Results comparison table

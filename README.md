# Payment Service

A production-oriented Spring Boot Payment Service designed to explore
reliability and distributed-systems concepts in a realistic payment workflow.

This project was built as an architecture-learning project, with a strong
focus on correctness under concurrent requests, failures, retries, and
distributed ownership.

---

## Architecture

```text
                         Client
                           |
                           v
                  +------------------+
                  |  Payment Service |
                  +--------+---------+
                           |
              +------------+------------+
              |                         |
              v                         v
         +---------+              +-------------+
         |  Redis  |              | PostgreSQL  |
         +---------+              +-------------+
              |                         |
              |                         +------------------+
              |                         |                  |
              |                         v                  v
              |                  Payment Table       Outbox Table
              |
              +-- Lease / Ownership
              +-- Lease Renewal
              +-- Fencing Token
              |
              v
         Distributed
         Coordination

                         Outbox
                           |
                           v
                         Kafka
                           |
                           v
                  payment.created
                           |
                           v
                  Kafka Consumer
                           |
                           v
                  Processed Event DB
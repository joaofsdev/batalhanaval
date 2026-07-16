# ADR: Single-Thread ScheduledExecutorService for Disconnection Timeouts

## Status

Accepted

## Context

The `DisconnectionService` uses a `ScheduledExecutorService` to manage grace period timeouts when players disconnect from active games. When a player disconnects, a delayed task is scheduled to apply a loss or cancel the game if the player doesn't reconnect within the grace period.

We needed to decide on the threading model for this scheduler.

## Decision

We use `Executors.newSingleThreadScheduledExecutor()` — a single-thread pool.

## Rationale

1. **Sufficient for expected load**: The application targets fewer than 100 concurrent games. Even in the worst case (all players disconnecting simultaneously), 100 scheduled tasks with 30–60s delays are trivially handled by a single thread.

2. **Sequential execution safety**: A single thread guarantees that timeout callbacks execute sequentially, eliminating potential race conditions when multiple timeouts fire concurrently for the same game (e.g., mutual disconnection edge cases).

3. **Simplicity**: No thread pool sizing, no contention concerns, no need for additional synchronization beyond the `ConcurrentHashMap` used for `pendingTimeouts`.

4. **Low overhead**: Each timeout task is short-lived (a DB read, status update, and WebSocket notification). Blocking time per task is negligible.

## Consequences

- If the application scales to thousands of concurrent games, the single thread could become a bottleneck. In that scenario, upgrade to `Executors.newScheduledThreadPool(N)` with appropriate synchronization.
- If a task throws an uncaught exception, the thread is replaced automatically by the executor, but the specific task is lost. This is mitigated by try-catch blocks in all scheduled callbacks.

## Upgrade Path

Replace `newSingleThreadScheduledExecutor()` with `newScheduledThreadPool(Runtime.getRuntime().availableProcessors())` and ensure all shared state access in callbacks remains thread-safe.

# System-Wide Master Seed Dataset Specification

---

## 1. Members
* **10 Diamond Members**
* **15 Gold Members**
* **15 Silver Members**

---

## 2. Guests

### A. Diamond Member Guests (10 Guests)
* 0 Strikes x 8
* 1 Strike x 2

### B. Gold Member Guests (15 Guests)
* 0 Strikes x 11
* 1 Strike x 2
* 2 Strikes x 2

### C. Silver Member Guests (15 Guests)
* 0 Strikes x 10
* 1 Strike x 2
* 2 Strikes x 3

### D. Non-Member Guests (25 Guests)
* 0 Strikes x 20
* 1 Strike x 3
* 2 Strikes x 2

---

## 3. Rooms

### A. Luxury Rooms (10 Rooms)
* VACANT_CLEAN x 4
* OCCUPIED x 4
* DIRTY x 1
* CLEANING x 1

### B. Suite Rooms (10 Rooms)
* VACANT_CLEAN x 4
* OCCUPIED x 4
* DIRTY x 1
* INSPECTED x 1

### C. Standard Rooms (10 Rooms)
* VACANT_CLEAN x 4
* OCCUPIED x 4
* DIRTY x 1
* CLEANING x 1

---

## 4. Reservations

### A. Standard Queue Waiting Reservations (10 Reservations - Non-Members)
* 4 Luxury room waiting (Queue wait time range: 10m – 30m)
* 3 Suite room waiting (Queue wait time range: 12m – 25m)
* 3 Standard room waiting (Queue wait time range: 15m – 35m)

### B. Advance Reserved Reservations (10 Reservations)
* 3 Luxury room reserved (Diamond x 1, Gold x 1, Non-member x 1)
* 4 Suite room reserved (Gold x 1, Silver x 2, Non-member x 1)
* 3 Standard room reserved (Silver x 1, Non-member x 2)

### C. Standard Active Checked-In Stays (5 Reservations - Non-Members)
* 2 Luxury room checked-in (Queue wait time range: 15m – 25m)
* 2 Suite room checked-in (Queue wait time range: 18m – 30m)
* 1 Standard room checked-in (Queue wait time range: 20m – 25m)

### D. Standard Completed Checked-Out Stays (5 Reservations - Non-Members)
* 2 Luxury room checked-out (Queue wait time range: 12m – 22m)
* 2 Suite room checked-out (Queue wait time range: 15m – 28m)
* 1 Standard room checked-out (Queue wait time range: 20m – 25m)

### E. VIP Priority Waiting Reservations (18 Reservations)
* **6 Luxury room waiting:**
  * Diamond x 3 (Queue wait time range: 5m – 12m — SLA Met)
  * Diamond x 1 (Queue wait time range: 35m – 45m — Boiling & SLA Violated)
  * Gold x 2 (Queue wait time range: 15m – 20m — SLA Met)
* **7 Suite room waiting:**
  * Gold x 2 (Queue wait time range: 10m – 22m — SLA Met)
  * Gold x 1 (Queue wait time range: 40m – 50m — Boiling & SLA Violated)
  * Silver x 4 (Queue wait time range: 15m – 40m — SLA Met)
* **5 Standard room waiting:**
  * Gold x 1 (Queue wait time range: 18m – 22m — SLA Met)
  * Gold x 1 (Queue wait time range: 42m – 48m — Boiling & SLA Violated)
  * Silver x 3 (Queue wait time range: 20m – 44m — SLA Met)

### F. VIP Room Holding Bay Allocated Reservations (8 Reservations)
* **3 Luxury room allocated:**
  * Diamond x 2 (Queue wait time range: 8m – 12m; Holding time used range: 4m – 8m; Allowed grace: 10m)
  * Gold x 1 (Queue wait time range: 15m – 22m; Holding time used range: 10m – 14m; Allowed grace: 15m)
* **3 Suite room allocated:**
  * Diamond x 1 (Queue wait time range: 10m – 14m; Holding time used range: 5m – 8m; Allowed grace: 10m)
  * Gold x 2 (Queue wait time range: 20m – 28m; Holding time used range: 10m – 14m; Allowed grace: 15m)
* **2 Standard room allocated:**
  * Silver x 2 (Queue wait time range: 28m – 40m; Holding time used range: 14m – 18m; Allowed grace: 20m)

### G. VIP Active Checked-In Stays (12 Reservations)
* **4 Luxury room checked-in:**
  * Diamond x 3 (Queue wait time range: 8m – 14m; Holding time used range: 3m – 6m; Allowed grace: 10m — SLA Met)
  * Gold x 1 (Queue wait time range: 20m – 25m; Holding time used range: 8m – 12m; Allowed grace: 15m — SLA Met)
* **5 Suite room checked-in:**
  * Gold x 2 (Queue wait time range: 18m – 25m; Holding time used range: 8m – 12m; Allowed grace: 15m — SLA Met)
  * Silver x 3 (Queue wait time range: 20m – 40m; Holding time used range: 12m – 17m; Allowed grace: 20m — SLA Met)
* **3 Standard room checked-in:**
  * Gold x 1 (Queue wait time range: 20m – 26m; Holding time used range: 8m – 11m; Allowed grace: 15m — SLA Met)
  * Silver x 2 (Queue wait time range: 30m – 42m; Holding time used range: 14m – 18m; Allowed grace: 20m — SLA Met)

### H. VIP Completed Checked-Out Stays (6 Reservations)
* **2 Luxury room checked-out:**
  * Diamond x 2 (Queue wait time range: 8m – 12m; Holding time used range: 4m – 6m; Allowed grace: 10m — SLA Met)
* **2 Suite room checked-out:**
  * Gold x 1 (Queue wait time range: 20m – 25m; Holding time used range: 8m – 12m; Allowed grace: 15m — SLA Met)
  * Silver x 1 (Queue wait time range: 35m – 40m; Holding time used range: 14m – 18m; Allowed grace: 20m — SLA Met)
* **2 Standard room checked-out:**
  * Silver x 2 (Queue wait time range: 32m – 42m; Holding time used range: 12m – 17m; Allowed grace: 20m — SLA Met)

### I. VIP Penalty No-Show Reservations (6 Reservations)
* **2 Diamond no-show:**
  * 1 Strike x 2 (Queue wait time range: 8m – 12m; Holding time used: 10m [Max Grace]; Allowed grace: 10m — Evicted: NO)
* **2 Gold no-show:**
  * 2 Strikes x 2 (Queue wait time range: 18m – 24m; Holding time used: 15m [Max Grace]; Allowed grace: 15m — Evicted: YES)
* **2 Silver no-show:**
  * 2 Strikes x 2 (Queue wait time range: 28m – 36m; Holding time used: 20m [Max Grace]; Allowed grace: 20m — Evicted: YES)

### J. Standard No-Show Reservations (4 Reservations)
* 2 Standard no-show (Non-member x 2)
* 1 Suite no-show (Non-member x 1)
* 1 Luxury no-show (Silver x 1)
* Spread over today, yesterday and 3 days ago, so the report date filters return different sets.

---

## 5. Housekeeping Staff & Tasks

* 6 Housekeeping Staff (2 per shift: MORNING, AFTERNOON, NIGHT)
* DIRTY room assignments x 2 (staff assigned to a task, hasn't started cleaning yet — task status ASSIGNED, room status DIRTY)
* CLEANING room assignments x 2 (staff assigned AND cleaning started — task status IN_PROGRESS, room status CLEANING)

---

## 6. Booking Settings (booking_settings.dat)

The seed writes no settings file, so the factory defaults apply and the counts above are calibrated
to them.

* Queue capacity 8 per room type, expansion off
* Hold grace 15m, max strikes 3, requeue on lapse on
* VIP bypass enforced, override allowed
* Same day advance booking off, lead days 365
* One place per line, but a guest may stand in several lines

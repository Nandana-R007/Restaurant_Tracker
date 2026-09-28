# Restaurant Table Reservation and Order Tracker
Spring Boot 3 + MySQL backend (plus a tiny plain HTML/JS page) for a campus restaurant.

## 1. Problem & solution
Reservations are taken by phone and orders written on paper slips, causing mix-ups. This backend manages **reservations by time slot**, tracks **orders (with many items) per table**, **bills** them, and **frees the table**.

## 2. Tech
Java 17, Spring Boot 3.3, Maven, Spring Web, Spring Data JPA (Hibernate), Jakarta Validation, MySQL, Postman. No security / Docker / microservices.

## 3. Run
1. Install Java 17, Maven, MySQL. Start MySQL.
2. Open `src/main/resources/application.properties` and **replace `YOUR_MYSQL_USERNAME` and `YOUR_MYSQL_PASSWORD`** with your MySQL login. (`createDatabaseIfNotExist=true` creates `restaurant_db` automatically.)
3. In the project folder: `mvn spring-boot:run`
4. Frontend: open **http://localhost:8080** (served from `src/main/resources/static/index.html`, same origin, so no CORS setup). APIs: `http://localhost:8080/api/...`
5. Tax is configurable: `restaurant.tax.rate=0.05` (5%).

## 4. Architecture
```
Client (Postman / index.html)
   -> Controller   (HTTP only: @RequestBody, @Valid, @PathVariable)   thin
   -> Service      (ALL business rules, @Transactional)
   -> Repository   (JpaRepository, database access only)
   -> Hibernate/JPA -> MySQL
```
Layers only talk downward. Controllers never touch repositories; entities are returned to clients only through DTOs (records) so there is no infinite JSON recursion.

## 5. Database (created automatically by `ddl-auto=update`)
| Table | PK | Foreign keys |
|---|---|---|
| restaurant_tables | id | - |
| reservations | id | table_id -> restaurant_tables.id |
| orders | id | table_id -> restaurant_tables.id, reservation_id -> reservations.id |
| order_items | id | order_id -> orders.id |
| bills | id | order_id -> orders.id (UNIQUE = one-to-one) |
```
RestaurantTable 1 --- * Reservation      (@ManyToOne in Reservation)
RestaurantTable 1 --- * Order            (@ManyToOne in Order)
Reservation     1 --- * Order            (@ManyToOne in Order)
Order           1 --- * OrderItem        (@OneToMany(mappedBy="order", cascade=ALL, orphanRemoval=true))
Order           1 --- 1 Bill             (@OneToOne in Bill)
```
"Orders" is the SQL name because `order` is a reserved word. Enums are stored as text (`EnumType.STRING`).

## 6. Business rules & where they live
| Rule | Location |
|---|---|
| Table must be FREE (walk-in, seating) -> "Table T2 is currently occupied." | `ReservationService.checkFree` |
| No overlap: `existing.start < newEnd AND existing.end > newStart`, only CONFIRMED/SEATED block | `ReservationService.checkNoOverlap` + repo method `existsByTableIdAndReservationDateAndStatusIn...` |
| Capacity -> "Table capacity is insufficient for this party size." | `checkCapacity` |
| Order only if table OCCUPIED and a SEATED reservation exists | `OrderService.create` |
| Total = sum(qty x unitPrice) from DB items, tax = subtotal x rate (HALF_UP, 2 dp) | `BillService.generate` |
| After billing: Order BILLED, Reservation COMPLETED, Table FREE | `BillService.generate` |
| Order status one step at a time PLACED->PREPARING->READY->SERVED | `OrderService.updateStatus` |

Design note: an *advance* reservation is checked for capacity, valid time and overlap; the FREE/OCCUPIED check is applied when the customer is **seated** (check-in) and for **walk-ins**, otherwise a table busy now could never be booked for tonight.

## 7. API endpoints
| Method | URL | Purpose |
|---|---|---|
| POST | /api/tables | create table |
| GET | /api/tables, /api/tables/{id}, /api/tables/available | list / one / FREE tables |
| PUT | /api/tables/{id} | update number/capacity |
| DELETE | /api/tables/{id} | delete (not if occupied / has history) |
| POST | /api/reservations | reserve |
| POST | /api/reservations/walk-in | seat walk-in now |
| GET | /api/reservations (?tableId=&date=), /active, /{id} | read |
| PUT | /api/reservations/{id}/status | `{"status":"SEATED"}` etc. |
| DELETE | /api/reservations/{id} | cancel (CONFIRMED only) |
| POST | /api/orders | place order |
| GET | /api/orders, /api/orders/{id}, /api/tables/{tableId}/orders | read |
| PUT | /api/orders/{id}/status | next status |
| POST | /api/bills/order/{orderId} | generate bill |
| GET | /api/bills/{id}, /api/orders/{orderId}/bill | read bill |

## 8. Postman sequence (IDs assume a fresh database; Content-Type: application/json)
1. **Create tables** `POST /api/tables` three times: `{"tableNumber":"T1","capacity":2}`, `{"tableNumber":"T2","capacity":4}`, `{"tableNumber":"T3","capacity":6}` -> 201, status FREE.
2. **GET /api/tables** -> 3 tables, all FREE.
3. **Reserve T2** `POST /api/reservations`
   `{"customerName":"Priya","phone":"9876543210","reservationDate":"2026-09-28","startTime":"19:00","endTime":"20:00","partySize":4,"tableId":2}` -> 201, `"id":1,"status":"CONFIRMED"`
4. **Overlap** same body with `"startTime":"19:30","endTime":"20:30"` -> 400 `BUSINESS_RULE_VIOLATION` "Table T2 already has a reservation that overlaps this time window." (Back-to-back `20:00-21:00` succeeds.)
5. **Another table** `tableId":3, "customerName":"Ravi", partySize 5` -> 201 (id 2).
6. **Seat** `PUT /api/reservations/1/status` `{"status":"SEATED"}` -> SEATED; `GET /api/tables/2` -> OCCUPIED.
7. **Order on a FREE table** `POST /api/orders` `{"tableId":1,"items":[{"itemName":"Tea","quantity":1,"unitPrice":20}]}` -> 400 "Cannot place order because the table is not currently seated."
8. **Order on seated T2** `POST /api/orders`
   `{"tableId":2,"reservationId":1,"items":[{"itemName":"Pizza","quantity":2,"unitPrice":150},{"itemName":"Coffee","quantity":2,"unitPrice":30},{"itemName":"Sandwich","quantity":1,"unitPrice":80}]}` -> 201, order id 1, PLACED.
9. **GET /api/orders/1** -> three items with `lineTotal` 300.00 / 60.00 / 80.00.
10. **Status** `PUT /api/orders/1/status`: PREPARING, then READY, then SERVED (skipping a step -> 400).
11. **Bill** `POST /api/bills/order/1` (no body) ->
```json
{"billId":1,"orderId":1,"tableNumber":"T2","items":[...],"subtotal":440.00,"tax":22.00,"total":462.00,"billedAt":"2026-09-28T19:55:10","paymentStatus":"PAID"}
```
12. **Verify** `GET /api/orders/1` -> BILLED; `GET /api/reservations/1` -> COMPLETED; `GET /api/tables/2` -> FREE.
13. **Book T2 again** for 20:00-21:00 (or any free slot) -> 201.

Error format: `{"timestamp":"...","status":400,"error":"VALIDATION_ERROR","message":"Validation failed","fieldErrors":{"partySize":"must be greater than 0"}}`; 404 uses `NOT_FOUND`.

## 9. Request flow: POST /api/reservations
Postman -> `ReservationController.create` -> Jackson turns JSON into `ReservationRequest` (`@RequestBody`) -> `@Valid` checks `@NotBlank/@NotNull/@Positive` (fail => `GlobalExceptionHandler` returns 400 with fieldErrors) -> `ReservationService.create`: start<end -> date not past -> table exists (404) -> capacity -> overlap -> `ReservationRepository.save` -> Hibernate generates `INSERT` -> MySQL -> entity -> `ReservationResponse` -> JSON 201.

**POST /api/orders:** `@Valid` (items non-empty, qty/price positive) -> `OrderService.create`: table exists -> table OCCUPIED -> SEATED reservation found -> build `Order` + `OrderItem`s -> `orderRepository.save` (cascade inserts items) -> `OrderResponse`.

**POST /api/bills/order/{orderId}:** `@PathVariable orderId` -> `BillService.generate`: find order -> not billed -> `orderItemRepository.findByOrderId` -> loop: `unitPrice x quantity` -> subtotal -> tax -> total -> save Bill -> Order BILLED -> Reservation COMPLETED -> Table FREE (one `@Transactional`, so all-or-nothing) -> `BillResponse`.

## 10. Annotations
**JPA:** `@Entity` class = DB table. `@Table(name=..)` chooses table name. `@Id` primary key. `@GeneratedValue(IDENTITY)` MySQL auto-increment. `@Column` column settings (unique, precision). `@Enumerated(STRING)` store enum name. `@ManyToOne` many rows point to one (holds the foreign key). `@OneToMany(mappedBy="order")` the other side; `mappedBy` names the field owning the FK; `cascade=ALL` saving an Order saves its items; `orphanRemoval` deletes removed items. `@OneToOne` one row <-> one row (Order-Bill). `@JoinColumn(name="table_id")` the FK column name.
**Spring:** `@SpringBootApplication` starts app + component scan. `@Service` business-logic bean. `@Repository` data-access bean. `@RestController` class returning JSON. `@RequestMapping("/api/tables")` base URL. `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping` HTTP method + sub-URL. `@Transactional` one DB transaction per method. `@Value` reads a property. `@ResponseStatus` sets HTTP code.
**Request binding:** `@PathVariable` = value inside URL path (`/tables/{id}`); `@RequestParam` = query string (`?date=2026-09-28`), good for optional filters; `@RequestBody` = JSON body converted to a Java object.
**Validation:** `@Valid` triggers validation of the body; `@NotNull` value present; `@NotBlank` non-null and not just spaces (Strings); `@Positive` > 0; `@Size(min,max)` length; `@NotEmpty` list not empty.
**Errors:** `@RestControllerAdvice` one class handling exceptions for all controllers; `@ExceptionHandler(X.class)` method run when X is thrown.
**`JpaRepository<RestaurantTable, Long>`:** first type = entity managed, second = type of its `@Id`. Gives `save, findById, findAll, deleteById, existsById` free. Method names such as `findByStatus` become SQL automatically.

## 11. @OneToMany vs @ManyToOne vs @OneToOne
`@ManyToOne` (Reservation -> Table): many reservations, one table; the FK column lives here. `@OneToMany` (Order -> items): one order, many items; the inverse side of a ManyToOne. `@OneToOne` (Bill -> Order): exactly one bill per order (unique FK).

## 12. Viva Q&A
- **Why DTOs?** Avoid bidirectional recursion, hide internals, control the JSON shape.
- **Why logic in services?** Controllers stay thin; rules are reusable and testable; DB constraints alone cannot give friendly messages.
- **Why BigDecimal?** double has rounding errors for money.
- **Overlap condition?** `existing.start < newEnd && existing.end > newStart`; 7-8 vs 8-9 doesn't overlap because `8:00 > 8:00` is false.
- **Why not trust client total?** A client could send a wrong/low total; the server recomputes from stored items.
- **Why @Transactional on billing?** Bill + order + reservation + table updates succeed or fail together.
- **Why `orders` table name?** `ORDER` is reserved in SQL.
- **`ddl-auto=update`?** Hibernate creates/updates tables from entities.
- **Constructor injection?** Immutable dependencies, easy testing, no `@Autowired` field.

## 13. Known limits / future ideas
A bill closes the sitting, so bill once the customer is done (one order per sitting is the intended flow). Ideas: status-change notifications, pagination/sorting on lists.

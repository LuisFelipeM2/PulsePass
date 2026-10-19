| # | Método | Ruta | Controller | Body / parámetros | HTTP éxito |
|---|---|---|---|---|---|
| 1 | GET | /api/venues/{code} | VenueController | path: code | 200 |
| 2 | GET | /api/venues/active | VenueController | — | 200 |
| 3 | GET | /api/artists/{id} | ArtistController | path: id | 200 |
| 4 | GET | /api/artists/by-stage-name?stageName=... | ArtistController | query: stageName | 200 |
| 5 | GET | /api/artists/active | ArtistController | — | 200 |
| 6 | POST | /api/events | EventController | body: CreateEventRequest | 201 |
| 7 | GET | /api/events/{eventCode} | EventController | path: eventCode | 200 |
| 8 | GET | /api/events/published | EventController | — | 200 |
| 9 | PATCH | /api/events/{eventCode}/publish | EventController | path: eventCode | 200 |
| 10 | POST | /api/events/{eventCode}/artists/{artistId} | EventController | path: eventCode, artistId | 200 |
| 11 | GET | /api/events/by-artist?stageName=... | EventController | query: stageName | 200 |
| 12 | POST | /api/users | UserController | body: RegisterUserRequest | 201 |
| 13 | GET | /api/users/by-email?email=... | UserController | query: email | 200 |
| 14 | GET | /api/users/by-username?username=... | UserController | query: username | 200 |
| 15 | POST | /api/tickets | TicketController | body: PurchaseTicketRequest | 201 |
| 16 | GET | /api/tickets/{ticketCode} | TicketController | path: ticketCode | 200 |
| 17 | GET | /api/tickets/by-user?email=... | TicketController | query: email | 200 |
| 18 | GET | /api/events/{eventCode}/tickets/paid | TicketController | path: eventCode | 200 |
| 19 | PATCH | /api/tickets/{ticketCode}/cancel | TicketController | path: ticketCode | 200 |
| 20 | PATCH | /api/tickets/{ticketCode}/use | TicketController | path: ticketCode | 200 |
| 21 | POST | /api/auth/login | AuthController | body: LoginRequest | 200 |
| 22 | GET | /api/auth/me | AuthController | header: Authorization Bearer | 200 |
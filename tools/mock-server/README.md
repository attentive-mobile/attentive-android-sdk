# Attentive Inbox Mock Server

A local Express server that simulates the Attentive inbox API for SDK development and testing. Includes a browser dashboard for managing messages and simulating network conditions.

## Quick Start

```bash
npm install
npm start
```

The server starts on port 3000 (configurable via `PORT` env var).

```bash
PORT=8080 npm start          # custom port
DELAY=2000 npm start         # start with 2s response delay
```

## Accessing the Server

| Context | URL |
|---------|-----|
| Browser dashboard | http://localhost:3000 |
| Android emulator | http://10.0.2.2:3000 |
| iOS simulator | http://localhost:3000 |
| Physical device (Android/iOS) | http://<your-local-ip>:3000 |

The startup log prints the correct URLs for your machine.

**Platform notes:**
- **Android emulator** uses `10.0.2.2` to reach the host machine's localhost.
- **iOS simulator** shares the host's network stack, so `localhost` works directly.
- **Physical devices** (both platforms) must be on the same Wi-Fi network as your machine. Use your machine's local IP (printed at startup).

## API Endpoints

### GET /inbox/messages

Fetch paginated messages.

**Query Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| offset | int | 0 | Number of messages to skip |
| limit | int | 20 | Max messages to return |

**Response:**
```json
{
  "messages": [
    {
      "id": "msg_001",
      "title": "Welcome to Attentive!",
      "body": "Thanks for joining us. Check out our latest offers.",
      "timestamp": 1714800000000,
      "isRead": false,
      "imageUrl": "https://picsum.photos/seed/inbox1/400/300",
      "actionUrl": "https://example.com/offer/1",
      "style": "Small"
    }
  ],
  "unreadCount": 75,
  "hasMoreMessages": true,
  "nextOffset": 20
}
```

### POST /inbox/messages

Add a new message.

**Request Body:**
```json
{
  "title": "Sale Alert",
  "body": "50% off everything!",
  "imageUrl": "https://example.com/image.jpg",
  "actionUrl": "https://example.com/sale",
  "style": "Small"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | string | no | Message title (default: "New Message") |
| body | string | no | Message body (default: "") |
| imageUrl | string | no | Image URL |
| actionUrl | string | no | Tap action URL |
| style | string | no | "Small" or "Large" (default: "Small") |

**Response:** `201` with the created message object.

### PATCH /inbox/messages/:id

Update a message's read status.

**Request Body:**
```json
{
  "isRead": true
}
```

**Response:** `204` No Content

### DELETE /inbox/messages/:id

Soft-delete a message.

**Response:** `204` No Content

### POST /inbox/messages/reset

Reset inbox to 100 freshly generated messages.

**Response:**
```json
{
  "message": "Inbox reset",
  "count": 100
}
```

### GET /settings

Get current server settings.

**Response:**
```json
{
  "delayMs": 0
}
```

### POST /settings

Update server settings (e.g., simulate network delay).

**Request Body:**
```json
{
  "delayMs": 3000
}
```

**Response:**
```json
{
  "delayMs": 3000
}
```

## Network Delay Simulation

You can simulate slow network conditions to test loading states:

1. **At startup:** `DELAY=3000 npm start`
2. **At runtime via API:** `curl -X POST localhost:3000/settings -H 'Content-Type: application/json' -d '{"delayMs": 3000}'`
3. **Via the dashboard:** Use the "Delay" dropdown in the top-right of the actions bar.

The delay applies to all `/inbox` routes.

## Dashboard

Open http://localhost:3000 in a browser to:

- View all messages with read/unread status
- Add new messages
- Mark messages as read/unread
- Delete messages
- Reset the inbox
- Adjust network delay

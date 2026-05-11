const express = require('express');
const path = require('path');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

let delayMs = parseInt(process.env.DELAY) || 0;

function simulateDelay(req, res, next) {
  if (delayMs > 0) {
    console.log(`  ⏳ Delaying ${delayMs}ms...`);
    setTimeout(next, delayMs);
  } else {
    next();
  }
}

app.use('/inbox', simulateDelay);

const STYLES = ['small', 'large'];

const SAMPLE_IMAGES = [
  'https://as1.ftcdn.net/v2/jpg/03/98/30/92/1000_F_398309275_84cKyqzV2RLTbYmBtt0dzpZkEvqapPZo.jpg',
  'https://picsum.photos/seed/inbox1/400/300',
  'https://picsum.photos/seed/inbox2/400/300',
  'https://picsum.photos/seed/inbox3/400/300',
  'https://picsum.photos/seed/inbox4/400/300',
];

const SAMPLE_MESSAGES = [
  { title: 'Welcome to Attentive!', body: 'Thanks for joining us. Check out our latest offers.' },
  { title: 'New Sale Alert', body: '50% off on all items this weekend!' },
  { title: 'Your Order Has Shipped', body: 'Your order #12345 is on its way!' },
  { title: 'Your cart is waiting', body: 'Pick up where you left off!' },
  { title: 'Flash Sale Ending Soon', body: 'Last chance to save big on summer essentials.' },
  { title: 'New Arrivals Just Dropped', body: 'Be the first to shop our newest collection.' },
  { title: 'Exclusive Member Offer', body: 'Unlock 30% off your next purchase — just for you.' },
  { title: 'We Miss You!', body: "It's been a while. Here's a special deal to welcome you back." },
  { title: 'Review Your Recent Purchase', body: 'Tell us what you think and earn rewards.' },
  { title: 'Limited Edition Launch', body: 'Only 100 units available. Get yours before they sell out.' },
];

let messages = [];
let nextId = 1;

function generateMessages(count) {
  const now = Date.now();
  const result = [];
  for (let i = 0; i < count; i++) {
    const sample = SAMPLE_MESSAGES[(nextId - 1) % SAMPLE_MESSAGES.length];
    const hasImage = nextId % 3 === 0;
    result.push({
      id: `msg_${String(nextId).padStart(3, '0')}`,
      title: nextId <= SAMPLE_MESSAGES.length ? sample.title : `${sample.title} (#${nextId})`,
      body: sample.body,
      timestamp: now - (nextId * 3600000),
      isRead: nextId % 4 === 0,
      imageUrl: hasImage ? SAMPLE_IMAGES[nextId % SAMPLE_IMAGES.length] : null,
      actionUrl: `https://example.com/offer/${nextId}`,
      style: nextId % 5 === 0 ? 'Large' : 'Small',
    });
    nextId++;
  }
  return result;
}

messages = generateMessages(100);

// --- API Routes ---

// GET /inbox/messages — paginated message list
app.get('/inbox/messages', (req, res) => {
  const offset = parseInt(req.query.offset) || 0;
  const limit = parseInt(req.query.limit) || 20;

  const page = messages.filter(m => !m._deleted).slice(offset, offset + limit);
  const remaining = messages.filter(m => !m._deleted).length;
  const hasMore = offset + limit < remaining;

  console.log(`GET /inbox/messages offset=${offset} limit=${limit} → ${page.length} messages (${remaining} total)`);

  res.json({
    messages: page.map(({ _deleted, ...m }) => m),
    unreadCount: messages.filter(m => !m._deleted && !m.isRead).length,
    hasMoreMessages: hasMore,
    nextOffset: hasMore ? offset + limit : null,
  });
});

// PATCH /inbox/messages/:id — update read status
app.patch('/inbox/messages/:id', (req, res) => {
  const msg = messages.find(m => m.id === req.params.id && !m._deleted);
  if (!msg) return res.status(404).json({ error: 'Message not found' });

  if (req.body.isRead !== undefined) {
    msg.isRead = req.body.isRead;
    console.log(`PATCH /inbox/messages/${req.params.id} → isRead=${msg.isRead}`);
  }
  res.status(204).end();
});

// DELETE /inbox/messages/:id — soft delete
app.delete('/inbox/messages/:id', (req, res) => {
  const msg = messages.find(m => m.id === req.params.id && !m._deleted);
  if (!msg) return res.status(404).json({ error: 'Message not found' });

  msg._deleted = true;
  console.log(`DELETE /inbox/messages/${req.params.id}`);
  res.status(204).end();
});

// POST /inbox/messages/reset — reset all data
app.post('/inbox/messages/reset', (req, res) => {
  nextId = 1;
  messages = generateMessages(100);
  console.log('POST /inbox/messages/reset → regenerated 100 messages');
  res.json({ message: 'Inbox reset', count: messages.length });
});

// POST /inbox/messages — add a custom message
app.post('/inbox/messages', (req, res) => {
  const msg = {
    id: `msg_${String(nextId).padStart(3, '0')}`,
    title: req.body.title || 'New Message',
    body: req.body.body || '',
    timestamp: Date.now(),
    isRead: false,
    imageUrl: req.body.imageUrl || null,
    actionUrl: req.body.actionUrl || null,
    style: req.body.style || 'Small',
  };
  nextId++;
  messages.unshift(msg);
  console.log(`POST /inbox/messages → created ${msg.id}: "${msg.title}"`);
  res.status(201).json(msg);
});

// --- Settings ---

app.get('/settings', (req, res) => {
  res.json({ delayMs });
});

app.post('/settings', (req, res) => {
  if (req.body.delayMs !== undefined) {
    delayMs = Math.max(0, parseInt(req.body.delayMs) || 0);
    console.log(`POST /settings → delayMs=${delayMs}`);
  }
  res.json({ delayMs });
});

// --- Dashboard ---
app.use(express.static(path.join(__dirname, 'public')));

app.listen(PORT, '0.0.0.0', () => {
  const ifaces = require('os').networkInterfaces();
  const localIp = Object.values(ifaces).flat()
    .find(i => i.family === 'IPv4' && !i.internal)?.address || 'localhost';

  console.log('');
  console.log('=== Attentive Inbox Mock Server ===');
  console.log('');
  console.log(`  Dashboard:  http://localhost:${PORT}`);
  console.log(`  API:        http://localhost:${PORT}/inbox/messages`);
  console.log('');
  console.log('  Android emulator: http://10.0.2.2:' + PORT);
  console.log(`  Physical device:  http://${localIp}:${PORT}`);
  console.log('');
  console.log('Endpoints:');
  console.log('  GET    /inbox/messages?offset=0&limit=20');
  console.log('  PATCH  /inbox/messages/:id  { "isRead": true }');
  console.log('  DELETE /inbox/messages/:id');
  console.log('  POST   /inbox/messages      { "title": "...", "body": "..." }');
  console.log('  POST   /inbox/messages/reset');
  console.log('');
});

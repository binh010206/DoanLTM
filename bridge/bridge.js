
const WebSocket = require('ws');
const http = require('http');

const WS_PORT = 8080;
const CPP_SERVER = 'http://127.0.0.1:9999/status';
const POLL_INTERVAL = 500; // ms

const wss = new WebSocket.Server({ port: WS_PORT });

let clientCount = 0;
let lastData = '';

function fetchAndBroadcast() {
    http.get(CPP_SERVER, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
            // Chi broadcast khi du lieu thay doi (tiet kiem bang thong)
            if (data !== lastData) {
                lastData = data;
                let sent = 0;
                wss.clients.forEach(client => {
                    if (client.readyState === WebSocket.OPEN) {
                        client.send(data);
                        sent++;
                    }
                });
                if (sent > 0) {
                    // Chi log khi co thay doi
                    const parsed = JSON.parse(data);
                    const occ = parsed.stats?.occupancy || 0;
                    console.log(`  [>] Broadcast: ${occ}/3 o | ${sent} dashboard(s)`);
                }
            }
        });
    }).on('error', (err) => {
        // C++ server chua chay
    });
}

// Polling lien tuc
setInterval(fetchAndBroadcast, POLL_INTERVAL);

wss.on('connection', (ws) => {
    clientCount++;
    console.log(`  [+] Dashboard ket noi (${clientCount} active)`);

    // Gui data ngay khi connect
    if (lastData) {
        ws.send(lastData);
    }

    ws.on('close', () => {
        clientCount--;
        console.log(`  [-] Dashboard ngat (${clientCount} active)`);
    });
});

console.log('');
console.log('  =============================================');
console.log('  WebSocket Bridge v1.0');
console.log('  =============================================');
console.log(`  WebSocket : ws://127.0.0.1:${WS_PORT}`);
console.log(`  Polling   : ${CPP_SERVER}`);
console.log(`  Interval  : ${POLL_INTERVAL}ms`);
console.log('  =============================================');
console.log('');
console.log('  Dang cho Dashboard ket noi...');
console.log('');

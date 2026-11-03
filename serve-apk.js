const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8080;
const APK_PATH = path.join(__dirname, 'NothingLens-v1.0.apk');

const server = http.createServer((req, res) => {
    if (req.url === '/' || req.url === '/download') {
        if (!fs.existsSync(APK_PATH)) {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            return res.end('APK not found. Please build it first.');
        }

        const stat = fs.statSync(APK_PATH);
        res.writeHead(200, {
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Disposition': 'attachment; filename="NothingLens-v1.0.apk"',
            'Content-Length': stat.size
        });

        const stream = fs.createReadStream(APK_PATH);
        stream.pipe(res);
    } else {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(`
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Download Nothing Lens</title>
                <style>
                    body { background: #0B0C0E; color: #FFF; font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
                    .card { background: #16171A; border: 1px solid #2C2E35; padding: 30px; border-radius: 20px; max-width: 320px; }
                    .btn { display: inline-block; background: #D71921; color: #FFF; padding: 14px 28px; border-radius: 30px; text-decoration: none; font-weight: bold; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Nothing Lens</h2>
                    <p style="color:#8E9096;">Document Scanner & PDF Studio</p>
                    <a href="/download" class="btn">Download APK (16.6 MB)</a>
                </div>
            </body>
            </html>
        `);
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`\n======================================================`);
    console.log(`APK Download Server running on port ${PORT}!`);
    console.log(`On your Nothing Phone, open Chrome and visit:`);
    console.log(`   http://10.141.10.228:${PORT}/`);
    console.log(`======================================================\n`);
});

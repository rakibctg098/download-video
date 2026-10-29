const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn, execFile } = require('child_process');

process.on('uncaughtException', (err) => {
  console.error('[Uncaught Exception]', err.message);
});
process.on('unhandledRejection', (err) => {
  console.error('[Unhandled Rejection]', err);
});

const PORT = 3000;
const ROOT_DIR = __dirname;
const YTDLP_BIN = fs.existsSync(path.join(ROOT_DIR, 'bin', 'yt-dlp'))
  ? path.join(ROOT_DIR, 'bin', 'yt-dlp')
  : '/tmp/yt-dlp';

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.mp4': 'video/mp4'
};

function sanitizeFilename(name) {
  if (!name) return 'youtube_video.mp4';
  let clean = name.replace(/[^a-zA-Z0-9_\-\.\u0980-\u09FF\s]/g, '').trim().replace(/\s+/g, '_');
  if (!clean.toLowerCase().endsWith('.mp4')) {
    clean += '.mp4';
  }
  return clean.substring(0, 80);
}

const server = http.createServer((req, res) => {
  res.on('error', () => {});

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost:3000'}`);
  const pathname = parsedUrl.pathname;

  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // API 1: Extract YouTube Info & Direct Stream
  if (pathname === '/api/youtube-info') {
    const targetUrl = parsedUrl.searchParams.get('url');
    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Missing url parameter' }));
      return;
    }

    console.log(`[YouTube Info] Extracting info for: ${targetUrl}`);
    execFile(YTDLP_BIN, [
      '--js-runtimes', 'node:node',
      '-j',
      '--no-playlist',
      targetUrl
    ], { maxBuffer: 10 * 1024 * 1024, timeout: 25000 }, (err, stdout, stderr) => {
      if (err) {
        console.error('[YouTube Info Error]', err.message || stderr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: 'Failed to extract video info' }));
        return;
      }

      try {
        const info = JSON.parse(stdout);
        const title = info.title || 'YouTube Video';
        const safeName = sanitizeFilename(title);
        const streamApiUrl = `/api/youtube-stream?url=${encodeURIComponent(targetUrl)}&title=${encodeURIComponent(safeName)}`;

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          title: title,
          thumbnail: info.thumbnail || null,
          duration: info.duration || null,
          author: info.uploader || info.channel || null,
          filename: safeName,
          directDownloadUrl: streamApiUrl
        }));
      } catch (parseErr) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: 'JSON parse error' }));
      }
    });
    return;
  }

  // API 2: Direct In-App Stream / Download
  if (pathname === '/api/youtube-stream') {
    const targetUrl = parsedUrl.searchParams.get('url');
    const customTitle = parsedUrl.searchParams.get('title');

    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'text/plain' });
      res.end('Missing url parameter');
      return;
    }

    const filename = sanitizeFilename(customTitle || 'video.mp4');
    console.log(`[YouTube Stream] Streaming directly: ${targetUrl} as ${filename}`);

    res.writeHead(200, {
      'Content-Type': 'video/mp4',
      'Content-Disposition': `attachment; filename="${filename}"`,
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache'
    });

    const ytProcess = spawn(YTDLP_BIN, [
      '--js-runtimes', 'node:node',
      '-f', 'best[ext=mp4]/best',
      targetUrl,
      '-o', '-'
    ]);

    ytProcess.stdout.on('error', () => {});
    ytProcess.stderr.on('error', () => {});

    ytProcess.stdout.pipe(res);

    req.on('close', () => {
      try {
        ytProcess.kill('SIGKILL');
      } catch (_) {}
    });

    ytProcess.on('error', (err) => {
      console.error('[Stream Error]', err);
      if (!res.headersSent) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('Stream error: ' + err.message);
      }
    });
    return;
  }

  // Health check
  if (pathname === '/healthz' || pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', time: Date.now() }));
    return;
  }

  // Serve static files
  let safePath = path.normalize(pathname).replace(/^(\.\.[\/\\])+/, '');
  if (safePath === '/' || safePath === '\\') {
    safePath = '/index.html';
  }

  let filePath = path.join(ROOT_DIR, safePath);

  // If path doesn't exist, try index.html
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(ROOT_DIR, 'index.html');
  }

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('File not found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    const isNoCache = ext === '.html' || ext === '.json' || filePath.endsWith('sw.js');
    res.writeHead(200, {
      'Content-Type': contentType,
      'Cache-Control': isNoCache ? 'no-cache, no-store, must-revalidate, max-age=0' : 'public, max-age=86400'
    });
    res.end(content);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`VideoSaver Server running on http://0.0.0.0:${PORT}`);
});

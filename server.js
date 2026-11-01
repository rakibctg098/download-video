const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn, execFile } = require('child_process');
const axios = require('axios');
const { createDecipheriv } = require('crypto');

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

function extractYouTubeVideoId(url) {
  if (!url) return null;
  const match = url.match(/(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=|shorts\/))([\w-]{11})/);
  return match ? match[1] : null;
}

const decodeSavetube = (enc) => {
  try {
    const secret_key = 'C5D58EF67A7584E4A29F6C35BBC4EB12';
    const data = Buffer.from(enc, 'base64');
    const iv = data.slice(0, 16);
    const content = data.slice(16);
    const key = Buffer.from(secret_key, 'hex');
    const decipher = createDecipheriv('aes-128-cbc', key, iv);
    let decrypted = Buffer.concat([decipher.update(content), decipher.final()]);
    return JSON.parse(decrypted.toString());
  } catch (e) {
    return null;
  }
};

async function resolveDirectYouTubeMp4(videoId, preferredQuality = '720') {
  if (!videoId) return null;
  const canonicalUrl = `https://www.youtube.com/watch?v=${videoId}`;
  const qualities = [preferredQuality, '720', '360', '480', '1080'];

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const cdnRes = await axios.get('https://media.savetube.vip/api/random-cdn', { timeout: 6000 });
      const cdn = cdnRes.data && cdnRes.data.cdn ? cdnRes.data.cdn : 'cdn403.savetube.vip';

      const infoget = (await axios.post(`https://${cdn}/v2/info`, { url: canonicalUrl }, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36',
          'Referer': 'https://save-tube.com/'
        },
        timeout: 8000
      })).data;

      const info = decodeSavetube(infoget.data);
      if (!info || !info.key) continue;

      for (const q of qualities) {
        try {
          const downloadRes = (await axios.post(`https://${cdn}/download`, {
            downloadType: 'video',
            quality: `${q}`,
            key: info.key
          }, {
            headers: {
              'Content-Type': 'application/json',
              'User-Agent': 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36',
              'Referer': 'https://save-tube.com/'
            },
            timeout: 10000
          })).data;

          if (downloadRes.data && downloadRes.data.downloadUrl) {
            return {
              title: info.title || 'YouTube Video',
              thumbnail: info.thumbnail || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
              duration: info.duration || null,
              quality: `${q}p`,
              downloadUrl: downloadRes.data.downloadUrl
            };
          }
        } catch (_) {}
      }
    } catch (err) {
      console.warn(`[Savetube Attempt ${attempt}] failed:`, err.message);
    }
  }

  // Secondary fallback: @vreden/youtube_scraper
  try {
    const ytScraper = require('@vreden/youtube_scraper');
    const vRes = await ytScraper.ytmp4(canonicalUrl, 720);
    if (vRes && vRes.download && vRes.download.url) {
      return {
        title: (vRes.metadata && vRes.metadata.title) || 'YouTube Video',
        thumbnail: (vRes.metadata && (vRes.metadata.thumbnail || vRes.metadata.image)) || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
        duration: vRes.metadata && vRes.metadata.seconds,
        quality: vRes.download.quality || '720p',
        downloadUrl: vRes.download.url
      };
    }
  } catch (err2) {
    console.warn('[Vreden Scraper Fallback] failed:', err2.message);
  }

  return null;
}

const server = http.createServer(async (req, res) => {
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

  // API 1: Extract YouTube Info & Direct 1-Click MP4 Download URL
  if (pathname === '/api/youtube-info') {
    const targetUrl = parsedUrl.searchParams.get('url');
    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Missing url parameter' }));
      return;
    }

    const videoId = extractYouTubeVideoId(targetUrl);
    console.log(`[YouTube Info] Processing: ${targetUrl} (Video ID: ${videoId})`);

    try {
      const directResult = await resolveDirectYouTubeMp4(videoId, '720');
      if (directResult && directResult.downloadUrl) {
        const safeName = sanitizeFilename(directResult.title);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          title: directResult.title,
          thumbnail: directResult.thumbnail,
          duration: directResult.duration,
          filename: safeName,
          quality: directResult.quality,
          hdUrl: directResult.downloadUrl,
          sdUrl: directResult.downloadUrl,
          directDownloadUrl: directResult.downloadUrl
        }));
        return;
      }
    } catch (ex) {
      console.warn('[YouTube Info Direct] resolution error:', ex.message);
    }

    // Fallback: oEmbed metadata + stream route
    try {
      let oembedTitle = 'YouTube Video';
      let oembedThumb = videoId ? `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg` : null;
      try {
        const oembedRes = await axios.get(`https://www.youtube.com/oembed?url=${encodeURIComponent(targetUrl)}&format=json`, { timeout: 4000 });
        if (oembedRes.data) {
          oembedTitle = oembedRes.data.title || oembedTitle;
          oembedThumb = oembedRes.data.thumbnail_url || oembedThumb;
        }
      } catch (_) {}

      const safeName = sanitizeFilename(oembedTitle);
      const streamApiUrl = `/api/youtube-stream?url=${encodeURIComponent(targetUrl)}&title=${encodeURIComponent(safeName)}`;

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: true,
        title: oembedTitle,
        thumbnail: oembedThumb,
        filename: safeName,
        directDownloadUrl: streamApiUrl,
        hdUrl: streamApiUrl,
        sdUrl: streamApiUrl
      }));
    } catch (parseErr) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Extraction error' }));
    }
    return;
  }

  // API 2: Direct 1-Click Stream / Download (Clean Direct MP4 Download, ZERO External Sites)
  if (pathname === '/api/youtube-stream') {
    const targetUrl = parsedUrl.searchParams.get('url');
    const customTitle = parsedUrl.searchParams.get('title');

    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'text/plain' });
      res.end('Missing url parameter');
      return;
    }

    const videoId = extractYouTubeVideoId(targetUrl);
    const filename = sanitizeFilename(customTitle || 'video.mp4');
    console.log(`[YouTube Stream] 1-Click direct request for: ${targetUrl} as ${filename}`);

    // Try resolving direct CDN MP4 download URL
    if (videoId) {
      try {
        const directResult = await resolveDirectYouTubeMp4(videoId, '720');
        if (directResult && directResult.downloadUrl) {
          console.log(`[YouTube Stream] Successfully resolved direct CDN URL: ${directResult.downloadUrl}`);
          res.writeHead(302, {
            'Location': directResult.downloadUrl,
            'Cache-Control': 'no-cache'
          });
          res.end();
          return;
        }
      } catch (e) {
        console.warn('[YouTube Stream] Direct resolver error:', e.message);
      }
    }

    // Secondary fallback: yt-dlp direct pipe
    console.log(`[YouTube Stream] Fallback to yt-dlp pipe for: ${targetUrl}`);
    const ytProcess = spawn(YTDLP_BIN, [
      '--js-runtimes', 'node:node',
      '-f', 'best[ext=mp4]/best',
      targetUrl,
      '-o', '-'
    ]);

    let hasSentHeaders = false;

    ytProcess.stdout.once('data', (chunk) => {
      hasSentHeaders = true;
      res.writeHead(200, {
        'Content-Type': 'video/mp4',
        'Content-Disposition': `attachment; filename="${filename}"`,
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      });
      res.write(chunk);
      ytProcess.stdout.pipe(res);
    });

    ytProcess.stdout.on('error', () => {});
    ytProcess.stderr.on('data', (d) => {
      console.log('[yt-dlp stderr]', d.toString().trim());
    });

    req.on('close', () => {
      try {
        ytProcess.kill('SIGKILL');
      } catch (_) {}
    });

    ytProcess.on('close', (code) => {
      if (!hasSentHeaders) {
        console.error(`[YouTube Stream] yt-dlp exited with code ${code} without data`);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: 'Could not stream video. Please try again.' }));
      }
    });

    ytProcess.on('error', (err) => {
      console.error('[Stream Error]', err);
      if (!hasSentHeaders) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: 'Server streaming error' }));
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

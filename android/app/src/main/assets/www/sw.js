const CACHE = 'dedsec-demo-v1';
const FILES = ['./', './index.html', './manifest.json', './icon.svg', './assets/deadsec-bg.png', './assets/recon-bg.png', './assets/alerts-bg.png', './assets/icon-recon.png', './assets/icon-alerts.png', './assets/icon-payloads.png', './assets/icon-settings.png'];
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(FILES)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  event.respondWith(caches.match(event.request).then(cached => cached || fetch(event.request).then(response => {
    const copy = response.clone(); caches.open(CACHE).then(cache => cache.put(event.request, copy)); return response;
  }).catch(() => caches.match('./index.html'))));
});

// Guarda Sprunkialict en el celular/tablet para que abra rápido y funcione sin internet.
// Si cambias el juego, sube los archivos nuevos: se actualiza solo la siguiente vez que se abra con internet.
const CACHE = 'musica-v1';
const ARCHIVOS = ['./', 'index.html', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ARCHIVOS)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  // borra versiones viejas de este juego
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE && k.startsWith('musica-')).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  e.respondWith(caches.open(CACHE).then(async c => {
    const guardado = await c.match(e.request, { ignoreSearch: true });
    const red = fetch(e.request).then(r => { if (r && r.ok && r.type === 'basic') c.put(e.request, r.clone()); return r; }).catch(() => guardado);
    return guardado || red;
  }));
});

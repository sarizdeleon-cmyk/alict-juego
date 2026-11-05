// Guarda el juego en la tablet/celular para que abra rápido y funcione sin internet.
// Si cambias el juego, sube los archivos nuevos: se actualiza solo la siguiente vez que se abra con internet.
const CACHE = 'zombis-v1';
const ARCHIVOS = ['./', 'index.html', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png'];
// cachés de los juegos que antes vivían en esta misma dirección (ya no se usan aquí)
const VIEJOS = ['alict-v4', 'sprunkialict-v1'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ARCHIVOS)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE && (VIEJOS.includes(k) || k.startsWith('zombis-'))).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  const ruta = new URL(e.request.url).pathname;
  if (ruta.includes('/didacticos/') || ruta.includes('/sprunkialict/')) return; // los otros juegos tienen su propio guardado
  e.respondWith(caches.open(CACHE).then(async c => {
    const guardado = await c.match(e.request, { ignoreSearch: true });
    const red = fetch(e.request).then(r => { if (r && r.ok && r.type === 'basic') c.put(e.request, r.clone()); return r; }).catch(() => guardado);
    return guardado || red;
  }));
});

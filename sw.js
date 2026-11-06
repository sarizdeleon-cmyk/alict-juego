// Guarda el menú de Alict en la tablet para que abra rápido y funcione sin internet.
// Cada juego (zombis/, didacticos/, sprunkialict/, tragones/, corre/, pelea/) tiene su propio service worker y su propia caché:
// este solo cuida el menú y deja pasar todo lo que sea de un juego.
// Si cambias el menú, sube los archivos nuevos: se actualiza solo la siguiente vez que se abra con internet.
const CACHE = 'launcher-v4';
const JUEGOS = ['/zombis/', '/didacticos/', '/sprunkialict/', '/tragones/', '/corre/', '/pelea/'];
const ARCHIVOS = ['./', 'index.html', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ARCHIVOS)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  // borra versiones viejas del menú (y nada más: las cachés de los juegos no se tocan)
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE && k.startsWith('launcher-')).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  const ruta = new URL(e.request.url).pathname;
  if (JUEGOS.some(j => ruta.includes(j))) return;       // lo manejan los service workers de cada juego
  e.respondWith(caches.open(CACHE).then(async c => {
    const guardado = await c.match(e.request, { ignoreSearch: true });
    const red = fetch(e.request).then(r => { if (r && r.ok && r.type === 'basic') c.put(e.request, r.clone()); return r; }).catch(() => guardado);
    return guardado || red;
  }));
});

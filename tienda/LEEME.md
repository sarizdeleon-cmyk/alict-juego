# Tienda Alict

Página con las apps gratis para Android: una sección por app con capturas, descripción, permisos y botón de descarga.
Link: https://sarizdeleon-cmyk.github.io/alict-juego/tienda/ (cada app tiene su enlace directo, por ejemplo `/tienda/#zonas-a-evitar`).

## Carpetas
- `index.html`: la página (no hay que tocarla para agregar apps).
- `apps.json`: la lista de apps con sus textos, colores, capturas y archivo APK.
- `apk/`: los archivos `.apk`.
- `img/<app>/`: el ícono (`icono.webp`) y las capturas (`1.webp`, `2.webp`…).
- `estrellas.sql`: crea en Supabase la tabla y las funciones de las estrellas (se pega una sola vez en Supabase > SQL Editor).
- `actualizar_tienda.py`: calcula tamaño, huella SHA-256 y fecha de cada APK dentro de `apps.json`.

## Agregar una app nueva
1. Sube el `.apk` a `apk/` y las imágenes a `img/<id-de-la-app>/`.
2. Copia un bloque de `apps.json` y cambia sus datos (`id` sin espacios ni acentos; `color` es el del botón; `fondo` el de la sección).
3. Corre `python3 actualizar_tienda.py` (o déjalo y se muestra sin tamaño exacto hasta la próxima vez).

## Actualizar una app
Reemplaza el `.apk` en `apk/` con el mismo nombre y corre `python3 actualizar_tienda.py`: cambia sola la fecha y el SHA-256.
Las apps están firmadas siempre con la misma llave, así que la versión nueva se instala encima de la anterior sin borrar datos.

## Estrellas y comentarios
- Cada app (nueva o vieja) trae sola el panel "Califica y deja tu comentario": estrellas de 1 a 5 y un cuadro que abre WhatsApp (+52 992 108 3800) con el mensaje listo (app, estrellas y comentario).
- El promedio que ve todo el mundo sale de Supabase. Los datos de conexión están al inicio de `apps.json` (`calificaciones`: `url` y `key` publicable). Si se borra ese bloque, las estrellas se guardan solo en el teléfono y el comentario por WhatsApp sigue funcionando.
- Si `estrellas.sql` no se ha ejecutado, la página no muestra promedios y todo lo demás funciona igual; al ejecutarlo, empiezan a aparecer solos.
- Cada teléfono cuenta una sola vez por app (si cambia de opinión, se actualiza su voto).

## APK muy grandes
GitHub no acepta archivos de más de 100 MB. El Descargador de videos se compila solo para teléfonos de 64 bits para quedar en ~57 MB.

## No incluidas a propósito
Comisiones Incelcorp (uso interno del negocio) y Juegos de Alict para TV (llevan fotos de la familia).

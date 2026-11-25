# Tienda Alict

Página con las apps gratis para Android: una sección por app con capturas, descripción, permisos y botón de descarga.
Link: https://sarizdeleon-cmyk.github.io/alict-juego/tienda/ (cada app tiene su enlace directo, por ejemplo `/tienda/#zonas-a-evitar`).

## Carpetas
- `index.html`: la página (no hay que tocarla para agregar apps).
- `apps.json`: la lista de apps con sus textos, colores, capturas y archivo APK.
- `apk/`: los archivos `.apk`.
- `img/<app>/`: el ícono (`icono.webp`) y las capturas (`1.webp`, `2.webp`…).
- `actualizar_tienda.py`: calcula tamaño, huella SHA-256 y fecha de cada APK dentro de `apps.json`.

## Agregar una app nueva
1. Sube el `.apk` a `apk/` y las imágenes a `img/<id-de-la-app>/`.
2. Copia un bloque de `apps.json` y cambia sus datos (`id` sin espacios ni acentos; `color` es el del botón; `fondo` el de la sección).
3. Corre `python3 actualizar_tienda.py` (o déjalo y se muestra sin tamaño exacto hasta la próxima vez).

## Actualizar una app
Reemplaza el `.apk` en `apk/` con el mismo nombre y corre `python3 actualizar_tienda.py`: cambia sola la fecha y el SHA-256.
Las apps están firmadas siempre con la misma llave, así que la versión nueva se instala encima de la anterior sin borrar datos.

## No incluidas a propósito
Comisiones Incelcorp (uso interno del negocio) y Juegos de Alict para TV (llevan fotos de la familia).

# Juegos de Alict para Android TV

App de Android TV (también sirve en tablet) con el menú de Alict —bienvenida con su nombre, música, desfile de todos los personajes y galería «Mis amigos»— y **todos los juegos dentro**: funciona sin internet.

- **Menú**: flechas del control para moverse, **OK** para entrar. **Atrás** sale de la galería, vuelve de un juego al menú, o cierra la app.
- **Dentro de un juego**: las flechas mueven una estrella (cursor) y **OK** «toca» la pantalla (mantener OK = mantener el dedo). Sirve para cualquier juego. También funciona con un control de videojuegos (palanca + botón A).
- **Mis apps**: botón arriba a la derecha (flecha hacia arriba desde los juegos) con todas las demás apps instaladas en la tele; OK abre la app. Además la app se declara como pantalla de inicio: si la tele deja cambiar el inicio, al apretar el botón Inicio la ofrece (si se elige, Inicio siempre vuelve a este menú).
- Sin permiso de internet: no puede abrir nada de afuera.

## Cómo se compila
GitHub Actions (`.github/workflows/tv-apk.yml`) copia `tv-home/` (menú de la tele) y las carpetas de los juegos del sitio dentro de `app/src/main/assets/www/`, compila con Gradle y publica `JuegosDeAlict.apk` en **Releases → tv**.
Se ejecuta solo cuando cambia algo de `tv-apk/`, o a mano en la pestaña **Actions → APK para Android TV → Run workflow** (por ejemplo después de agregar un juego nuevo al sitio).

Link fijo de descarga: https://github.com/sarizdeleon-cmyk/alict-juego/releases/download/tv/JuegosDeAlict.apk

## Archivos
- `tv-home/`: menú de la tele ya armado (`index.html` + `personajes/`). Se genera desde el proyecto del menú (`build_tv.py`).
- `app/`: la app de Android (Java, sin librerías externas).
- `alict.keystore`: llave de firma fija (para poder instalar cada versión nueva encima de la anterior). Es una llave de uso familiar, sin valor fuera de esta app.

## Créditos
Ver `CREDITOS.md` (personajes y sprites de terceros, hojas de fans).

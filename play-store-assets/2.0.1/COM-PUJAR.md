# Recursos de la fitxa de Cronos 2.0.1

- `icon_512.png`: icona de Play, 512 × 512, PNG RGBA. Símbol sense text i
  amb menys marge que la icona adaptativa del mòbil.
- `feature_graphic_1024x500.png`: gràfic de funcions nou, 1024 × 500, PNG RGB.
- `phone/01_rellotge.png`, `02_radio.png`, `03_alarmes.png` i
  `04_configuracio.png`: captures reals de la variant R8 separada al Galaxy
  S21 Ultra, sense les barres del sistema, ajustades sense deformar la
  interfície a un llenç
  9:16 de 1440 × 2560. No són captures generades ni de l'app antiga.

## Canviar la fitxa a Play Console

1. Tria **Cronos** → **Augmenta el nombre d'usuaris / Grow users** →
   **Presència a Google Play / Store presence** → **Fitxa principal / Main
   store listing** (en algunes versions: **Fitxes de la botiga**).
2. Selecciona l'idioma català de la fitxa.
3. A **Gràfics**, substitueix la **icona de l'aplicació** per `icon_512.png`
   i el **gràfic de funcions** per `feature_graphic_1024x500.png`.
4. A **Captures de telèfon**, elimina les antigues i afegeix les quatre
   imatges de `phone/`: Rellotge, Ràdio, Alarmes i Configuració. Ordena-les
   arrossegant-les i posa un text alternatiu breu a cada captura.
5. Revisa també les imatges de tauleta, Chromebook i XR, si en tens. No
   reutilitzis captures de telèfon com si fossin d'altres dispositius.
6. Si tens altres idiomes amb gràfics propis o fitxes personalitzades,
   actualitza'ls també perquè no continuïn mostrant imatges antigues.
7. **Desa** → **Visió general de publicació / Publishing overview** →
   **Envia els canvis a revisió**. Si tens la publicació gestionada activada,
   un cop aprovats hauràs de prémer **Publica els canvis**.

Pujar els fitxers només a la biblioteca d'assets no els assigna a la fitxa:
cal seleccionar-los als camps corresponents i desar-la.

La fitxa es comparteix entre producció i canals de prova. Canviar aquests
gràfics no requereix un AAB nou, però canviar la icona instal·lada al mòbil
sí que requereix actualitzar l'app.

## AAB nou

Primer puja l'AAB **release** 2.0.1 (codi 10) a **Prova interna** i comprova'l.
Després promou aquesta mateixa versió a producció; no cal tornar a generar-la
ni tornar a pujar un altre AAB amb el mateix codi.

**No pugis l'APK `releaseTest` a Play.** Té el paquet separat
`com.filldemaia.cronos.r8test`, signatura de proves i el nom «Cronos · Prova R8».

## Notes de la versió

```text
<ca>
Optimització del codi amb R8 per reduir la mida i millorar l'eficiència de l'aplicació.
Icona actualitzada: el mateix símbol, sense el text «CRONOS».
</ca>
```

Referència oficial: https://support.google.com/googleplay/android-developer/answer/9866151

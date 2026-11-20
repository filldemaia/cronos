# Recursos de la fitxa de cronos 2.0.1

Els fitxers per pujar a Play Console es guarden a `play-store-assets/2.0.1/`, una carpeta local que Git no publica. Les imatges del web i del README són a [`docs/captures/pixel-8/`](captures/pixel-8/) i [`docs/captures/tab-s9/`](captures/tab-s9/).

- `icon_512.png`: icona de Play, 512 × 512, PNG RGBA. Símbol sense text i
  amb menys marge que la icona adaptativa del mòbil.
- `feature_graphic_1024x500.png`: gràfic de funcions nou, 1024 × 500, PNG RGB.
- `phone/01_rellotge.png`, `02_radio.png`, `03_alarmes.png` i
  `04_reproduccio.png`: captures reals de cronos Nightly 2.0.1 en un Galaxy
  S21 Ultra, presentades dins d’un marc de Pixel 8 en format 9:16
  (1080 × 1920). El marc és de presentació; la captura no s’ha estirat.
- `phone-sense-marc/`: les mateixes quatre captures sense reenquadrar.
  Google [recomana fer servir captures sense marc](https://developer.android.com/distribute/marketing-tools/device-art-generator) per a la fitxa de Play.
- `tablet/01_rellotge.png`, `02_radio.png`, `03_alarmes.png` i
  `04_reproduccio.png`: captures reals de cronos Nightly 2.0.1 en una Galaxy
  Tab A (8", 2019), girada a horitzontal i presentada dins d’un marc de Galaxy
  Tab S9. Són PNG RGB 1920 × 1080 (16:9), sense alfa, tal com demana Play per
  a tauleta. El marc és de presentació; la captura no s’ha estirat.
- `tablet-sense-marc/`: les quatre captures originals de tauleta, en 1280 × 704
  i sense reenquadrar. Les originals també són a
  [`docs/captures/tablet/`](captures/tablet/).
- `fastlane/metadata/android/en-US/images/tabletScreenshots/`: les mateixes
  imatges de `tablet/`, numerades de l'1 al 4 i a punt per pujar a Play.

Per regenerar les imatges emmarcades del web, el README, Fastlane i Play,
executa `python tools/frame_pixel8.py` i `python tools/frame_tab_s9.py`.

Per generar de nou la icona i el gràfic, executa `python tools/prepare_icons.py`.
Per fer captures bàsiques amb cronos Nightly, executa `python tools/capture_store_screens.py`
amb el telèfon connectat i desbloquejat. El resultat es desa a `captures-basiques/`
sense substituir la selecció de la fitxa. Per a la fitxa, mostra alarmes ja
programades i el reproductor amb una emissora sonant.
Els recursos antics són a `play-store-assets/historics/`; no els facis servir
com a captures de la versió actual per a tauleta, Chromebook o XR.

## Canviar la fitxa a Play Console

1. Tria **cronos** → **Augmenta el nombre d'usuaris / Grow users** →
   **Presència a Google Play / Store presence** → **Fitxa principal / Main
   store listing** (en algunes versions: **Fitxes de la botiga**).
2. Selecciona l'idioma català de la fitxa.
3. A **Gràfics**, substitueix la **icona de l'aplicació** per `icon_512.png`
   i el **gràfic de funcions** per `feature_graphic_1024x500.png`.
4. A **Captures de telèfon**, elimina les antigues i afegeix les quatre
   imatges de `phone/`: Rellotge, Ràdio, Reproducció i Alarmes. Ordena-les
   arrossegant-les i posa un text alternatiu breu a cada captura.
5. A **Captures de tauleta**, afegeix les quatre imatges horitzontals de
   `tablet/` (1920 × 1080): Rellotge, Ràdio, Reproducció i Alarmes. No
   reutilitzis captures de telèfon com si fossin de tauleta, ni al revés.
   Revisa també Chromebook i XR, si en tens.
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
`com.filldemaia.cronos.r8test` i signatura de proves. La build de
proves es anomena «cronos Nightly».

El llançament a producció és l'AAB de `release`: **2.0.1/10**.

## Notes de la versió

```text
<ca>
Optimització del codi amb R8 per reduir la mida i millorar l'eficiència de l'aplicació.
Icona actualitzada: el mateix símbol, sense el nom escrit.
</ca>
```

Referència oficial: https://support.google.com/googleplay/android-developer/answer/9866151

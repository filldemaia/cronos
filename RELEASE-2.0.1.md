# Cronos 2.0.1 — preparació i proves

## Dos paquets diferents

| Ús | Paquet | Signatura | Optimització |
| --- | --- | --- | --- |
| Play (`release`, codi 10) | `com.filldemaia.cronos` | Clau release local | R8 + reducció de recursos |
| Prova USB (`releaseTest`) | `com.filldemaia.cronos.r8test` | Clau debug | R8 + reducció de recursos |

La variant de proves **no és debuggable**: prova el codi optimitzat, no el
comportament d'una build debug. Es diu **Cronos · Prova R8**. El sufix fa
que tingui un UID, dades, WorkManager i alarmes independents dels de Play.
No s'ha d'instal·lar l'APK release sobre la còpia de Play ni esborrar-la.

## Comprovacions locals

- 63 tests d'unitat passats (0 errors i 0 fallades).
- Lint de release: 0 errors; 85 avisos i 7 indicacions pendents de millora.
- AAB signat: versió i codi comprovats llegint el manifest dins de l'AAB.
- `proguard.map` i `r8.json` inclosos a l'AAB.
- SHA-256 dels DEX contrastat amb les metadades d'R8.

| Metadada d'R8 | Percentatge permès |
| --- | --- |
| Ofuscació | 98,34 % |
| Optimització | 97,67 % |
| Reducció | 98,33 % |

DEX sense comprimir: **3.953.144 bytes** (per sota dels 10 MB del llindar).
L'AAB pesa uns **7,80 MB**; això no és la mida exacta de la descàrrega de
Play. Play recalcularà les seves puntuacions quan es pugi el bundle.

## Ordres reproduïbles

```powershell
.\gradlew.bat testDebugUnitTest lintRelease bundleRelease assembleReleaseTest
python tools\verify_aab.py app\build\outputs\bundle\release\app-release.aab
```

Amb el mòbil connectat, desbloquejat i autoritzat per depuració USB:

```powershell
.\gradlew.bat installReleaseTest
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.filldemaia.cronos.r8test/com.filldemaia.cronos.MainActivity
python tools\capture_store_screens.py
```

Les proves en dispositiu es fan amb ADB sobre l'APK real optimitzat, sense
regles especials que mantinguin biblioteques senceres per al runner de test.
El runner AndroidX Test antic referenciava classes compartides que R8 havia
eliminat; no s'ha utilitzat com a prova passada ni s'ha afeblit R8 per fer-lo
funcionar. La suite d'unitat continua executant-se al JVM.

## Proves al dispositiu

Dispositiu: Samsung Galaxy S21 Ultra (SM-G998B). Paquet de prova separat,
no debuggable, signat amb la clau de debug i amb les mateixes opcions d'R8
que la build release. La versió i les dates d'instal·lació/actualització del
paquet de Play s'han comparat abans i després: sense canvis.

La prova inicial va detectar una fallada real d'arrencada:
`WorkDatabase_Impl.<init>()` s'havia eliminat. S'ha corregit amb una regla
precisa per conservar el constructor sense arguments de la implementació
Room utilitzada per WorkManager. L'arrencada en fred s'ha tornat a provar.

Abans de publicar, provar **en la variant R8**:

- [x] Arrencada i navegació per les quatre pestanyes; icona nova instal·lada.
- [x] Catalunya Ràdio: estat PLAYING del sistema, pausa, represa, rebobinat
  de 10 segons i retorn a DIRECTE.
- [x] Crear i editar una alarma, desar volum/fade-in i comprovar persistència
  després de forçar l'aturada i tornar a obrir l'app.
- [x] Alarma exacta registrada a AlarmManager i prova real de +5 segons:
  receptor i servei executats amb l'app en segon pla, fins a PLAYING.
- [x] Fallback local amb una emissora inexistent simulada via l'ordre del
  servei, sense desactivar la xarxa del telèfon; alarma local detectada a la
  notificació i estat PLAYING. Aturada explícita del to.
- [x] Eliminar l'alarma temporal i confirmar «Cap alarma»; cap alarma futura
  del paquet de proves pendent al sistema.
- [x] Captures reals noves de les quatre pestanyes, sense la finestra PiP.
- [ ] Prova prolongada amb pantalla apagada / Doze i estalvi de bateria.
- [ ] Fallback amb caiguda real de connexió (diferent del cas simulat).
- [ ] Verificar rearmament després d'un reinici (amb permís de l'usuari).
- [ ] Afegir i redimensionar els tres widgets; revisar actualització i clic.

No s'ha reiniciat el telèfon ni s'ha canviat la seva connectivitat per no
interrompre altres aplicacions. Tampoc s'ha modificat la pantalla d'inici
afegint-hi widgets de prova.

En ser una instal·lació separada, aquesta prova **no demostra** una migració
de les dades de la còpia de Play. Cal comprovar també l'actualització per
un canal de proves de Play en un dispositiu de prova abans del desplegament.

## Fitxers a lliurar

- AAB per a Play: `app/build/outputs/bundle/release/app-release.aab`.
- APK de prova USB: `app/build/outputs/apk/releaseTest/app-releaseTest.apk`.
- Mapping de la build de Play: `app/build/outputs/mapping/release/mapping.txt`.
- Recursos de la fitxa i instruccions: `play-store-assets/2.0.1/COM-PUJAR.md`.

No publiquis la variant `releaseTest`. Primer prova el bundle release al
canal intern de Play i després promou el mateix artefacte a producció.

# Cronos et diu l'hora en català

> L'hora en català tradicional, bonica i llegible, amb un fons que canvia de color segons la sortida i la posta de sol — i, a més, una ràdio i un despertador.

Cronos és una aplicació Android que mostra l'hora de la manera tradicional catalana ("És un quart de set del matí", "Falten cinc minuts per a les tres de la tarda"), amb una interfície moderna. A partir de la versió 2.0 també inclou una **ràdio en català** i un **despertador** que et desperta amb l'emissora que triïs (amb un to local de seguretat si no hi ha xarxa).

## Característiques

- **Interfície renovada**: disseny modern amb Jetpack Compose.
- **Fons segons el sol**: el degradat passa pel matí, migdia, tarda, vespre i nit seguint la sortida i la posta de sol reals del teu lloc (calculades astronòmicament a partir de la teva ciutat).
- **Hora catalana tradicional** amb el format complet: quarts, minuts, segons i moment del dia.
- **Data completa**: "Avui és dilluns, 10 d'agost del 2026".
- **Salutació** segons el moment del dia ("Bona tarda!", "Bon vespre", ...).
- **Tres widgets** de diferents mides per a la pantalla d'inici.
- **Segons opcionals**: es poden activar i desactivar a gust els segons ("Falten sis minuts i tres segons per a tres quarts de deu de la nit").
- **Suport per temes**: compatible amb mode clar i fosc del sistema.
- Compatible amb Android 8.0+ (API 26).

## Ràdio i despertador

- **16 emissores en català**: Catalunya Ràdio, Catalunya Informació, Catalunya Música, iCat, RAC1, RAC1+1, RAC105, Flaixbac, Flaix FM, Ràdio Estel, À Punt Ràdio, Ràdio Klara, IB3 Ràdio, Ona Mediterrània, Ràdio Fraga i Ràdio Arrels.
- **Reproducció en segon pla**: la ràdio continua sonant amb la pantalla apagada (servei en primer pla amb control multimèdia).
- **Despertador amb ràdio**: programa una alarma que sona amb l'emissora que triïs.
- **Cadena de seguretat**: URLs alternatives, emissores de reserva i, finalment, un to local sense xarxa perquè l'alarma no falli mai.
- **Timeshift**: rebobina i torna a escoltar els últims minuts de l'emissió.

## Exemples del format

| Hora | Es mostra |
| --- | --- |
| 12:00 | Són les dotze del migdia |
| 15:15 | És un quart de quatre de la tarda |
| 20:30 | Són dos quarts de nou de la nit |
| 22:45 | Són tres quarts d'onze de la nit |
| 08:06 | Són les vuit i sis minuts del matí |
| 14:53 | Falten set minuts per a les tres de la tarda |
| 21:39:03 (amb segons) | Falten sis minuts i tres segons per a tres quarts de deu de la nit |

## Widgets

| Widget | Mida | Fons | Contingut |
| --- | --- | --- | --- |
| **Cronos - Hora Catalana** | 4×1 | transparent | l'hora tradicional |
| **Cronos - Targeta de colors** | 3×2 | el color del moment del dia | l'hora tradicional |
| **Cronos - Widget gran** | 4×2 | transparent | data + hora tradicional |

## Configuració

Obre la roda dentada de la pantalla principal:

- **Mostrar l'hora digital**: mostra l'hora digital sota la tradicional (desactivada per defecte).
- **Segons a l'hora digital**: inclou els segons (HH:mm:ss) només si l'hora digital és activa.
- **Segons a l'hora tradicional**: afegeix els segons per escrit a la frase ("… i N segons").
- **Mida de l'hora**: regulable amb un control lliscant (18–48; per defecte 28).
- **Ubicació**: tria la teva ciutat (o coordenades) per calcular la sortida i la posta de sol.
- **Restablir la configuració**: torna tots els paràmetres als valors per defecte.

## Com Utilitzar l'Aplicació

### Aplicació Principal
- Obre **Cronos** des del calaix d'aplicacions
- Veuràs l'hora catalana destacada al centre, amb la data a sobre.
- El fons canviarà de color segons el moment del dia
- Per canviar els paràmetres hi ha una roda de configuració amunt a la dreta.
- Per escoltar la ràdio o programar l'alarma, entra a la secció de ràdio.

### Afegir el Widget
1. Mantén premuda la pantalla d'inici
2. Selecciona "Widgets"
3. Busca "Cronos": hi ha tres mides/estils per triar
4. Arrossega el widget a la pantalla d'inici
5. Redimensiona'l segons les teves preferències

## Per a Desenvolupadors

### Tecnologies Utilitzades
- **Kotlin**: Llenguatge principal
- **Jetpack Compose**: UI moderna i reactiva
- **Media3 (ExoPlayer)**: reproducció de la ràdio (HLS i MP3)
- **DataStore i kotlinx-serialization**: persistència i estacions
- **WorkManager i AlarmManager**: despertador i reprogramació
- **Android Widget API**: per al widget de pantalla d'inici
- **Coroutines**: Gestió asíncrona
- **Material Design 3**: Components i estils moderns

### Com Contribuir
1. Fes un fork del projecte
2. Crea una branca per la teva funcionalitat
3. Commit dels teus canvis
4. Push a la branca
5. Obre un Pull Request

## Arquitectura (resum)

```
com.filldemaia.cronos/
├── CatalanTimeFormatter.kt     # Lògica de l'hora (minuts i segons per escrit)
├── CatalanDateFormatter.kt     # Formatació de la data en català (app + widget gran)
├── SolarCalculator.kt          # Sortida i posta de sol a partir de la ubicació
├── MainActivity.kt             # UI en Jetpack Compose
├── SettingsRepository.kt       # Preferències (SharedPreferences)
├── SettingsScreen.kt           # Pantalla d'ajustaments
├── CronosBaseWidget.kt         # Base comuna dels tres widgets (cicle de vida i refresc)
├── CronosWidget.kt / CronosWidgetApple.kt / CronosWidgetLarge.kt
├── WidgetUpdateScheduler.kt    # Una sola alarma exacta per minut (fallback inexacte)
├── CronosWidgetTickReceiver.kt # Tick que actualitza tots els widgets
├── BootReceiver.kt             # Reprograma el tick després d'un reinici
├── ui/theme/                   # Paleta unificada app + widgets
└── radio/                      # Ràdio i despertador
    ├── alarm/                  # Programació i rearmament de les alarmes
    ├── playback/               # Servei de reproducció, ring buffer i timeshift
    ├── data/                   # Models, repositoris i emmagatzematge protegit
    ├── domain/                 # Lògica de negoci (planificació, fallback…)
    ├── reliability/            # Comprovacions de fiabilitat de l'alarma
    └── ui/                     # Pantalles de ràdio, alarmes i configuració
```

Decisions tècniques destacades: paleta unificada, una única alarma exacta per a tota l'app (amb comprovació de permís i fallback), i mida d'hora estable regulable per l'usuari. La ràdio fa servir un `ring buffer` en disc per al timeshift i una cadena de fallback (URLs alternatives → emissores de reserva → to local) perquè l'alarma sigui fiable.

## Instal·lació

- **Requisits**: Android 8.0 (API 26) o superior.
- **APK directe**: descarrega la darrera `app-release.aab` / APK des de la secció [Releases](https://github.com/filldemaia/cronos/releases) i instal·la'l amb "Fonts desconegudes" activat.
- **Des del codi**: clona el repositori, obre'l amb Android Studio o similars i executa.
- **A Google Play**: [Com a Cronos a Google Play](https://play.google.com/store/apps/details?id=com.filldemaia.cronos).

## Bateria

- Els widgets es refresquen amb **una sola alarma per minut** (exacta amb `USE_EXACT_ALARM`, amb fallback inexacte), independentment del nombre de widgets.
- La pantalla només recalcula el que canvia: l'hora tradicional al canvi de minut, la data al canvi de dia, i els segons només quan els tens activats.
- El despertador fa servir una alarma exacta per sonar a l'hora prevista, i el servei de ràdio s'atura quan no hi ha reproducció.

## Privadesa

Cronos **no recull cap dada personal**: no hi ha publicitat, ni analítica, ni comptes. L'aplicació només es connecta a internet quan tu decideixes escoltar la ràdio, i aquesta connexió es fa directament amb els servidors de l'emissora (que reben l'adreça IP del dispositiu, com en qualsevol streaming). Vegeu la [política de privadesa](https://filldemaia.github.io/cronos/privacy.html).

## Llicència

Projecte llicenciat sota la **GPL v3 o posterior** — vegeu [LICENSE.md](LICENSE.md).

## Agraïments

- A **Gabriel Mizrahi Mejias**, per les seves contribucions inicials.
- A la **catalanitat**, per la seva bellesa, la seva història i la seva mera existència.
- A **Catalunya**, terra de tradicions, cultura i llengua, que ens ha ensenyat que fins i tot el pas de les hores pot convertir-se en una expressió de la nostra identitat.
- Als **catalans i catalanes** que, malgrat els contratemps, mantenen viva la nostra llengua fent-la servir cada dia.
- I a la **tradició de dir les hores en català**, una petita part del nostre patrimoni lingüístic que mereix ser conservada i transmesa.

## Contacte

Èrik Lledó — lledoerik@gmail.com - [GitHub](https://github.com/filldemaia)

---

**Fet amb amor per ajudar a preservar la llengua, la cultura i la identitat catalana.**

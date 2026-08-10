# Cronos et diu l'hora en català

> L'hora en català tradicional, bonica i llegible, amb un fons que canvia de color segons el moment del dia.

Cronos és una aplicació Android que mostra l'hora de la manera tradicional catalana ("És un quart de set del matí", "Falten cinc minuts per les tres de la tarda") amb una interfície moderna.

## Característiques

- **Interfície renovada**: disseny modern amb Jetpack Compose.
- **Gradient dinàmic en temps real**: el fons passa pel matí, migdia, tarda, vespre i nit amb una transició suau.
- **Hora catalana tradicional** amb el format complet: quarts, minuts, segons i moment del dia.
- **Data completa**: "Avui és dilluns, 10 d'agost del 2026".
- **Salutació** segons el moment del dia ("Bona tarda!", "Bon vespre", ...).
- **Tres widgets** de diferentes mides per a la pantalla d'inici.
- **Segons opcionals**: es poden activar i desactivar a gust els segons ("Falten sis minuts i tres segons per tres quarts de deu de la nit").
- **Suport per temes**: compatible amb mode clar i fosc del sistema.
- Compatible amb Android 7.0+ (API 24).

## Exemples del format

| Hora | Es mostra |
| --- | --- |
| 12:00 | Són les dotze del migdia |
| 15:15 | És un quart de quatre de la tarda |
| 20:30 | Són dos quarts de nou de la nit |
| 22:45 | Són tres quarts d'onze de la nit |
| 08:06 | Són les vuit i sis minuts del matí |
| 14:53 | Falten set minuts per les tres de la tarda |
| 21:39:03 (amb segons) | Falten sis minuts i tres segons per tres quarts de deu de la nit |

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
- **Restableixer la configuració**: torna tots els paràmetres als valors per defecte.

## Com Utilitzar l'Aplicació

### Aplicació Principal
- Obre **Cronos** des del calaix d'aplicacions
- Veuràs l'hora catalana destacada al centre, amb la data a sobre.
- El fons canviarà de color segons el moment del dia
- Per canviar els parametres hi ha una roda de configuració amunt a la dreta.

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
- **Android Widget API**: Per al widget de pantalla d'inici
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
com.example.cronos/
├── CatalanTimeFormatter.kt     # Lògica de l'hora (minuts i segons per escrit)
├── MainActivity.kt             # UI en Jetpack Compose
├── SettingsRepository.kt       # Preferències (SharedPreferences)
├── SettingsScreen.kt           # Pantalla d'ajustaments
├── CronosWidget.kt / CronosWidgetApple.kt / CronosWidgetLarge.kt
├── WidgetUpdateScheduler.kt    # Una sola alarma inexacta per minut
├── CronosWidgetTickReceiver.kt # Tick que actualitza tots els widgets
├── BootReceiver.kt             # Reprograma el tick després d'un reinici
└── ui/theme/                   # Paleta unificada app + widgets
```

Decisions tècniques destacades: paleta unificada (`TimePalette.paletteForHour()`), una única alarma inexacta per tota l'app, i mida d'hora estable regulable per l'usuari.

## Instal·lació

- **Requisits**: Android 7.0 (API 24) o superior.
- **APK directe**: descarrega la darrera `app-release.aab` / APK des de la secció [Releases](https://github.com/lledoerik/cronos/releases) i instal·la'l amb "Fonts desconegudes" activat.
- **Des del codi**: clona el repositori, obre'l amb Android Studio o similars i executa.
- **Futurament estarà disponible a Google Play i a F-Droid.**

## Bateria

- Els widgets es refresquen amb **una sola alarma inexacta per minut** (`setAndAllowWhileIdle`), independentment del nombre de widgets — cap alarma exacta, cap permís especial.
- La pantalla només recalcula el que canvia: l'hora tradicional al canvi de minut, la data al canvi de dia, i els segons només quan els tens activats.

## Privadesa

Cronos **no recull cap dada**. No hi ha publicitat, no hi ha analítica, no hi ha comptes ni permisos d'accés a xarxa: tot funciona localment al dispositiu.

## Llicència

Projecte llicenciat sota la **GPL v3 o posterior** — vegeu [LICENSE.md](LICENSE.md).

## Agraïments

- A **Gabriel Mizrahi Mejias**, per les seves contribucions inicials.
- A la **catalanitat**, per la seva bellesa, la seva història i la seva mera existència.
- A **Catalunya**, terra de tradicions, cultura i llengua, que ens ha ensenyat que fins i tot el pas de les hores pot convertir-se en una expressió de la nostra identitat.
- Als **catalans i catalanes** que, malgrat els contratemps, mantenen viva la nostra llengua fent-la servir cada dia.
- I a la **tradició de dir les hores en català**, una petita part del nostre patrimoni lingüístic que mereix ser conservada i transmesa.

## Contacte

Èrik Calvo Lledó — [GitHub](https://github.com/lledoerik)

---

## 📋 Apèndix: text per a la fitxa de Google Play

**Títol:** Cronos

**Resum (80 caràcters):**
> Consulta fàcilment les hores en català.

**Descripció completa (ja en català, pots afegir alguna en llengua que vulguis):**
> Gaudeix de l'hora en català amb una aplicació elegant i fàcil d'usar. "És un quart de set del matí", "Falten cinc minuts per les tres de la tarda"... Cronos t'ho mostra amb claredad i un degradat de fons que canvia segons el moment del dia.
>
> · Hora catalana completa amb quarts, minuts i moment del dia
> · Data completa amb dia de la setmana ("Avui és dilluns, 10 d'agost del 2026")
> · Tres widgets per a la pantalla d'inici (transparents i un amb color del moment del dia)
> · Segons opcionals, per escrit a la frase tradicional
> · Mida de l'hora ajustable i configuració restableixible
> · Molt eficient: una sola alarma per minut, sense permisos especials
>
> Cronos no recull cap dada i funciona 100% sense connexió.

**Privadesa:** Dades que es comparteixen amb tercers: **Cap**. Dades recollides: **Cap**. (No cal URL de política de privacitat amb aquesta combinació.)

**Categoria:** Ajuts / Personalització. **Temàtiques de contingut:** Sense restriccions.

---

**Fet amb amor per ajudar a preservar la llengua, la cultura i la identitat catalana.**

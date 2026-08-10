# Cronos et diu l'Hora en català

> L'hora en català tradicional, bonica i llegible, amb un fons que canvia de color amb el moment del dia.

Cronos és una aplicació Android que mostra l'hora de la manera tradicional catalana ("És un quart de set del matí", "Falten cinc minuts per les tres de la tarda") amb una interfície moderna, tres widgets per a la pantalla d'inici i un consum de bateria mínim.

## Característiques

- **Interfície renovada**: disseny modern amb Jetpack Compose.
- **Gradient dinàmic en temps real**: el fons passa pel matí, migdia, tarda, vespre i nit amb una transició suau (els colors no s'han tocat: són els que fan la gràcia del projecte 😄).
- **Hora catalana tradicional** amb el format complet: quarts, minuts i moment del dia.
- **Data completa**: "Avui és dilluns, 10 d'agost del 2026", amb dia de la setmana en minúscules i article davant de l'any.
- **Salutació** segons el moment del dia ("Bon dia, Catalunya!", "Bona tarda!"...) i bloc de salutació + data a l'esquerra, ben amunt de la pantalla.
- **Tres widgets** per a la pantalla d'inici, amb previsualització real en afegir-los.
- **Segons opcionals**: a l'hora digital i per escrit a la frase tradicional ("Falten sis minuts i tres segons per tres quarts de deu de la nit").
- **Eficient en bateria**: una sola alarma inexacta per minut per a tots els widgets; la pantalla només refà la feina que canvia.
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
| **Cronos - Hora Catalana** | 4×1 | transparent (text blanc amb ombra) | l'hora tradicional |
| **Cronos - Targeta de colors** | 3×2 | **sí**, el color del moment del dia — l'únic amb fons | l'hora tradicional |
| **Cronos - Widget gran** | 4×2 | transparent | data + hora tradicional (sense hora digital) |

Els widgets transparents es llegeixen sobre qualsevol fons de pantalla, i el de color es pinta segons la mateixa franja horària que el fons de l'app.

## Configuració

Obre la roda dentada de la pantalla principal:

- **Mostrar l'hora digital**: mostra l'hora digital sota la tradicional (desactivada per defecte).
- **Segons a l'hora digital**: inclou els segons (HH:mm:ss). Bloquejat mentre l'hora digital estigui amagada.
- **Segons a l'hora tradicional**: afegeix els segons per escrit a la frase ("… i N segons").
- **Mida de l'hora**: regulable amb un control lliscant (18–48; per defecte 28).
- **Restableixer la configuració**: torna tots els paràmetres als valors per defecte.

## Com Utilitzar l'Aplicació

### Aplicació Principal
- Obre **Cronos** des del calaix d'aplicacions
- Veuràs l'hora catalana destacada al centre, amb la data i la salutació a dalt
- El fons canviarà de color segons el moment del dia

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
- **Des del codi**: clona el repositori, obre'l amb Android Studio i executa.
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

**Títol:** Cronos — Hora Catalana

**Resum (80 caràcters):**
> L'hora en català tradicional, amb el fons que canvia amb el moment del dia.

**Descripció completa (ja en català, pots afegir alguna en llengua que vulguis):**
> Descobreix com es diu l'hora en català tradicional amb una app elegant i fàcil d'usar. "És un quart de set del matí", "Falten cinc minuts per les tres de la tarda"... Cronos t'ho mostra amb lletra gran i un degradat de fons que canvia pel matí, el migdia, la tarda, el vespre i la nit.
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

**Fet amb amor per ajudar a preservar la llengua i la cultura catalana.**
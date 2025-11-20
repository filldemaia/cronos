# Horacat - Hora Catalana per Android 🕐

Una aplicació Android elegant i moderna per mostrar l'hora en català tradicional, ajudant a preservar aquesta bella tradició lingüística.

## 🌟 Característiques Principals

### ✨ Noves Millores (v2.0)
- **Interfície Completament Renovada**: Disseny modern amb Jetpack Compose
- **Gradient Dinàmic**: El fons canvia segons l'hora del dia (alba, matí, tarda, vespre, nit)
- **Widget Millorat**: Actualització automàtica cada minut amb disseny elegant
- **Millor Rendiment**: Codi optimitzat i eliminació de duplicacions
- **Suport per Temes**: Compatible amb mode clar i fosc del sistema
- **Animacions Fluides**: Transicions suaus en els canvis d'hora

### 📱 Funcionalitats
- **Hora Catalana Tradicional**: Mostra l'hora amb el format complet tradicional (quarts, minuts, moment del dia)
- **Hora Digital**: Visualització complementària en format 24h
- **Data en Català**: Mostra el dia de la setmana i la data completa
- **Widget per Pantalla d'Inici**: Accés ràpid a l'hora catalana sense obrir l'app
- **Actualització en Temps Real**: L'hora s'actualitza automàticament cada segon

## 📋 Exemples del Format d'Hora Catalana

- **12:00** → "Són les dotze del migdia"
- **15:15** → "És un quart de quatre de la tarda"
- **20:30** → "Són dos quarts de nou del vespre"
- **22:45** → "Són tres quarts d'onze de la nit"
- **08:07** → "Són les vuit i set minuts del matí"
- **14:53** → "Falten set minuts per les tres de la tarda"

## 🏗️ Arquitectura Millorada

### Estructura del Projecte
```
com.example.horacat/
├── CatalanTimeFormatter.kt    # Lògica centralitzada per formatar l'hora
├── MainActivity.kt             # UI principal amb Jetpack Compose
├── HoracatWidget.kt           # Widget optimitzat
├── BootReceiver.kt            # Gestió del reinici del dispositiu
└── ui/
    └── theme/                 # Temes i estils de l'aplicació
```

### Millores Tècniques Implementades

1. **Eliminació de Codi Duplicat**: Tota la lògica de formatació està centralitzada a `CatalanTimeFormatter`
2. **Jetpack Compose**: UI moderna i declarativa més fàcil de mantenir
3. **Gestió Eficient del Widget**: Actualitzacions programades intel·ligentment
4. **Suport per Diferents Versions d'Android**: Compatible des d'Android 7.0 (API 24)
5. **Optimització de Bateria**: El widget s'actualitza només quan és necessari

## 🛠️ Instal·lació

### Requisits
- Android 7.0 (API 24) o superior
- Android Studio Arctic Fox o superior (per desenvolupadors)

### Opcions d'Instal·lació

#### Opció 1: APK Directe
1. Descarrega el fitxer `horacat-v2.0.apk` des de la secció [Releases](https://github.com/eriklledo/horacat/releases)
2. Activa "Fonts desconegudes" als ajustos del teu dispositiu
3. Obre l'APK descarregat i instal·la l'aplicació

#### Opció 2: Compilar des del Codi Font
```bash
# Clona el repositori
git clone https://github.com/eriklledo/horacat.git

# Obre el projecte amb Android Studio
# Compila i executa l'aplicació
```

## 🎯 Com Utilitzar l'Aplicació

### Aplicació Principal
1. Obre **Horacat** des del calaix d'aplicacions
2. Veuràs l'hora catalana destacada al centre
3. El fons canviarà de color segons el moment del dia

### Afegir el Widget
1. Mantén premuda la pantalla d'inici
2. Selecciona "Widgets"
3. Busca "Horacat"
4. Arrossega el widget a la pantalla d'inici
5. Redimensiona'l segons les teves preferències

## 🔧 Per a Desenvolupadors

### Tecnologies Utilitzades
- **Kotlin**: Llenguatge principal
- **Jetpack Compose**: UI moderna i reactiva
- **Android Widget API**: Per al widget de pantalla d'inici
- **Coroutines**: Gestió asíncrona
- **Material Design 3**: Components i estils moderns

### Com Contribuir
1. Fes un fork del projecte
2. Crea una branca per la teva funcionalitat (`git checkout -b feature/NovaFuncionalitat`)
3. Commit els teus canvis (`git commit -m 'Afegeix nova funcionalitat'`)
4. Push a la branca (`git push origin feature/NovaFuncionalitat`)
5. Obre un Pull Request

## 📝 Llicència

Aquest projecte està llicenciat sota la **GPL 3.0** o posterior - veure [LICENSE.md](LICENSE.md) per més detalls.

## 🙏 Agraïments

- **Gabriel Mizrahi Mejias** - Contribucions inicials
- La comunitat catalana per mantenir vives aquestes tradicions
- Tots els usuaris que ajuden a preservar la llengua

## 📧 Contacte

Èrik Calvo Lledó - [GitHub](https://github.com/eriklledo)

---

**Fet amb ❤️ per ajudar a preservar la llengua i cultura catalanes**

# Auditoria de Cronos — revisió tancada

Projecte: `com.filldemaia.cronos`. Estat i pendents de la revisió.

## Línia base

- Línia base: 31 tests passats; `assembleDebug` correcte; `lintDebug` amb 29 errors NewApi, 89 avisos i 4 indicacions.
- Després de pujar minSdk a 26: 34 tests passats, `assembleDebug` i `lintDebug` correctes.
- `dumpsys alarm` només de lectura confirma les tres alarmes actives de `com.filldemaia.cronos`: 12/11/2026 06:00, 13/11/2026 07:00 i 14/11/2026 09:00.

## Alarmes i fiabilitat: observacions de la línia base

- La generació del to local pot llançar una excepció abans del fallback; el tractament d'errors del servei i l'avís d'emergència necessiten revisió i proves.
- El resultat del rearmament no sempre es comprova; la còpia protegida pot quedar desactualitzada o ser sobreescrita després del desbloqueig.
- «Només demà» confirma abans que acabin desada i rearmament. Prova amb alarma real: **NO VERIFICAT**.
- Un PendingIntent existent no demostra per si sol que l'alarma estigui registrada; la política de No molestar desconeguda pot presentar-se com a segura.
- Widgets i ràdio programen alarmes per camins diferents; tots dos són crítics.

## Compose, rendiment i R8

- Les pantalles utilitzen `collectAsStateWithLifecycle`; el rellotge actualitza estat cada segon. El cost real de recomposició és **NO VERIFICAT**: cal mesurar-lo en dispositiu.
- `PlaybackService` copia periòdicament l'índex complet del buffer al fil principal. Impacte real **NO VERIFICAT**; cal perfilar temps i memòria. No modificar el buffer en aquesta tanda.
- El release habitual té la minificació R8 desactivada. Una compilació experimental `assembleRelease` amb R8 activat mitjançant un script d'inicialització extern ha acabat correctament i ha executat `minifyReleaseWithR8`; execució, reflexió i serialització amb l'APK minificat **NO VERIFICAT**.

## Codi mort

Llista confirmada: **cap element encara**. La cerca de referències textuals no és una prova suficient de l'absència d'usos indirectes.

Candidats pendents: `AlarmScheduler.clearTestAlarm`, `ScheduleRepository.disableOneShot`, `DeviceProtectedStore.removeSchedule`, `FrameTimeline.frameAtOrBefore`, `frameAtOrAfter`, `timeAtOffset`, i el tema `com.filldemaia.cronos.ui.theme.CronosTheme`. Lint també assenyala 26 recursos aparentment no utilitzats. Cal contrastar-los amb recursos, variants, tests i referències dinàmiques abans de confirmar-los. No se n'ha eliminat cap.

## Fora d'abast

HTTP en clar: inventariar alternatives abans de restringir-lo. Navegador multimèdia exportat: provar clients abans de limitar accessos. Wake locks: mesurar consum i verificar alliberament. Backup: provar restauració entre dispositius. Cursa del buffer: reproduir-la amb prova concurrent. Cap d'aquests punts no s'ha de modificar en aquesta tanda.

## Estat de la revisió

El commit `1f59ee0` cobreix parcialment el to: una fallada de generació o del reproductor prova el to del sistema, i si tots dos fallen intenta l'avís d'emergència. Les decisions es proven amb falsos; àudio i notificacions Android **NO VERIFICAT**. El sistema pot denegar notificacions o acabar el procés, i no es pot garantir so en tots els casos.

Canvis posteriors: `d7fd407` continua després d'un error de foreground i intenta avisar; `cef29c0` reintenta el rearmament després del dispar i avisa si falla; `b903ed6` confirma «Només demà» després de desar i armar; `87cd89c` tracta com a desconegut un PendingIntent existent i els estats No molestar que no es poden determinar. Tests purs amb falsos; integració Android **NO VERIFICADA**.

El commit `e82767c` fa les escriptures protegides atòmiques, serialitza les escriptures dins del procés i informa de fallades. Els avisos d'error es mantenen en memòria i es perden quan el procés acaba. Funcionament al dispositiu **NO VERIFICAT**.

**PENDENT — conciliació CE/DE:** `armAll()` encara pot sobreescriure una còpia protegida més recent amb dades antigues de DataStore. En particular, una alarma d'un sol cop disparada abans del primer desbloqueig pot tornar a quedar activa després de desbloquejar. Cal dissenyar i provar una conciliació versionada i transaccional, incloent escriptures concurrents, reinicis i interrupcions. Aquest cas concret afecta alarmes d'un sol cop; no s'ha verificat que les diàries siguin immunes a altres inconsistències de la còpia.

Android no ofereix en general una API pública per enumerar les alarmes de l'app i verificar cada PendingIntent; el diagnòstic informa desconeixement en lloc d'afirmar que estan registrades. No s'han eliminat recursos ni codi, ni aplicat optimitzacions.

Verificació final: 49 tests passats, `assembleDebug` i `lintDebug` correctes. `adb` no ha trobat cap dispositiu connectat en la comprovació final; les tres alarmes **NO VERIFICADES** en aquesta darrera comprovació (la lectura anterior havia coincidit amb la línia base).

L'APK debug té el paquet `com.filldemaia.cronos.debug`, diferent de l'app real `com.filldemaia.cronos`: instal·lar-lo no actualitza l'app real ni comparteix les seves dades o alarmes. No s'ha instal·lat cap APK durant la revisió.

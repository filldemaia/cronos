# Ginys de Cronos

| Nom | Component conservat | Mida del text per defecte |
| --- | --- | --- |
| Data I Hora | `CronosWidgetLarge` | 27 |
| Hora | `CronosWidget` | 27 |
| Senyera | `CronosWidgetApple` | 27 |

Els noms interns es conserven perquè l'actualització no desconnecti els
widgets que ja són a la pantalla d'inici.

## Mida del text

**Configuració → Widgets** permet ajustar cada tipus per separat, de 12 a 50.
Tots els rellotges (l'app i els tres widgets) comparteixen aquest rang i el
valor per defecte de 27.
El valor és el màxim desitjat: si no hi cap la frase completa, es redueix
el text per evitar retalls. A «Data I Hora» la data manté una mida menor
que l'hora. Els ajustos s'apliquen en deixar anar el control, també als
widgets ja afegits, i es recalculen en redimensionar-los.

El restabliment dels widgets no modifica el rellotge ni les alarmes, i
«Restableix el rellotge» no modifica les mides dels widgets.
La previsualització aplica els mateixos `RemoteViews` que el llançador;
no és una imatge aproximada del disseny.

Els lliscants de l'app (mida de l'hora, volum de l'alarma i les tres mides
dels widgets) comparteixen un mateix component prim i uniforme, sense marques
gruixudes.

## Senyera

- Quatre franges vermelles sobre cinc franges grogues, fixes i horitzontals.
- Senyera completa, sense panells opacs ni colors solars.
- Una lona negra translúcida i uniforme (`#40000000`) sobre tota la bandera.
- Text `#F7F4EC` amb una ombra suau, que li dona volum i el separa de les
  franges sense enfosquir-les.
- Radi exterior del sistema a Android 12 o posterior; 24 dp a versions
  anteriors. Marge de text de 16 dp.
- No necessita ciutat, coordenades ni connexió.

La lona uniforme manté la bandera visible per igual i l'ombra del text aporta
profunditat, una de les alternatives de la tècnica G18 del W3C per a text
sobre fons amb patrons.

Com que `RemoteViews` no admet un `TextView` personalitzat, només les lletres
es dibuixen en un bitmap transparent (amb l'ombra) i la lona és un degradat
sòlid vectorial que s'escala amb el widget. El bitmap respecta la mida
escollida i l'escala del sistema, té un pressupost de memòria limitat i
exposa la frase com a descripció accessible.

## Refresc dels widgets ja afegits

El refresc configura els botons de cada orientació **abans** de combinar
els `RemoteViews`. La versió anterior ho feia després: Android rebutjava
l'acció i el widget del llançador no rebia el text ni els canvis de mida,
encara que la previsualització dins de l'app funcionés.

Els layouts nous forcen una reinflació de les vistes antigues sense canviar
els components dels providers ni els identificadors dels widgets. No cal
eliminar-los ni tornar-los a afegir.

Fonts consultades:

- [Android: estil dels widgets](https://developer.android.com/design/ui/mobile/guides/widgets/style)
- [Android: visibilitat i contrast del text](https://developer.android.com/guide/topics/ui/accessibility/apps#color-contrast)
- [W3C G18: contrast sobre fons uniformes i amb patrons](https://www.w3.org/WAI/WCAG22/Techniques/general/G18)

## Comprovacions del 18 de novembre

- 68 proves d'unitat passades, incloses cinc proves noves de valors i límits.
- Lint release sense errors; queden avisos previs del projecte.
- APK Nightly optimitzat amb R8, instal·lat únicament al perfil principal.
- Tres controls independents, persistència després de reobrir, restabliment
  de widgets sense modificar l'opció dels segons del rellotge.
- `RemoteViews` dels tres widgets renderitzats al Galaxy S21 Ultra, amb
  valors per defecte i amb mides diferents; frases llargues llegibles.
- Sense errors d'inflació als logs ni fitxers de proves deixats al telèfon.
- Cronos de Play i el seu widget existent no s'han modificat.

La comprovació visual s'ha fet amb la previsualització dels layouts reals,
no afegint widgets ni movent icones a la pantalla d'inici de l'usuari.
Queden pendents la prova de redimensionament en un host real i els casos
de mides mínimes amb escala de lletra del sistema molt elevada.

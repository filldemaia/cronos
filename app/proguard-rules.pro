# WidgetUpdateScheduler instancia aquests providers per reflexió.
# Només cal conservar-ne el constructor sense arguments; la resta del
# codi pot continuar optimitzant-se. AGP preserva els noms del manifest.
-keepclassmembers class com.filldemaia.cronos.CronosWidget {
    public <init>();
}
-keepclassmembers class com.filldemaia.cronos.CronosWidgetApple {
    public <init>();
}
-keepclassmembers class com.filldemaia.cronos.CronosWidgetLarge {
    public <init>();
}

# Room 2.x (transitiu de WorkManager) crea la implementació de la base
# de dades per reflexió. Amb strictFullModeForKeepRules, el -keep sense
# membres de la biblioteca no implica conservar aquest constructor.
-keepclassmembers class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}

# Les biblioteques (Media3, WorkManager, DataStore i serialization) aporten
# les seves consumer rules. No bloquegem paquets sencers amb -keep **.
# Conservem les línies per desofuscar informes amb mapping.txt.
-keepattributes SourceFile,LineNumberTable

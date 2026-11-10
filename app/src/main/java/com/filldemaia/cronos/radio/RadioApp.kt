package com.filldemaia.cronos.radio

import android.app.Application
import android.content.Context
import android.os.UserManager
import com.filldemaia.cronos.radio.di.AppContainer
import com.filldemaia.cronos.radio.reliability.ReliabilityWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RadioApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // En obrir el procés, rearmem per si el fabricant o una actualització
        // han eliminat les alarmes. També s'executa en direct boot, abans del
        // primer desbloqueig, gràcies a la còpia protegida pel dispositiu.
        appScope.launch {
            runCatching { container.alarmScheduler.armAll() }
            if (userUnlocked()) {
                runCatching { container.refreshReliabilityNotification() }
            }
        }
        if (userUnlocked()) {
            ReliabilityWorker.enqueue(this)
        }
    }

    private fun userUnlocked(): Boolean = runCatching {
        (getSystemService(Context.USER_SERVICE) as UserManager).isUserUnlocked
    }.getOrDefault(true)
}

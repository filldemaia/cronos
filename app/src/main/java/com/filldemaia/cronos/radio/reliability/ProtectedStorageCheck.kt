package com.filldemaia.cronos.radio.reliability

import com.filldemaia.cronos.radio.data.model.CheckLevel
import com.filldemaia.cronos.radio.data.model.ReliabilityAction
import com.filldemaia.cronos.radio.data.model.ReliabilityCheck

internal fun protectedStorageCheck(errors: Map<String, String>): ReliabilityCheck? {
    if (errors.isEmpty()) return null
    return ReliabilityCheck(
        id = "protected_storage",
        title = "Còpia d'alarmes abans del desbloqueig",
        detail = "No s'ha pogut actualitzar la còpia protegida. Revisa l'emmagatzematge i torna a rearmar.",
        level = CheckLevel.ERROR,
        actionLabel = "Rearma",
        action = ReliabilityAction.ARM_ALARMS
    )
}

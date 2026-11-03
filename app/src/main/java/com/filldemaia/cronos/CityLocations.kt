package com.filldemaia.cronos

/**
 * Ciutats amb les seves coordenades, per calcular la sortida i la posta
 * de sol. Són les principals poblacions dels territoris de parla catalana,
 * amb coordenades en graus decimals (WGS84). No hi ha cap connexió ni
 * permís de localització: l'usuari tria la ciutat a Configuració.
 */
data class CityLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

object Cities {

    /** Valor per defecte quan l'usuari no ha triat res. */
    val default: CityLocation = CityLocation("Barcelona", 41.3874, 2.1686)

    val all: List<CityLocation> = listOf(
        CityLocation("Alacant", 38.3452, -0.4810),
        CityLocation("Andorra la Vella", 42.5063, 1.5218),
        CityLocation("Barcelona", 41.3874, 2.1686),
        CityLocation("Castelló de la Plana", 39.9864, -0.0513),
        CityLocation("Eivissa", 38.9067, 1.4206),
        CityLocation("Girona", 41.9794, 2.8214),
        CityLocation("Lleida", 41.6176, 0.6200),
        CityLocation("Maó", 39.8895, 4.2643),
        CityLocation("Palma", 39.5696, 2.6502),
        CityLocation("Perpinyà", 42.6887, 2.8948),
        CityLocation("Tarragona", 41.1189, 1.2445),
        CityLocation("València", 39.4699, -0.3763),
    )
}

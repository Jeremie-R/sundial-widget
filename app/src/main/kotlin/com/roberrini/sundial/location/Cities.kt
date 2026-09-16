package com.roberrini.sundial.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class City(val id: String, val name: String, val country: String, val lat: Double, val lon: Double) {
    val label: String get() = "$name, $country"
}

/**
 * Offline list for the manual fallback. City-level accuracy is all sunrise/sunset needs:
 * 50 km of error moves the times by a couple of minutes.
 */
object Cities {

    val all: List<City> = listOf(
        // France
        c("paris", "Paris", "France", 48.86, 2.35), c("marseille", "Marseille", "France", 43.30, 5.37),
        c("lyon", "Lyon", "France", 45.76, 4.84), c("toulouse", "Toulouse", "France", 43.60, 1.44),
        c("nice", "Nice", "France", 43.71, 7.26), c("nantes", "Nantes", "France", 47.22, -1.55),
        c("strasbourg", "Strasbourg", "France", 48.57, 7.75), c("montpellier", "Montpellier", "France", 43.61, 3.88),
        c("bordeaux", "Bordeaux", "France", 44.84, -0.58), c("lille", "Lille", "France", 50.63, 3.06),
        c("rennes", "Rennes", "France", 48.11, -1.68), c("grenoble", "Grenoble", "France", 45.19, 5.72),
        // Europe
        c("london", "London", "United Kingdom", 51.51, -0.13), c("edinburgh", "Edinburgh", "United Kingdom", 55.95, -3.19),
        c("dublin", "Dublin", "Ireland", 53.35, -6.26), c("brussels", "Brussels", "Belgium", 50.85, 4.35),
        c("amsterdam", "Amsterdam", "Netherlands", 52.37, 4.90), c("luxembourg", "Luxembourg", "Luxembourg", 49.61, 6.13),
        c("geneva", "Geneva", "Switzerland", 46.20, 6.14), c("zurich", "Zurich", "Switzerland", 47.38, 8.54),
        c("berlin", "Berlin", "Germany", 52.52, 13.41), c("munich", "Munich", "Germany", 48.14, 11.58),
        c("hamburg", "Hamburg", "Germany", 53.55, 9.99), c("frankfurt", "Frankfurt", "Germany", 50.11, 8.68),
        c("vienna", "Vienna", "Austria", 48.21, 16.37), c("prague", "Prague", "Czechia", 50.08, 14.44),
        c("warsaw", "Warsaw", "Poland", 52.23, 21.01), c("budapest", "Budapest", "Hungary", 47.50, 19.04),
        c("copenhagen", "Copenhagen", "Denmark", 55.68, 12.57), c("oslo", "Oslo", "Norway", 59.91, 10.75),
        c("stockholm", "Stockholm", "Sweden", 59.33, 18.07), c("helsinki", "Helsinki", "Finland", 60.17, 24.94),
        c("reykjavik", "Reykjavík", "Iceland", 64.15, -21.94), c("tromso", "Tromsø", "Norway", 69.65, 18.96),
        c("madrid", "Madrid", "Spain", 40.42, -3.70), c("barcelona", "Barcelona", "Spain", 41.39, 2.17),
        c("lisbon", "Lisbon", "Portugal", 38.72, -9.14), c("rome", "Rome", "Italy", 41.90, 12.50),
        c("milan", "Milan", "Italy", 45.46, 9.19), c("naples", "Naples", "Italy", 40.85, 14.27),
        c("athens", "Athens", "Greece", 37.98, 23.73), c("istanbul", "Istanbul", "Türkiye", 41.01, 28.98),
        c("bucharest", "Bucharest", "Romania", 44.43, 26.10), c("kyiv", "Kyiv", "Ukraine", 50.45, 30.52),
        c("moscow", "Moscow", "Russia", 55.76, 37.62),
        // Africa & Middle East
        c("casablanca", "Casablanca", "Morocco", 33.57, -7.59), c("algiers", "Algiers", "Algeria", 36.75, 3.06),
        c("tunis", "Tunis", "Tunisia", 36.81, 10.18), c("cairo", "Cairo", "Egypt", 30.04, 31.24),
        c("dakar", "Dakar", "Senegal", 14.72, -17.47), c("abidjan", "Abidjan", "Côte d'Ivoire", 5.36, -4.01),
        c("lagos", "Lagos", "Nigeria", 6.52, 3.38), c("nairobi", "Nairobi", "Kenya", -1.29, 36.82),
        c("addis", "Addis Ababa", "Ethiopia", 9.03, 38.74), c("johannesburg", "Johannesburg", "South Africa", -26.20, 28.05),
        c("capetown", "Cape Town", "South Africa", -33.93, 18.42), c("antananarivo", "Antananarivo", "Madagascar", -18.88, 47.51),
        c("telaviv", "Tel Aviv", "Israel", 32.09, 34.78), c("beirut", "Beirut", "Lebanon", 33.89, 35.50),
        c("riyadh", "Riyadh", "Saudi Arabia", 24.71, 46.68), c("dubai", "Dubai", "UAE", 25.20, 55.27),
        c("tehran", "Tehran", "Iran", 35.69, 51.39),
        // Asia & Oceania
        c("karachi", "Karachi", "Pakistan", 24.86, 67.01), c("delhi", "New Delhi", "India", 28.61, 77.21),
        c("mumbai", "Mumbai", "India", 19.08, 72.88), c("bangalore", "Bengaluru", "India", 12.97, 77.59),
        c("dhaka", "Dhaka", "Bangladesh", 23.81, 90.41), c("bangkok", "Bangkok", "Thailand", 13.76, 100.50),
        c("hanoi", "Hanoi", "Vietnam", 21.03, 105.85), c("singapore", "Singapore", "Singapore", 1.35, 103.82),
        c("jakarta", "Jakarta", "Indonesia", -6.21, 106.85), c("manila", "Manila", "Philippines", 14.60, 120.98),
        c("hongkong", "Hong Kong", "China", 22.32, 114.17), c("shanghai", "Shanghai", "China", 31.23, 121.47),
        c("beijing", "Beijing", "China", 39.90, 116.41), c("taipei", "Taipei", "Taiwan", 25.03, 121.57),
        c("seoul", "Seoul", "South Korea", 37.57, 126.98), c("tokyo", "Tokyo", "Japan", 35.68, 139.69),
        c("osaka", "Osaka", "Japan", 34.69, 135.50), c("sydney", "Sydney", "Australia", -33.87, 151.21),
        c("melbourne", "Melbourne", "Australia", -37.81, 144.96), c("perth", "Perth", "Australia", -31.95, 115.86),
        c("auckland", "Auckland", "New Zealand", -36.85, 174.76), c("noumea", "Nouméa", "New Caledonia", -22.28, 166.46),
        c("papeete", "Papeete", "French Polynesia", -17.54, -149.57),
        // Americas
        c("anchorage", "Anchorage", "USA", 61.22, -149.90), c("vancouver", "Vancouver", "Canada", 49.28, -123.12),
        c("seattle", "Seattle", "USA", 47.61, -122.33), c("sanfrancisco", "San Francisco", "USA", 37.77, -122.42),
        c("losangeles", "Los Angeles", "USA", 34.05, -118.24), c("denver", "Denver", "USA", 39.74, -104.99),
        c("chicago", "Chicago", "USA", 41.88, -87.63), c("houston", "Houston", "USA", 29.76, -95.37),
        c("toronto", "Toronto", "Canada", 43.65, -79.38), c("montreal", "Montréal", "Canada", 45.50, -73.57),
        c("newyork", "New York", "USA", 40.71, -74.01), c("washington", "Washington, DC", "USA", 38.91, -77.04),
        c("miami", "Miami", "USA", 25.76, -80.19), c("mexico", "Mexico City", "Mexico", 19.43, -99.13),
        c("bogota", "Bogotá", "Colombia", 4.71, -74.07), c("lima", "Lima", "Peru", -12.05, -77.04),
        c("santiago", "Santiago", "Chile", -33.45, -70.67), c("buenosaires", "Buenos Aires", "Argentina", -34.60, -58.38),
        c("saopaulo", "São Paulo", "Brazil", -23.55, -46.63), c("rio", "Rio de Janeiro", "Brazil", -22.91, -43.17),
        c("cayenne", "Cayenne", "French Guiana", 4.94, -52.33), c("fortdefrance", "Fort-de-France", "Martinique", 14.62, -61.07),
        c("pointeapitre", "Pointe-à-Pitre", "Guadeloupe", 16.24, -61.53), c("saintdenis", "Saint-Denis", "Réunion", -20.88, 55.45),
    ).sortedBy { it.name }

    fun byId(id: String?): City? = id?.let { wanted -> all.firstOrNull { it.id == wanted } }

    /** Closest listed city, used to describe a device location as "Near Lyon". */
    fun nearest(lat: Double, lon: Double): City = all.minBy { distanceKm(lat, lon, it.lat, it.lon) }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun c(id: String, name: String, country: String, lat: Double, lon: Double) = City(id, name, country, lat, lon)
}

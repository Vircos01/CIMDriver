import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.util.Locale

fun main() {
    val startLat = 52.3676
    val startLon = 4.9041
    val endLat = 51.9225
    val endLon = 4.47917
    val coords = String.format(Locale.US, "%f,%f;%f,%f", startLon, startLat, endLon, endLat)
    val urlString = "https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson"
    println("URL: $urlString")
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    val response = connection.inputStream.bufferedReader().readText()
    val jsonObject = JSONObject(response)
    println("Code: " + jsonObject.getString("code"))
    val routes = jsonObject.getJSONArray("routes")
    val firstRoute = routes.getJSONObject(0)
    val geometry = firstRoute.getJSONObject("geometry")
    val coordinates = geometry.getJSONArray("coordinates")
    println("Points count: " + coordinates.length())
}

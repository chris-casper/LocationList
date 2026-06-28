// app/src/main/java/im/casper/locationlist/data/KmlExport.kt
package im.casper.locationlist.data

/**
 * Builds a standard KML document from saved locations. Locations are grouped into <Folder>
 * elements by their first group (so a round-trip through [KmlImport] restores that group);
 * full groups, tags, and notes are also written to <ExtendedData> so nothing is lost.
 * Only locations that have coordinates are exported.
 */
object KmlExport {

    fun build(locations: List<Location>, documentName: String = "LocationList export"): String {
        val withCoords = locations.filter { it.latitude != null && it.longitude != null }

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        sb.append("  <Document>\n")
        sb.append("    <name>").append(esc(documentName)).append("</name>\n")

        val grouped = withCoords.groupBy { it.groups.firstOrNull() }

        // Ungrouped placemarks sit directly under the document.
        grouped[null]?.forEach { appendPlacemark(sb, it, "    ") }

        // Then a folder per group, alphabetically.
        grouped.entries
            .filter { it.key != null }
            .sortedBy { it.key }
            .forEach { (group, items) ->
                sb.append("    <Folder>\n")
                sb.append("      <name>").append(esc(group!!)).append("</name>\n")
                items.forEach { appendPlacemark(sb, it, "      ") }
                sb.append("    </Folder>\n")
            }

        sb.append("  </Document>\n")
        sb.append("</kml>\n")
        return sb.toString()
    }

    private fun appendPlacemark(sb: StringBuilder, loc: Location, indent: String) {
        val lat = loc.latitude ?: return
        val lng = loc.longitude ?: return

        sb.append("$indent<Placemark>\n")
        sb.append("$indent  <name>").append(esc(loc.name)).append("</name>\n")
        if (loc.description.isNotBlank()) {
            sb.append("$indent  <description>").append(esc(loc.description)).append("</description>\n")
        }

        if (loc.groups.isNotEmpty() || loc.tags.isNotEmpty() || loc.notes.isNotBlank()) {
            sb.append("$indent  <ExtendedData>\n")
            if (loc.groups.isNotEmpty()) {
                sb.append("$indent    <Data name=\"groups\"><value>")
                    .append(esc(loc.groups.joinToString(", "))).append("</value></Data>\n")
            }
            if (loc.tags.isNotEmpty()) {
                sb.append("$indent    <Data name=\"tags\"><value>")
                    .append(esc(loc.tags.joinToString(", "))).append("</value></Data>\n")
            }
            if (loc.notes.isNotBlank()) {
                sb.append("$indent    <Data name=\"notes\"><value>")
                    .append(esc(loc.notes)).append("</value></Data>\n")
            }
            sb.append("$indent  </ExtendedData>\n")
        }

        // KML coordinates are longitude,latitude (lng first).
        sb.append("$indent  <Point><coordinates>$lng,$lat</coordinates></Point>\n")
        sb.append("$indent</Placemark>\n")
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

// app/src/main/java/im/casper/locationlist/data/KmlImport.kt
package im.casper.locationlist.data

import android.content.Context
import android.net.Uri
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses a KML or KMZ file (chosen via the Storage Access Framework) into a list of
 * [Location] objects ready to insert. KML <Folder> names become a group on each contained
 * placemark. Photos inside KMZ archives are not imported in this version.
 */
object KmlImport {

    fun parse(context: Context, uri: Uri): List<Location> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return emptyList()
        val kmlBytes = if (isZip(bytes)) extractKmlFromKmz(bytes) else bytes
        return if (kmlBytes == null) emptyList() else parseKml(kmlBytes)
    }

    // KMZ (zip) files start with the bytes "PK".
    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    private fun extractKmlFromKmz(bytes: ByteArray): ByteArray? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var firstKml: ByteArray? = null
            var entry = zip.nextEntry
            while (entry != null) {
                val nameLc = entry.name.lowercase()
                if (nameLc.endsWith(".kml")) {
                    val data = zip.readBytes()
                    // doc.kml is the conventional main document; prefer it.
                    if (nameLc == "doc.kml" || nameLc.endsWith("/doc.kml")) return data
                    if (firstKml == null) firstKml = data
                }
                entry = zip.nextEntry
            }
            return firstKml
        }
    }

    private fun parseKml(bytes: ByteArray): List<Location> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        doc.documentElement.normalize()
        val out = mutableListOf<Location>()
        walk(doc.documentElement, currentGroup = null, out = out)
        return out
    }

    // Recurse through the tree, carrying the enclosing folder name as the group.
    private fun walk(node: Element, currentGroup: String?, out: MutableList<Location>) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child !is Element) continue
            when (localName(child)) {
                "Folder" -> walk(child, directChildText(child, "name")?.trim() ?: currentGroup, out)
                "Document" -> walk(child, currentGroup, out)
                "Placemark" -> parsePlacemark(child, currentGroup)?.let { out.add(it) }
                else -> walk(child, currentGroup, out)
            }
        }
    }

    private fun parsePlacemark(placemark: Element, group: String?): Location? {
        val coordsText = firstDescendant(placemark, "coordinates")?.textContent?.trim()
            ?: return null
        // A Point's coordinates are "lng,lat[,alt]" — note KML puts longitude first.
        val firstTuple = coordsText.split(Regex("\\s+")).firstOrNull { it.isNotBlank() }
            ?: return null
        val parts = firstTuple.split(",")
        if (parts.size < 2) return null
        val lng = parts[0].trim().toDoubleOrNull() ?: return null
        val lat = parts[1].trim().toDoubleOrNull() ?: return null

        val name = directChildText(placemark, "name")?.trim().orEmpty()
        val description = directChildText(placemark, "description")?.trim().orEmpty()

        return Location(
            name = name.ifBlank { "Imported location" },
            latitude = lat,
            longitude = lng,
            description = description,
            notes = "",
            groups = group?.let { listOf(it) } ?: emptyList(),
            tags = emptyList(),
            photoUris = emptyList(),
        )
    }

    private fun localName(element: Element): String = element.tagName.substringAfterLast(':')

    // First *direct* child element with the given (un-prefixed) tag name.
    private fun directChildText(parent: Element, tag: String): String? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val c = children.item(i)
            if (c is Element && localName(c) == tag) return c.textContent
        }
        return null
    }

    // First descendant element (any depth) with the given tag name.
    private fun firstDescendant(parent: Element, tag: String): Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val c = children.item(i)
            if (c is Element) {
                if (localName(c) == tag) return c
                firstDescendant(c, tag)?.let { return it }
            }
        }
        return null
    }
}

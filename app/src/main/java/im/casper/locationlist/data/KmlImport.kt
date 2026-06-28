// app/src/main/java/im/casper/locationlist/data/KmlImport.kt
package im.casper.locationlist.data

import android.content.Context
import android.net.Uri
import im.casper.locationlist.util.ImageStorage
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses a KML or KMZ file into [Location] objects. KML <Folder> names become a group.
 * For KMZ archives, photos referenced by a placemark (via <img src> in the description or
 * an image <href>) that are present in the archive are extracted and saved.
 *
 * Limitation: photos referenced through a *shared* <Style> (styleUrl) rather than inline are
 * not resolved; remote (http) image URLs are not downloaded.
 */
object KmlImport {

    private val IMAGE_EXTS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")

    fun parse(context: Context, uri: Uri): List<Location> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return emptyList()
        return if (isZip(bytes)) {
            val archive = readKmz(bytes)
            val kml = archive.kml ?: return emptyList()
            parseKml(kml) { placemark -> extractPhotos(context, placemark, archive.images) }
        } else {
            parseKml(bytes) { emptyList() }
        }
    }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    private class Kmz(val kml: ByteArray?, val images: Map<String, ByteArray>)

    private fun readKmz(bytes: ByteArray): Kmz {
        var kml: ByteArray? = null
        var firstKml: ByteArray? = null
        val images = HashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val nameLc = entry.name.lowercase()
                    val data = zip.readBytes()
                    when {
                        nameLc.endsWith(".kml") -> {
                            if (nameLc == "doc.kml" || nameLc.endsWith("/doc.kml")) kml = data
                            else if (firstKml == null) firstKml = data
                        }
                        IMAGE_EXTS.any { nameLc.endsWith(it) } -> images[entry.name] = data
                    }
                }
                entry = zip.nextEntry
            }
        }
        return Kmz(kml ?: firstKml, images)
    }

    private fun parseKml(bytes: ByteArray, photosFor: (Element) -> List<String>): List<Location> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        doc.documentElement.normalize()
        val out = mutableListOf<Location>()
        walk(doc.documentElement, null, out, photosFor)
        return out
    }

    private fun walk(
        node: Element,
        group: String?,
        out: MutableList<Location>,
        photosFor: (Element) -> List<String>,
    ) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child !is Element) continue
            when (localName(child)) {
                "Folder" ->
                    walk(child, directChildText(child, "name")?.trim() ?: group, out, photosFor)
                "Document" -> walk(child, group, out, photosFor)
                "Placemark" -> parsePlacemark(child, group, photosFor)?.let { out.add(it) }
                else -> walk(child, group, out, photosFor)
            }
        }
    }

    private fun parsePlacemark(
        placemark: Element,
        group: String?,
        photosFor: (Element) -> List<String>,
    ): Location? {
        val coordsText = firstDescendant(placemark, "coordinates")?.textContent?.trim()
            ?: return null
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
            photoUris = photosFor(placemark),
        )
    }

    private fun extractPhotos(
        context: Context,
        placemark: Element,
        images: Map<String, ByteArray>,
    ): List<String> {
        if (images.isEmpty()) return emptyList()

        val candidates = LinkedHashSet<String>()
        // 1) <img src="..."> inside the (often HTML) description.
        directChildText(placemark, "description")?.let { desc ->
            Regex("""src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .findAll(desc).forEach { candidates.add(it.groupValues[1]) }
        }
        // 2) any <href> descendants (e.g. an inline IconStyle icon).
        allDescendants(placemark, "href").forEach { candidates.add(it.textContent.trim()) }

        val saved = mutableListOf<String>()
        for (candidate in candidates) {
            val base = candidate.substringAfterLast('/').lowercase()
            if (IMAGE_EXTS.none { base.endsWith(it) }) continue
            val match = images.entries.firstOrNull { (entryName, _) ->
                entryName.substringAfterLast('/').equals(base, ignoreCase = true)
            } ?: continue
            saved.add(ImageStorage.saveBytes(context, match.value))
        }
        return saved
    }

    private fun localName(element: Element): String = element.tagName.substringAfterLast(':')

    private fun directChildText(parent: Element, tag: String): String? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val c = children.item(i)
            if (c is Element && localName(c) == tag) return c.textContent
        }
        return null
    }

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

    private fun allDescendants(parent: Element, tag: String): List<Element> {
        val result = mutableListOf<Element>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val c = children.item(i)
            if (c is Element) {
                if (localName(c) == tag) result.add(c)
                result.addAll(allDescendants(c, tag))
            }
        }
        return result
    }
}

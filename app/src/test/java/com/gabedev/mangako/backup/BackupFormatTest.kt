package com.gabedev.mangako.backup

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val format = BackupFormat()

    @Test
    fun `version 2 round trip preserves volume ownership and preview counts`() {
        val payload = payload(
            volumes = listOf(
                volume(id = "v1", number = 1f, owned = true),
                volume(id = "v2", number = 2f, owned = false),
            )
        )

        val parsed = format.decode(
            format.encode(payload, createdAt = CREATED_AT, appVersionCode = 7)
        )

        assertEquals(BackupFormat.FORMAT_VERSION, parsed.document.formatVersion)
        assertEquals(payload, parsed.document.payload)
        assertEquals(1, parsed.preview.mangaCount)
        assertEquals(2, parsed.preview.volumeCount)
        assertEquals(1, parsed.preview.ownedVolumeCount)
    }

    @Test
    fun `version 1 ownedVolumes defaults every restored volume to owned`() {
        val parsed = format.decode(VERSION_1_BACKUP.trimIndent().encodeToByteArray())

        assertEquals(1, parsed.document.formatVersion)
        assertEquals("v1", parsed.document.payload.collection.single().volumes.single().id)
        assertTrue(parsed.document.payload.collection.single().volumes.single().owned)
    }

    @Test
    fun `changed payload is rejected when checksum no longer matches`() {
        val encoded = format.encode(payload(), CREATED_AT, 1)
            .decodeToString()
            .replace("Frieren", "Tampered")

        val error = assertFailsWith<IllegalArgumentException> {
            format.decode(encoded.encodeToByteArray())
        }

        assertEquals("Backup checksum is invalid", error.message)
    }

    @Test
    fun `unsupported format version is rejected`() {
        val encoded = format.encode(payload(), CREATED_AT, 1)
            .decodeToString()
            .replace("\"formatVersion\":2", "\"formatVersion\":3")

        val error = assertFailsWith<IllegalArgumentException> {
            format.decode(encoded.encodeToByteArray())
        }

        assertEquals("Unsupported backup version", error.message)
    }

    @Test
    fun `volume referencing another manga is rejected`() {
        val invalid = payload(volumes = listOf(volume().copy(mangaId = "other")))

        val error = assertFailsWith<IllegalArgumentException> {
            format.decode(format.encode(invalid, CREATED_AT, 1))
        }

        assertEquals("Volume references another manga", error.message)
    }

    @Test
    fun `duplicate logical numbered volume is rejected`() {
        val invalid = payload(
            volumes = listOf(
                volume(id = "v1", number = 1f),
                volume(id = "v2", number = 1f),
            )
        )

        val error = assertFailsWith<IllegalArgumentException> {
            format.decode(format.encode(invalid, CREATED_AT, 1))
        }

        assertEquals("Duplicate volume", error.message)
    }

    @Test
    fun `invalid settings enum is rejected`() {
        val invalid = payload().copy(
            settings = settings().copy(backupFrequency = "HOURLY")
        )

        val error = assertFailsWith<IllegalArgumentException> {
            format.decode(format.encode(invalid, CREATED_AT, 1))
        }

        assertEquals("Invalid backup frequency", error.message)
    }

    private fun payload(
        volumes: List<BackupVolume> = listOf(volume()),
    ) = BackupPayload(
        collection = listOf(
            BackupManga(
                id = "m1",
                title = "Frieren",
                volumes = volumes,
            )
        ),
        settings = settings(),
    )

    private fun volume(
        id: String = "v1",
        number: Float = 1f,
        owned: Boolean = true,
    ) = BackupVolume(
        id = id,
        mangaId = "m1",
        title = "Frieren",
        number = number,
        locale = "ja",
        isSpecialEdition = false,
        owned = owned,
    )

    private fun settings() = BackupSettings(
        viewMode = "grid",
        collectionDensity = 2,
        catalogIntegration = "MANGAKO",
        navigationBarStyle = "CLASSIC",
        backupFrequency = "ON_CHANGE",
    )

    private companion object {
        const val CREATED_AT = "2026-09-04T12:00:00Z"
        const val VERSION_1_BACKUP = """
            {
              "formatVersion": 1,
              "createdAt": "2026-09-04T12:00:00Z",
              "appVersionCode": 1,
              "payload": {
                "collection": [{
                  "id": "m1",
                  "title": "Frieren",
                  "altTitle": null,
                  "type": null,
                  "coverId": null,
                  "coverFileName": null,
                  "coverUrl": "",
                  "authorId": null,
                  "author": null,
                  "status": null,
                  "volumeCount": 0,
                  "originalLanguage": null,
                  "ownedVolumes": [{
                    "id": "v1",
                    "mangaId": "m1",
                    "title": "Frieren",
                    "coverUrl": "",
                    "number": 1.0,
                    "locale": "ja",
                    "isSpecialEdition": false,
                    "createdAt": null,
                    "updatedAt": null
                  }]
                }],
                "settings": {
                  "viewMode": "grid",
                  "collectionDensity": 2,
                  "catalogIntegration": "MANGAKO",
                  "navigationBarStyle": "CLASSIC",
                  "backupFrequency": "ON_CHANGE"
                }
              },
              "checksumSha256": "58667483a1ef976626346efa3b54fde00595a833477dd4c43c140275886b5815"
            }
        """
    }
}

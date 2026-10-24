package dev.ricardo.mdviewer.data.io

import dev.ricardo.mdviewer.data.repo.AnnotationRepository
import dev.ricardo.mdviewer.data.repo.DocumentRepository
import dev.ricardo.mdviewer.data.scan.MarkdownIndex
import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.data.store.LibraryState
import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.domain.model.QuoteSelector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnnotationTransferTest {

    private class Fixture {
        val dir: File = File.createTempFile("transfer", "").apply { delete(); mkdirs() }
        val store = JsonStore(File(dir, "library.json"), LibraryState.serializer()) { LibraryState() }
        val annotations = AnnotationRepository(store)
        val documents = DocumentRepository(store, annotations, MarkdownIndex())
        val transfer = AnnotationTransfer(annotations, documents)
        val notes: File = File(dir, "notes").apply { mkdirs() }
        fun clean() = dir.deleteRecursively()
    }

    private fun annotation(docUri: String, id: String = "a1") = Annotation(
        id = id,
        docUri = docUri,
        kind = AnnotationKind.HIGHLIGHT,
        color = "#FFE8A3",
        selector = QuoteSelector("quoted words", "before ", " after", 5, 17, "b0"),
        note = "why it matters",
        tags = listOf("idea"),
        createdAt = 100L,
        updatedAt = 100L,
    )

    @Test
    fun `export produces a decodable sidecar and a readable digest`() = runTest {
        val fixture = Fixture()
        try {
            val file = File(fixture.notes, "essay.md").apply { writeText("before quoted words after") }
            val opened = fixture.documents.open(file.path)
            fixture.annotations.save(annotation(opened.document.uri))

            val bundle = fixture.transfer.buildExport(opened.document.uri)
            val sidecar = SidecarCodec.decode(bundle.json)

            assertEquals(SIDECAR_SCHEMA, sidecar.schema)
            assertEquals("essay.md", sidecar.document.name)
            assertEquals("a1", sidecar.annotations.single().id)
            assertTrue(bundle.digest.contains("quoted words"))
            assertTrue(bundle.suggestedJsonName.endsWith(".md.annot.json"))
        } finally {
            fixture.clean()
        }
    }

    @Test
    fun `import merges into a fresh device and matches the source by hash`() = runTest {
        val fixture = Fixture()
        try {
            val source = File(fixture.notes, "source.md").apply { writeText("before quoted words after") }
            val target = File(fixture.notes, "target.md").apply { writeText("a different article body") }
            val sourceOpened = fixture.documents.open(source.path)
            fixture.documents.open(target.path)
            fixture.annotations.save(annotation(sourceOpened.document.uri))

            val bundle = fixture.transfer.buildExport(sourceOpened.document.uri)

            // A second library store stands in for another device: it shares the files but not the data file.
            val deviceStore = JsonStore(File(fixture.dir, "device-b.json"), LibraryState.serializer()) { LibraryState() }
            val deviceAnnotations = AnnotationRepository(deviceStore)
            val deviceDocuments = DocumentRepository(deviceStore, deviceAnnotations, MarkdownIndex())
            val deviceTransfer = AnnotationTransfer(deviceAnnotations, deviceDocuments)
            deviceDocuments.open(source.path)
            deviceDocuments.open(target.path)

            val outcome = deviceTransfer.importSidecar(bundle.json, target.path)

            assertEquals(1, outcome.incomingCount)
            assertEquals(1, outcome.merge?.added)
            assertEquals("source.md", outcome.matchedDocument?.name)
            assertEquals(1, deviceAnnotations.observeForDocument(target.path).first().size)

            val again = deviceTransfer.importSidecar(bundle.json, target.path)
            assertEquals(0, again.merge?.added)
            assertEquals(1, again.merge?.skipped)
        } finally {
            fixture.clean()
        }
    }
}

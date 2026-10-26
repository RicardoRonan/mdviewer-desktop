package dev.ricardo.mdviewer.data.scan

import java.io.File

data class IndexedMarkdown(
    val openUri: String,
    val name: String,
    val folder: String,
    val size: Long,
    val modifiedAt: Long,
)

/**
 * Walks a library folder on disk. The pure walking/folder-paging logic is shared with the Android
 * app; only the storage roots and permission checks differ, and desktop has none.
 */
class MarkdownIndex {

    fun scan(root: File?): List<IndexedMarkdown> {
        if (root == null || !root.isDirectory) return emptyList()
        return collect(listOf(root))
    }
}

internal val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd", "mdwn")

private val SKIP_DIRECTORIES = setOf(
    "node_modules",
    ".git",
    ".gradle",
    ".idea",
    ".thumbnails",
    "lost+found",
    "build",
)

private const val MAX_DEPTH = 16
private const val MAX_FILES = 5000

internal fun isMarkdownName(name: String): Boolean {
    val dot = name.lastIndexOf('.')
    if (dot <= 0 || dot == name.lastIndex) return false
    return name.substring(dot + 1).lowercase() in MARKDOWN_EXTENSIONS
}

internal fun collect(roots: List<File>): List<IndexedMarkdown> {
    val found = LinkedHashMap<String, IndexedMarkdown>()
    val visited = HashSet<String>()
    val volumes = roots.mapNotNull { root ->
        runCatching { root.canonicalFile }.getOrNull()?.takeIf { it.isDirectory }
    }
    volumes.forEach { start -> walk(start, start, 0, visited, found) }
    return found.values.sortedWith(compareBy({ it.folder.lowercase() }, { it.name.lowercase() }))
}

internal data class FolderChild(
    val name: String,
    val path: String,
    val markdownCount: Int,
)

internal data class FolderPage(
    val folders: List<FolderChild>,
    val files: List<IndexedMarkdown>,
)

/** One level of the library. [path] is "" at the library root, otherwise a relative folder path. */
internal fun folderPage(files: List<IndexedMarkdown>, path: String): FolderPage {
    val folders = LinkedHashMap<String, Int>()
    val here = mutableListOf<IndexedMarkdown>()
    val prefix = if (path.isEmpty()) "" else "$path/"
    for (file in files) {
        val folder = file.folder.trim('/')
        val inScope = path.isEmpty() || folder == path || folder.startsWith(prefix)
        if (!inScope) continue
        val rest = when {
            path.isEmpty() -> folder
            folder == path -> ""
            else -> folder.removePrefix(prefix)
        }
        if (rest.isEmpty()) {
            here += file
        } else {
            val child = rest.substringBefore('/')
            val childPath = if (path.isEmpty()) child else "$path/$child"
            folders[childPath] = (folders[childPath] ?: 0) + 1
        }
    }
    return FolderPage(
        folders = folders.map { (childPath, count) ->
            FolderChild(
                name = childPath.substringAfterLast('/'),
                path = childPath,
                markdownCount = count,
            )
        }.sortedBy { it.name.lowercase() },
        files = here.sortedBy { it.name.lowercase() },
    )
}

private fun walk(
    root: File,
    dir: File,
    depth: Int,
    visited: MutableSet<String>,
    found: MutableMap<String, IndexedMarkdown>,
) {
    if (depth > MAX_DEPTH || found.size >= MAX_FILES) return
    val canonical = runCatching { dir.canonicalPath }.getOrNull() ?: return
    if (!visited.add(canonical)) return
    val children = runCatching { dir.listFiles() }.getOrNull() ?: return
    for (child in children) {
        if (found.size >= MAX_FILES) return
        val directory = runCatching { child.isDirectory }.getOrDefault(false)
        if (directory) {
            if (child.name.startsWith(".") || child.name in SKIP_DIRECTORIES) continue
            walk(root, child, depth + 1, visited, found)
        } else if (runCatching { child.isFile }.getOrDefault(false) && isMarkdownName(child.name)) {
            val file = runCatching { child.canonicalFile }.getOrDefault(child)
            // The logical folder path is always '/'-separated regardless of OS, because
            // folderPage and the library UI split on '/'. Filesystem paths (openUri) keep
            // native separators.
            val relative = file.parentFile?.path
                ?.removePrefix(root.path)
                ?.replace('\\', '/')
                ?.trim('/')
                .orEmpty()
            found.putIfAbsent(
                file.path,
                IndexedMarkdown(
                    openUri = file.path,
                    name = file.name,
                    folder = relative,
                    size = file.length(),
                    modifiedAt = file.lastModified(),
                ),
            )
        }
    }
}

package io.xymul.projtm.core

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private const val INDENT_ATTRIBUTE = "{http://xml.apache.org/xslt}indent-amount"

// Empty document with the given root element and attributes.
fun newDocument(rootName: String, rootAttributes: Map<String, String> = emptyMap()): Document {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    val root = document.createElement(rootName)
    rootAttributes.forEach { (key, value) -> root.setAttribute(key, value) }
    document.appendChild(root)
    return document
}

// Parses a file, returns null when it is missing or broken.
fun parseDocument(file: Path): Document? {
    if (!Files.isRegularFile(file)) return null
    val factory = DocumentBuilderFactory.newInstance()
    runCatching { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
    runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    return runCatching { factory.newDocumentBuilder().parse(file.toFile()) }.getOrNull()
}

// Writes through a temporary file and moves it into place, so readers never see half a file.
fun writeDocumentAtomically(document: Document, file: Path) {
    val parent = file.parent
    if (parent != null) Files.createDirectories(parent)
    val temporary = file.resolveSibling(file.fileName.toString() + ".tmp")
    val transformer = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty(INDENT_ATTRIBUTE, "4")
    }
    Files.newOutputStream(temporary).use { output ->
        transformer.transform(DOMSource(document), StreamResult(output))
    }
    runCatching {
        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }.onFailure {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
    }
}

fun Document.rootElement(): Element = documentElement

// Adds a child element, optionally with text and attributes.
fun Document.childElement(
    parent: Element,
    name: String,
    text: String? = null,
    attributes: Map<String, String> = emptyMap(),
): Element {
    val element = createElement(name)
    attributes.forEach { (key, value) -> element.setAttribute(key, value) }
    if (text != null) element.appendChild(createTextNode(text))
    parent.appendChild(element)
    return element
}

fun Element.childElements(name: String): List<Element> =
    childNodesOf(this).filter { it.nodeType == Node.ELEMENT_NODE && it.nodeName == name }
        .map { it as Element }

fun Element.firstChildElement(name: String): Element? = childElements(name).firstOrNull()

fun Element.childText(name: String): String? = firstChildElement(name)?.textContent?.trim()

fun Element.firstChildText(): String? = textContent?.trim()

private fun childNodesOf(element: Element): List<Node> {
    val nodes: NodeList = element.childNodes
    return (0 until nodes.length).map { nodes.item(it) }
}

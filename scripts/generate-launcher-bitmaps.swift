// Rasterize the existing Android vector; no separate artwork to maintain.
// Run from the repository root: swift scripts/generate-launcher-bitmaps.swift
import AppKit
import Foundation

final class VectorReader: NSObject, XMLParserDelegate {
    var paths: [(String, String)] = []
    func parser(_ parser: XMLParser, didStartElement name: String,
                namespaceURI: String?, qualifiedName: String?, attributes: [String: String]) {
        if name == "path", let color = attributes["android:fillColor"],
           let path = attributes["android:pathData"] { paths.append((color, path)) }
    }
}

func color(_ hex: String) -> CGColor {
    let value = UInt32(hex.dropFirst(), radix: 16)!
    return CGColor(colorSpace: CGColorSpace(name: CGColorSpace.sRGB)!, components: [
        CGFloat((value >> 16) & 255) / 255,
        CGFloat((value >> 8) & 255) / 255,
        CGFloat(value & 255) / 255, 1])!
}

// The source vector uses absolute M/L/Q/Z commands only. Fail on other syntax.
func path(_ data: String) -> CGPath {
    let expression = try! NSRegularExpression(pattern: "[A-Za-z]|-?[0-9]+(?:\\.[0-9]+)?")
    let text = data as NSString
    let tokens = expression.matches(in: data, range: NSRange(location: 0, length: text.length))
        .map { text.substring(with: $0.range) }
    let result = CGMutablePath()
    var i = 0
    func point() -> CGPoint {
        let point = CGPoint(x: Double(tokens[i])!, y: Double(tokens[i + 1])!)
        i += 2
        return point
    }
    while i < tokens.count {
        let command = tokens[i]; i += 1
        switch command {
        case "M": result.move(to: point())
        case "L": result.addLine(to: point())
        case "Q": let control = point(); result.addQuadCurve(to: point(), control: control)
        case "Z": result.closeSubpath()
        default: fatalError("Unsupported vector command: \(command)")
        }
    }
    return result
}

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let resources = root.appendingPathComponent("app/src/main/res")
let reader = VectorReader()
let parser = XMLParser(contentsOf: resources.appendingPathComponent("drawable/ic_launcher_foreground.xml"))!
parser.delegate = reader
precondition(parser.parse() && !reader.paths.isEmpty)
let colors = try String(contentsOf: resources.appendingPathComponent("values/colors.xml"), encoding: .utf8)
let backgroundExpression = try NSRegularExpression(pattern: "name=\"launcher_green\">(#[0-9A-Fa-f]{6})")
let match = backgroundExpression.firstMatch(in: colors, range: NSRange(colors.startIndex..., in: colors))!
let background = (colors as NSString).substring(with: match.range(at: 1))

func render(size: Int, round: Bool, to destination: URL) throws {
    let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8,
                            bytesPerRow: size * 4, space: CGColorSpace(name: CGColorSpace.sRGB)!,
                            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    context.translateBy(x: 0, y: CGFloat(size))
    context.scaleBy(x: CGFloat(size) / 72, y: -CGFloat(size) / 72)
    let bounds = CGRect(x: 0, y: 0, width: 72, height: 72)
    context.addPath(round ? CGPath(ellipseIn: bounds, transform: nil)
                    : CGPath(roundedRect: bounds, cornerWidth: 16, cornerHeight: 16, transform: nil))
    context.clip()
    context.setFillColor(color(background)); context.fill(bounds)
    // The central 72dp is the visible adaptive icon viewport.
    context.translateBy(x: -18, y: -18)
    for (fill, data) in reader.paths {
        context.setFillColor(color(fill)); context.addPath(path(data)); context.fillPath()
    }
    let bitmap = NSBitmapImageRep(cgImage: context.makeImage()!)
    try FileManager.default.createDirectory(at: destination.deletingLastPathComponent(), withIntermediateDirectories: true)
    try bitmap.representation(using: .png, properties: [:])!.write(to: destination)
}

for (density, size) in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)] {
    for round in [false, true] {
        try render(size: size, round: round, to: resources.appendingPathComponent("mipmap-\(density)/ic_launcher\(round ? "_round" : "_legacy").png"))
    }
}
try render(size: 512, round: false,
           to: root.appendingPathComponent("docs/design/2026-10-31-cream/icon-distribution-512.png"))

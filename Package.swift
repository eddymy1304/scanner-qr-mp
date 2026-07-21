// swift-tools-version:5.9
// Consumo del scanner desde apps iOS nativas vía SPM.
// El binario se publica como asset en cada GitHub Release; el checksum lo
// actualiza el workflow de release automáticamente.
import PackageDescription

let package = Package(
    name: "LaCaleraScanner",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "LaCaleraScanner", targets: ["LaCaleraScanner"])
    ],
    targets: [
        .binaryTarget(
            name: "LaCaleraScanner",
            url: "https://github.com/lacalera/scanner-qr-mp/releases/download/v0.1.0/LaCaleraScanner.xcframework.zip",
            checksum: "0000000000000000000000000000000000000000000000000000000000000000" // REEMPLAZADO_POR_CI
        )
    ]
)

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
            url: "https://github.com/eddymy1304/scanner-qr-mp/releases/download/v0.1.0/LaCaleraScanner.xcframework.zip",
            checksum: "63e4cf99b58951e7566c8c398afd78f97cdad7ed97417102d0fad0590baef58a" // REEMPLAZADO_POR_CI
        )
    ]
)

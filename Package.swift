// swift-tools-version:5.9
// Consumo del scanner desde apps iOS nativas vía SPM.
// El binario se publica como asset en cada GitHub Release; el checksum lo
// actualiza el workflow de release automáticamente.
import PackageDescription

let package = Package(
    name: "ScannerQrMp",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "ScannerQrMp", targets: ["ScannerQrMp"])
    ],
    targets: [
        .binaryTarget(
            name: "ScannerQrMp",
            url: "https://github.com/eddymy1304/scanner-qr-mp/releases/download/v0.2.0/ScannerQrMp.xcframework.zip",
            checksum: "57ef0ca48a8c63563ad3b6ab1551a3f8fea425f3b97b7db18118f42dabf02774" // REEMPLAZADO_POR_CI
        )
    ]
)

import SwiftUI
import ScannerQrMp

@main
struct ScannerSampleApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @State private var lastResult: String = "—"
    @State private var showScanner = false

    var body: some View {
        VStack(spacing: 24) {
            Text("scanner-qr-mp · Swift nativo")
                .font(.headline)
            Text("Último escaneo: \(lastResult)")
                .font(.body)
            Button("Escanear") {
                showScanner = true
            }
            .buttonStyle(.borderedProminent)
        }
        .fullScreenCover(isPresented: $showScanner) {
            ScannerScreen(
                onScanned: { value in
                    lastResult = value
                    showScanner = false
                },
                onDismiss: { showScanner = false }
            )
            .ignoresSafeArea()
        }
    }
}

/// Wrapper SwiftUI del UIViewController que expone la librería.
struct ScannerScreen: UIViewControllerRepresentable {
    let onScanned: (String) -> Void
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        let config = ScannerConfigBuilder()
        config.formats = [BarcodeFormat.qrcode, BarcodeFormat.ean13, BarcodeFormat.code128]
        config.singleShot = true
        config.showCloseButton = true

        return ScannerViewControllerKt.ScannerViewController(
            config: config,
            onScanned: { result in onScanned(result.rawValue) },
            onError: { error in print("Scanner error: \(error)") },
            onDismissRequest: { onDismiss() }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

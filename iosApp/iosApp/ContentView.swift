import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        guard let path = try? ClinicalCrypto.databasePath() else {
            return UIHostingController(rootView: ContentUnavailableView(
                "Storage unavailable",
                systemImage: "lock",
                description: Text("Close and reopen Gestiora to try again.")
            ))
        }
        return MainViewControllerKt.MainViewController(databasePath: path, crypto: ClinicalCrypto())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

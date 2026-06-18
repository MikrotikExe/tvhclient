import SwiftUI
import Shared
import MobileVLCKit

/// VLCKit prehravac pre live stream. Na rozdiel od AVPlayer zvlada raw
/// MPEG-TS cez HTTP vratane MPEG-2 videa a MP2/AC3 zvuku — co je vacsina
/// DVB-S/T2 kanalov. Server nemusi transkodovat (pass profil).
///
/// Auth: credentials vlozene priamo do URL (user:pass@host) — VLCKit ich
/// z URL pouzije. Pre plain aj digest funguje cez libvlc HTTP stack.
struct PlayerView: UIViewRepresentable {
    let urlString: String

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.backgroundColor = .black
        let player = VLCMediaPlayer()
        context.coordinator.player = player
        player.drawable = view
        if let url = URL(string: urlString) {
            player.media = VLCMedia(url: url)
            player.play()
        }
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator() }

    static func dismantleUIView(_ uiView: UIView, coordinator: Coordinator) {
        coordinator.player?.stop()
        coordinator.player = nil
    }

    class Coordinator {
        var player: VLCMediaPlayer?
    }
}

/// Obrazovka prehravaca — fullscreen, s nazvom kanalu.
struct PlayerScreen: View {
    let channelUuid: String
    let channelTitle: String
    @Environment(\.dismiss) private var dismiss

    private var streamUrl: String? {
        guard let server = Tvh.shared.store.active() else { return nil }
        // liveUrl s creds v URL (pre VLCKit)
        return Tvh.shared.liveUrl(
            server: server,
            channelUuid: channelUuid,
            channelTitle: channelTitle,
            profile: server.profile.isEmpty ? "pass" : server.profile
        )
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let url = streamUrl {
                PlayerView(urlString: url).ignoresSafeArea()
            } else {
                Text(NSLocalizedString("no_active_server", comment: ""))
                    .foregroundColor(.white)
            }
            VStack {
                HStack {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundColor(.white.opacity(0.8))
                    }
                    .padding()
                    Spacer()
                    Text(channelTitle).foregroundColor(.white).padding()
                }
                Spacer()
            }
        }
    }
}

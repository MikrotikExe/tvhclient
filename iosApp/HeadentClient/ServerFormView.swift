import SwiftUI
import Shared

struct ServerFormView: View {
    let existing: TvhServer?

    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var host = ""
    @State private var port = "9981"
    @State private var useHttps = false
    @State private var username = ""
    @State private var password = ""

    @State private var testing = false
    @State private var testMessage: String? = nil
    @State private var testOk = false

    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField(NSLocalizedString("field_name", comment: ""), text: $name)
                    TextField(NSLocalizedString("field_host", comment: ""), text: $host)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .keyboardType(.URL)
                    TextField(NSLocalizedString("field_port", comment: ""), text: $port)
                        .keyboardType(.numberPad)
                    Toggle(NSLocalizedString("field_https", comment: ""), isOn: $useHttps)
                }
                Section {
                    TextField(NSLocalizedString("field_username", comment: ""), text: $username)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    SecureField(NSLocalizedString("field_password", comment: ""), text: $password)
                }
                Section {
                    Button {
                        runTest()
                    } label: {
                        if testing {
                            HStack {
                                ProgressView()
                                Text(NSLocalizedString("testing", comment: ""))
                            }
                        } else {
                            Text(NSLocalizedString("test_connection", comment: ""))
                        }
                    }
                    .disabled(testing || host.isEmpty)

                    if let msg = testMessage {
                        Text(msg).foregroundColor(testOk ? .green : .red)
                    }
                }
            }
            .navigationTitle(NSLocalizedString(
                existing == nil ? "add_server" : "edit_server", comment: ""))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("cancel", comment: "")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("save", comment: "")) {
                        save()
                        dismiss()
                    }
                    .disabled(host.isEmpty || Int32(port) == nil)
                }
            }
            .onAppear {
                if let s = existing {
                    name = s.name
                    host = s.host
                    port = String(s.port)
                    useHttps = s.useHttps
                    username = s.username
                    password = s.password
                }
            }
        }
    }

    private func buildServer() -> TvhServer? {
        guard let p = Int32(port), !host.isEmpty else { return nil }
        return TvhServer(
            id: existing?.id ?? Tvh.shared.newServerId(),
            name: name.isEmpty ? host : name,
            host: host.trimmingCharacters(in: .whitespaces),
            port: p,
            useHttps: useHttps,
            username: username.trimmingCharacters(in: .whitespaces),
            password: password
        )
    }

    private func save() {
        guard let server = buildServer() else { return }
        Tvh.shared.store.upsert(server: server)
    }

    private func runTest() {
        guard let server = buildServer() else { return }
        testing = true
        testMessage = nil
        Task {
            do {
                let result = try await Tvh.shared.testConnection(server: server)
                await MainActor.run {
                    testing = false
                    switch result {
                    case let ok as ConnectionResult.Success:
                        testOk = true
                        let ver = ok.info.swVersion ?? "?"
                        testMessage = String(
                            format: NSLocalizedString("test_ok", comment: ""), ver)
                    case is ConnectionResult.AuthFailed:
                        testOk = false
                        testMessage = NSLocalizedString("test_auth_failed", comment: "")
                    case let err as ConnectionResult.HttpError:
                        testOk = false
                        testMessage = String(
                            format: NSLocalizedString("test_http_error", comment: ""),
                            err.httpCode)
                    case let err as ConnectionResult.NetworkError:
                        testOk = false
                        testMessage = String(
                            format: NSLocalizedString("test_network_error", comment: ""),
                            err.message)
                    default:
                        testOk = false
                        testMessage = "?"
                    }
                }
            } catch {
                await MainActor.run {
                    testing = false
                    testOk = false
                    testMessage = error.localizedDescription
                }
            }
        }
    }
}

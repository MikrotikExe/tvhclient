import SwiftUI
import Shared

struct ServersView: View {
    @State private var servers: [TvhServer] = []
    @State private var activeId: String? = nil
    @State private var editing: TvhServer? = nil
    @State private var showForm = false

    var body: some View {
        NavigationView {
            List {
                if servers.isEmpty {
                    Text(NSLocalizedString("no_servers", comment: ""))
                        .foregroundColor(.secondary)
                }
                ForEach(servers, id: \.id) { server in
                    Button {
                        Tvh.shared.store.activeId = server.id
                        reload()
                    } label: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(server.name).font(.headline)
                                Text("\(server.host):\(server.port)" + (server.useHttps ? " (HTTPS)" : ""))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            if server.id == activeId {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                            }
                        }
                    }
                    .swipeActions {
                        Button(role: .destructive) {
                            Tvh.shared.store.delete(id: server.id)
                            reload()
                        } label: {
                            Label(NSLocalizedString("delete", comment: ""), systemImage: "trash")
                        }
                        Button {
                            editing = server
                            showForm = true
                        } label: {
                            Label(NSLocalizedString("edit", comment: ""), systemImage: "pencil")
                        }
                    }
                }
            }
            .navigationTitle(NSLocalizedString("servers_title", comment: ""))
            .toolbar {
                Button {
                    editing = nil
                    showForm = true
                } label: {
                    Image(systemName: "plus")
                }
            }
            .sheet(isPresented: $showForm, onDismiss: reload) {
                ServerFormView(existing: editing)
            }
            .onAppear(perform: reload)
        }
    }

    private func reload() {
        servers = Tvh.shared.store.list()
        activeId = Tvh.shared.store.activeId
    }
}

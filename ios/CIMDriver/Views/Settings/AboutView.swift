import SwiftUI

struct AboutView: View {
    private var appVersionText: String {
        let shortVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        let gitCommit = (Bundle.main.object(forInfoDictionaryKey: "GIT_COMMIT") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        if let gitCommit, !gitCommit.isEmpty, gitCommit != "$(GIT_COMMIT)", gitCommit != "unknown" {
            return "Versie \(shortVersion)-\(gitCommit)"
        }

        let displayVersion = Bundle.main.object(forInfoDictionaryKey: "APP_DISPLAY_VERSION") as? String
        let cleanedDisplayVersion = displayVersion?.trimmingCharacters(in: .whitespacesAndNewlines)

        if let cleanedDisplayVersion, !cleanedDisplayVersion.isEmpty, cleanedDisplayVersion != "$(APP_DISPLAY_VERSION)", cleanedDisplayVersion != shortVersion, cleanedDisplayVersion != "\(shortVersion)-", cleanedDisplayVersion != "unknown" {
            return "Versie \(cleanedDisplayVersion)"
        }

        return "Versie \(shortVersion)"
    }
    
    private let credits = [
        "OpenStreetMap (Kaartdata) - OpenStreetMap Contributors (ODbL)",
        "Nominatim (Geocoding fallback) - OpenStreetMap Foundation",
        "OSRM (Open Source Routing Machine) - Project OSRM",
        "Core Location & SwiftUI - Apple",
        "SwiftData & WidgetKit - Apple",
        "Swift Standard Library - Apple"
    ]
    
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 12) {
                    Image("Banner")
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    
                    Text(appVersionText + " • Alpha")
                        .font(.headline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color(.secondarySystemGroupedBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 14))

                    Text("CIMDriver Alpha helpt je met automatische ritten- en werkurenregistratie op iPhone. Deze alpha-versie is actief in ontwikkeling en kan nog wijzigen.")
                        .font(.subheadline)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 24)
                }
                .padding(.top, 24)
                .padding(.horizontal)
                
                aboutCard(
                    title: "Gemaakt door",
                    icon: "person.circle.fill"
                ) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Remco Visser")
                            .font(.title3)
                            .fontWeight(.semibold)
                        Text("m.b.v. Google Gemini AI")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                
                aboutCard(
                    title: "Ondersteun de ontwikkeling",
                    icon: "heart.circle.fill"
                ) {
                    Link(destination: URL(string: "https://ko-fi.com/vircos01")!) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Steun CIMDriver via Ko-fi")
                                    .foregroundStyle(.primary)
                                Text("Help mee om de app verder te ontwikkelen.")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "arrow.up.right.square")
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                
                aboutCard(
                    title: "Credits & Open Source",
                    icon: "curlybraces") {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("CIMDriver maakt gebruik van diverse open-source bibliotheken. Veel dank aan de community voor de volgende componenten:")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        
                        ForEach(credits, id: \.self) { credit in
                            HStack(alignment: .top, spacing: 8) {
                                Text("•")
                                    .foregroundStyle(.green)
                                Text(credit)
                                    .font(.body)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                
                aboutCard(
                    title: "Licenties & Voorwaarden",
                    icon: "doc.text.fill"
                ) {
                    NavigationLink(destination: LicenseView()) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Bekijk licenties en voorwaarden")
                                    .foregroundStyle(.primary)
                                Text("Open de juridische informatie van CIMDriver.")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.bottom, 24)
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Over CIMDriver")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    @ViewBuilder
    private func aboutCard<Content: View>(title: String, icon: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(.green)
                Text(title)
                    .font(.headline)
            }
            
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .padding(.horizontal)
    }
}

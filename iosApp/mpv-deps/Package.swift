// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "NaphMpvDeps",
    platforms: [.iOS(.v15)],
    dependencies: [
        .package(url: "https://github.com/NuvioMedia/MPVKit.git", branch: "main")
    ],
    targets: [
        .target(
            name: "NaphMpvDeps",
            dependencies: [
                .product(name: "MPVKit", package: "MPVKit")
            ],
            path: "Sources"
        )
    ]
)

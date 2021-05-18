// swift-tools-version:5.3
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "PyrusServiceDesk",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v9)
    ],
    products: [
        .library(
            name: "PyrusServiceDesk",
            targets: ["PyrusServiceDesk"]),
    ],
    targets: [
        .target(
            name: "PyrusServiceDesk",
            dependencies: [],
            path: "PyrusServiceDeskIOS/PyrusServiceDesk",
            exclude: [
                "PyrusServiceDeskAndroid/",
                "PyrusServiceDesk.h",
                "Info.plist"
            ],
            resources: [
                .process("images/")
            ]),
        .testTarget(
            name: "PyrusServiceDeskTests",
            dependencies: ["PyrusServiceDesk"],
            path: "PyrusServiceDeskIOS/PyrusServiceDeskTests")
    ]
)

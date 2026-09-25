import SwiftUI

extension Color {
    static let cimNavy = Color(red: 0.098, green: 0.145, blue: 0.263)       // #192543
    static let cimNavyDark = Color(red: 0.063, green: 0.098, blue: 0.180)   // #10192E
    static let cimNavyLight = Color(red: 0.141, green: 0.196, blue: 0.333)  // #243255
    static let cimGreen = Color(red: 0.078, green: 0.718, blue: 0.306)      // #14B74E
    static let cimGreenLight = Color(red: 0.157, green: 0.780, blue: 0.435) // #28C76F
    static let cimGreenPastel = Color(red: 0.490, green: 0.871, blue: 0.667)// #7DDEAA
    static let cimBluePastel = Color(red: 0.659, green: 0.769, blue: 0.878) // #A8C4E0
    
    static var cimBackground: Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(red: 0.071, green: 0.071, blue: 0.071, alpha: 1) // #121212
                : UIColor(red: 0.961, green: 0.961, blue: 0.961, alpha: 1) // #F5F5F5
        })
    }
    
    static var cimSurface: Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(red: 0.118, green: 0.118, blue: 0.141, alpha: 1) // #1E1E24
                : UIColor.white
        })
    }
    
    static var cimPrimary: Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(red: 0.157, green: 0.780, blue: 0.435, alpha: 1) // Green Light
                : UIColor(red: 0.098, green: 0.145, blue: 0.263, alpha: 1) // Navy
        })
    }
}

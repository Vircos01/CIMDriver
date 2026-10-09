import UIKit
import Vision

enum OdometerTextRecognizer {
    static func recognize(in image: UIImage) async -> String? {
        guard let cgImage = image.cgImage else { return nil }

        return await Task.detached(priority: .userInitiated) {
            let request = VNRecognizeTextRequest()
            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = false

            do {
                try VNImageRequestHandler(cgImage: cgImage).perform([request])
                let candidates = (request.results ?? []).compactMap { observation in
                    observation.topCandidates(1).first?.string
                }
                return candidates.compactMap(reading(in:)).max { $0.count < $1.count }
            } catch {
                return nil
            }
        }.value
    }

    static func reading(in text: String) -> String? {
        guard let expression = try? NSRegularExpression(pattern: #"[0-9][0-9\s.,]{2,10}"#) else { return nil }
        let range = NSRange(text.startIndex..., in: text)
        let readings = expression.matches(in: text, range: range).compactMap { match -> String? in
            guard let matchRange = Range(match.range, in: text) else { return nil }
            let digits = text[matchRange].filter { $0.isNumber }
            return (4...8).contains(digits.count) ? String(digits) : nil
        }
        return readings.max { $0.count < $1.count }
    }
}
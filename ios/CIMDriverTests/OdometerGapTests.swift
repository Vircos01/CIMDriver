import XCTest
@testable import CIMDriver

final class OdometerGapTests: XCTestCase {

    func testAnalyzeEmptyTripsWithMatchingOdometer() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1000
        )
        
        let gaps = OdometerGapAnalyzer.analyze(vehicle: vehicle, trips: [])
        XCTAssertTrue(gaps.isEmpty)
        XCTAssertTrue(OdometerGapAnalyzer.isChainAirtight(vehicle: vehicle, trips: []))
    }

    func testAnalyzeEmptyTripsWithDivergentOdometer() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1050
        )
        
        let gaps = OdometerGapAnalyzer.analyze(vehicle: vehicle, trips: [])
        XCTAssertEqual(gaps.count, 1)
        XCTAssertEqual(gaps[0].gapType, .currentStandGap)
        XCTAssertEqual(gaps[0].gapKm, 50)
        XCTAssertFalse(OdometerGapAnalyzer.isChainAirtight(vehicle: vehicle, trips: []))
    }

    func testAnalyzeContinuousTrips() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1030
        )
        
        let t1 = Date()
        let t2 = t1.addingTimeInterval(1800)
        let t3 = t2.addingTimeInterval(600)
        let t4 = t3.addingTimeInterval(1800)
        
        let trip1 = Trip(
            vehicle: vehicle,
            startTime: t1,
            endTime: t2,
            distanceMeters: 10000,
            odometerStart: 1000,
            odometerEnd: 1010
        )
        let trip2 = Trip(
            vehicle: vehicle,
            startTime: t3,
            endTime: t4,
            distanceMeters: 20000,
            odometerStart: 1010,
            odometerEnd: 1030
        )
        
        let gaps = OdometerGapAnalyzer.analyze(vehicle: vehicle, trips: [trip1, trip2])
        XCTAssertTrue(gaps.isEmpty)
        XCTAssertTrue(OdometerGapAnalyzer.isChainAirtight(vehicle: vehicle, trips: [trip1, trip2]))
    }

    func testAnalyzeFirstTripGap() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1060
        )
        
        let trip1 = Trip(
            vehicle: vehicle,
            startTime: Date(),
            distanceMeters: 10000,
            odometerStart: 1050,
            odometerEnd: 1060
        )
        
        let gaps = OdometerGapAnalyzer.analyze(vehicle: vehicle, trips: [trip1])
        XCTAssertEqual(gaps.count, 1)
        XCTAssertEqual(gaps[0].gapType, .firstTripGap)
        XCTAssertEqual(gaps[0].gapKm, 50)
    }

    func testAnalyzeMissingKmAndCurrentStandGap() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1100
        )
        
        let t1 = Date()
        let t2 = t1.addingTimeInterval(1800)
        let t3 = t2.addingTimeInterval(600)
        let t4 = t3.addingTimeInterval(1800)
        
        let trip1 = Trip(
            vehicle: vehicle,
            startTime: t1,
            endTime: t2,
            distanceMeters: 10000,
            odometerStart: 1000,
            odometerEnd: 1010
        )
        // Jump from 1010 to 1040 (30 km gap)
        let trip2 = Trip(
            vehicle: vehicle,
            startTime: t3,
            endTime: t4,
            distanceMeters: 20000,
            odometerStart: 1040,
            odometerEnd: 1060
        )
        // Vehicle odometerCurrent is 1100, last trip ends at 1060 (40 km gap)
        
        let gaps = OdometerGapAnalyzer.analyze(vehicle: vehicle, trips: [trip1, trip2])
        XCTAssertEqual(gaps.count, 2)
        XCTAssertEqual(gaps[0].gapType, .missingKm)
        XCTAssertEqual(gaps[0].gapKm, 30)
        XCTAssertEqual(gaps[1].gapType, .currentStandGap)
        XCTAssertEqual(gaps[1].gapKm, 40)
    }

    func testSyncVehicleOdometerFixType() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1050
        )
        
        let trip1 = Trip(
            vehicle: vehicle,
            startTime: Date(),
            distanceMeters: 10000,
            odometerStart: 1000,
            odometerEnd: 1010
        )
        
        let gaps = OdometerGapAnalyzer.analyze(vehicle: vehicle, trips: [trip1])
        XCTAssertEqual(gaps.count, 1)
        let currentGap = gaps[0]
        XCTAssertEqual(currentGap.gapType, .currentStandGap)
        
        let syncFix = currentGap.suggestedFixes.first { $0.type == .syncVehicleOdometer }
        XCTAssertNotNil(syncFix)
        XCTAssertTrue(syncFix!.title.contains("1010 km"))
    }

    func testRecalculatorFixesChain() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1050
        )
        
        let t1 = Date()
        let t2 = t1.addingTimeInterval(1800)
        let t3 = t2.addingTimeInterval(3600)
        let t4 = t3.addingTimeInterval(1800)
        
        // Inverted or wrong odometers in raw data
        let trip1 = Trip(
            vehicle: vehicle,
            startTime: t1,
            endTime: t2,
            distanceMeters: 8000, // 8 km
            odometerStart: 500,
            odometerEnd: 508
        )
        let trip2 = Trip(
            vehicle: vehicle,
            startTime: t3,
            endTime: t4,
            distanceMeters: 12000, // 12 km
            odometerStart: 800,
            odometerEnd: 812
        )
        
        let preview = OdometerRecalculator.preview(
            vehicle: vehicle,
            trips: [trip1, trip2]
        )
        
        XCTAssertEqual(preview.totalTrips, 2)
        XCTAssertEqual(preview.modifiedTripsCount, 2)
        
        // trip1 should start at 1000 and end at 1008
        let update1 = preview.updates.first { $0.trip.id == trip1.id }!
        XCTAssertEqual(update1.newStart, 1000)
        XCTAssertEqual(update1.newEnd, 1008)
        
        // trip2 should start at 1008 and end at 1020
        let update2 = preview.updates.first { $0.trip.id == trip2.id }!
        XCTAssertEqual(update2.newStart, 1008)
        XCTAssertEqual(update2.newEnd, 1020)
    }

    func testRecalculatorPreservesOdometerCheckCheckpoint() {
        let vehicle = Vehicle(
            name: "Test Auto",
            licensePlate: "AB-123-C",
            make: "Tesla",
            model: "Model 3",
            odometerStart: 1000,
            odometerCurrent: 1200
        )
        
        let t1 = Date()
        let t2 = t1.addingTimeInterval(1800)
        let tCheck = t2.addingTimeInterval(1800)
        let t3 = tCheck.addingTimeInterval(1800)
        let t4 = t3.addingTimeInterval(1800)
        
        let trip1 = Trip(
            vehicle: vehicle,
            startTime: t1,
            endTime: t2,
            distanceMeters: 10000, // 10 km
            odometerStart: 1000,
            odometerEnd: 1010
        )
        // Check performed at tCheck with photo reading 1050 km (leaving 40 km gap before check)
        let check = OdometerCheck(
            vehicleId: vehicle.id,
            timestamp: tCheck,
            registeredOdometer: 1010,
            correctedOdometer: 1050
        )
        let trip2 = Trip(
            vehicle: vehicle,
            startTime: t3,
            endTime: t4,
            distanceMeters: 15000, // 15 km
            odometerStart: 1050,
            odometerEnd: 1065
        )
        
        let preview = OdometerRecalculator.preview(
            vehicle: vehicle,
            trips: [trip1, trip2],
            odometerChecks: [check]
        )
        
        XCTAssertEqual(preview.preservedChecksCount, 1)
        let update2 = preview.updates.first { $0.trip.id == trip2.id }!
        // Should continue from checkpoint 1050
        XCTAssertEqual(update2.newStart, 1050)
        XCTAssertEqual(update2.newEnd, 1065)
    }
}

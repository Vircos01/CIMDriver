import { app } from '@zeppos/sdk';

App({
  onInit() {
    console.log('[CIMDriver] App-side initialized');

    // Store the latest Android-side values in settings or service state.
    // This layer is the bridge between the phone and the watch.
    // Example: sync trip summary, fuel status, or availability state.
    this.state = {
      lastTripStatus: 'idle',
      latestDistanceKm: 0,
      latestFuelStatus: 'ready'
    };
  },

  onCall(req) {
    console.log('[CIMDriver] onCall', req);

    const type = req && req.type ? req.type : 'unknown';

    if (type === 'sync-status') {
      return {
        ok: true,
        status: this.state.lastTripStatus,
        distanceKm: this.state.latestDistanceKm,
        fuelStatus: this.state.latestFuelStatus
      };
    }

    if (type === 'set-status') {
      this.state.lastTripStatus = req.status || this.state.lastTripStatus;
      this.state.latestDistanceKm = Number(req.distanceKm || this.state.latestDistanceKm);
      this.state.latestFuelStatus = req.fuelStatus || this.state.latestFuelStatus;

      return { ok: true };
    }

    return { ok: false, message: 'Unsupported request' };
  }
});

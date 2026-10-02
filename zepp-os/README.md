# CIMDriver Zepp OS companion app

This folder is an actual starter project for a Zepp OS mini program for Amazfit smartwatches that can work alongside the Android app.

## Goal

Keep the watch app lightweight and let the phone-side companion handle the heavier sync work:

- show a quick driving summary on the watch
- sync trip / fueling / charging status with the Android app
- display the current vehicle state in a compact card
- refresh from a configurable backend or Android summary endpoint

## Structure

- `app.json` — Zepp OS mini program configuration
- `app-side/index.js` — phone-side companion logic
- `page/index.js` — watch screen UI that fetches the live summary JSON
- `setting/index.js` — settings screen for the API URL and refresh interval
- `api-example.json` — example response expected from the Android side

## Expected summary payload

The watch page expects a JSON payload like this:

```json
{
  "tripStatus": "driving",
  "distanceKm": 18.4,
  "fuelStatus": "tanking",
  "lastUpdated": 1760000000000
}
```

The Android app can expose this through a simple endpoint such as:

```text
GET /api/zepp/summary
```

## Typical data flow

1. The Android app computes the current trip/fuel status.
2. It exposes a small JSON summary endpoint.
3. The Zepp mini program fetches that JSON and updates the watch UI.
4. The settings screen lets the user point the app to the correct URL.

This is a practical starter that can be expanded to the exact CIMDriver workflow.

# Test strategy

This document describes how CIMDriver should be tested functionally, technically, and operationally. Because the application utilizes background services, system events, local storage, widgets, and car integration, a combination of automated and manual tests is required.

## Test layers

The minimum test pyramid for CIMDriver consists of:

- unit tests for utilities, classification, time logic, calculations, and normalization;
- database and repository tests for Room queries, entities, and storage behavior;
- ViewModel tests for state, filtering, user actions, and derived data;
- instrumentation tests for Android integrations such as services, receivers, notifications, and widgets;
- manual system tests for Bluetooth, location, reboot, recovery, and Android Auto/Automotive.

## Core scenarios

### Tracking

- Start tracking via UI.
- Start tracking via Bluetooth event.
- Stop tracking via disconnect.
- Stop tracking manually.
- Verify duplicate start calls.
- Verify correct foreground notification.
- Verify storage of location points and trip metadata.

### Recovery

- Restart device during an active trip.
- Restart app process during an active trip.
- Trigger `MY_PACKAGE_REPLACED` during saved state.
- Start recovery without permissions.
- Start recovery with corrupt or missing state.
- Verify that recovery does not produce duplicate trips.

### Data

- Add trips, addresses, vehicles, and work hours.
- Check filtering, sorting, and deletion.
- Test export and backup behavior.
- Ensure that database changes do not break existing data.

### UI

- Check loading, empty, and error states.
- Check screen navigation, back behavior, and deep-link style routes.
- Check display of dashboard, detail pages, and forms.
- Check localized resources.

### Integrations

- Check widgets after update and after reboot.
- Check Android Auto/Automotive screens.
- Check workers for backup and data lifecycle.
- Check notification actions and system receivers.

## Acceptance criteria

A change may only be considered stable once the relevant unit, repository, ViewModel, and integration tests have been executed and the corresponding critical manual scenarios have been validated. For tracking and recovery, error and interruption paths must be explicitly included, not just the happy flow.

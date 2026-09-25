# Runbooks and operational management

This document describes operational procedures for the management, troubleshooting, and recovery of CIMDriver. The emphasis is on situations where background behavior, data, or configuration is not working as expected.

## Incident: tracking does not start

Check in this order:

1. Are location and Bluetooth permissions present?
2. Is the foreground notification allowed?
3. Is the relevant Bluetooth device active and correctly recognized?
4. Is the service status in diagnostics set to stopped, failed, or recovering?
5. Have there been any recent boot or package replacement events?
6. Are there any visible log lines or error messages?

## Incident: tracking recovers incorrectly after reboot

1. Check if `BOOT_COMPLETED` or `MY_PACKAGE_REPLACED` was relevant.
2. Check if the recovery state is still valid.
3. Check if an active trip already existed.
4. Check if duplicate start paths were triggered simultaneously.
5. Check if notification and service status are consistent.

## Incident: data missing or appears corrupt

1. Check database changes or recent migrations.
2. Check export and backup behavior.
3. Check if records are still visible via repository and ViewModel paths.
4. Check if filtering or UI state unintentionally hides the data.

## Incident: widget or car screen shows outdated data

1. Check if the widget or car component is still being updated.
2. Check repository and ViewModel-dependent paths.
3. Check lifecycle and timing of background updates.
4. Check if screen or widget state is being built from an outdated source.

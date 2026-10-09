# Background task reference

## Purpose

Record here all background tasks used by CIMDriver on iOS. These tasks form an important part of the runtime architecture, because tracking, recovery, and maintenance cannot run indefinitely like on Android.

## Document per task

- Identifier.
- Trigger or registration point.
- Expected input and used repositories.
- Success and failure behavior.
- Time budget and cancellation behavior.
- Diagnostics and observability.

## Expected categories

- Tracking recovery task.
- Data cleanup or maintenance task.
- Export follow-up or file processing.
- Widget or summary refresh where permitted.

# Tracking and recovery

## Runtime behavior

The tracking layer monitors location, motion, and vehicle signals and converts them into trip candidates and active trips. The implementation must be robust against interruptions such as app suspension, permission changes, and temporary signal loss.

## Recovery strategy

- Store minimal checkpoint data locally.
- Recover active or recently interrupted trips upon app start.
- Log important recovery decisions for diagnostics and support.

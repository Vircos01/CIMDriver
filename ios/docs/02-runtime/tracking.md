# Tracking en herstel

## Runtimegedrag

De trackinglaag bewaakt locatie-, bewegings- en voertuigsignalen en zet die om in ritkandidaten en actieve ritten. De implementatie moet robuust zijn bij onderbrekingen zoals app suspension, permissiewijzigingen en tijdelijke signaaluitval.

## Herstelstrategie

- Bewaar minimale checkpointdata lokaal.
- Herstel actieve of recent onderbroken ritten bij appstart.
- Log belangrijke herstelbeslissingen voor diagnose en support.

# Runbooks en operationeel beheer

Dit document beschrijft operationele handelingen voor beheer, probleemoplossing en herstel van CIMDriver. De nadruk ligt op situaties waarin background behavior, data of configuratie niet werkt zoals verwacht.

## Incident: tracking start niet

Controleer in deze volgorde:

1. Zijn locatie- en Bluetooth-permissies aanwezig?
2. Is de foreground notification toegestaan?
3. Is het relevante Bluetooth-apparaat actief en correct herkend?
4. Staat de servicestatus in diagnostiek op gestopt, falend of herstellend?
5. Zijn er recente boot- of package replacement-events geweest?
6. Zijn er logregels of foutmeldingen zichtbaar?

## Incident: tracking herstelt onjuist na reboot

1. Controleer of `BOOT_COMPLETED` of `MY_PACKAGE_REPLACED` relevant was.
2. Controleer of recoverystate nog geldig is.
3. Controleer of er al een actieve rit bestond.
4. Controleer of dubbele startpaden tegelijk zijn geactiveerd.
5. Controleer of notificatie en servicestatus consistent zijn.

## Incident: gegevens ontbreken of lijken corrupt

1. Controleer databasewijzigingen of recente migraties.
2. Controleer export- en backupgedrag.
3. Controleer of records nog zichtbaar zijn via repository- en ViewModel-paden.
4. Controleer of filtering of UI-state de data onbedoeld verbergt.

## Incident: widget of car-scherm toont verouderde data

1. Controleer of de widget- of car-component nog wordt bijgewerkt.
2. Controleer repository- en ViewModel-afhankelijke paden.
3. Controleer lifecycle en timing van achtergrondupdates.
4. Controleer of scherm- of widgetstate uit een verouderde bron wordt opgebouwd.

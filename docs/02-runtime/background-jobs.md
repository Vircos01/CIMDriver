# Achtergrondtaken

Naast services bevat CIMDriver ook workergebaseerde achtergrondverwerking. In de package `worker` zijn ten minste `BackupWorker` en `DataLifecycleWorker` aanwezig.

## Rollen

Workers zijn bedoeld voor processen die niet per se aan een actieve foreground service of direct zichtbaar scherm gekoppeld hoeven te zijn. Binnen deze app lijken backup en datalifecycle de primaire voorbeelden van dergelijke achtergrondprocessen.

## Plaats in de architectuur

De aanwezigheid van workers naast services benadrukt dat CIMDriver twee soorten achtergrondwerk kent: direct runtimekritische processen zoals tracking en meer geplande of onderhoudsgerichte processen zoals backup en gegevensbeheer. Dat onderscheid is belangrijk voor zowel documentatie als teststrategie.

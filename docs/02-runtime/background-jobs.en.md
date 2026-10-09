# Background jobs

In addition to services, CIMDriver also includes worker-based background processing. The `worker` package contains at least `BackupWorker` and `DataLifecycleWorker`.

## Roles

Workers are intended for processes that do not necessarily need to be linked to an active foreground service or a directly visible screen. Within this app, backup and data lifecycle seem to be the primary examples of such background processes.

## Place in the architecture

The presence of workers alongside services emphasizes that CIMDriver has two types of background work: directly runtime-critical processes such as tracking, and more scheduled or maintenance-oriented processes such as backup and data management. This distinction is important for both documentation and testing strategy.

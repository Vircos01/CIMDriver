# Entity and DAO reference

This document is intended as an in-depth addition to the data model. It describes how entities and DAOs should be documented and what developers should pay attention to when making changes.

## Documenting per entity

At a minimum, the following should be recorded per entity:

- functional purpose;
- primary key;
- important fields;
- mandatory and optional values;
- relationships with other entities;
- source of the data;
- mutation paths;
- impact on export, backup, and dashboards.

## Documenting per DAO

At a minimum, the following should be recorded per DAO:

- which queries are available;
- which filters or sorts are applied;
- which methods perform write operations;
- which methods are used by services, ViewModels, or widgets;
- which queries are critical for performance or consistency.

## Change guidelines

Never modify an entity or DAO in isolation. Always check the impact on migrations, repositories, dashboards, detail screens, widgets, export, backup, and recovery.

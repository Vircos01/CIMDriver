# Widgets

CIMDriver includes widget support at both the manifest and source code level. The manifest declares two widget receivers and the source code contains multiple widget-related Kotlin files and XML configurations.

## Components

The widget layer consists of `CIMDriverWidgetProvider`, `CIMDriverWidget`, `CIMDriverWidgetReceiver`, and related XML files such as `widget_info.xml`, `cimdriver_widget_info.xml`, and `widget_layout.xml`. This makes it clear that widgets are not only passively configured, but also possess their own runtime logic.

## Role within the app

Widgets provide homescreen functionality outside the main app. In an app like CIMDriver, it is obvious that widgets provide quick status information, recent data, or direct actions regarding trip registration or overviews, although the precise content per widget must be recorded in further component documentation.

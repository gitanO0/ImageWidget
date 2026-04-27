# ImageWidget

A flexible Android Home Screen widget built with **Jetpack Glance** that displays images from any URL. Perfect for monitoring weather maps, webcams, or just keeping your favorite photos on your home screen.

## Features

- **Multiple Instances**: Add as many widgets as you like, each with its own unique configuration.
- **Custom URLs**: Support for both HTTP and HTTPS image sources.
- **Periodic Refresh**: Automatically update images at set intervals (15 min to 24 hours).
- **Advanced Fitting**: Choose between "Crop to Fit", "Fit Content", and "Stretch" to display your image exactly how you want.
- **Zoom Control**: Digital zoom support (1x to 2x) for focusing on specific parts of an image.
- **Skip Night Mode**: Pause updates during specific hours to save battery and data.
- **Profiles**: Save and load configuration presets for quick setup.
- **Material 3 UI**: Clean, modern configuration interface.

## Tech Stack

- **Jetpack Compose**: For the app and configuration UI.
- **Jetpack Glance**: For building the AppWidget using Compose-like syntax.
- **WorkManager**: Reliable background fetching of images.
- **Kotlin Coroutines**: For asynchronous network and file operations.
- **SharedPreferences**: Local persistence of widget configurations.

## How It Works

1. **Configuration**: When you add a widget, `WidgetConfigActivity` allows you to set the image URL and refresh parameters.
2. **Persistence**: Settings are stored per `appWidgetId` in `WidgetState`.
3. **Background Updates**: `ImageRefreshWorker` handles the heavy lifting—downloading the image, following redirects, and handling `User-Agent` requirements for strict servers.
4. **Rendering**: The image is saved locally and rendered via `GlanceAppWidget` using a `Bitmap`.

## Getting Started

1. Clone this repository.
2. Open in **Android Studio Jellyfish** or newer.
3. Build and run on your device.
4. Go to your home screen, long-press, select Widgets, and find "ImageWidget".

## Screenshots

*(Add screenshots here)*

## License

```text
MIT License

Copyright (c) 2024 Royce

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
...
```

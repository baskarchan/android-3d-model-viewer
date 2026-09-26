# 3D Model Viewer

Android app for the Infusory screening task. Load multiple 3D models on screen at once, drag/resize them, rotate/zoom them individually, and show part labels.

## What's used

- Kotlin, traditional Android Views (no Compose)
- SceneView (wraps Google's Filament engine) for the 3D rendering
- minSdk 24

## How it works

Each model you add gets its own container on screen — basically a box you can drag and resize. Tap the Move/Rotate button to switch into interaction mode: now dragging rotates the model instead of moving the box, and pinching zooms the camera instead of resizing the box. Tap Labels to show part names pulled from the model file. Tap X to remove it.

All 5 models can be on screen together and they don't interfere with each other.

## Why Filament/SceneView

Needed something built for mobile rendering rather than a general 3D engine, and Filament gives good control over render settings (shadows, quality, resolution) which matters since the whole point of this task is running smoothly on weak devices. SceneView made getting a model loading and on screen much faster than raw Filament, while still letting me drop down to Filament's camera/transform APIs directly where I needed to (mainly for projecting label positions onto the screen).

## Performance choices

The biggest one: there's only **one** Filament engine shared across all 5 models, instead of each one creating its own. Creating an engine is expensive (it sets up an actual GPU context), so doing that 5 times instead of once would hurt both load time and memory, especially on a low-RAM device.

Other things:
- Model bytes are cached so re-adding the same model doesn't re-read the file
- Label positions only get recalculated when the camera actually moves (rotating/zooming), not every single frame
- Touch handling is one simple handler per container, not layered gesture systems fighting each other

## What I'd do with more time

- Actually wire in shadow-off / lower quality / dynamic resolution settings on the Filament view — designed for but not applied yet
- Compress the two heaviest models (Fiagena and solarsystem, both ~5-6MB)
- Profile on real low-end hardware and get actual frame-time numbers instead of just design intent
- Handle labels overlapping each other when a model has a lot of close-together parts

## Known issues

- Ran into a bug during development where the buttons (Move, Labels, X) weren't clickable because touch interception was grabbing everything — fixed by only intercepting touches outside the buttons row
- No overlap handling for labels that are close together
- Containers are always square
- Nothing is saved — closing the app clears everything
- app crashed in ModelContainerView 
- zoom out not working

## Tested on

 Android 16. Didn't have access to an actual low-end/budget device, so performance on weaker hardware is based on the design decisions above rather than measured directly.

## Performance — measured, not just designed for

Profiled on a Google Pixel 6a using Android Studio's Profiler:

- With 2 models loaded and idle: 339.5MB total (Graphics 129.1MB, Native 40.4MB), CPU ~4%.
- With more models loaded: 449.3MB total (Graphics 184.2MB, Native 93MB), CPU ~0% while idle.
- Closing a model produces an immediate, sharp drop in total memory in the
  Profiler's live graph — confirming `ModelContainerView.dispose()` /
  `sceneView.destroy()` actually releases native Filament resources rather
  than just detaching the view.

Not yet captured: CPU/frame-time while actively dragging or rotating a model
(the number that most directly answers "does it stay smooth during
interaction" per the task's target of 30fps+).
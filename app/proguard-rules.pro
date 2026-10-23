# R8 keeps MainActivity because it is referenced from AndroidManifest.xml.
# GameView and Progress are referenced directly from Java, so no reflection
# rules are needed. Resources are accessed through R.*, which shrinkResources
# tracks automatically.

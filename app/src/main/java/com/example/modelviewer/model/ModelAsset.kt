package com.example.modelviewer

/**
 * The fixed catalog of the 5 bundled .glb files. File names must match exactly
 * what you drop into app/src/main/assets/models/. Update this list once your
 * real filenames from the shared "GLB Models for Task" folder are known.
 */
enum class ModelAsset(val displayName: String, val assetPath: String) {
    BULB("Bulb", "Test_Models/Bulb.glb"),
    FIAGENA("Fiagena", "Test_Models/Fiagena.glb"),
    LUNGS("Lungs", "Test_Models/Lungs.glb"),
    MICROSCOPE("Microscope", "Test_Models/Microscope.glb"),
    SOLAR_SYSTEM("Solar System", "Test_Models/solarsystem.glb"),
}

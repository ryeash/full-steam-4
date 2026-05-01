package com.fullsteam.model;

import lombok.Getter;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Getter
public enum UnitType {
    WORKER(
            "Worker",
            "Harvests resources from the map and constructs new buildings for your faction.",
            50,
            10,
            75,
            100.0,
            5,
            1.0,
            100,
            15.0,
            0xFFFF00,
            BuildingType.HEADQUARTERS,
            5,
            300.0,
            Elevation.GROUND,
            UnitCategory.WORKER,
            Set.of(),
            0,
            'K'
    ),

    INFANTRY(
            "Infantry",
            "Core rifle infantry—fast, affordable, and effective against light targets.",
            75,
            5,
            128,
            120.0,
            18,
            2.0,
            170,
            12.0,
            0x00FF00,
            BuildingType.BARRACKS,
            10,
            350.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(),
            2,
            'I'
    ),

    SHOTGUN_INFANTRY(
            "Shotgun Infantry",
            "Close-quarters specialist; devastating burst damage that falls off at range.",
            120,
            7,
            140,
            115.0,
            35,
            1.5,
            130,
            12.0,
            0x228B22,
            BuildingType.BARRACKS,
            13,
            340.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB),
            2,
            'H'
    ),

    LASER_INFANTRY(
            "Laser Infantry",
            "Armored trooper with a sustained-fire laser rifle for longer reach and punch.",
            120,
            7,
            128,
            120.0,
            20,
            1.5,
            180,
            12.0,
            0x00FFFF,
            BuildingType.BARRACKS,
            8,
            360.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(),
            2,
            'L'
    ),

    MEDIC(
            "Medic",
            "Support infantry that heals nearby friendlies on cooldown; cannot attack.",
            100,
            8,
            90,
            110.0,
            0,
            0.0,
            0,
            12.0,
            0xFFFFFF,
            BuildingType.BARRACKS,
            8,
            340.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB),
            1,
            'M'
    ),

    ROCKET_SOLDIER(
            "Rocket Soldier",
            "Anti-armor infantry; rockets excel versus vehicles and hardened targets.",
            150,
            8,
            112,
            110.0,
            40,
            0.8,
            200,
            12.0,
            0xFF8800,
            BuildingType.BARRACKS,
            15,
            370.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB),
            2,
            'R'
    ),

    SNIPER(
            "Sniper",
            "Long-range marksman with slow, heavy shots—fragile but lethal from distance.",
            200,
            10,
            80,
            100.0,
            65,
            0.5,
            345,
            12.0,
            0x8B4513,
            BuildingType.BARRACKS,
            12,
            500.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB),
            3,
            'N'
    ),

    ENGINEER(
            "Engineer",
            "Repairs damaged friendly vehicles and buildings on cooldown; cannot attack.",
            150,
            12,
            105,
            105.0,
            0,
            0.0,
            0,
            13.0,
            0x00CED1,
            BuildingType.BARRACKS,
            10,
            330.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB),
            1,
            'E'
    ),

    SPY(
            "Spy",
            "Permanently cloaked infiltrator; tracker darts tag enemies to grant your team vision on them.",
            300,
            20,
            60,
            115.0,
            0,
            0.1,
            500,
            12.0,
            0x2F4F4F,
            BuildingType.BARRACKS,
            15,
            500.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            7,
            'Y'
    ),

    GRENADIER(
            "Grenadier",
            "Lobs explosive grenades with area damage—strong versus clumped units and structures.",
            175,
            9,
            85,
            95.0,
            25,
            1.2,
            150,
            12.0,
            0x8B4513,
            BuildingType.BARRACKS,
            8,
            300.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(),
            4,
            'G'
    ),

    MINIGUNNER(
            "Minigunner",
            "Suppression specialist with an extreme fire rate and modest damage per bullet.",
            140,
            8,
            120,
            105.0,
            8,
            5.0,
            160,
            12.0,
            0x556B2F,
            BuildingType.BARRACKS,
            12,
            340.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(),
            2,
            'U'
    ),

    JEEP(
            "Jeep",
            "Fast light scout car with strong vision for mapping and early harassment.",
            200,
            10,
            168,
            180.0,
            26,
            3.0,
            207,
            20.0,
            0x00FFFF,
            BuildingType.FACTORY,
            20,
            450.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(),
            3,
            'J'
    ),

    TANK(
            "Tank",
            "Main battle tank—slow, heavily armored, and built to brawl with enemy armor.",
            400,
            15,
            390,
            80.0,
            68,
            1.2,
            240,
            27.0,
            0x8888FF,
            BuildingType.FACTORY,
            30,
            400.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB),
            4,
            'T'
    ),

    FLAK_TANK(
            "Flak Tank",
            "Mobile anti-air platform that shreds aircraft; lighter than a main battle tank.",
            350,
            12,
            280,
            90.0,
            30,
            1.5,
            300,
            24.0,
            0xA0A0A0,
            BuildingType.FACTORY,
            25,
            420.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB),
            3,
            'F'
    ),

    SAM_LAUNCHER(
            "SAM Launcher",
            "Long-range anti-air missile battery on treads—fragile but devastating to flyers.",
            450,
            18,
            170,
            75.0,
            80,
            0.7,
            380,
            24.0,
            0x708090,
            BuildingType.FACTORY,
            28,
            480.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            5,
            'Y'
    ),

    SHIELD_TANK(
            "Shield Tank",
            "Support armor that projects a protective shield bubble for nearby allies; unarmed.",
            550,
            22,
            320,
            70.0,
            0,
            0.0,
            0,
            26.0,
            0x9370DB,
            BuildingType.FACTORY,
            30,
            350.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            6,
            'H'
    ),

    SPIDER_MINE(
            "Spider Mine",
            "Cheap mobile mine that detonates for massive damage when enemies enter proximity.",
            75,
            6,
            40,
            150.0,
            150,
            0.0,
            0,
            8.0,
            0x8B4513,
            BuildingType.FACTORY,
            3,
            250.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB),
            3,
            'P'
    ),

    APC(
            "APC",
            "Armored transport; garrisoned infantry can fire out while the APC moves.",
            250,
            14,
            280,
            95.0,
            0,
            0.0,
            0,
            22.0,
            0x696969,
            BuildingType.FACTORY,
            18,
            350.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB),
            5,
            'K'
    ),

    ARTILLERY(
            "Artillery",
            "Slow siege cannon with extreme range—needs escorts and spotters to shine.",
            500,
            20,
            180,
            60.0,
            117,
            0.5,
            437,
            25.0,
            0xFF00FF,
            BuildingType.FACTORY,
            40,
            420.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB),
            6,
            'R'
    ),

    GIGANTONAUT(
            "Gigantonaut",
            "Colossal self-propelled siege piece; high range and damage, low speed.",
            1200,
            35,
            360,
            30.0,
            250,
            0.3,
            450,
            35.0,
            0x8B0000,
            BuildingType.FACTORY,
            60,
            200.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            9,
            'G'
    ),

    CLOAK_TANK(
            "Cloak Tank",
            "Stealthed medium tank—stays hidden until it fires or is detected at close range.",
            800,
            25,
            260,
            100.0,
            28,
            1.5,
            200,
            28.0,
            0x2F4F4F,
            BuildingType.FACTORY,
            45,
            380.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            3,
            'C'
    ),

    RAIDER(
            "Raider",
            "Blazing-fast raider craft for deep strikes, flanks, and hunting soft targets.",
            900,
            28,
            364,
            220.0,
            55,
            2.2,
            180,
            22.0,
            0xDC143C,
            BuildingType.FACTORY,
            45,
            520.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            8,
            'N'
    ),

    COLOSSUS(
            "Colossus",
            "Massive bipedal war walker with huge health and multi-projectile cannons.",
            1600,
            45,
            2640,
            40.0,
            95,
            0.9,
            250,
            43.0,
            0x4B0082,
            BuildingType.FACTORY,
            75,
            490.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            9,
            'O'
    ),

    TRIDENT_TROOPER(
            "Trident Trooper",
            "Tech-alliance rifleman firing instant-hit energy beams instead of ballistic rounds.",
            170,
            12,
            136,
            115.0,
            14,
            2.0,
            148,
            12.0,
            0x00FF7F,
            BuildingType.BARRACKS,
            12,
            355.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB),
            2,
            'T'
    ),

    ION_RANGER(
            "Ion Ranger",
            "Long-range beam sniper—slow shots with very high single-target burst at distance.",
            250,
            12,
            96,
            105.0,
            50,
            0.6,
            300,
            12.0,
            0x9370DB,
            BuildingType.BARRACKS,
            14,
            500.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            3,
            'O'
    ),

    PHOTON_SCOUT(
            "Photon Scout",
            "Fast beam-armed scout vehicle with excellent vision for tech-army reconnaissance.",
            220,
            11,
            154,
            190.0,
            20,
            2.5,
            180,
            18.0,
            0x7FFF00,
            BuildingType.FACTORY,
            22,
            460.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB),
            3,
            'V'
    ),

    BEAM_TANK(
            "Beam Tank",
            "Heavy armored tank mounting sustained laser fire—durable mid-line breaker.",
            450,
            16,
            400,
            75.0,
            52,
            1.3,
            209,
            30.0,
            0x00FA9A,
            BuildingType.FACTORY,
            32,
            410.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            4,
            'B'
    ),

    PULSE_ARTILLERY(
            "Pulse Artillery",
            "Slow beam artillery platform—melts static defenses and blobs from extreme range.",
            550,
            22,
            168,
            55.0,
            90,
            0.6,
            380,
            26.0,
            0xFFD700,
            BuildingType.FACTORY,
            42,
            430.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            5,
            'U'
    ),

    PHOTON_TITAN(
            "Photon Titan",
            "Super-heavy walker with a high damage beam weapon.",
            1400,
            40,
            420,
            35.0,
            280,
            0.4,
            460,
            32.0,
            0x00FF00,
            BuildingType.FACTORY,
            65,
            480.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            8,
            'I'
    ),

    ANDROID(
            "Android",
            "Autonomous soldier produced free by the Android Factory—no credits cost, no upkeep.",
            0,
            15,
            100,
            110.0,
            22,
            1.5,
            180,
            13.0,
            0x00CED1,
            BuildingType.ANDROID_FACTORY,
            0,
            340.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.POWER_PLANT, BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            0,
            'Z'
    ),

    // ===== AIR UNITS =====

    SCOUT_DRONE(
            "Scout Drone",
            "Fast cheap VTOL with best-in-class vision—ideal for air scouting and light harassment.",
            150,
            12,
            80,
            200.0,
            8,
            2.5,
            150,
            12.0,
            0x87CEEB,
            BuildingType.AIRFIELD,
            15,
            600.0,
            Elevation.LOW,
            UnitCategory.FLYER,
            Set.of(),
            2,
            'E'
    ),

    HELICOPTER(
            "Attack Helicopter",
            "Versatile low-altitude gunship firing rockets at ground targets; controllable like a tank.",
            350,
            18,
            150,
            150.0,
            35,
            1.8,
            220,
            16.0,
            0x8B4513,
            BuildingType.AIRFIELD,
            25,
            450.0,
            Elevation.LOW,
            UnitCategory.FLYER,
            Set.of(BuildingType.RESEARCH_LAB),
            3,
            'H'
    ),

    LASER_GUNSHIP(
            "Laser Gunship",
            "Advanced VTOL with instant-hit lasers—precision air-to-ground without ballistic delay.",
            750,
            40,
            280,
            140.0,
            35,
            1.8,
            260,
            22.0,
            0x00BFFF,
            BuildingType.AIRFIELD,
            45,
            450.0,
            Elevation.LOW,
            UnitCategory.FLYER,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            8,
            'L'
    ),

    BOMBER(
            "Bomber",
            "Strategic bomber housed at the airfield; flies a player-ordered sortie then returns to berth.",
            800,
            60,
            250,
            220.0,
            200,
            0.5,
            0,
            21.0,
            0x2F4F4F,
            BuildingType.AIRFIELD,
            50,
            400.0,
            Elevation.HIGH,
            UnitCategory.FLYER,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            8,
            'B'
    ),

    INTERCEPTOR(
            "Interceptor",
            "High-speed fighter for air superiority; scrambles from the airfield and uses sortie fuel.",
            600,
            45,
            200,
            290.0,
            100,
            2.0,
            300,
            14.0,
            0xFF4500,
            BuildingType.AIRFIELD,
            40,
            500.0,
            Elevation.HIGH,
            UnitCategory.FLYER,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            6,
            'I'
    ),

    GUNSHIP(
            "Gunship",
            "Heavy sortie attack craft with dual weapons—durable airfield-housed fire support platform.",
            1100,
            50,
            380,
            160.0,
            40,
            2.0,
            280,
            31.0,
            0x8B0000,
            BuildingType.AIRFIELD,
            55,
            480.0,
            Elevation.HIGH,
            UnitCategory.FLYER,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            8,
            'G'
    );

    private final String displayName;
    private final String description;
    private final int resourceCost;
    private final int buildTimeSeconds;
    private final double maxHealth;
    private final double movementSpeed;
    private final double damage;
    private final double attackRate;
    private final double attackRange;
    private final double size;
    private final int color;
    private final BuildingType producedBy;
    private final int upkeepCost;
    private final double visionRange;
    private final Elevation elevation;
    private final UnitCategory category;
    private final Set<BuildingType> requiredBuildings;
    private final int pointCost;
    private final Character hotkey;

    /**
     * Create physics fixtures for this unit type
     * This allows each unit to have custom shapes (including multi-fixture compound shapes)
     * Returns a list of Convex shapes that will be added to the unit's physics body
     * Most units return a single fixture, but complex units can return multiple fixtures
     * for compound shapes (e.g., hourglass, dumbbell, star shapes)
     */
    public List<Convex> createPhysicsFixtures() {
        return switch (this) {
            // Basic Infantry - tactical soldier with armor plating and weapon mount
            case INFANTRY -> {
                // Main body: elongated pentagon (armored torso)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.6, -size * 0.4),  // Back left
                        new Vector2(-size * 0.1, -size * 0.7),  // Left shoulder (armor plate)
                        new Vector2(size * 0.8, 0),             // Front point (weapon direction)
                        new Vector2(-size * 0.1, size * 0.7),   // Right shoulder (armor plate)
                        new Vector2(-size * 0.6, size * 0.4)    // Back right
                };
                Convex torso = Geometry.createPolygon(mainBody);

                // Left armor plate (shoulder guard)
                Vector2[] leftPlate = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.75),
                        new Vector2(size * 0.2, -size * 0.85),
                        new Vector2(size * 0.4, -size * 0.65)
                };
                Convex plateLeft = Geometry.createPolygon(leftPlate);

                // Right armor plate (shoulder guard)
                Vector2[] rightPlate = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.75),
                        new Vector2(size * 0.4, size * 0.65),
                        new Vector2(size * 0.2, size * 0.85)
                };
                Convex plateRight = Geometry.createPolygon(rightPlate);

                yield List.of(torso, plateLeft, plateRight);
            }

            // Shotgun Infantry - heavy assault trooper with wide barrel and reinforced stance
            case SHOTGUN_INFANTRY -> {
                // Main body: wider, more aggressive pentagon (heavy assault build)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.7, -size * 0.5),  // Back left (wider stance)
                        new Vector2(-size * 0.2, -size * 0.8),  // Left shoulder (heavy armor)
                        new Vector2(size * 0.7, 0),             // Front point (wide barrel)
                        new Vector2(-size * 0.2, size * 0.8),   // Right shoulder (heavy armor)
                        new Vector2(-size * 0.7, size * 0.5)    // Back right (wider stance)
                };
                Convex torso = Geometry.createPolygon(mainBody);

                // Left barrel extension (shotgun spread indicator)
                Vector2[] leftBarrel = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.4),
                        new Vector2(size * 0.9, -size * 0.3),
                        new Vector2(size * 0.7, -size * 0.1)
                };
                Convex barrelLeft = Geometry.createPolygon(leftBarrel);

                // Right barrel extension (shotgun spread indicator)
                Vector2[] rightBarrel = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.4),
                        new Vector2(size * 0.7, size * 0.1),
                        new Vector2(size * 0.9, size * 0.3)
                };
                Convex barrelRight = Geometry.createPolygon(rightBarrel);

                yield List.of(torso, barrelLeft, barrelRight);
            }

            // Grenadier - bulky infantry with ammo pouches and grenade launcher
            case GRENADIER -> {
                // Main body: stocky pentagon (carrying heavy equipment)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.4),  // Back left
                        new Vector2(-size * 0.3, -size * 0.9),  // Left shoulder (ammo pouch)
                        new Vector2(size * 0.6, 0),             // Front point (launcher)
                        new Vector2(-size * 0.3, size * 0.9),   // Right shoulder (ammo pouch)
                        new Vector2(-size * 0.8, size * 0.4)    // Back right
                };
                Convex torso = Geometry.createPolygon(mainBody);

                // Left ammo pouch (bulky equipment)
                Vector2[] leftPouch = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.6),
                        new Vector2(-size * 0.5, -size * 0.9),
                        new Vector2(-size * 0.3, -size * 0.7)
                };
                Convex pouchLeft = Geometry.createPolygon(leftPouch);

                // Right ammo pouch (bulky equipment)
                Vector2[] rightPouch = new Vector2[]{
                        new Vector2(-size * 0.9, size * 0.6),
                        new Vector2(-size * 0.3, size * 0.7),
                        new Vector2(-size * 0.5, size * 0.9)
                };
                Convex pouchRight = Geometry.createPolygon(rightPouch);

                yield List.of(torso, pouchLeft, pouchRight);
            }

            // Minigunner - heavy weapons specialist with rotating barrel assembly
            case MINIGUNNER -> {
                // Main body: reinforced pentagon (braced for recoil)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.45), // Back left (wide stance)
                        new Vector2(-size * 0.2, -size * 0.85), // Left shoulder (heavy support)
                        new Vector2(size * 0.9, 0),             // Front point (barrel assembly)
                        new Vector2(-size * 0.2, size * 0.85),  // Right shoulder (heavy support)
                        new Vector2(-size * 0.8, size * 0.45)   // Back right (wide stance)
                };
                Convex torso = Geometry.createPolygon(mainBody);

                // Rotating barrel assembly (top)
                Vector2[] topBarrel = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.5),
                        new Vector2(size, -size * 0.35),
                        new Vector2(size * 0.9, -size * 0.15)
                };
                Convex barrelTop = Geometry.createPolygon(topBarrel);

                // Rotating barrel assembly (bottom)
                Vector2[] bottomBarrel = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.5),
                        new Vector2(size * 0.9, size * 0.15),
                        new Vector2(size, size * 0.35)
                };
                Convex barrelBottom = Geometry.createPolygon(bottomBarrel);

                // Ammo belt/feed (left side)
                Vector2[] ammoLeft = new Vector2[]{
                        new Vector2(-size * 0.6, -size * 0.7),
                        new Vector2(-size * 0.2, -size * 0.95),
                        new Vector2(size * 0.1, -size * 0.75)
                };
                Convex beltLeft = Geometry.createPolygon(ammoLeft);

                // Ammo belt/feed (right side)
                Vector2[] ammoRight = new Vector2[]{
                        new Vector2(-size * 0.6, size * 0.7),
                        new Vector2(size * 0.1, size * 0.75),
                        new Vector2(-size * 0.2, size * 0.95)
                };
                Convex beltRight = Geometry.createPolygon(ammoRight);

                yield List.of(torso, barrelTop, barrelBottom, beltLeft, beltRight);
            }

            // Laser Infantry - angular prism design with crystalline focusing arrays
            case LASER_INFANTRY -> {
                // Create a multi-faceted prism shape with focusing crystals
                // Main body: elongated diamond (prism core)
                Vector2[] mainPrism = new Vector2[]{
                        new Vector2(-size * 0.7, -size * 0.3),  // Back left
                        new Vector2(size * 0.9, -size * 0.4),   // Front left (angular)
                        new Vector2(size * 1.1, 0),             // Front point (beam emitter)
                        new Vector2(size * 0.9, size * 0.4),    // Front right (angular)
                        new Vector2(-size * 0.7, size * 0.3)    // Back right
                };
                Convex core = Geometry.createPolygon(mainPrism);

                // Left focusing crystal (small triangle)
                Vector2[] leftCrystal = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.5),
                        new Vector2(size * 0.6, -size * 0.6),
                        new Vector2(size * 0.5, -size * 0.3)
                };
                Convex leftFocus = Geometry.createPolygon(leftCrystal);

                // Right focusing crystal (small triangle)
                Vector2[] rightCrystal = new Vector2[]{
                        new Vector2(size * 0.2, size * 0.5),
                        new Vector2(size * 0.5, size * 0.3),
                        new Vector2(size * 0.6, size * 0.6)
                };
                Convex rightFocus = Geometry.createPolygon(rightCrystal);

                yield List.of(core, leftFocus, rightFocus);
            }

            // Plasma Trooper - energy prism with plasma containment geometry
            case TRIDENT_TROOPER -> {
                // Main plasma containment chamber (hexagonal prism)
                Vector2[] chamber = new Vector2[]{
                        new Vector2(-size * 0.7, 0),
                        new Vector2(-size * 0.3, -size * 0.5),
                        new Vector2(size * 0.5, -size * 0.5),
                        new Vector2(size, 0),             // Front emitter
                        new Vector2(size * 0.5, size * 0.5),
                        new Vector2(-size * 0.3, size * 0.5)
                };
                Convex mainChamber = Geometry.createPolygon(chamber);

                // Plasma accelerator spikes (fractal-like angular protrusions)
                Vector2[] topSpike = new Vector2[]{
                        new Vector2(size * 0.1, -size * 0.6),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size * 0.5, -size * 0.6)
                };
                Convex topAccel = Geometry.createPolygon(topSpike);

                Vector2[] bottomSpike = new Vector2[]{
                        new Vector2(size * 0.1, size * 0.6),
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(size * 0.3, size * 0.8)
                };
                Convex bottomAccel = Geometry.createPolygon(bottomSpike);

                yield List.of(mainChamber, topAccel, bottomAccel);
            }

            // Rocket Soldier - wider pentagon (anti-vehicle specialist with launcher)
            case ROCKET_SOLDIER -> {
                // Pentagon with wider shoulders for rocket launcher
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.6),// Back left
                        new Vector2(size * 0.3, -size * 0.9), // Left shoulder (wide)
                        new Vector2(size * 1.1, 0),           // Front point (pointing right)
                        new Vector2(size * 0.3, size * 0.9),  // Right shoulder (wide)
                        new Vector2(-size * 0.8, size * 0.6)  // Back right
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Sniper - precision marksman with elongated rifle and stabilizing bipod
            case SNIPER -> {
                // Main body: narrow elongated hexagon (prone shooter profile)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.35), // Back left
                        new Vector2(-size * 0.3, -size * 0.5),  // Mid-left (narrow waist)
                        new Vector2(size * 0.5, -size * 0.4),   // Front left
                        new Vector2(size, 0),             // Rifle tip (pointing right)
                        new Vector2(size * 0.5, size * 0.4),    // Front right
                        new Vector2(-size * 0.3, size * 0.5),   // Mid-right (narrow waist)
                        new Vector2(-size * 0.9, size * 0.35)   // Back right
                };
                Convex body = Geometry.createPolygon(mainBody);

                // Scope mount (top detail)
                Vector2[] scope = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.4),
                        new Vector2(size * 0.6, -size * 0.5),
                        new Vector2(size * 0.7, -size * 0.35)
                };
                Convex scopeMount = Geometry.createPolygon(scope);

                // Stock/rear support
                Vector2[] stock = new Vector2[]{
                        new Vector2(-size, -size * 0.25),
                        new Vector2(-size * 0.6, -size * 0.3),
                        new Vector2(-size * 0.6, size * 0.3),
                        new Vector2(-size, size * 0.25)
                };
                Convex rearStock = Geometry.createPolygon(stock);

                yield List.of(body, scopeMount, rearStock);
            }

            // Ion Ranger - complex multi-lens focusing array for long-range ion beam
            case ION_RANGER -> {
                // Main body: elongated focusing chamber
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.4),
                        new Vector2(size * 0.4, -size * 0.5),
                        new Vector2(size * 1.3, 0),             // Primary lens tip
                        new Vector2(size * 0.4, size * 0.5),
                        new Vector2(-size * 0.9, size * 0.4)
                };
                Convex focusingBody = Geometry.createPolygon(mainBody);

                // Primary focusing lens (forward angular prism)
                Vector2[] primaryLens = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.3),
                        new Vector2(size * 1.1, -size * 0.2),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 1.1, size * 0.2),
                        new Vector2(size * 0.7, size * 0.3)
                };
                Convex lens1 = Geometry.createPolygon(primaryLens);

                // Secondary refractor (top)
                Vector2[] topRefractor = new Vector2[]{
                        new Vector2(size * 0.0, -size * 0.6),
                        new Vector2(size * 0.4, -size * 0.75),
                        new Vector2(size * 0.6, -size * 0.55)
                };
                Convex refractorTop = Geometry.createPolygon(topRefractor);

                // Secondary refractor (bottom)
                Vector2[] bottomRefractor = new Vector2[]{
                        new Vector2(size * 0.0, size * 0.6),
                        new Vector2(size * 0.6, size * 0.55),
                        new Vector2(size * 0.4, size * 0.75)
                };
                Convex refractorBottom = Geometry.createPolygon(bottomRefractor);

                // Tertiary focusing crystals (fractal detail)
                Vector2[] leftCrystal = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.5),
                        new Vector2(size * 0.1, -size * 0.65),
                        new Vector2(size * 0.2, -size * 0.5)
                };
                Convex crystalLeft = Geometry.createPolygon(leftCrystal);

                Vector2[] rightCrystal = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.5),
                        new Vector2(size * 0.2, size * 0.5),
                        new Vector2(size * 0.1, size * 0.65)
                };
                Convex crystalRight = Geometry.createPolygon(rightCrystal);

                yield List.of(focusingBody, lens1, refractorTop, refractorBottom, crystalLeft, crystalRight);
            }

            // Worker/Support units - circular for easy navigation
            case WORKER, MEDIC, ENGINEER -> List.of(Geometry.createCircle(size));

            // Spy - sleek infiltrator with low-profile stealth design
            case SPY -> {
                // Main body: elongated diamond (streamlined profile) - simplified to be convex
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, 0),             // Back center
                        new Vector2(-size * 0.3, -size * 0.5),   // Mid-left
                        new Vector2(size * 0.7, -size * 0.35),   // Front left
                        new Vector2(size, 0),              // Front point (nose)
                        new Vector2(size * 0.7, size * 0.35),    // Front right
                        new Vector2(-size * 0.3, size * 0.5)     // Mid-right
                };
                Convex body = Geometry.createPolygon(mainBody);

                // Stealth device (top small detail)
                Vector2[] stealthTop = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.5),
                        new Vector2(size * 0.5, -size * 0.6),
                        new Vector2(size * 0.6, -size * 0.4)
                };
                Convex deviceTop = Geometry.createPolygon(stealthTop);

                // Stealth device (bottom small detail)
                Vector2[] stealthBottom = new Vector2[]{
                        new Vector2(size * 0.2, size * 0.5),
                        new Vector2(size * 0.6, size * 0.4),
                        new Vector2(size * 0.5, size * 0.6)
                };
                Convex deviceBottom = Geometry.createPolygon(stealthBottom);

                // Tracker gun (small frontal detail)
                Vector2[] tracker = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.2),
                        new Vector2(size * 1.1, 0),
                        new Vector2(size * 0.7, size * 0.2)
                };
                Convex trackerGun = Geometry.createPolygon(tracker);

                yield List.of(body, deviceTop, deviceBottom, trackerGun);
            }

            // Android - synthetic combat unit with angular robotic chassis
            case ANDROID -> {
                // Central core: diamond chassis (robotic torso)
                Vector2[] core = new Vector2[]{
                        new Vector2(-size * 0.7, 0),            // Back center
                        new Vector2(0, -size * 0.7),            // Top center
                        new Vector2(size * 0.9, 0),             // Front center (head)
                        new Vector2(0, size * 0.7)              // Bottom center
                };
                Convex chassis = Geometry.createPolygon(core);

                // Upper left arm assembly
                Vector2[] leftArmUpper = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.75),
                        new Vector2(size * 0.1, -size * 0.95),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size * 0.1, -size * 0.6)
                };
                Convex armLU = Geometry.createPolygon(leftArmUpper);

                // Upper right arm assembly
                Vector2[] rightArmUpper = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.75),
                        new Vector2(size * 0.1, size * 0.6),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(size * 0.1, size * 0.95)
                };
                Convex armRU = Geometry.createPolygon(rightArmUpper);

                // Lower left arm (weapon mount)
                Vector2[] leftArmLower = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.75),
                        new Vector2(size * 0.7, -size * 0.85),
                        new Vector2(size * 0.9, -size * 0.6),
                        new Vector2(size * 0.6, -size * 0.5)
                };
                Convex armLL = Geometry.createPolygon(leftArmLower);

                // Lower right arm (weapon mount)
                Vector2[] rightArmLower = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.75),
                        new Vector2(size * 0.6, size * 0.5),
                        new Vector2(size * 0.9, size * 0.6),
                        new Vector2(size * 0.7, size * 0.85)
                };
                Convex armRL = Geometry.createPolygon(rightArmLower);

                // Head/sensor array (angular trapezoid)
                Vector2[] head = new Vector2[]{
                        new Vector2(size * 0.5, -size * 0.4),
                        new Vector2(size, -size * 0.25),
                        new Vector2(size, size * 0.25),
                        new Vector2(size * 0.5, size * 0.4)
                };
                Convex sensorArray = Geometry.createPolygon(head);

                // Left leg strut
                Vector2[] leftLeg = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 0.5),
                        new Vector2(-size * 0.2, -size * 0.85),
                        new Vector2(size * 0.0, -size * 0.75),
                        new Vector2(-size * 0.2, -size * 0.4)
                };
                Convex legL = Geometry.createPolygon(leftLeg);

                // Right leg strut
                Vector2[] rightLeg = new Vector2[]{
                        new Vector2(-size * 0.5, size * 0.5),
                        new Vector2(-size * 0.2, size * 0.4),
                        new Vector2(size * 0.0, size * 0.75),
                        new Vector2(-size * 0.2, size * 0.85)
                };
                Convex legR = Geometry.createPolygon(rightLeg);

                // Rear power pack (small angular detail)
                Vector2[] powerPack = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.3),
                        new Vector2(-size * 0.5, -size * 0.35),
                        new Vector2(-size * 0.5, size * 0.35),
                        new Vector2(-size * 0.8, size * 0.3)
                };
                Convex power = Geometry.createPolygon(powerPack);

                yield List.of(chassis, armLU, armRU, armLL, armRL, sensorArray, legL, legR, power);
            }

            // Scout Drone - X-shaped quadcopter with 4 rotors
            case SCOUT_DRONE -> {
                // Central hub (small diamond)
                Convex hub = Geometry.createPolygonalCircle(4, size * 0.4);

                // Four rotor arms extending from center
                // Front-left rotor arm
                Vector2[] armFL = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.2),
                        new Vector2(-size * 0.7, size * 0.7),
                        new Vector2(-size * 0.85, size * 0.55),
                        new Vector2(-size * 0.35, size * 0.05)
                };
                Convex rotorArmFL = Geometry.createPolygon(armFL);

                // Front-right rotor arm
                Vector2[] armFR = new Vector2[]{
                        new Vector2(size * 0.2, size * 0.2),
                        new Vector2(size * 0.35, size * 0.05),
                        new Vector2(size * 0.85, size * 0.55),
                        new Vector2(size * 0.7, size * 0.7)
                };
                Convex rotorArmFR = Geometry.createPolygon(armFR);

                // Rear-left rotor arm
                Vector2[] armRL = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.2),
                        new Vector2(-size * 0.35, -size * 0.05),
                        new Vector2(-size * 0.85, -size * 0.55),
                        new Vector2(-size * 0.7, -size * 0.7)
                };
                Convex rotorArmRL = Geometry.createPolygon(armRL);

                // Rear-right rotor arm
                Vector2[] armRR = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.2),
                        new Vector2(size * 0.7, -size * 0.7),
                        new Vector2(size * 0.85, -size * 0.55),
                        new Vector2(size * 0.35, -size * 0.05)
                };
                Convex rotorArmRR = Geometry.createPolygon(armRR);

                yield List.of(hub, rotorArmFL, rotorArmFR, rotorArmRL, rotorArmRR);
            }

            // Bomber - Heavy aircraft with fuselage and wings
            case BOMBER -> {
                // Main fuselage (elongated hexagon - aircraft body)
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.9, 0),                    // Nose (front)
                        new Vector2(size * 0.3, size * 0.35),          // Top-front
                        new Vector2(-size * 0.7, size * 0.35),         // Top-rear
                        new Vector2(-size * 0.9, 0),                   // Tail (rear)
                        new Vector2(-size * 0.7, -size * 0.35),        // Bottom-rear
                        new Vector2(size * 0.3, -size * 0.35)          // Bottom-front
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Left wing (swept back)
                Vector2[] leftWing = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.4),          // Inner front
                        new Vector2(-size * 0.6, size * 0.95),         // Outer tip
                        new Vector2(-size * 0.75, size * 0.85),        // Outer rear
                        new Vector2(-size * 0.35, size * 0.3)          // Inner rear
                };
                Convex wingLeft = Geometry.createPolygon(leftWing);

                // Right wing (swept back, mirrored)
                Vector2[] rightWing = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.4),         // Inner front
                        new Vector2(-size * 0.35, -size * 0.3),        // Inner rear
                        new Vector2(-size * 0.75, -size * 0.85),       // Outer rear
                        new Vector2(-size * 0.6, -size * 0.95)         // Outer tip
                };
                Convex wingRight = Geometry.createPolygon(rightWing);

                yield List.of(body, wingLeft, wingRight);
            }

            // Helicopter - Attack helicopter with main fuselage, tail boom, and rotor
            case HELICOPTER -> {
                // Main fuselage (bulbous cockpit/body)
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.7, 0),                    // Nose (front)
                        new Vector2(size * 0.4, size * 0.4),           // Top-front
                        new Vector2(-size * 0.2, size * 0.45),         // Top-mid
                        new Vector2(-size * 0.5, size * 0.25),         // Top-rear
                        new Vector2(-size * 0.5, -size * 0.25),        // Bottom-rear
                        new Vector2(-size * 0.2, -size * 0.45),        // Bottom-mid
                        new Vector2(size * 0.4, -size * 0.4)           // Bottom-front
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Tail boom (thin elongated section extending back)
                Vector2[] tailBoom = new Vector2[]{
                        new Vector2(-size * 0.4, size * 0.15),
                        new Vector2(-size * 0.95, size * 0.12),
                        new Vector2(-size * 0.95, -size * 0.12),
                        new Vector2(-size * 0.4, -size * 0.15)
                };
                Convex tail = Geometry.createPolygon(tailBoom);

                // Left landing skid
                Vector2[] leftSkid = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.5),
                        new Vector2(-size * 0.3, size * 0.6),
                        new Vector2(-size * 0.35, size * 0.5),
                        new Vector2(size * 0.25, size * 0.4)
                };
                Convex skidLeft = Geometry.createPolygon(leftSkid);

                // Right landing skid
                Vector2[] rightSkid = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.5),
                        new Vector2(size * 0.25, -size * 0.4),
                        new Vector2(-size * 0.35, -size * 0.5),
                        new Vector2(-size * 0.3, -size * 0.6)
                };
                Convex skidRight = Geometry.createPolygon(rightSkid);

                yield List.of(body, tail, skidLeft, skidRight);
            }

            // Laser Gunship - Futuristic VTOL with laser weapon pods
            case LASER_GUNSHIP -> {
                // Main fuselage (angular, tech-focused design)
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.85, 0),                   // Nose (front, sharp)
                        new Vector2(size * 0.5, size * 0.35),          // Top-front
                        new Vector2(-size * 0.3, size * 0.4),          // Top-mid
                        new Vector2(-size * 0.6, size * 0.2),          // Top-rear
                        new Vector2(-size * 0.6, -size * 0.2),         // Bottom-rear
                        new Vector2(-size * 0.3, -size * 0.4),         // Bottom-mid
                        new Vector2(size * 0.5, -size * 0.35)          // Bottom-front
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Left laser weapon pod (angular housing)
                Vector2[] leftPod = new Vector2[]{
                        new Vector2(size * 0.6, size * 0.45),          // Front inner
                        new Vector2(size * 0.8, size * 0.55),          // Front outer (emitter)
                        new Vector2(-size * 0.2, size * 0.65),         // Rear outer
                        new Vector2(-size * 0.3, size * 0.5)           // Rear inner
                };
                Convex podLeft = Geometry.createPolygon(leftPod);

                // Right laser weapon pod (angular housing, mirrored)
                Vector2[] rightPod = new Vector2[]{
                        new Vector2(size * 0.6, -size * 0.45),         // Front inner
                        new Vector2(-size * 0.3, -size * 0.5),         // Rear inner
                        new Vector2(-size * 0.2, -size * 0.65),        // Rear outer
                        new Vector2(size * 0.8, -size * 0.55)          // Front outer (emitter)
                };
                Convex podRight = Geometry.createPolygon(rightPod);

                // Rear stabilizer/engine (diamond shape)
                Vector2[] stabilizer = new Vector2[]{
                        new Vector2(-size * 0.5, 0),                   // Front point
                        new Vector2(-size * 0.75, size * 0.15),        // Top point
                        new Vector2(-size * 0.95, 0),                  // Rear point
                        new Vector2(-size * 0.75, -size * 0.15)        // Bottom point
                };
                Convex rearStab = Geometry.createPolygon(stabilizer);

                // Cockpit canopy (small angular prism at front)
                Vector2[] canopy = new Vector2[]{
                        new Vector2(size * 0.7, 0),                    // Front point
                        new Vector2(size * 0.4, size * 0.2),           // Top
                        new Vector2(size * 0.2, size * 0.15),          // Rear top
                        new Vector2(size * 0.2, -size * 0.15),         // Rear bottom
                        new Vector2(size * 0.4, -size * 0.2)           // Bottom
                };
                Convex cockpit = Geometry.createPolygon(canopy);

                yield List.of(body, podLeft, podRight, rearStab, cockpit);
            }

            // Interceptor - Sleek delta-wing fighter jet
            case INTERCEPTOR -> {
                // Main fuselage (streamlined triangle body)
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.95, 0),                   // Nose (front, sharp point)
                        new Vector2(size * 0.2, size * 0.25),          // Top-mid
                        new Vector2(-size * 0.8, size * 0.2),          // Top-rear
                        new Vector2(-size * 0.95, 0),                  // Tail (rear center)
                        new Vector2(-size * 0.8, -size * 0.2),         // Bottom-rear
                        new Vector2(size * 0.2, -size * 0.25)          // Bottom-mid
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Left delta wing (swept-back triangular)
                Vector2[] leftWing = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.3),           // Inner front
                        new Vector2(-size * 0.4, size * 0.95),         // Outer tip
                        new Vector2(-size * 0.7, size * 0.75),         // Outer rear
                        new Vector2(-size * 0.3, size * 0.25)          // Inner rear
                };
                Convex wingLeft = Geometry.createPolygon(leftWing);

                // Right delta wing (swept-back triangular, mirrored)
                Vector2[] rightWing = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.3),          // Inner front
                        new Vector2(-size * 0.3, -size * 0.25),        // Inner rear
                        new Vector2(-size * 0.7, -size * 0.75),        // Outer rear
                        new Vector2(-size * 0.4, -size * 0.95)         // Outer tip
                };
                Convex wingRight = Geometry.createPolygon(rightWing);

                // Tail fins (vertical stabilizers)
                Vector2[] tailFin = new Vector2[]{
                        new Vector2(-size * 0.6, 0),
                        new Vector2(-size * 0.85, size * 0.15),
                        new Vector2(-size * 0.95, 0),
                        new Vector2(-size * 0.85, -size * 0.15)
                };
                Convex fin = Geometry.createPolygon(tailFin);

                yield List.of(body, wingLeft, wingRight, fin);
            }

            // Gunship - Heavy attack aircraft with prominent swept wings
            case GUNSHIP -> {
                // Main fuselage (heavy gunship body) - hexagon, counter-clockwise
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.85, 0),                   // 1. Nose (rightmost)
                        new Vector2(size * 0.5, size * 0.35),          // 2. Top-front (narrower body)
                        new Vector2(-size * 0.6, size * 0.35),         // 3. Top-rear
                        new Vector2(-size * 0.95, 0),                  // 4. Tail (leftmost)
                        new Vector2(-size * 0.6, -size * 0.35),        // 5. Bottom-rear
                        new Vector2(size * 0.5, -size * 0.35)          // 6. Bottom-front
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Tail stabilizer - triangle, counter-clockwise
                // Start from rightmost point, go counter-clockwise (up then down)
                Vector2[] tail = new Vector2[]{
                        new Vector2(-size * 0.85, 0),                  // 1. Front point (rightmost)
                        new Vector2(-size * 1.05, size * 0.25),        // 2. Top point (larger)
                        new Vector2(-size * 1.05, -size * 0.25)        // 3. Bottom point
                };
                Convex stabilizer = Geometry.createPolygon(tail);

                Vector2[] wing = new Vector2[]{
                        new Vector2(-size * 0.1, size * 1.1),          // 1. top right
                        new Vector2(-size * 0.4, size * 1.1),          // 2. top left
                        new Vector2(-size * 0.4, -size * 1.1),          // 4. bottom left
                        new Vector2(-size * 0.1, -size * 1.1),         // 5. bottom right
                        new Vector2(size * 0.4, 0)           // 6. mid-point on fuselage
                };
                Convex wingPoly = Geometry.createPolygon(wing);

                yield List.of(wingPoly, body, stabilizer);
            }

            // Jeep - fast light vehicle with angular chassis and armor plating
            case JEEP -> {
                // Main chassis: elongated hexagon (streamlined body)
                Vector2[] chassis = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.4),
                        new Vector2(size * 0.3, -size * 0.6),
                        new Vector2(size * 1.1, 0),
                        new Vector2(size * 0.3, size * 0.6),
                        new Vector2(-size * 0.9, size * 0.4)
                };
                Convex mainBody = Geometry.createPolygon(chassis);

                // Top armor plate (small angular detail)
                Vector2[] topPlate = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.65),
                        new Vector2(size * 0.4, -size * 0.75),
                        new Vector2(size * 0.6, -size * 0.55)
                };
                Convex plateTop = Geometry.createPolygon(topPlate);

                // Bottom armor plate (small angular detail)
                Vector2[] bottomPlate = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.65),
                        new Vector2(size * 0.6, size * 0.55),
                        new Vector2(size * 0.4, size * 0.75)
                };
                Convex plateBottom = Geometry.createPolygon(bottomPlate);

                yield List.of(mainBody, plateTop, plateBottom);
            }

            // Photon Scout - angular reflector array vehicle with prismatic beam deflectors
            case PHOTON_SCOUT -> {
                // Main chassis: elongated hexagon
                Vector2[] chassis = new Vector2[]{
                        new Vector2(-size, -size * 0.4),
                        new Vector2(size * 0.5, -size * 0.6),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(-size, size * 0.4)
                };
                Convex mainChassis = Geometry.createPolygon(chassis);

                // Front reflector array (angular prism)
                Vector2[] frontReflector = new Vector2[]{
                        new Vector2(size * 0.8, -size * 0.4),
                        new Vector2(size * 1.4, -size * 0.3),
                        new Vector2(size * 1.5, 0),
                        new Vector2(size * 1.4, size * 0.3),
                        new Vector2(size * 0.8, size * 0.4)
                };
                Convex frontArray = Geometry.createPolygon(frontReflector);

                // Top beam deflector (fractal wing)
                Vector2[] topDeflector = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.7),
                        new Vector2(size * 0.8, -size * 0.85),
                        new Vector2(size * 0.9, -size * 0.6)
                };
                Convex deflectorTop = Geometry.createPolygon(topDeflector);

                // Bottom beam deflector (fractal wing)
                Vector2[] bottomDeflector = new Vector2[]{
                        new Vector2(size * 0.2, size * 0.7),
                        new Vector2(size * 0.9, size * 0.6),
                        new Vector2(size * 0.8, size * 0.85)
                };
                Convex deflectorBottom = Geometry.createPolygon(bottomDeflector);

                // Rear energy collector (small angular piece)
                Vector2[] rearCollector = new Vector2[]{
                        new Vector2(-size * 1.1, -size * 0.25),
                        new Vector2(-size * 0.7, -size * 0.3),
                        new Vector2(-size * 0.7, size * 0.3),
                        new Vector2(-size * 1.1, size * 0.25)
                };
                Convex collector = Geometry.createPolygon(rearCollector);

                yield List.of(mainChassis, frontArray, deflectorTop, deflectorBottom, collector);
            }

            // Flak Tank - anti-aircraft vehicle with flak cannon and stabilizers
            case FLAK_TANK -> {
                // Main hull: hexagonal platform
                Vector2[] hull = new Vector2[]{
                        new Vector2(-size * 0.85, -size * 0.35),
                        new Vector2(-size * 0.35, -size * 0.75),
                        new Vector2(size * 0.35, -size * 0.75),
                        new Vector2(size * 0.85, -size * 0.35),
                        new Vector2(size * 0.85, size * 0.35),
                        new Vector2(size * 0.35, size * 0.75),
                        new Vector2(-size * 0.35, size * 0.75),
                        new Vector2(-size * 0.85, size * 0.35)
                };
                Convex mainHull = Geometry.createPolygon(hull);

                // Flak cannon mount (elevated turret platform)
                Vector2[] cannon = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.5),
                        new Vector2(size * 0.4, -size * 0.55),
                        new Vector2(size * 0.7, -size * 0.25),
                        new Vector2(size * 0.7, size * 0.25),
                        new Vector2(size * 0.4, size * 0.55),
                        new Vector2(-size * 0.2, size * 0.5)
                };
                Convex cannonMount = Geometry.createPolygon(cannon);

                // Left stabilizer (for recoil absorption)
                Vector2[] leftStab = new Vector2[]{
                        new Vector2(-size * 0.4, -size * 0.8),
                        new Vector2(size * 0.0, -size * 0.9),
                        new Vector2(size * 0.2, -size * 0.75)
                };
                Convex stabLeft = Geometry.createPolygon(leftStab);

                // Right stabilizer (for recoil absorption)
                Vector2[] rightStab = new Vector2[]{
                        new Vector2(-size * 0.4, size * 0.8),
                        new Vector2(size * 0.2, size * 0.75),
                        new Vector2(size * 0.0, size * 0.9)
                };
                Convex stabRight = Geometry.createPolygon(rightStab);

                yield List.of(mainHull, cannonMount, stabLeft, stabRight);
            }

            // SAM Launcher - surface-to-air missile platform with vertical launch tubes
            case SAM_LAUNCHER -> {
                // Main hull: hexagonal platform (missile carrier base)
                Vector2[] hull = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.4),
                        new Vector2(-size * 0.4, -size * 0.8),
                        new Vector2(size * 0.4, -size * 0.8),
                        new Vector2(size * 0.9, -size * 0.4),
                        new Vector2(size * 0.9, size * 0.4),
                        new Vector2(size * 0.4, size * 0.8),
                        new Vector2(-size * 0.4, size * 0.8),
                        new Vector2(-size * 0.9, size * 0.4)
                };
                Convex mainHull = Geometry.createPolygon(hull);

                // Missile launcher array (raised platform with tubes)
                Vector2[] launcherArray = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 0.6),
                        new Vector2(size * 0.3, -size * 0.7),
                        new Vector2(size * 0.7, -size * 0.3),
                        new Vector2(size * 0.7, size * 0.3),
                        new Vector2(size * 0.3, size * 0.7),
                        new Vector2(-size * 0.5, size * 0.6)
                };
                Convex launcher = Geometry.createPolygon(launcherArray);

                // Left missile tube (vertical launch)
                Vector2[] leftTube = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.75),
                        new Vector2(size * 0.1, -size * 0.85),
                        new Vector2(size * 0.3, -size * 0.7),
                        new Vector2(size * 0.2, -size * 0.55)
                };
                Convex tubeLeft = Geometry.createPolygon(leftTube);

                // Right missile tube (vertical launch)
                Vector2[] rightTube = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.75),
                        new Vector2(size * 0.2, size * 0.55),
                        new Vector2(size * 0.3, size * 0.7),
                        new Vector2(size * 0.1, size * 0.85)
                };
                Convex tubeRight = Geometry.createPolygon(rightTube);

                // Radar array (targeting system)
                Vector2[] radar = new Vector2[]{
                        new Vector2(size * 0.5, -size * 0.25),
                        new Vector2(size * 0.85, -size * 0.15),
                        new Vector2(size * 0.85, size * 0.15),
                        new Vector2(size * 0.5, size * 0.25)
                };
                Convex radarArray = Geometry.createPolygon(radar);

                yield List.of(mainHull, launcher, tubeLeft, tubeRight, radarArray);
            }

            // Shield Tank - mobile shield generator with energy projectors
            case SHIELD_TANK -> {
                // Main hull: large octagonal platform (bigger than normal tank)
                Vector2[] hull = new Vector2[]{
                        new Vector2(-size * 0.95, -size * 0.45),
                        new Vector2(-size * 0.45, -size * 0.85),
                        new Vector2(size * 0.45, -size * 0.85),
                        new Vector2(size * 0.95, -size * 0.45),
                        new Vector2(size * 0.95, size * 0.45),
                        new Vector2(size * 0.45, size * 0.85),
                        new Vector2(-size * 0.45, size * 0.85),
                        new Vector2(-size * 0.95, size * 0.45)
                };
                Convex mainHull = Geometry.createPolygon(hull);

                // Shield generator core (central energy projector)
                Vector2[] core = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.3),
                        new Vector2(size * 0.2, -size * 0.4),
                        new Vector2(size * 0.5, 0),
                        new Vector2(size * 0.2, size * 0.4),
                        new Vector2(-size * 0.3, size * 0.3)
                };
                Convex shieldCore = Geometry.createPolygon(core);

                // Left energy projector (emits shield field)
                Vector2[] leftProj = new Vector2[]{
                        new Vector2(-size * 0.6, -size * 0.7),
                        new Vector2(-size * 0.2, -size * 0.9),
                        new Vector2(size * 0.1, -size * 0.75)
                };
                Convex projLeft = Geometry.createPolygon(leftProj);

                // Right energy projector (emits shield field)
                Vector2[] rightProj = new Vector2[]{
                        new Vector2(-size * 0.6, size * 0.7),
                        new Vector2(size * 0.1, size * 0.75),
                        new Vector2(-size * 0.2, size * 0.9)
                };
                Convex projRight = Geometry.createPolygon(rightProj);

                yield List.of(mainHull, shieldCore, projLeft, projRight);
            }

            // Spider Mine - compact spider-like mine with legs
            case SPIDER_MINE -> {
                // Main body: small diamond core (mine casing)
                Vector2[] core = new Vector2[]{
                        new Vector2(-size * 0.6, 0),             // Back
                        new Vector2(0, -size * 0.6),             // Bottom
                        new Vector2(size * 0.8, 0),              // Front (sensor)
                        new Vector2(0, size * 0.6)               // Top
                };
                Convex body = Geometry.createPolygon(core);

                // Front sensor array (targeting system)
                Vector2[] sensor = new Vector2[]{
                        new Vector2(size * 0.5, -size * 0.3),
                        new Vector2(size, 0),
                        new Vector2(size * 0.5, size * 0.3)
                };
                Convex sensorArray = Geometry.createPolygon(sensor);

                // Legs - using simple triangles with guaranteed CCW winding
                // For CCW: vertices should go counter-clockwise when viewed from above

                // Left front leg (negative Y side, front)
                Vector2[] legLF = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.8),    // Bottom-right
                        new Vector2(0, -size * 0.5),             // Top
                        new Vector2(-size * 0.1, -size * 0.7)    // Bottom-left
                };
                Convex leftFrontLeg = Geometry.createPolygon(legLF);

                // Left rear leg (negative Y side, rear)
                Vector2[] legLR = new Vector2[]{
                        new Vector2(-size * 0.7, -size * 0.6),   // Bottom-right
                        new Vector2(-size * 0.5, -size * 0.3),   // Top
                        new Vector2(-size * 0.6, -size * 0.4)    // Bottom-left
                };
                Convex leftRearLeg = Geometry.createPolygon(legLR);

                // Right front leg (positive Y side, front)
                Vector2[] legRF = new Vector2[]{
                        new Vector2(-size * 0.1, size * 0.7),    // Top-left
                        new Vector2(0, size * 0.5),              // Bottom
                        new Vector2(size * 0.2, size * 0.8)      // Top-right
                };
                Convex rightFrontLeg = Geometry.createPolygon(legRF);

                // Right rear leg (positive Y side, rear)
                Vector2[] legRR = new Vector2[]{
                        new Vector2(-size * 0.6, size * 0.4),    // Top-left
                        new Vector2(-size * 0.5, size * 0.3),    // Bottom
                        new Vector2(-size * 0.7, size * 0.6)     // Top-right
                };
                Convex rightRearLeg = Geometry.createPolygon(legRR);

                yield List.of(body, sensorArray, leftFrontLeg, leftRearLeg, rightFrontLeg, rightRearLeg);
            }

            // APC - armored box-shaped troop transport
            case APC -> {
                // Main hull: large rectangular body (troop compartment) - simplified to octagon for convexity
                Vector2[] hull = new Vector2[]{
                        new Vector2(-size, -size * 0.6),   // Back left
                        new Vector2(size * 0.7, -size * 0.7),    // Front left (extended)
                        new Vector2(size, -size * 0.4),    // Front-left corner (angled)
                        new Vector2(size, size * 0.4),     // Front-right corner (angled)
                        new Vector2(size * 0.7, size * 0.7),     // Front right (extended)
                        new Vector2(-size, size * 0.6)     // Back right
                };
                Convex mainHull = Geometry.createPolygon(hull);

                // Front armor plate (angled for protection)
                Vector2[] frontArmor = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.5),
                        new Vector2(size * 1.05, -size * 0.3),
                        new Vector2(size * 1.05, size * 0.3),
                        new Vector2(size * 0.7, size * 0.5)
                };
                Convex armorFront = Geometry.createPolygon(frontArmor);

                // Rear door/ramp (troop exit) - fixed vertex order
                Vector2[] rearDoor = new Vector2[]{
                        new Vector2(-size * 1.05, -size * 0.4),
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(-size * 0.9, size * 0.5),
                        new Vector2(-size * 1.05, size * 0.4)
                };
                Convex door = Geometry.createPolygon(rearDoor);

                // Top hatch/firing ports (left)
                Vector2[] leftHatch = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.7),
                        new Vector2(size * 0.2, -size * 0.75),
                        new Vector2(size * 0.3, -size * 0.6)
                };
                Convex hatchLeft = Geometry.createPolygon(leftHatch);

                // Top hatch/firing ports (right)
                Vector2[] rightHatch = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.7),
                        new Vector2(size * 0.3, size * 0.6),
                        new Vector2(size * 0.2, size * 0.75)
                };
                Convex hatchRight = Geometry.createPolygon(rightHatch);

                yield List.of(mainHull, armorFront, door, hatchLeft, hatchRight);
            }

            // Tank - main battle tank with turret platform and armor plating
            case TANK -> {
                // Main hull: octagonal platform
                Vector2[] hull = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.4),
                        new Vector2(-size * 0.4, -size * 0.8),
                        new Vector2(size * 0.4, -size * 0.8),
                        new Vector2(size * 0.9, -size * 0.4),
                        new Vector2(size * 0.9, size * 0.4),
                        new Vector2(size * 0.4, size * 0.8),
                        new Vector2(-size * 0.4, size * 0.8),
                        new Vector2(-size * 0.9, size * 0.4)
                };
                Convex mainHull = Geometry.createPolygon(hull);

                // Turret base (hexagon on top of hull)
                Vector2[] turretBase = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.5),
                        new Vector2(size * 0.2, -size * 0.6),
                        new Vector2(size * 0.6, -size * 0.3),
                        new Vector2(size * 0.6, size * 0.3),
                        new Vector2(size * 0.2, size * 0.6),
                        new Vector2(-size * 0.3, size * 0.5)
                };
                Convex turret = Geometry.createPolygon(turretBase);

                // Side armor plates (left)
                Vector2[] leftArmor = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 0.85),
                        new Vector2(size * 0.1, -size * 0.95),
                        new Vector2(size * 0.3, -size * 0.8)
                };
                Convex armorLeft = Geometry.createPolygon(leftArmor);

                // Side armor plates (right)
                Vector2[] rightArmor = new Vector2[]{
                        new Vector2(-size * 0.5, size * 0.85),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(size * 0.1, size * 0.95)
                };
                Convex armorRight = Geometry.createPolygon(rightArmor);

                yield List.of(mainHull, turret, armorLeft, armorRight);
            }

            // Beam Tank - multi-faceted prism platform with crystalline beam array
            case BEAM_TANK -> {
                // Main platform: wide octagonal base
                Vector2[] platform = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(-size * 0.3, -size * 0.9),
                        new Vector2(size * 0.3, -size * 0.9),
                        new Vector2(size * 0.9, -size * 0.5),
                        new Vector2(size, 0),
                        new Vector2(size * 0.9, size * 0.5),
                        new Vector2(size * 0.3, size * 0.9),
                        new Vector2(-size * 0.3, size * 0.9),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex mainPlatform = Geometry.createPolygon(platform);

                // Primary focusing prism (center front)
                Vector2[] primaryPrism = new Vector2[]{
                        new Vector2(size * 0.4, -size * 0.3),
                        new Vector2(size * 1.1, -size * 0.2),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 1.1, size * 0.2),
                        new Vector2(size * 0.4, size * 0.3)
                };
                Convex centerPrism = Geometry.createPolygon(primaryPrism);

                // Upper beam array (fractal antenna)
                Vector2[] upperArray = new Vector2[]{
                        new Vector2(-size * 0.1, -size),
                        new Vector2(size * 0.3, -size * 1.15),
                        new Vector2(size * 0.6, -size),
                        new Vector2(size * 0.4, -size * 0.8)
                };
                Convex topArray = Geometry.createPolygon(upperArray);

                // Lower beam array (fractal antenna)
                Vector2[] lowerArray = new Vector2[]{
                        new Vector2(-size * 0.1, size),
                        new Vector2(size * 0.4, size * 0.8),
                        new Vector2(size * 0.6, size),
                        new Vector2(size * 0.3, size * 1.15)
                };
                Convex bottomArray = Geometry.createPolygon(lowerArray);

                // Left energy lens
                Vector2[] leftLens = new Vector2[]{
                        new Vector2(size * 0.0, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.85),
                        new Vector2(size * 0.5, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.55)
                };
                Convex lensLeft = Geometry.createPolygon(leftLens);

                // Right energy lens
                Vector2[] rightLens = new Vector2[]{
                        new Vector2(size * 0.0, size * 0.7),
                        new Vector2(size * 0.3, size * 0.55),
                        new Vector2(size * 0.5, size * 0.7),
                        new Vector2(size * 0.3, size * 0.85)
                };
                Convex lensRight = Geometry.createPolygon(rightLens);

                // Rear power crystal (angular)
                Vector2[] powerCrystal = new Vector2[]{
                        new Vector2(-size, -size * 0.3),
                        new Vector2(-size * 0.6, -size * 0.4),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size, size * 0.3)
                };
                Convex crystal = Geometry.createPolygon(powerCrystal);

                yield List.of(mainPlatform, centerPrism, topArray, bottomArray, lensLeft, lensRight, crystal);
            }

            // Cloak Tank - sleek diamond (low profile, streamlined)
            case CLOAK_TANK -> {
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size, 0),          // Back point
                        new Vector2(0, -size * 0.7),          // Left point (narrow)
                        new Vector2(size, 0),           // Front point (sleek, pointing right)
                        new Vector2(0, size * 0.7)            // Right point (narrow)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Artillery - long-range siege weapon with stabilizer outriggers
            case ARTILLERY -> {
                // Main body: elongated hexagon (gun platform)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(size * 0.2, -size * 0.7),
                        new Vector2(size * 0.8, -size * 0.4),
                        new Vector2(size * 0.8, size * 0.4),
                        new Vector2(size * 0.2, size * 0.7),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex platform = Geometry.createPolygon(mainBody);

                // Left stabilizer outrigger (for recoil stability)
                Vector2[] leftStabilizer = new Vector2[]{
                        new Vector2(-size * 0.4, -size * 0.75),
                        new Vector2(size * 0.0, -size * 0.95),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size * 0.1, -size * 0.65)
                };
                Convex stabLeft = Geometry.createPolygon(leftStabilizer);

                // Right stabilizer outrigger (for recoil stability)
                Vector2[] rightStabilizer = new Vector2[]{
                        new Vector2(-size * 0.4, size * 0.75),
                        new Vector2(size * 0.1, size * 0.65),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(size * 0.0, size * 0.95)
                };
                Convex stabRight = Geometry.createPolygon(rightStabilizer);

                // Rear counterweight (balance for long barrel)
                Vector2[] counterweight = new Vector2[]{
                        new Vector2(-size, -size * 0.3),
                        new Vector2(-size * 0.6, -size * 0.4),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size, size * 0.3)
                };
                Convex weight = Geometry.createPolygon(counterweight);

                yield List.of(platform, stabLeft, stabRight, weight);
            }

            // Pulse Artillery - complex fractal lens array with multi-stage beam amplification
            case PULSE_ARTILLERY -> {
                // Main body: large angular platform (convex octagon)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 1.1, 0),
                        new Vector2(-size * 0.6, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size, -size * 0.4),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size, size * 0.4),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(-size * 0.6, size * 0.7)
                };
                Convex artilleryBody = Geometry.createPolygon(mainBody);

                // Primary lens chamber (forward focusing array - simplified convex)
                Vector2[] primaryChamber = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.4),
                        new Vector2(size * 1.3, -size * 0.3),
                        new Vector2(size * 1.4, 0),
                        new Vector2(size * 1.3, size * 0.3),
                        new Vector2(size * 0.7, size * 0.4)
                };
                Convex primaryLens = Geometry.createPolygon(primaryChamber);

                // Upper amplifier array (convex trapezoid)
                Vector2[] topAmplifier = new Vector2[]{
                        new Vector2(size * 0.0, -size * 0.85),
                        new Vector2(size * 0.4, -size),
                        new Vector2(size * 0.8, -size * 0.95),
                        new Vector2(size * 0.6, -size * 0.75)
                };
                Convex ampTop = Geometry.createPolygon(topAmplifier);

                // Lower amplifier array (convex trapezoid)
                Vector2[] bottomAmplifier = new Vector2[]{
                        new Vector2(size * 0.0, size * 0.85),
                        new Vector2(size * 0.6, size * 0.75),
                        new Vector2(size * 0.8, size * 0.95),
                        new Vector2(size * 0.4, size)
                };
                Convex ampBottom = Geometry.createPolygon(bottomAmplifier);

                // Left side crystal array (triangle)
                Vector2[] leftArray = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.85),
                        new Vector2(size * 0.1, -size),
                        new Vector2(size * 0.2, -size * 0.8)
                };
                Convex arrayLeft = Geometry.createPolygon(leftArray);

                // Right side crystal array (triangle)
                Vector2[] rightArray = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.85),
                        new Vector2(size * 0.2, size * 0.8),
                        new Vector2(size * 0.1, size)
                };
                Convex arrayRight = Geometry.createPolygon(rightArray);

                // Upper focusing crystal (small triangle)
                Vector2[] upperCrystal = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.6),
                        new Vector2(size * 0.6, -size * 0.7),
                        new Vector2(size * 0.5, -size * 0.5)
                };
                Convex crystalU = Geometry.createPolygon(upperCrystal);

                // Lower focusing crystal (small triangle)
                Vector2[] lowerCrystal = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.6),
                        new Vector2(size * 0.5, size * 0.5),
                        new Vector2(size * 0.6, size * 0.7)
                };
                Convex crystalL = Geometry.createPolygon(lowerCrystal);

                // Rear power core (convex pentagon)
                Vector2[] powerCore = new Vector2[]{
                        new Vector2(-size * 1.2, 0),
                        new Vector2(-size * 0.8, -size * 0.5),
                        new Vector2(-size * 0.6, 0),
                        new Vector2(-size * 0.8, size * 0.5)
                };
                Convex core = Geometry.createPolygon(powerCore);

                yield List.of(artilleryBody, primaryLens, ampTop, ampBottom,
                        arrayLeft, arrayRight, crystalU, crystalL, core);
            }

            // Gigantonaut - trapezoid (wide at back, tapered at front for heavy artillery look)
            case GIGANTONAUT -> {
                // Create a trapezoid: wide at back, narrow at front
                // Vertices must be in counter-clockwise order for dyn4j
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.9),  // Back left (wide)
                        new Vector2(size * 1.2, -size * 0.6),  // Front left (tapered)
                        new Vector2(size * 1.2, size * 0.6),  // Front right (tapered)
                        new Vector2(-size * 0.8, size * 0.9)   // Back right (wide)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            case RAIDER -> {
                // Main body: aggressive arrow-like chassis (stretched forward)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(size * 0.5, -size * 0.6),
                        new Vector2(size * 1.3, 0),
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex chassis = Geometry.createPolygon(mainBody);

                // Upper wing blade (aggressive swept-back design)
                Vector2[] upperWing = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 0.7),
                        new Vector2(size * 0.2, -size),
                        new Vector2(size * 0.8, -size * 0.85),
                        new Vector2(size * 0.6, -size * 0.6)
                };
                Convex wingUpper = Geometry.createPolygon(upperWing);

                // Lower wing blade (aggressive swept-back design)
                Vector2[] lowerWing = new Vector2[]{
                        new Vector2(-size * 0.5, size * 0.7),
                        new Vector2(size * 0.6, size * 0.6),
                        new Vector2(size * 0.8, size * 0.85),
                        new Vector2(size * 0.2, size)
                };
                Convex wingLower = Geometry.createPolygon(lowerWing);

                // Upper secondary blade (fractal aggression)
                Vector2[] upperSecondary = new Vector2[]{
                        new Vector2(size * 0.1, -size * 0.75),
                        new Vector2(size * 0.5, -size * 0.95),
                        new Vector2(size * 0.7, -size * 0.75)
                };
                Convex bladeUS = Geometry.createPolygon(upperSecondary);

                // Lower secondary blade (fractal aggression)
                Vector2[] lowerSecondary = new Vector2[]{
                        new Vector2(size * 0.1, size * 0.75),
                        new Vector2(size * 0.7, size * 0.75),
                        new Vector2(size * 0.5, size * 0.95)
                };
                Convex bladeLS = Geometry.createPolygon(lowerSecondary);

                // Rear stabilizer fins (speed aesthetic - upper)
                Vector2[] rearFinUpper = new Vector2[]{
                        new Vector2(-size, -size * 0.4),
                        new Vector2(-size * 0.5, -size * 0.8),
                        new Vector2(-size * 0.3, -size * 0.6),
                        new Vector2(-size * 0.6, -size * 0.4)
                };
                Convex finU = Geometry.createPolygon(rearFinUpper);

                // Rear stabilizer fins (speed aesthetic - lower)
                Vector2[] rearFinLower = new Vector2[]{
                        new Vector2(-size, size * 0.4),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size * 0.3, size * 0.6),
                        new Vector2(-size * 0.5, size * 0.8)
                };
                Convex finL = Geometry.createPolygon(rearFinLower);

                // Engine/power core (rear energy source)
                Vector2[] powerCore = new Vector2[]{
                        new Vector2(-size * 1.1, 0),
                        new Vector2(-size * 0.8, -size * 0.35),
                        new Vector2(-size * 0.6, 0),
                        new Vector2(-size * 0.8, size * 0.35)
                };
                Convex engine = Geometry.createPolygon(powerCore);

                yield List.of(chassis, wingUpper, wingLower, bladeUS, bladeLS, finU, finL, engine);
            }

            // Photon Titan - massive crystalline energy platform with nested prism arrays
            case PHOTON_TITAN -> {
                // Main platform: large convex crystalline base (octagon)
                Vector2[] mainPlatform = new Vector2[]{
                        new Vector2(-size * 1.2, 0),
                        new Vector2(-size * 0.8, -size * 0.9),
                        new Vector2(size * 0.0, -size * 1.1),
                        new Vector2(size * 0.8, -size * 0.9),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 0.8, size * 0.9),
                        new Vector2(size * 0.0, size * 1.1),
                        new Vector2(-size * 0.8, size * 0.9)
                };
                Convex titanBase = Geometry.createPolygon(mainPlatform);

                // Primary energy core (central convex hexagon)
                Vector2[] energyCore = new Vector2[]{
                        new Vector2(-size * 0.4, 0),
                        new Vector2(-size * 0.2, -size * 0.5),
                        new Vector2(size * 0.3, -size * 0.5),
                        new Vector2(size * 0.6, 0),
                        new Vector2(size * 0.3, size * 0.5),
                        new Vector2(-size * 0.2, size * 0.5)
                };
                Convex core = Geometry.createPolygon(energyCore);

                // Primary beam emitter (forward prism - pentagon)
                Vector2[] beamEmitter = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.3),
                        new Vector2(size * 1.4, -size * 0.2),
                        new Vector2(size * 1.5, 0),
                        new Vector2(size * 1.4, size * 0.2),
                        new Vector2(size * 0.7, size * 0.3)
                };
                Convex emitter = Geometry.createPolygon(beamEmitter);

                // Upper crystalline array (convex quadrilateral)
                Vector2[] upperCrystalArray = new Vector2[]{
                        new Vector2(size * 0.0, -size * 1.15),
                        new Vector2(size * 0.5, -size * 1.25),
                        new Vector2(size * 0.7, -size * 0.95),
                        new Vector2(size * 0.3, -size * 0.85)
                };
                Convex upperArray = Geometry.createPolygon(upperCrystalArray);

                // Lower crystalline array (convex quadrilateral)
                Vector2[] lowerCrystalArray = new Vector2[]{
                        new Vector2(size * 0.0, size * 1.15),
                        new Vector2(size * 0.3, size * 0.85),
                        new Vector2(size * 0.7, size * 0.95),
                        new Vector2(size * 0.5, size * 1.25)
                };
                Convex lowerArray = Geometry.createPolygon(lowerCrystalArray);

                // Left wing prism array (convex quadrilateral)
                Vector2[] leftWingArray = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.85),
                        new Vector2(-size * 0.2, -size * 1.05),
                        new Vector2(size * 0.2, -size * 0.95),
                        new Vector2(size * 0.0, -size * 0.75)
                };
                Convex leftWing = Geometry.createPolygon(leftWingArray);

                // Right wing prism array (convex quadrilateral)
                Vector2[] rightWingArray = new Vector2[]{
                        new Vector2(-size * 0.8, size * 0.85),
                        new Vector2(size * 0.0, size * 0.75),
                        new Vector2(size * 0.2, size * 0.95),
                        new Vector2(-size * 0.2, size * 1.05)
                };
                Convex rightWing = Geometry.createPolygon(rightWingArray);

                // Upper focusing prism (triangle)
                Vector2[] upperPrism = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.65),
                        new Vector2(size * 0.7, -size * 0.75),
                        new Vector2(size * 0.6, -size * 0.55)
                };
                Convex prismU = Geometry.createPolygon(upperPrism);

                // Lower focusing prism (triangle)
                Vector2[] lowerPrism = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.65),
                        new Vector2(size * 0.6, size * 0.55),
                        new Vector2(size * 0.7, size * 0.75)
                };
                Convex prismL = Geometry.createPolygon(lowerPrism);

                // Upper micro-crystal (triangle)
                Vector2[] microTop = new Vector2[]{
                        new Vector2(size * 0.4, -size * 0.95),
                        new Vector2(size * 0.6, -size * 1.05),
                        new Vector2(size * 0.7, -size * 0.9)
                };
                Convex microT = Geometry.createPolygon(microTop);

                // Lower micro-crystal (triangle)
                Vector2[] microBottom = new Vector2[]{
                        new Vector2(size * 0.4, size * 0.95),
                        new Vector2(size * 0.7, size * 0.9),
                        new Vector2(size * 0.6, size * 1.05)
                };
                Convex microB = Geometry.createPolygon(microBottom);

                // Rear power resonator (convex pentagon)
                Vector2[] powerResonator = new Vector2[]{
                        new Vector2(-size * 1.3, 0),
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(-size * 0.7, 0),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex resonator = Geometry.createPolygon(powerResonator);

                yield List.of(titanBase, core, emitter, upperArray, lowerArray,
                        leftWing, rightWing, prismU, prismL, microT, microB, resonator);
            }

            // Colossus - massive robotic walker with mechanical leg assemblies
            case COLOSSUS -> {
                // Central chassis (main robotic body)
                Vector2[] centralBody = new Vector2[]{
                        new Vector2(-size * 0.6, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size * 0.7, -size * 0.4),
                        new Vector2(size * 0.7, size * 0.4),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(-size * 0.6, size * 0.7)
                };
                Convex chassis = Geometry.createPolygon(centralBody);

                // Upper torso (head/sensor assembly)
                Vector2[] upperTorso = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.85),
                        new Vector2(size * 0.2, -size),
                        new Vector2(size * 0.6, -size * 0.75),
                        new Vector2(size * 0.4, -size * 0.55)
                };
                Convex torso = Geometry.createPolygon(upperTorso);

                // Lower torso (power core section)
                Vector2[] lowerTorso = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.85),
                        new Vector2(size * 0.4, size * 0.55),
                        new Vector2(size * 0.6, size * 0.75),
                        new Vector2(size * 0.2, size)
                };
                Convex lowerCore = Geometry.createPolygon(lowerTorso);

                // Left leg assembly (upper - mechanical joint)
                Vector2[] leftLegUpper = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.9),
                        new Vector2(-size * 0.3, -size * 1.15),
                        new Vector2(size * 0.0, -size * 1.1),
                        new Vector2(-size * 0.2, -size * 0.85)
                };
                Convex legLU = Geometry.createPolygon(leftLegUpper);

                // Right leg assembly (upper - mechanical joint)
                Vector2[] rightLegUpper = new Vector2[]{
                        new Vector2(-size * 0.8, size * 0.9),
                        new Vector2(-size * 0.2, size * 0.85),
                        new Vector2(size * 0.0, size * 1.1),
                        new Vector2(-size * 0.3, size * 1.15)
                };
                Convex legRU = Geometry.createPolygon(rightLegUpper);

                // Left leg extension (lower segment)
                Vector2[] leftLegLower = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 1.2),
                        new Vector2(-size * 0.1, -size * 1.3),
                        new Vector2(size * 0.2, -size * 1.15),
                        new Vector2(size * 0.1, -size)
                };
                Convex legLL = Geometry.createPolygon(leftLegLower);

                // Right leg extension (lower segment)
                Vector2[] rightLegLower = new Vector2[]{
                        new Vector2(-size * 0.5, size * 1.2),
                        new Vector2(size * 0.1, size),
                        new Vector2(size * 0.2, size * 1.15),
                        new Vector2(-size * 0.1, size * 1.3)
                };
                Convex legRL = Geometry.createPolygon(rightLegLower);

                // Front arm/weapon mount (left)
                Vector2[] frontArmLeft = new Vector2[]{
                        new Vector2(size * 0.5, -size * 0.6),
                        new Vector2(size * 0.9, -size * 0.7),
                        new Vector2(size, -size * 0.4),
                        new Vector2(size * 0.7, -size * 0.35)
                };
                Convex armFL = Geometry.createPolygon(frontArmLeft);

                // Front arm/weapon mount (right)
                Vector2[] frontArmRight = new Vector2[]{
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(size * 0.7, size * 0.35),
                        new Vector2(size, size * 0.4),
                        new Vector2(size * 0.9, size * 0.7)
                };
                Convex armFR = Geometry.createPolygon(frontArmRight);

                // Rear stabilizer strut (mechanical support - left)
                Vector2[] rearStrutLeft = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(-size * 0.5, -size * 0.75),
                        new Vector2(-size * 0.3, -size * 0.6),
                        new Vector2(-size * 0.6, -size * 0.4)
                };
                Convex strutL = Geometry.createPolygon(rearStrutLeft);

                // Rear stabilizer strut (mechanical support - right)
                Vector2[] rearStrutRight = new Vector2[]{
                        new Vector2(-size * 0.9, size * 0.5),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size * 0.3, size * 0.6),
                        new Vector2(-size * 0.5, size * 0.75)
                };
                Convex strutR = Geometry.createPolygon(rearStrutRight);

                // Back power unit (reactor/engine)
                Vector2[] backPower = new Vector2[]{
                        new Vector2(-size * 0.9, 0),
                        new Vector2(-size * 0.6, -size * 0.35),
                        new Vector2(-size * 0.4, 0),
                        new Vector2(-size * 0.6, size * 0.35)
                };
                Convex reactor = Geometry.createPolygon(backPower);

                yield List.of(chassis, torso, lowerCore, legLU, legRU, legLL, legRL,
                        armFL, armFR, strutL, strutR, reactor);
            }
        };
    }

    UnitType(String displayName,
             String description,
             int resourceCost,
             int buildTimeSeconds,
             double maxHealth,
             double movementSpeed,
             double damage,
             double attackRate,
             double attackRange,
             double size,
             int color,
             BuildingType producedBy,
             int upkeepCost,
             double visionRange,
             Elevation elevation,
             UnitCategory category,
             Set<BuildingType> requiredBuildings,
             int pointCost,
             Character hotkey) {
        this.displayName = displayName;
        this.description = description;
        this.resourceCost = resourceCost;
        this.buildTimeSeconds = buildTimeSeconds;
        this.maxHealth = maxHealth;
        this.movementSpeed = movementSpeed;
        this.damage = damage;
        this.attackRate = attackRate;
        this.attackRange = attackRange;
        this.size = size;
        this.color = color;
        this.producedBy = producedBy;
        this.upkeepCost = upkeepCost;
        this.visionRange = visionRange;
        this.elevation = elevation;
        this.category = category;
        this.requiredBuildings = requiredBuildings != null ? requiredBuildings : Set.of();
        this.pointCost = pointCost;
        this.hotkey = hotkey;
    }

    public static List<UnitType> sorted() {
        return Arrays.stream(UnitType.values())
                .sorted(Comparator.comparing((UnitType u) -> u.getRequiredBuildings().size())
                        .thenComparing(UnitType::getResourceCost))
                .toList();
    }

    /**
     * Get the special ability for this unit type
     */
    public SpecialAbility getSpecialAbility() {
        return switch (this) {
            case MEDIC -> SpecialAbility.HEAL;
            case ENGINEER -> SpecialAbility.REPAIR;
            case CLOAK_TANK -> SpecialAbility.CLOAK;
            case SPIDER_MINE -> SpecialAbility.SPIDER_MINE;
            default -> SpecialAbility.NONE;
        };
    }

    /**
     * Check if this unit has a special ability
     */
    public boolean hasSpecialAbility() {
        return getSpecialAbility() != SpecialAbility.NONE;
    }

    /**
     * Check if this unit can attack
     */
    public boolean canAttack() {
        return this != WORKER && this != MEDIC && this != ENGINEER && this != SPIDER_MINE && this != APC;
    }

    /**
     * Check if this unit can harvest resources
     */
    public boolean canHarvest() {
        return this == WORKER;
    }

    /**
     * Check if this unit can construct buildings
     */
    public boolean canBuild() {
        return this == WORKER;
    }

    /**
     * Check if this unit can heal other units
     */
    public boolean canHeal() {
        return this == MEDIC;
    }

    /**
     * Check if this unit can repair buildings/vehicles
     */
    public boolean canRepair() {
        return this == ENGINEER;
    }

    /**
     * Check if this is a support unit (non-combat)
     */
    public boolean isSupport() {
        return this == MEDIC || this == ENGINEER;
    }

    /**
     * Check if this is an infantry unit (can garrison in bunkers)
     */
    public boolean isInfantry() {
        return getCategory() == UnitCategory.INFANTRY;
    }

    /**
     * Check if this is an air unit (can fly over obstacles, different rendering)
     */
    public boolean isAirUnit() {
        return getCategory() == UnitCategory.FLYER;
    }

    /**
     * Check if this is a sortie-based unit (not player-controllable, executes missions and returns to base)
     */
    public boolean isSortieBased() {
        return this == BOMBER || this == INTERCEPTOR || this == GUNSHIP;
    }
}


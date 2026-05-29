package com.fullsteam.model;

import lombok.Getter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
// ArmorType is in the same package – no import needed

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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            return List.of(Geometry.createCircle(size));
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createIsoscelesTriangle(size, size * 1.2);
            torso.rotate(-Math.PI / 2);
            return List.of(torso);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createIsoscelesTriangle(size, size);
            torso.rotate(-Math.PI / 2);
            Convex barrel = Geometry.createIsoscelesTriangle(size * 0.4, size * 0.4);
            barrel.rotate(Math.PI / 2);
            barrel.translate(size * 0.7, 0);
            return List.of(torso, barrel);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createIsoscelesTriangle(size, size * 1.2);
            torso.rotate(-Math.PI / 2);
            Convex emitter = Geometry.createPolygonalEllipse(4, size * 0.48, size * 0.58);
            emitter.translate(size * 0.75, 0);
            return List.of(torso, emitter);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            return List.of(Geometry.createCircle(size));
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createPolygonalCircle(5, size, Math.PI / 5);
            torso.rotate(Math.PI);
            Convex launcher = Geometry.createRectangle(size * 1.33, size * 0.32);
            launcher.translate(size * 0.5, -size * .33);
            return List.of(torso, launcher);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createPolygonalCapsule(1, size * 1.6, size * 0.75);
            Convex barrel = Geometry.createRectangle(size * 0.95, size * 0.16);
            barrel.translate(size * 0.65, 0);
            Convex scope = Geometry.createCircle(size * 0.2);
            scope.translate(size * 0.25, -size * 0.32);
            return List.of(torso, barrel, scope);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            return List.of(Geometry.createCircle(size));
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createPolygonalCapsule(8, size * 1.7, size * 0.7);
            Convex dart = Geometry.createRectangle(size * 0.35, size * 0.18);
            dart.translate(size * .7, 0);
            return List.of(torso, dart);
        }
    },
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
            14.0,
            0x8B4513,
            BuildingType.BARRACKS,
            8,
            300.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(),
            4,
            'G'
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createPolygonalCircle(5, size, Math.PI / 5);
            torso.rotate(Math.PI);
            Convex ammo = Geometry.createRectangle(size * 0.4, size);
            ammo.translate(-size * 0.5, 0);
            return List.of(torso, ammo);
        }
    },
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
            14.0,
            0x556B2F,
            BuildingType.BARRACKS,
            12,
            340.0,
            Elevation.GROUND,
            UnitCategory.INFANTRY,
            Set.of(),
            2,
            'U'
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createPolygonalCircle(5, size, Math.PI / 5);
            torso.rotate(Math.PI);
            Convex barrel = Geometry.createRectangle(size * 0.85, size * 0.32);
            barrel.translate(size * .7, 0);
            Convex belt = Geometry.createRectangle(size * 0.35, size * 0.55);
            belt.translate(-size * 0.6, 0);
            return List.of(torso, barrel, belt);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex hull = Geometry.createPolygonalCapsule(2, size * 1.8, size);
            Convex windshield = Geometry.createRectangle(size * 0.4, size * 0.7);
            windshield.translate(size * 0.2, 0);
            return List.of(hull, windshield);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex hull = Geometry.createPolygonalCapsule(2, size * 1.5, size);
            Convex turret = Geometry.createCircle(size * 0.25);
            turret.translate(-size * 0.1, 0);
            Convex barrel = Geometry.createRectangle(size * 0.75, size * 0.14);
            barrel.translate(size * 0.2, 0);
            return List.of(hull, barrel, turret);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex chassis = Geometry.createPolygonalCapsule(2, size * 1.7, size);
            Convex turret = Geometry.createCircle(size * 0.3);
            Polygon upper = Geometry.createRectangle(size * 0.5, size * 0.12);
            upper.translate(size * 0.2, -size * .2);
            Polygon lower = mirrorAcrossX(upper);
            return List.of(chassis, turret, upper, lower);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex chassis = Geometry.createPolygonalCapsule(2, size * 1.5, size);
            Rectangle turret = Geometry.createSquare(size * 0.2);
            turret.translate(-size * .1, 0);
            Rectangle tubeL = Geometry.createRectangle(size * 0.5, size * 0.12);
            tubeL.translate(-size * .1, -size * 0.2);
            Convex tubeR = mirrorAcrossX(tubeL);
            return List.of(chassis, turret, tubeL, tubeR);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex chassis = Geometry.createPolygonalCapsule(2, size * 1.4, size * 1.2);
            Polygon upper = Geometry.createPolygon(
                    new Vector2(size * 0.3, 0),
                    new Vector2(size * 0.3, size * .2),
                    new Vector2(-size * 0.3, size * 0.3),
                    new Vector2(-size * 0.2, 0));
            Polygon lower = mirrorAcrossX(upper);
            Circle core = Geometry.createCircle(size * .12);
            return List.of(chassis, upper, lower, core);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex body = Geometry.createPolygonalCircle(6, size * 0.65);
            Convex sensor = Geometry.createCircle(size * 0.22);
            sensor.translate(size * 0.7, 0);
            Polygon frontL = Geometry.createRectangle(size * 0.55, size * 0.18);
            frontL.rotate(-Math.PI / 4);
            frontL.translate(size * 0.5, -size * 0.5);
            Polygon frontR = mirrorAcrossX(frontL);
            Polygon legL = Geometry.createRectangle(size * 0.55, size * 0.18);
            legL.rotate(Math.PI / 4);
            legL.translate(-size * 0.5, -size * 0.5);
            Polygon legR = mirrorAcrossX(legL);
            return List.of(body, frontL, frontR, legL, legR);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex hull = Geometry.createPolygonalCapsule(2, size * 1.7, size * 1.25);
            Convex armor = Geometry.createRectangle(size * .9, size * 0.25);
            armor.translate(0, size * 0.5);
            Convex ramp = Geometry.createRectangle(size * 0.9, size * 0.25);
            ramp.translate(0, -size * 0.5);
            return List.of(hull, armor, ramp);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex platform = Geometry.createPolygonalCapsule(2, size * 1.5, size * 1.1);
            Convex barrel = Geometry.createRectangle(size * 1.5, size * 0.25);
            barrel.translate(0, 0);
            Convex weight = Geometry.createRectangle(size * 0.2, size * 0.65);
            weight.translate(-size * 0.65, 0);
            return List.of(platform, barrel, weight);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Vector2[] trapezoid = new Vector2[]{
                    new Vector2(-size * 0.85, -size * 0.9),
                    new Vector2(size * 1.2, -size * 0.55),
                    new Vector2(size * 1.2, size * 0.55),
                    new Vector2(-size * 0.85, size * 0.9)
            };
            Convex hull = Geometry.createPolygon(trapezoid);
            Convex turret = Geometry.createCircle(size * 0.42);
            turret.translate(size * 0.35, 0);
            return List.of(hull, turret);
        }
    },
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
            22.0,
            0x2F4F4F,
            BuildingType.FACTORY,
            45,
            380.0,
            Elevation.GROUND,
            UnitCategory.VEHICLE,
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
            3,
            'C'
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            return List.of(Geometry.createPolygonalEllipse(8, size * 2.0, size * 1.1));
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex chassis = Geometry.createPolygonalCapsule(2, size * 2.0, size);
            Polygon upperBlade = Geometry.createPolygon(
                    new Vector2(size * 0.3, size * 0.55),
                    new Vector2(size * 0.7, size * 1.05),
                    new Vector2(-size * 0.1, size * 0.95),
                    new Vector2(-size * 0.2, size * 0.55)
            );
            upperBlade.translate(-size * 0.5, -size * .2);
            Polygon lowerBlade = mirrorAcrossX(upperBlade);
            return List.of(chassis, upperBlade, lowerBlade);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex chassis = Geometry.createPolygonalEllipse(6, size * 0.7, size * 1.6);
            Convex head = Geometry.createCircle(size * 0.2);
            Polygon legL = Geometry.createRectangle(size * 0.5, size * 0.18);
            legL.translate(0, -size * 0.55);
            Convex legR = mirrorAcrossX(legL);
            return List.of(legL, legR, chassis, head);
        }
    },
    TRIDENT_TROOPER(
            "Trident Trooper",
            "Short range, scatter-fire laser trooper.",
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createPolygonalCircle(6, size * 0.8);
            Convex barrel = Geometry.createIsoscelesTriangle(size * 0.6, size * 0.4);
            barrel.rotate(Math.PI / 2);
            barrel.translate(size * 0.65, 0);
            return List.of(torso, barrel);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex torso = Geometry.createPolygonalCircle(6, size * 0.8);
            Convex lens = Geometry.createPolygonalEllipse(6, size * 0.32, size * 0.42);
            lens.translate(size * .5, 0);
            Convex crystal = Geometry.createSquare(size * 0.4);
            crystal.translate(-size * 0.4, 0);
            return List.of(torso, lens, crystal);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex hull = Geometry.createPolygonalCapsule(2, size * 1.7, size * 0.9);
            Convex left = Geometry.createPolygonalEllipse(4, size * 0.35, size * 0.6);
            left.translate(-size * 0.6, 0);
            Convex center = Geometry.createPolygonalEllipse(4, size * 0.3, size * 0.4);
            center.translate(0, 0);
            Convex right = Geometry.createPolygonalEllipse(4, size * 0.25, size * 0.3);
            right.translate(size * 0.6, 0);
            return List.of(hull, left, center, right);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex hull = Geometry.createPolygonalCapsule(2, size * 1.8, size * 1.3);
            Convex crystal = Geometry.createPolygonalCapsule(1, size * 0.45, size * 0.6);
            crystal.translate(-size * 0.25, 0);
            Convex focus = Geometry.createPolygonalCapsule(1, size * 0.35, size * 0.5);
            focus.translate(size * 0.33, 0);
            return List.of(hull, crystal, focus);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex hull = Geometry.createPolygonalCapsule(2, size * 2, size);
            Convex lens = Geometry.createCircle(size * 0.33);
            lens.translate(-size * 0.15, 0);
            Rectangle ampL = Geometry.createRectangle(size * 0.6, size * 0.11);
            ampL.translate(0, -size * 0.25);
            ampL.rotate(Math.PI / 6);
            Convex ampR = mirrorAcrossX(ampL);
            return List.of(hull, lens, ampL, ampR);
        }
    },
    PHOTON_TITAN(
            "Photon Titan",
            "Super-heavy mech with a high damage beam weapon.",
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Polygon lower = Geometry.createPolygonalHalfEllipse(4, size * 1.8, size * 1.1);
            lower.translate(0, size * 0.33);
            Convex upper = mirrorAcrossX(lower);
            Convex core = Geometry.createCircle(size * 0.33);
            core.translate(-size * 0.25, 0);
            Convex emitter = Geometry.createPolygonalEllipse(4, size * 0.6, size * 0.8);
            emitter.translate(size * 0.4, 0);
            return List.of(lower, upper, core, emitter);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex chassis = Geometry.createPolygonalCircle(6, size * 0.75);
            Convex armL = Geometry.createRectangle(size * 0.7, size * 0.2);
            armL.translate(0, -size * 0.45);
            Convex armR = Geometry.createRectangle(size * 0.7, size * 0.2);
            armR.translate(0, size * 0.45);
            return List.of(chassis, armL, armR);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex hub = Geometry.createSquare(size * 0.45);
            Polygon armA = Geometry.createPolygonalCapsule(4, size * 1.8, size * 0.3);
            armA.rotate(Math.PI / 4);
            Polygon armB = Geometry.createPolygonalCapsule(4, size * 1.8, size * 0.3);
            armB.rotate(-Math.PI / 4);
            return List.of(hub, armA, armB);
        }
    },
    HELICOPTER(
            "Attack Helicopter",
            "Versatile low-altitude gunship firing rockets at ground targets.",
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex fuselage = Geometry.createPolygonalCapsule(8, size * 1.4, size * 0.95);
            Convex tailBoom = Geometry.createRectangle(size, size * 0.22);
            tailBoom.translate(-size * 0.95, 0);
            Convex rotorHub = Geometry.createCircle(size * 0.28);
            return List.of(fuselage, tailBoom, rotorHub);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex fuselage = Geometry.createPolygonalCapsule(6, size * 1.65, size * 0.85);
            Convex podL = Geometry.createRectangle(size * 0.8, size * 0.25);
            podL.translate(size * 0.15, -size * 0.6);
            Convex podR = Geometry.createRectangle(size * 0.8, size * 0.25);
            podR.translate(size * 0.15, size * 0.6);
            Polygon tailFin = Geometry.createPolygon(
                    new Vector2(-size * 0.6, 0),
                    new Vector2(-size, size * 0.2),
                    new Vector2(-size, -size * 0.2)
            );
            return List.of(fuselage, podL, podR, tailFin);
        }
    },
    CHINOOK(
            "Chinook",
            "Heavy transport helicopter (low altitude). Carries infantry; passengers cannot fire while embarked.",
            450,
            38,
            130,
            118.0,
            0,
            0.0,
            0,
            60.0,
            0x556B2F,
            BuildingType.AIRFIELD,
            34,
            520.0,
            Elevation.LOW,
            UnitCategory.FLYER,
            Set.of(BuildingType.RESEARCH_LAB),
            6,
            'Q'
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            return List.of(Geometry.createPolygonalCapsule(4, size, size * 0.38));
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex fuselage = Geometry.createPolygonalCapsule(6, size * 1.8, size * 0.7);
            Polygon upperWing = Geometry.createPolygon(
                    new Vector2(-size * 0.1, size * 0.35),
                    new Vector2(-size * 0.55, size * 0.95),
                    new Vector2(-size * 0.75, size * 0.85),
                    new Vector2(-size * 0.35, size * 0.3)
            );
            Polygon lowerWing = mirrorAcrossX(upperWing);
            return List.of(fuselage, upperWing, lowerWing);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex fuselage = Geometry.createPolygonalCapsule(6, size * 1.95, size * 0.55);
            Polygon upperWing = Geometry.createPolygon(
                    new Vector2(size * 0.3, size * 0.25),
                    new Vector2(-size * 0.35, size * 0.95),
                    new Vector2(-size * 0.65, size * 0.85),
                    new Vector2(-size * 0.3, size * 0.2)
            );
            Polygon lowerWing = mirrorAcrossX(upperWing);
            return List.of(fuselage, upperWing, lowerWing);
        }
    },
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
    ) {
        @Override
        public List<Convex> createPhysicsFixtures() {
            Convex fuselage = Geometry.createPolygonalCapsule(6, size * 1.7, size * 0.75);
            Polygon upperWing = Geometry.createPolygon(
                    new Vector2(size * 0.35, size * 0.05),
                    new Vector2(-size * 0.1, size * 1.1),
                    new Vector2(-size * 0.4, size * 1.1),
                    new Vector2(-size * 0.4, size * 0.3)
            );
            Polygon lowerWing = mirrorAcrossX(upperWing);
            Polygon tailFin = Geometry.createPolygon(
                    new Vector2(-size * 0.85, 0),
                    new Vector2(-size * 1.05, size * 0.22),
                    new Vector2(-size * 1.05, -size * 0.22)
            );
            return List.of(fuselage, upperWing, lowerWing, tailFin);
        }
    };
    private final String displayName;
    private final String description;
    private final int resourceCost;
    private final int buildTimeSeconds;
    private final double maxHealth;
    private final double movementSpeed;
    private final double damage;
    private final double attackRate;
    private final double attackRange;
    // Package-private so enum-constant anonymous bodies (which are static nested subclasses,
    // not inner classes) can reference `size` directly from their createPhysicsFixtures()
    // overrides without going through Lombok's getSize() accessor.
    final double size;
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
     * Create physics fixtures for this unit type.
     * Each enum constant declares its own override above; the unit's {@link Body}
     * is built from this list in {@link Unit}.
     */
    public abstract List<Convex> createPhysicsFixtures();

    /**
     * Mirror a polygon across the y=0 line (so a feature on the +Y side gets a matching
     * partner on the -Y side). dyn4j's {@code flipAlongTheXAxis(p, point)} preserves CCW
     * winding and convexity, so the result is safe to add to a {@code Body}.
     */
    static Polygon mirrorAcrossX(Polygon p) {
        return Geometry.flipAlongTheXAxis(p, new Vector2(0, 0));
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
     * Armor classification for the damage matrix.
     * Infantry → UNARMORED/LIGHT, vehicles → MEDIUM/HEAVY, super-heavies → HEAVY
     */
    public ArmorType getArmorType() {
        return switch (this) {
            // Unarmored – soft infantry with no protective plating
            case WORKER, MEDIC, SPY -> ArmorType.UNARMORED;

            // Light – basic infantry and fast vehicles
            case INFANTRY, SHOTGUN_INFANTRY, ROCKET_SOLDIER, SNIPER, ENGINEER,
                 GRENADIER, MINIGUNNER, ANDROID, LASER_INFANTRY, TRIDENT_TROOPER,
                 ION_RANGER, PHOTON_SCOUT, RAIDER, SCOUT_DRONE -> ArmorType.LIGHT;

            // Medium – light vehicles and airframes
            case JEEP, APC, CHINOOK, HELICOPTER, LASER_GUNSHIP, BOMBER, GUNSHIP,
                 FLAK_TANK, SAM_LAUNCHER, INTERCEPTOR, SPIDER_MINE -> ArmorType.MEDIUM;

            // Heavy – main battle tanks and heavy walkers
            case TANK, CLOAK_TANK, SHIELD_TANK, BEAM_TANK, PULSE_ARTILLERY,
                 ARTILLERY -> ArmorType.HEAVY;

            // Fortified – super-heavies
            case GIGANTONAUT, COLOSSUS, PHOTON_TITAN -> ArmorType.FORTIFIED;
        };
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
        return this != WORKER && this != MEDIC && this != ENGINEER && this != SPIDER_MINE && this != APC
                && this != CHINOOK;
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


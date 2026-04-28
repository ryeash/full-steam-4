package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.component.NukeSiloComponent;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Player-triggered strategic abilities ({@code CommandAbility} in UI copy).
 * Each type is unlocked by a specific building and uses a per-player cooldown.
 * Runtime execution is implemented per constant via {@link CommandAbilityEffect} (see {@link #execute(CommandAbilityExecutionContext)}).
 */
@Getter
public enum CommandAbilityType implements CommandAbilityEffect {

    /**
     * Large ground-only explosion at a target point. Unlocked by {@link BuildingType#STRIKE_RELAY}.
     */
    STRIKE_PACKAGE(
            "Strike Package",
            "Call in a concentrated bombardment at a ground location.",
            BuildingType.STRIKE_RELAY,
            90_000L,
            220.0,
            420.0
    ) {
        @Override
        public CommandAbilityOutcome execute(CommandAbilityExecutionContext ctx) {
            Vector2 t = ctx.requireGroundTargetOrWarn();
            if (t == null) {
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.isWithinWorldBounds(t)) {
                ctx.warnTargetOutsideBattlefield();
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.validateSourceRelayIfPresent(STRIKE_PACKAGE)) {
                return CommandAbilityOutcome.FAILED;
            }
            ctx.placeFieldEffect(STRIKE_PACKAGE, FieldEffectType.EXPLOSION, t);
            return CommandAbilityOutcome.NEED_DEFAULT_WRAP_UP;
        }
    },

    /**
     * Anti-air burst at a point—devastating to aircraft; does not harm ground targets.
     * Unlocked by {@link BuildingType#STRIKE_RELAY} (same uplink as {@link #STRIKE_PACKAGE}).
     */
    FLAK_BURST(
            "AA Barrage",
            "Concentrated flak at a sky location—shreds low- and high-altitude aircraft.",
            BuildingType.STRIKE_RELAY,
            70_000L,
            240.0,
            340.0
    ) {
        @Override
        public CommandAbilityOutcome execute(CommandAbilityExecutionContext ctx) {
            Vector2 t = ctx.requireGroundTargetOrWarn();
            if (t == null) {
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.isWithinWorldBounds(t)) {
                ctx.warnTargetOutsideBattlefield();
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.validateSourceRelayIfPresent(FLAK_BURST)) {
                return CommandAbilityOutcome.FAILED;
            }
            ctx.placeFieldEffect(FLAK_BURST, FieldEffectType.FLAK_EXPLOSION, t);
            return CommandAbilityOutcome.NEED_DEFAULT_WRAP_UP;
        }
    },

    /**
     * Begin timed arming at the player's {@link BuildingType#NUKE_SILO}. No map target.
     */
    NUKE_ARM(
            "Arm Warhead",
            "Begin fueling and targeting sequence at your silo (takes several minutes).",
            BuildingType.NUKE_SILO,
            0L,
            0.0,
            0.0
    ) {
        @Override
        public CommandAbilityOutcome execute(CommandAbilityExecutionContext ctx) {
            var nuke = ctx.findNukeSilo();
            if (nuke.isEmpty()) {
                ctx.warn("No completed nuclear silo available.");
                return CommandAbilityOutcome.FAILED;
            }
            if (!nuke.get().tryStartArming()) {
                ctx.warn("Warhead is already arming or ready to launch.");
                return CommandAbilityOutcome.FAILED;
            }
            ctx.info("Nuclear warhead arming… (" + (NukeSiloComponent.ARM_DURATION_MS / 60_000L) + " min)");
            return CommandAbilityOutcome.FULLY_HANDLED;
        }
    },

    /**
     * Launch after silo reports {@link com.fullsteam.model.component.NukeSiloComponent.NukeArmState#ARMED}.
     */
    NUKE_LAUNCH(
            "Launch Nuke",
            "Detonate at a ground location—catastrophic area damage.",
            BuildingType.NUKE_SILO,
            240_000L,
            400.0,
            2500.0
    ) {
        @Override
        public CommandAbilityOutcome execute(CommandAbilityExecutionContext ctx) {
            Vector2 t = ctx.requireGroundTargetOrWarn();
            if (t == null) {
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.isWithinWorldBounds(t)) {
                ctx.warnTargetOutsideBattlefield();
                return CommandAbilityOutcome.FAILED;
            }
            var nuke = ctx.findNukeSilo();
            if (nuke.isEmpty() || !nuke.get().isLaunchReady()) {
                ctx.warn("Warhead is not ready—arm the silo first and wait for the sequence to complete.");
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.validateNukeSiloSourceIfPresent()) {
                return CommandAbilityOutcome.FAILED;
            }
            ctx.placeFieldEffect(NUKE_LAUNCH, FieldEffectType.EXPLOSION, t);
            nuke.get().consumeLaunchReadiness();
            ctx.info(NUKE_LAUNCH.getDisplayName() + " — detonation!");
            return CommandAbilityOutcome.NEED_COOLDOWN_ONLY;
        }
    },

    /**
     * Orbital infantry insert at a ground point. Unlocked by {@link BuildingType#JUMP_PAD}.
     */
    MARINE_DROP(
            "Marine Drop",
            "Call in a squad insert at a ground location (uses your light infantry roster).",
            BuildingType.JUMP_PAD,
            100_000L,
            90.0,
            0.0
    ) {
        @Override
        public CommandAbilityOutcome execute(CommandAbilityExecutionContext ctx) {
            Vector2 t = ctx.requireGroundTargetOrWarn();
            if (t == null) {
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.isWithinWorldBounds(t)) {
                ctx.warnTargetOutsideBattlefield();
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.validateSourceRelayIfPresent(MARINE_DROP)) {
                return CommandAbilityOutcome.FAILED;
            }
            if (!marineDropSquad(ctx, t)) {
                return CommandAbilityOutcome.FAILED;
            }
            return CommandAbilityOutcome.NEED_DEFAULT_WRAP_UP;
        }
    },

    /**
     * Temporary wide-area vision at a ground point. Unlocked by {@link BuildingType#SATCOM_ARRAY}.
     */
    SATELLITE_SWEEP(
            "Satellite Sweep",
            "Brief orbital recon—reveals a large area for your team for a short time.",
            BuildingType.SATCOM_ARRAY,
            88_000L,
            480.0,
            0.0
    ) {
        @Override
        public CommandAbilityOutcome execute(CommandAbilityExecutionContext ctx) {
            Vector2 t = ctx.requireGroundTargetOrWarn();
            if (t == null) {
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.isWithinWorldBounds(t)) {
                ctx.warnTargetOutsideBattlefield();
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.validateSourceRelayIfPresent(SATELLITE_SWEEP)) {
                return CommandAbilityOutcome.FAILED;
            }
            executeSatelliteSweep(ctx, t);
            return CommandAbilityOutcome.NEED_DEFAULT_WRAP_UP;
        }
    },

    /**
     * Multiple explosions along an east–west line through the target. Unlocked by {@link BuildingType#CARPET_PAD}.
     */
    CARPET_BOMB(
            "Carpet Bomb",
            "Sequential air strikes along an east–west corridor through the target point.",
            BuildingType.CARPET_PAD,
            92_000L,
            300.0,
            0.0
    ) {
        @Override
        public CommandAbilityOutcome execute(CommandAbilityExecutionContext ctx) {
            Vector2 t = ctx.requireGroundTargetOrWarn();
            if (t == null) {
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.isWithinWorldBounds(t)) {
                ctx.warnTargetOutsideBattlefield();
                return CommandAbilityOutcome.FAILED;
            }
            if (!ctx.validateSourceRelayIfPresent(CARPET_BOMB)) {
                return CommandAbilityOutcome.FAILED;
            }
            executeCarpetBomb(ctx, t);
            return CommandAbilityOutcome.NEED_DEFAULT_WRAP_UP;
        }
    };

    private static final Logger log = LoggerFactory.getLogger(CommandAbilityType.class);

    private static final long SATELLITE_REVEAL_DURATION_MS = 28_000L;
    private static final int CARPET_BOMB_STRIKE_COUNT = 9;
    private static final double CARPET_BOMB_STRIKE_RADIUS = 58.0;
    private static final double CARPET_BOMB_STRIKE_DAMAGE = 240.0;

    private static final List<UnitType> MARINE_DROP_UNIT_PRIORITY = List.of(
            UnitType.INFANTRY,
            UnitType.MINIGUNNER,
            UnitType.ROCKET_SOLDIER,
            UnitType.SHOTGUN_INFANTRY,
            UnitType.GRENADIER,
            UnitType.LASER_INFANTRY
    );

    private static final int MARINE_DROP_SQUAD_SIZE = 4;

    private final String displayName;
    private final String description;
    private final BuildingType unlockingBuilding;
    private final long cooldownMs;
    private final double effectRadius;
    private final double effectDamage;

    CommandAbilityType(String displayName, String description, BuildingType unlockingBuilding,
                       long cooldownMs, double effectRadius, double effectDamage) {
        this.displayName = displayName;
        this.description = description;
        this.unlockingBuilding = unlockingBuilding;
        this.cooldownMs = cooldownMs;
        this.effectRadius = effectRadius;
        this.effectDamage = effectDamage;
    }

    /**
     * If false, the client sends the order immediately with no map click (e.g. {@link #NUKE_ARM}).
     */
    public boolean requiresGroundTarget() {
        return this != NUKE_ARM;
    }

    private static void executeSatelliteSweep(CommandAbilityExecutionContext ctx, Vector2 target) {
        Player player = ctx.getGameEntities().getPlayerFactions().get(ctx.getPlayerId());
        long until = System.currentTimeMillis() + SATELLITE_REVEAL_DURATION_MS;
        player.setSatelliteReveal(new SatelliteReveal(
                ctx.getFaction().getTeamNumber(),
                new Vector2(target.x, target.y),
                SATELLITE_SWEEP.getEffectRadius(),
                until));
        log.info("Team {} satellite sweep at ({}, {}) r={}",
                ctx.getFaction().getTeamNumber(), target.x, target.y, SATELLITE_SWEEP.getEffectRadius());
    }

    private static void executeCarpetBomb(CommandAbilityExecutionContext ctx, Vector2 target) {
        double halfLen = CARPET_BOMB.getEffectRadius();
        int n = CARPET_BOMB_STRIKE_COUNT;
        double halfW = ctx.game().getGameConfig().getWorldWidth() / 2.0;
        double halfH = ctx.game().getGameConfig().getWorldHeight() / 2.0;
        double duration = FieldEffectType.EXPLOSION.getDefaultDuration();
        for (int i = 0; i < n; i++) {
            double frac = n <= 1 ? 0.5 : i / (double) (n - 1);
            double x = target.x + (frac - 0.5) * 2.0 * halfLen;
            double y = target.y;
            x = Math.max(-halfW, Math.min(halfW, x));
            y = Math.max(-halfH, Math.min(halfH, y));
            FieldEffect fe = new FieldEffect(
                    ctx.getPlayerId(),
                    FieldEffectType.EXPLOSION,
                    new Vector2(x, y),
                    CARPET_BOMB_STRIKE_RADIUS,
                    CARPET_BOMB_STRIKE_DAMAGE,
                    duration,
                    ctx.getFaction().getTeamNumber());
            ctx.game().getGameEntities().add(fe);
        }
        log.info("Player {} carpet bomb through ({}, {}) halfLen={}", ctx.getPlayerId(), target.x, target.y, halfLen);
    }

    /**
     * @return false if no eligible infantry type exists in the faction (caller must not apply cooldown)
     */
    private static boolean marineDropSquad(CommandAbilityExecutionContext ctx, Vector2 target) {
        Player faction = ctx.getFaction();
        int playerId = ctx.getPlayerId();
        UnitType dropType = MARINE_DROP_UNIT_PRIORITY.stream()
                .filter(t -> faction.getFactionDefinition().getUnitTypes().contains(t))
                .findFirst()
                .orElse(null);
        if (dropType == null) {
            ctx.warn("Marine Drop requires at least one light infantry type in your faction roster.");
            return false;
        }
        RTSGameManager game = ctx.game();
        double halfW = game.getGameConfig().getWorldWidth() / 2.0;
        double halfH = game.getGameConfig().getWorldHeight() / 2.0;
        double spread = 38.0;
        for (int i = 0; i < MARINE_DROP_SQUAD_SIZE; i++) {
            double angle = (Math.PI * 2 * i) / MARINE_DROP_SQUAD_SIZE;
            double x = target.x + Math.cos(angle) * spread;
            double y = target.y + Math.sin(angle) * spread;
            x = Math.max(-halfW + 20, Math.min(halfW - 20, x));
            y = Math.max(-halfH + 20, Math.min(halfH - 20, y));
            Unit unit = new Unit(
                    IdGenerator.nextEntityId(),
                    dropType,
                    x, y,
                    playerId,
                    faction.getTeamNumber(),
                    faction
            );
            unit.initializeComponents(game.getGameEntities());
            game.getGameEntities().add(unit);
            faction.getFactionDefinition().onUnitCreated(unit, faction, game);
        }
        log.info("Player {} marine drop {} x{} at ({}, {})", playerId, dropType, MARINE_DROP_SQUAD_SIZE, target.x, target.y);
        return true;
    }

    /**
     * Static metadata for game initialization (client HUD).
     */
    public static Map<String, Map<String, Object>> catalogForInitialization() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (CommandAbilityType t : values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("displayName", t.displayName);
            row.put("description", t.description);
            row.put("unlockingBuilding", t.unlockingBuilding.name());
            row.put("cooldownMs", t.cooldownMs);
            row.put("effectRadius", t.effectRadius);
            row.put("effectDamage", t.effectDamage);
            row.put("requiresGroundTarget", t.requiresGroundTarget());
            out.put(t.name(), row);
        }
        return out;
    }
}

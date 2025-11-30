package com.fullsteam.model.component;

import com.fullsteam.model.GameEvent;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.ResourceType;
import lombok.Getter;

/**
 * Component that handles banking and interest generation for buildings.
 * Banks accumulate interest over time and pay it out at regular intervals.
 * Interest generation is paused when the building has low power or is under construction.
 * <p>
 * Used by: BANK
 */
@Getter
public class BankComponent extends AbstractBuildingComponent {
    private static final double DEFAULT_INTEREST_INTERVAL = 30.0; // Pay interest every 30 seconds
    private static final double DEFAULT_INTEREST_RATE = 0.02; // 2% interest per interval

    private long lastInterestPayout = Long.MAX_VALUE;
    private final double interestInterval;
    private final double interestRate;

    /**
     * Create a bank component with default interest settings.
     */
    public BankComponent() {
        this(DEFAULT_INTEREST_INTERVAL, DEFAULT_INTEREST_RATE);
    }

    /**
     * Create a bank component with custom interest settings.
     *
     * @param interestInterval Time in seconds between interest payments
     * @param interestRate     Interest rate as a decimal (0.02 = 2%)
     */
    public BankComponent(double interestInterval, double interestRate) {
        this.interestInterval = interestInterval;
        this.interestRate = interestRate;
    }

    @Override
    public void update(boolean hasLowPower) {
        // Don't accumulate interest if under construction or low power
        if (building.isUnderConstruction() || hasLowPower) {
            return;
        }

        if (System.currentTimeMillis() >= lastInterestPayout + (long) (interestInterval * 1000D)) {
            lastInterestPayout = System.currentTimeMillis();
            PlayerFaction faction = gameEntities.getPlayerFactions().get(building.getOwnerId());
            int currentCredits = faction.getResourceAmount(ResourceType.CREDITS);
            int interest = (int) Math.round(currentCredits * interestRate);
            if (interest > 0) {
                faction.addResources(ResourceType.CREDITS, interest);
                // Notify player of interest payment (only if significant - 50+ credits)
                if (interest >= 50) {
                    gameEntities.getGameEventSender()
                            .accept(GameEvent.createPlayerEvent(
                                    "💰 Bank paid +" + interest + " credits interest",
                                    faction.getPlayerId(),
                                    GameEvent.EventCategory.INFO
                            ));
                }
            }
        }
    }

    @Override
    public void onConstructionComplete() {
        lastInterestPayout = System.currentTimeMillis();
    }
}





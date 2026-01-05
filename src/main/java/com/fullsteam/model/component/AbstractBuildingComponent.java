package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;

public abstract class AbstractBuildingComponent implements IBuildingComponent {
    protected GameEntities gameEntities;
    protected Building building;

    @Override
    public void init(GameEntities gameEntities, Building building) {
        this.gameEntities = gameEntities;
        this.building = building;
    }
}

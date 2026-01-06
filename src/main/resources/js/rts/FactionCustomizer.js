/**
 * Faction Customizer - UI for custom faction creation
 * Allows players to select units, buildings, and perks within a point budget
 */
class FactionCustomizer {
    constructor() {
        this.currentConfig = null;
        this.maxPoints = 100;
        this.spentPoints = 0;
        
        // Selections
        this.selectedUnits = new Set();
        this.selectedBuildings = new Set();
        this.selectedPerks = new Set();
        
        // Data loaded from API
        this.unitTemplates = [];
        this.buildingTemplates = [];
        this.perks = [];
        this.perkDependencies = {};
        this.presets = [];
        
        // UI state
        this.currentTab = 'units';
        this.isLoading = false;
    }
    
    /**
     * Initialize and load data from API
     */
    async initialize() {
        this.isLoading = true;
        
        try {
            // Load all customization data in parallel
            const [units, buildings, perks, perkDeps, presets] = await Promise.all([
                fetch('/api/rts/customization/templates/units').then(r => r.json()),
                fetch('/api/rts/customization/templates/buildings').then(r => r.json()),
                fetch('/api/rts/customization/perks').then(r => r.json()),
                fetch('/api/rts/customization/perks/dependencies').then(r => r.json()),
                fetch('/api/rts/customization/presets').then(r => r.json())
            ]);
            
            this.unitTemplates = units;
            this.buildingTemplates = buildings;
            this.perks = perks;
            this.perkDependencies = perkDeps;
            this.presets = presets;
            
            console.log('Faction customizer initialized:', {
                units: units.length,
                buildings: buildings.length,
                perks: perks.length,
                presets: presets.length
            });
            
            // Load from localStorage if available
            if (!this.loadFromLocalStorage()) {
                this.initializeEmpty();
            }
            
        } catch (error) {
            console.error('Failed to load customization data:', error);
            alert('Failed to load faction customization data. Please refresh.');
        } finally {
            this.isLoading = false;
        }
    }
    
    /**
     * Open the customizer modal
     */
    async openCustomizer(basePreset = null) {
        if (!this.unitTemplates.length) {
            await this.initialize();
        }
        
        if (basePreset) {
            await this.loadPreset(basePreset);
        } else {
            this.initializeEmpty();
        }
        
        this.showCustomizerUI();
    }
    
    /**
     * Load a preset configuration
     */
    async loadPreset(presetId) {
        try {
            const response = await fetch(`/api/rts/customization/presets/${presetId}`);
            const preset = await response.json();
            
            this.selectedUnits = new Set(preset.selectedUnits);
            this.selectedBuildings = new Set(preset.selectedBuildings);
            this.selectedPerks = new Set(preset.selectedPerks);
            this.spentPoints = preset.totalPointsSpent;
            
            this.currentConfig = preset;
            
            // Save to localStorage
            this.saveToLocalStorage();
            
        } catch (error) {
            console.error('Failed to load preset:', error);
            alert('Failed to load preset faction.');
        }
    }
    
    /**
     * Initialize with required minimum entities
     */
    initializeEmpty() {
        this.selectedUnits = new Set(['WORKER']);
        this.selectedBuildings = new Set(['HEADQUARTERS', 'POWER_PLANT']);
        this.selectedPerks = new Set();
        this.spentPoints = 0; // WORKER, HQ, and POWER_PLANT are free
        this.currentConfig = null;
    }
    
    /**
     * Save current configuration to localStorage
     */
    saveToLocalStorage() {
        const config = {
            displayName: this.currentConfig?.displayName || 'My Custom Faction',
            selectedUnits: Array.from(this.selectedUnits),
            selectedBuildings: Array.from(this.selectedBuildings),
            selectedPerks: Array.from(this.selectedPerks),
            spentPoints: this.spentPoints,
            basedOnPreset: this.currentConfig?.basedOnPreset || null,
            timestamp: Date.now()
        };
        
        localStorage.setItem('rts_custom_faction', JSON.stringify(config));
        console.log('Saved faction config to localStorage:', config);
    }
    
    /**
     * Load configuration from localStorage
     */
    loadFromLocalStorage() {
        try {
            const saved = localStorage.getItem('rts_custom_faction');
            if (!saved) {
                return false;
            }
            
            const config = JSON.parse(saved);
            
            // Restore selections
            this.selectedUnits = new Set(config.selectedUnits || ['WORKER']);
            this.selectedBuildings = new Set(config.selectedBuildings || ['HEADQUARTERS']);
            this.selectedPerks = new Set(config.selectedPerks || []);
            this.spentPoints = config.spentPoints || 0;
            
            // Restore config metadata
            this.currentConfig = {
                displayName: config.displayName,
                basedOnPreset: config.basedOnPreset,
                timestamp: config.timestamp
            };
            
            console.log('Loaded faction config from localStorage:', config);
            return true;
            
        } catch (error) {
            console.error('Failed to load from localStorage:', error);
            return false;
        }
    }
    
    /**
     * Clear localStorage
     */
    clearLocalStorage() {
        localStorage.removeItem('rts_custom_faction');
        console.log('Cleared faction config from localStorage');
    }
    
    /**
     * Get current configuration as DTO for API submission
     */
    getCurrentConfig() {
        return {
            displayName: this.currentConfig?.displayName || 'Custom Faction',
            themeColor: '#9370DB', // Default purple
            icon: '⚙️',
            selectedUnits: Array.from(this.selectedUnits),
            selectedBuildings: Array.from(this.selectedBuildings),
            selectedPerks: Array.from(this.selectedPerks),
            basedOnPreset: this.currentConfig?.basedOnPreset || null
        };
    }
    
    /**
     * Show the customizer modal
     */
    showCustomizerUI() {
        const html = `
            <div class="faction-customizer-modal" id="factionCustomizerModal">
                <div class="customizer-overlay" onclick="factionCustomizer.close()"></div>
                <div class="customizer-container">
                    <div class="customizer-header">
                        <h2>🎨 Customize Your Faction</h2>
                        <button type="button" class="close-btn" onclick="factionCustomizer.close()">✕</button>
                    </div>
                    
                    ${this.renderPointsBar()}
                    ${this.renderPresetSelector()}
                    ${this.renderTabs()}
                    
                    <div class="customizer-content" id="customizer-content">
                        ${this.renderCurrentTab()}
                    </div>
                    
                    ${this.renderFooter()}
                </div>
            </div>
        `;
        
        // Remove existing modal if present
        const existing = document.getElementById('factionCustomizerModal');
        if (existing) {
            existing.remove();
        }
        
        // Add to body
        document.body.insertAdjacentHTML('beforeend', html);
    }
    
    /**
     * Render points progress bar
     */
    renderPointsBar() {
        const remaining = this.maxPoints - this.spentPoints;
        const percent = (this.spentPoints / this.maxPoints) * 100;
        const statusClass = remaining < 0 ? 'over-budget' : remaining < 10 ? 'low' : '';
        
        return `
            <div class="points-bar ${statusClass}">
                <div class="points-display">
                    <span class="points-spent">${this.spentPoints}</span> / 
                    <span class="points-max">${this.maxPoints}</span> Points
                    <span class="points-remaining">(${remaining} remaining)</span>
                </div>
                <div class="points-progress">
                    <div class="points-fill" style="width: ${Math.min(percent, 100)}%"></div>
                </div>
            </div>
        `;
    }
    
    /**
     * Render preset selector dropdown
     */
    renderPresetSelector() {
        return `
            <div class="preset-selector">
                <label>Load Preset:</label>
                <select onchange="factionCustomizer.onPresetSelected(this.value)">
                    <option value="">-- Custom Faction --</option>
                    ${this.presets.map(preset => `
                        <option value="${preset.factionId}">${preset.icon} ${preset.displayName}</option>
                    `).join('')}
                </select>
            </div>
        `;
    }
    
    /**
     * Render tab buttons
     */
    renderTabs() {
        const tabs = [
            { id: 'units', label: 'Units', count: this.selectedUnits.size },
            { id: 'buildings', label: 'Buildings', count: this.selectedBuildings.size },
            { id: 'perks', label: 'Perks', count: this.selectedPerks.size },
            { id: 'summary', label: 'Summary', count: null }
        ];
        
        return `
            <div class="customizer-tabs">
                ${tabs.map(tab => `
                    <button 
                        type="button"
                        class="tab-btn ${this.currentTab === tab.id ? 'active' : ''}"
                        onclick="factionCustomizer.showTab('${tab.id}')">
                        ${tab.label}
                        ${tab.count !== null ? `<span class="count">(${tab.count})</span>` : ''}
                    </button>
                `).join('')}
            </div>
        `;
    }
    
    /**
     * Render current tab content
     */
    renderCurrentTab() {
        switch (this.currentTab) {
            case 'units':
                return this.renderUnitsTab();
            case 'buildings':
                return this.renderBuildingsTab();
            case 'perks':
                return this.renderPerksTab();
            case 'summary':
                return this.renderSummaryTab();
            default:
                return '<p>Unknown tab</p>';
        }
    }
    
    /**
     * Render units tab
     */
    renderUnitsTab() {
        // Group by category
        const grouped = {};
        for (const unit of this.unitTemplates) {
            if (!grouped[unit.category]) {
                grouped[unit.category] = [];
            }
            grouped[unit.category].push(unit);
        }
        
        return `
            <div class="tab-content units-tab">
                ${Object.entries(grouped).map(([category, units]) => `
                    <div class="entity-category">
                        <h3>${category}</h3>
                        <div class="entity-grid">
                            ${units.map(unit => this.renderUnitCard(unit)).join('')}
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }
    
    /**
     * Render a single unit card
     */
    renderUnitCard(unit) {
        const isSelected = this.selectedUnits.has(unit.unitType);
        const isRequired = unit.unitType === 'WORKER';
        const isBundled = unit.unitType === 'ANDROID';
        const hasFactory = this.selectedBuildings.has('ANDROID_FACTORY');
        const canAfford = this.canAfford(unit.pointCost);
        
        // ANDROID is auto-included with ANDROID_FACTORY
        if (isBundled) {
            return `
                <div class="entity-card ${hasFactory ? 'selected' : ''} ${!hasFactory ? 'disabled' : ''}">
                    <div class="card-header">
                        <span class="card-name">${unit.displayName}</span>
                        <span class="card-cost">Bundled</span>
                    </div>
                    <div class="card-stats">
                        HP: ${unit.maxHealth} | DMG: ${unit.damage} | RNG: ${unit.range}
                    </div>
                    <div class="card-description">Auto-included with Android Factory</div>
                    <div class="card-tags">
                        <span class="tag">BUNDLED</span>
                        <span class="tag">FREE</span>
                    </div>
                    <button 
                        type="button"
                        class="card-btn"
                        disabled>
                        ${hasFactory ? '✓ Included' : 'Requires Factory'}
                    </button>
                </div>
            `;
        }
        
        return `
            <div class="entity-card ${isSelected ? 'selected' : ''} ${!canAfford && !isSelected ? 'disabled' : ''}">
                <div class="card-header">
                    <span class="card-name">${unit.displayName}</span>
                    <span class="card-cost">${unit.pointCost} pts</span>
                </div>
                <div class="card-stats">
                    HP: ${unit.maxHealth} | DMG: ${unit.damage} | RNG: ${unit.range}
                </div>
                <div class="card-description">${unit.description}</div>
                <div class="card-tags">
                    ${unit.tags.slice(0, 3).map(tag => 
                        `<span class="tag">${tag}</span>`
                    ).join('')}
                </div>
                <button 
                    type="button"
                    class="card-btn"
                    onclick="factionCustomizer.toggleUnit('${unit.unitType}')"
                    ${isRequired || (!canAfford && !isSelected) ? 'disabled' : ''}>
                    ${isRequired ? '✓ Required' : isSelected ? 'Remove' : 'Add'}
                </button>
            </div>
        `;
    }
    
    /**
     * Render buildings tab
     */
    renderBuildingsTab() {
        // Group by category
        const grouped = {};
        for (const building of this.buildingTemplates) {
            if (!grouped[building.category]) {
                grouped[building.category] = [];
            }
            grouped[building.category].push(building);
        }
        
        return `
            <div class="tab-content buildings-tab">
                ${Object.entries(grouped).map(([category, buildings]) => `
                    <div class="entity-category">
                        <h3>${category}</h3>
                        <div class="entity-grid">
                            ${buildings.map(building => this.renderBuildingCard(building)).join('')}
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }
    
    /**
     * Render a single building card
     */
    renderBuildingCard(building) {
        const isSelected = this.selectedBuildings.has(building.buildingType);
        const isRequired = building.buildingType === 'HEADQUARTERS' || building.buildingType === 'POWER_PLANT';
        const canAfford = this.canAfford(building.pointCost);
        
        return `
            <div class="entity-card ${isSelected ? 'selected' : ''} ${!canAfford && !isSelected ? 'disabled' : ''}">
                <div class="card-header">
                    <span class="card-name">${building.displayName}</span>
                    <span class="card-cost">${building.pointCost} pts</span>
                </div>
                <div class="card-stats">
                    HP: ${building.maxHealth} | Power: ${building.powerValue > 0 ? '+' : ''}${building.powerValue}
                </div>
                <div class="card-description">${building.description}</div>
                <div class="card-tags">
                    ${building.tags.slice(0, 3).map(tag => 
                        `<span class="tag">${tag}</span>`
                    ).join('')}
                </div>
                <button 
                    type="button"
                    class="card-btn"
                    onclick="factionCustomizer.toggleBuilding('${building.buildingType}')"
                    ${isRequired || (!canAfford && !isSelected) ? 'disabled' : ''}>
                    ${isRequired ? '✓ Required' : isSelected ? 'Remove' : 'Add'}
                </button>
            </div>
        `;
    }
    
    /**
     * Render perks tab
     */
    renderPerksTab() {
        // Group by category
        const grouped = {};
        for (const perk of this.perks) {
            if (!grouped[perk.category]) {
                grouped[perk.category] = [];
            }
            grouped[perk.category].push(perk);
        }
        
        return `
            <div class="tab-content perks-tab">
                ${Object.entries(grouped).map(([category, perks]) => `
                    <div class="entity-category">
                        <h3>${category} Perks</h3>
                        <div class="entity-grid">
                            ${perks.map(perk => this.renderPerkCard(perk)).join('')}
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }
    
    /**
     * Render a single perk card
     */
    renderPerkCard(perk) {
        const isSelected = this.selectedPerks.has(perk.id);
        const canAfford = this.canAfford(perk.pointCost);
        const isAvailable = this.isPerkAvailable(perk);
        const missingDeps = this.getMissingDependencies(perk);
        const dependents = this.getDependentPerks(perk.id);
        
        let statusClass = '';
        let statusText = '';
        let canToggle = true;
        
        if (isSelected) {
            statusClass = 'selected';
            if (dependents.length > 0) {
                statusText = `<div class="warning">⚠️ ${dependents.length} perk(s) depend on this</div>`;
            }
        } else if (!isAvailable) {
            statusClass = 'locked';
            const depNames = missingDeps.map(id => {
                const dep = this.perks.find(p => p.id === id);
                return dep ? dep.displayName : id;
            }).join(', ');
            statusText = `<div class="locked-reason">🔒 Requires: ${depNames}</div>`;
            canToggle = false;
        } else if (!canAfford) {
            statusClass = 'disabled';
            statusText = `<div class="disabled-reason">💰 Not enough points</div>`;
            canToggle = false;
        }
        
        return `
            <div class="entity-card perk-card ${statusClass}">
                <div class="card-header">
                    <span class="card-name">${perk.displayName}</span>
                    <span class="card-cost">${perk.pointCost} pts</span>
                </div>
                <div class="card-description">${perk.description}</div>
                ${statusText}
                <button 
                    type="button"
                    class="card-btn"
                    onclick="factionCustomizer.togglePerk('${perk.id}')"
                    ${!canToggle ? 'disabled' : ''}>
                    ${isSelected ? 'Remove' : 'Add'}
                </button>
            </div>
        `;
    }
    
    /**
     * Render summary tab
     */
    renderSummaryTab() {
        const validation = this.validate();
        
        return `
            <div class="tab-content summary-tab">
                <div class="summary-section">
                    <h3>Your Faction</h3>
                    <div class="summary-stats">
                        <div class="stat">
                            <span class="stat-label">Points Used:</span>
                            <span class="stat-value ${this.spentPoints > this.maxPoints ? 'error' : ''}">
                                ${this.spentPoints} / ${this.maxPoints}
                            </span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">Units:</span>
                            <span class="stat-value">${this.selectedUnits.size}</span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">Buildings:</span>
                            <span class="stat-value">${this.selectedBuildings.size}</span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">Perks:</span>
                            <span class="stat-value">${this.selectedPerks.size}</span>
                        </div>
                    </div>
                </div>
                
                ${validation.errors.length > 0 ? `
                    <div class="summary-section validation-errors">
                        <h3>⚠️ Validation Errors</h3>
                        <ul>
                            ${validation.errors.map(error => `<li>${error}</li>`).join('')}
                        </ul>
                    </div>
                ` : `
                    <div class="summary-section validation-success">
                        <h3>✅ Configuration Valid</h3>
                        <p>Your faction is ready to play!</p>
                    </div>
                `}
                
                <div class="summary-section">
                    <h3>Selected Units (${this.selectedUnits.size})</h3>
                    <div class="summary-list">
                        ${Array.from(this.selectedUnits).map(unitType => {
                            const unit = this.unitTemplates.find(u => u.unitType === unitType);
                            return unit ? `<div class="summary-item">${unit.displayName} (${unit.pointCost} pts)</div>` : '';
                        }).join('')}
                    </div>
                </div>
                
                <div class="summary-section">
                    <h3>Selected Buildings (${this.selectedBuildings.size})</h3>
                    <div class="summary-list">
                        ${Array.from(this.selectedBuildings).map(buildingType => {
                            const building = this.buildingTemplates.find(b => b.buildingType === buildingType);
                            return building ? `<div class="summary-item">${building.displayName} (${building.pointCost} pts)</div>` : '';
                        }).join('')}
                    </div>
                </div>
                
                <div class="summary-section">
                    <h3>Selected Perks (${this.selectedPerks.size})</h3>
                    <div class="summary-list">
                        ${Array.from(this.selectedPerks).map(perkId => {
                            const perk = this.perks.find(p => p.id === perkId);
                            return perk ? `<div class="summary-item">${perk.displayName} (${perk.pointCost} pts)</div>` : '';
                        }).join('')}
                    </div>
                </div>
            </div>
        `;
    }
    
    /**
     * Render footer with action buttons
     */
    renderFooter() {
        const validation = this.validate();
        
        return `
            <div class="customizer-footer">
                <button 
                    type="button"
                    class="btn btn-primary"
                    onclick="factionCustomizer.saveAndApply()"
                    ${!validation.valid ? 'disabled' : ''}>
                    ✓ Done
                </button>
                <button type="button" class="btn btn-secondary" onclick="factionCustomizer.close()">
                    Cancel
                </button>
            </div>
        `;
    }
    
    // ===== EVENT HANDLERS =====
    
    showTab(tabId) {
        this.currentTab = tabId;
        this.updateUI();
    }
    
    async onPresetSelected(presetId) {
        if (!presetId) {
            this.initializeEmpty();
        } else {
            await this.loadPreset(presetId);
        }
        this.updateUI();
    }
    
    toggleUnit(unitType) {
        const unit = this.unitTemplates.find(u => u.unitType === unitType);
        if (!unit) return;
        
        if (unit.unitType === 'WORKER') return; // Required
        
        if (this.selectedUnits.has(unitType)) {
            this.selectedUnits.delete(unitType);
            this.spentPoints -= unit.pointCost;
        } else {
            if (this.canAfford(unit.pointCost)) {
                this.selectedUnits.add(unitType);
                this.spentPoints += unit.pointCost;
            }
        }
        
        this.saveToLocalStorage();
        this.updateUI();
    }
    
    toggleBuilding(buildingType) {
        const building = this.buildingTemplates.find(b => b.buildingType === buildingType);
        if (!building) return;
        
        // Required buildings
        if (building.buildingType === 'HEADQUARTERS' || building.buildingType === 'POWER_PLANT') return;
        
        if (this.selectedBuildings.has(buildingType)) {
            this.selectedBuildings.delete(buildingType);
            this.spentPoints -= building.pointCost;
            
            // Auto-remove bundled units
            if (buildingType === 'ANDROID_FACTORY') {
                this.selectedUnits.delete('ANDROID');
                // ANDROID is free (0 points), no point adjustment needed
            }
        } else {
            if (this.canAfford(building.pointCost)) {
                this.selectedBuildings.add(buildingType);
                this.spentPoints += building.pointCost;
                
                // Auto-add bundled units
                if (buildingType === 'ANDROID_FACTORY') {
                    this.selectedUnits.add('ANDROID');
                    // ANDROID is free (0 points), no point adjustment needed
                }
            }
        }
        
        this.saveToLocalStorage();
        this.updateUI();
    }
    
    togglePerk(perkId) {
        const perk = this.perks.find(p => p.id === perkId);
        if (!perk) return;
        
        if (this.selectedPerks.has(perkId)) {
            // Removing - check for dependents
            const dependents = this.getDependentPerks(perkId);
            if (dependents.length > 0) {
                const names = dependents.map(id => {
                    const p = this.perks.find(pk => pk.id === id);
                    return p ? p.displayName : id;
                }).join(', ');
                
                if (!confirm(`Removing this perk will also remove: ${names}. Continue?`)) {
                    return;
                }
                
                // Remove dependents
                for (const depId of dependents) {
                    const dep = this.perks.find(p => p.id === depId);
                    if (dep) {
                        this.selectedPerks.delete(depId);
                        this.spentPoints -= dep.pointCost;
                    }
                }
            }
            
            this.selectedPerks.delete(perkId);
            this.spentPoints -= perk.pointCost;
        } else {
            // Adding
            if (!this.isPerkAvailable(perk)) {
                alert('Cannot add this perk - missing dependencies');
                return;
            }
            if (!this.canAfford(perk.pointCost)) {
                alert('Not enough points!');
                return;
            }
            
            this.selectedPerks.add(perkId);
            this.spentPoints += perk.pointCost;
        }
        
        this.saveToLocalStorage();
        this.updateUI();
    }
    
    async saveAndApply() {
        const validation = this.validate();
        
        if (!validation.valid) {
            alert('Invalid configuration: ' + validation.errors.join(', '));
            return;
        }
        
        // Configuration is already saved to localStorage on every change
        // and will be automatically applied when starting/joining a game
        console.log('Faction customization complete:', {
            units: Array.from(this.selectedUnits),
            buildings: Array.from(this.selectedBuildings),
            perks: Array.from(this.selectedPerks),
            points: this.spentPoints
        });
        
        this.close();
    }
    
    close() {
        const modal = document.getElementById('factionCustomizerModal');
        if (modal) {
            modal.remove();
        }
    }
    
    // ===== UTILITY METHODS =====
    
    canAfford(cost) {
        return (this.spentPoints + cost) <= this.maxPoints;
    }
    
    isPerkAvailable(perk) {
        for (const depId of perk.dependsOn) {
            if (!this.selectedPerks.has(depId)) {
                return false;
            }
        }
        return true;
    }
    
    getMissingDependencies(perk) {
        return perk.dependsOn.filter(depId => !this.selectedPerks.has(depId));
    }
    
    getDependentPerks(perkId) {
        const dependents = [];
        for (const selectedId of this.selectedPerks) {
            const perk = this.perks.find(p => p.id === selectedId);
            if (perk && perk.dependsOn.includes(perkId)) {
                dependents.push(selectedId);
                // Recursively check dependents
                dependents.push(...this.getDependentPerks(selectedId));
            }
        }
        return [...new Set(dependents)]; // Remove duplicates
    }
    
    validate() {
        const errors = [];
        
        if (this.spentPoints > this.maxPoints) {
            errors.push(`Over budget: ${this.spentPoints} / ${this.maxPoints} points`);
        }
        
        if (!this.selectedUnits.has('WORKER')) {
            errors.push('WORKER is required');
        }
        
        if (!this.selectedBuildings.has('HEADQUARTERS')) {
            errors.push('HEADQUARTERS is required');
        }
        
        if (!this.selectedBuildings.has('POWER_PLANT')) {
            errors.push('POWER_PLANT is required');
        }
        
        // Check for at least one combat unit
        const hasCombat = Array.from(this.selectedUnits).some(unitType => {
            const unit = this.unitTemplates.find(u => u.unitType === unitType);
            return unit && unit.damage > 0 && unitType !== 'WORKER';
        });
        if (!hasCombat) {
            errors.push('At least one combat unit is required');
        }
        
        // Check for at least one production building
        const hasProduction = Array.from(this.selectedBuildings).some(buildingType => {
            const building = this.buildingTemplates.find(b => b.buildingType === buildingType);
            return building && building.producesUnitCategories.length > 0;
        });
        if (!hasProduction) {
            errors.push('At least one production building is required');
        }
        
        // Check perk dependencies
        for (const perkId of this.selectedPerks) {
            const perk = this.perks.find(p => p.id === perkId);
            if (perk) {
                const missing = this.getMissingDependencies(perk);
                if (missing.length > 0) {
                    const names = missing.map(id => {
                        const p = this.perks.find(pk => pk.id === id);
                        return p ? p.displayName : id;
                    }).join(', ');
                    errors.push(`${perk.displayName} requires: ${names}`);
                }
            }
        }
        
        return {
            valid: errors.length === 0,
            errors: errors
        };
    }
    
    updateUI() {
        // Re-render the entire UI
        this.showCustomizerUI();
    }
}

// Create global instance
const factionCustomizer = new FactionCustomizer();

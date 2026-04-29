/**
 * RTS Game Engine
 * Handles rendering, input, and game state for the RTS game
 */
class RTSEngine {
    constructor() {
        this.app = null;
        this.gameContainer = null;
        this.uiContainer = null;
        
        // Game entities
        this.units = new Map();
        this.buildings = new Map();
        this.obstacles = new Map();
        this.projectiles = new Map();
        this.beams = new Map();
        this.fieldEffects = new Map();
        
        // Static game data (loaded once from gameInitialization message)
        this.unitTypes = null; // Map of unit type name -> static properties
        this.buildingTypes = null; // Map of building type name -> static properties
        this.obstaclesStatic = null; // Map of obstacle id -> static properties
        this.initialized = false; // Flag to track if we've received initialization
        /** Milliseconds between army upkeep charges (from server). */
        this.armyUpkeepIntervalMs = 30000;

        /** Cached `availableBuildings` lookup by type; invalidated when faction menu data changes. */
        this._buildingInfoByType = null;
        /** When unchanged, {@link #updateBuildMenuAvailability} skips DOM work. */
        this._lastBuildMenuStateKey = null;
        /** Build preview: last mouse world position for movement detection. */
        this._buildPreviewMouse = { x: NaN, y: NaN };
        this._buildPreviewCacheType = null;
        this._buildPreviewValidity = true;
        this._buildPreviewFrame = 0;
        this._buildPreviewDrawn = { valid: null, size: null, type: null };
        /** Cached DOM refs for HUD (resource panel); filled in {@link #cacheHudDomRefs}. */
        this.dom = {};
        /** Cached unit info panel elements; refreshed if disconnected (e.g. after building UI). */
        this._unitInfoEls = null;
        
        // Player state
        this.gameId = null;
        this.myPlayerId = null;
        this.myFaction = null;
        this.myFactionData = null; // Faction data from API
        this.buildMenuGenerated = false; // Track if build menu has been generated
        this.myTeam = null;
        this.hasCenteredCamera = false;
        this.visionRange = 400; // Default, updated from server
        this.lastFogUpdate = 0; // Timestamp of last fog update
        this.fogUpdateInterval = 200; // Update fog every 200ms (5 times per second)
        
        // UI state
        // Biome
        this.biome = null;
        this.groundColor = 0x1a1a1a; // Default dark gray
        this.obstacleColor = 0x808080; // Default gray
        
        // Selection
        this.selectedUnits = new Set();
        this.selectedBuilding = null;
        this.selectionBox = null;
        this.isSelecting = false;
        this.selectionStart = null;
        /** Double-click unit select: same unit id within this window selects all that type (map-wide). */
        this._lastUnitSelectClickTime = 0;
        this._lastUnitSelectClickId = null;
        this._doubleClickSelectSameTypeMs = 400;

        // Camera
        this.camera = { x: 0, y: 0, zoom: 1.0 };
        this.worldBounds = { width: 4000, height: 4000 };
        
        // Input
        this.mouseWorldPos = { x: 0, y: 0 };
        this.keys = {};
        
        // Build mode
        this.buildMode = false;
        this.buildingType = null;
        this.buildPreview = null;
        
        // Attack-move mode
        this.attackMoveMode = false;
        
        // WebSocket
        this.websocket = null;
        
        // Special ability targeting mode
        this.specialAbilityTargetingMode = false;
        this.specialAbilityTargetType = null; // 'unit' or 'building'
        
        // Sortie / deploy targeting (airfield housed aircraft)
        this.sortieTargetingMode = false;
        this.sortieBuildingId = null;
        this.sortieHousedUnitId = null;
        this.sortieAircraftType = null;

        // Command abilities (strategic powers), e.g. Strike Package — catalog from gameInitialization
        this.commandAbilityTypes = null;
        this.commandAbilityTargetingMode = false;
        this.pendingCommandAbilityType = null;
        /** When {@link #updateCommandAbilitiesPanel} last rebuilt buttons (cooldown rounded to seconds). */
        this._commandAbilityPanelKey = null;
        
        // Right mouse: deferred order vs camera pan (drag)
        this.rightButtonDown = false;
        this.rightDragPanActive = false;
        this.rightDownScreen = null;
        this.lastRightPanScreen = null;
        this.pendingRightClickWorld = null;
        this.pendingRightForceAttack = false;
        
        this.init();
    }
    
    async init() {
        try {
            this.updateLoadingProgress(10, "Creating PixiJS application...");
            await this.initPixiApp();
            
            this.updateLoadingProgress(30, "Setting up camera and input...");
            this.setupCamera();
            this.setupInput();
            
            this.updateLoadingProgress(50, "Setting up UI...");
            this.setupUI();
            
            this.updateLoadingProgress(70, "Connecting to server...");
            await this.connectToServer();
            
            this.updateLoadingProgress(100, "Ready!");
            this.hideLoadingScreen();
            
        } catch (error) {
            console.error('RTS game initialization failed:', error);
            this.updateLoadingProgress(0, `Error: ${error.message}`);
        }
    }
    
    async initPixiApp() {
        this.app = new PIXI.Application();
        
        await this.app.init({
            width: window.innerWidth,
            height: window.innerHeight,
            backgroundColor: 0x000000, // Black background outside world bounds
            antialias: true,
            resolution: window.devicePixelRatio || 1,
            autoDensity: true
        });
        
        document.getElementById('pixi-container').appendChild(this.app.canvas);
        
        // Create containers
        this.gameContainer = new PIXI.Container();
        this.gameContainer.sortableChildren = true; // Enable z-index sorting
        
        /**
         * Z-Index Rendering Order (from back to front):
         * -2: World bounds (background)
         * -1: Obstacles and resource deposits
         *  0: Buildings
         *  1: Ground units
         * 1.5: Field effects (explosions, fire, etc.)
         *  2: Low altitude air units (Scout Drone)
         *  3: High altitude air units (Bomber, Interceptor)
         *  4: Projectiles (bullets, missiles)
         *  5: Beams (laser weapons)
         * 100: Fog of war overlay
         * 101: Selection box
         */
        
        this.fogContainer = new PIXI.Container(); // Fog of war overlay
        this.selectionBoxGraphics = new PIXI.Graphics(); // Selection box
        this.uiContainer = new PIXI.Container();
        
        // Create world bounds background (will be colored with biome color)
        this.worldBoundsGraphics = new PIXI.Graphics();
        
        // Flip Y-axis to match physics
        this.gameContainer.scale.y = -1;
        
        this.app.stage.addChild(this.gameContainer);
        this.app.stage.addChild(this.uiContainer);
        
        // Add world bounds first (bottom layer), then fog and selection box
        this.gameContainer.addChild(this.worldBoundsGraphics);
        this.worldBoundsGraphics.zIndex = -2; // Bottom layer - background
        
        this.gameContainer.addChild(this.fogContainer);
        this.fogContainer.zIndex = 100; // Top layer - fog of war
        
        this.gameContainer.addChild(this.selectionBoxGraphics);
        this.selectionBoxGraphics.zIndex = 101; // Top layer - selection box

        this.commandAbilityTargetGraphics = new PIXI.Graphics();
        this.gameContainer.addChild(this.commandAbilityTargetGraphics);
        this.commandAbilityTargetGraphics.zIndex = 102;
        
        // Handle resize
        window.addEventListener('resize', () => this.handleResize());
        
        // Start render loop
        this.app.ticker.add(() => this.update());
    }
    
    setupCamera() {
        this.camera.x = 0;
        this.camera.y = 0;
        this.camera.zoom = 0.5; // Start zoomed out
        this.updateCameraTransform();
    }
    
    /**
     * Draw the world bounds rectangle with the biome color
     * Everything outside this rectangle will be black
     */
    drawWorldBounds() {
        this.worldBoundsGraphics.clear();
        this.worldBoundsGraphics.rect(
            -this.worldBounds.width / 2,
            -this.worldBounds.height / 2,
            this.worldBounds.width,
            this.worldBounds.height
        );
        this.worldBoundsGraphics.fill(this.groundColor);
    }
    
    /**
     * Center camera on player's headquarters
     */
    centerCameraOnHQ(buildings) {
        if (!this.myPlayerId || !this.myTeam) return;
        
        // Find player's HQ
        for (const buildingData of buildings) {
            if (buildingData.ownerId === this.myPlayerId && 
                buildingData.type === 'HEADQUARTERS') {
                this.camera.x = buildingData.x;
                this.camera.y = buildingData.y;
                this.updateCameraTransform();
                this.hasCenteredCamera = true;
                break;
            }
        }
    }
    
    updateCameraTransform() {
        this.gameContainer.position.set(
            this.app.screen.width / 2,
            this.app.screen.height / 2
        );
        this.gameContainer.scale.set(this.camera.zoom, -this.camera.zoom);
        this.gameContainer.pivot.set(this.camera.x, this.camera.y);
    }
    
    setupInput() {
        // Mouse events
        this.app.canvas.addEventListener('mousedown', (e) => this.onMouseDown(e));
        this.app.canvas.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.app.canvas.addEventListener('mouseup', (e) => this.onMouseUp(e));
        this.app.canvas.addEventListener('contextmenu', (e) => e.preventDefault());
        this.app.canvas.addEventListener('wheel', (e) => this.onMouseWheel(e));
        
        // Keyboard events
        window.addEventListener('keydown', (e) => this.onKeyDown(e));
        window.addEventListener('keyup', (e) => this.onKeyUp(e));
        // Release right-drag even if pointer leaves canvas
        window.addEventListener('mouseup', (e) => this.onWindowMouseUp(e));
    }
    
    setupUI() {
        this.cacheHudDomRefs();
        this.installHotkeyLegend();
        // Setup build menu buttons (static HTML may have none; generateBuildMenu wires new buttons)
        document.querySelectorAll('.build-button').forEach(button => {
            button.addEventListener('click', () => {
                const buildingType = button.getAttribute('data-building');
                this.enterBuildMode(buildingType);
            });
        });
    }

    /**
     * Cache stable HUD elements. Call again if templates are re-injected.
     */
    cacheHudDomRefs() {
        this.dom = {
            playerInfo: document.getElementById('player-info'),
            creditsValue: document.getElementById('credits-value'),
            upkeepValue: document.getElementById('upkeep-value'),
            powerValue: document.getElementById('power-value'),
            lowPowerBanner: document.getElementById('low-power-banner'),
            resourcePanel: document.getElementById('resource-panel'),
            hudEconomy: document.getElementById('hud-economy'),
            hudHotkeys: document.getElementById('hud-hotkeys'),
            buildingInfoPanel: document.getElementById('building-info-panel'),
            commandAbilitiesPanel: document.getElementById('command-abilities-panel'),
            commandAbilitiesButtons: document.getElementById('command-abilities-buttons'),
        };
        this.refreshUnitInfoDomRefs();
    }

    /**
     * Clears client-only building selection and the building detail DOM.
     * Unit selection is unchanged; call {@link #clearUnitSelectionImmediate} separately when needed.
     */
    clearBuildingSelectionClient() {
        this.selectedBuilding = null;
        const bip = this.dom?.buildingInfoPanel || document.getElementById('building-info-panel');
        if (bip) {
            bip.innerHTML = '';
            bip.style.display = 'none';
        }
    }

    /** True if any currently selected local unit is a worker (build roster). */
    selectionIncludesWorker() {
        for (const id of this.selectedUnits) {
            const c = this.units.get(id);
            if (c?.unitData?.type === 'WORKER' && c.unitData.ownerId === this.myPlayerId) {
                return true;
            }
        }
        return false;
    }

    installHotkeyLegend() {
        const el = this.dom?.hudHotkeys;
        if (!el) {
            return;
        }
        el.innerHTML = [
            '<kbd>B</kbd> Build <span style="opacity:.78">(workers)</span>',
            '<kbd>Q</kbd> Attack-move',
            '<kbd>T</kbd> Special',
            '<kbd>U</kbd> Ungarrison',
            '<kbd>X</kbd> Scatter',
            '<kbd>Esc</kbd> Cancel',
        ].join(' \u00B7 ');
    }

    /** Re-query unit info panel nodes when present in the document (cleared by building production UI). */
    refreshUnitInfoDomRefs() {
        const panel = document.getElementById('unit-info-panel');
        const singleInfo = document.getElementById('single-unit-info');
        const multiInfo = document.getElementById('multi-unit-info');
        this._unitInfoEls = {
            panel,
            singleInfo,
            multiInfo,
            unitName: document.getElementById('unit-name'),
            unitHealth: document.getElementById('unit-health'),
            unitType: document.getElementById('unit-type'),
            unitHealthFill: document.getElementById('unit-health-fill'),
            abilityDiv: document.getElementById('unit-special-ability'),
            abilityName: document.getElementById('unit-ability-name'),
            unitCountList: document.getElementById('unit-count-list'),
        };
    }

    /**
     * Jackson often serializes {@code Map<Integer,?>} keys as strings; gameState.factions may use either.
     */
    getFactionStateForPlayer(factionsMap, playerId) {
        if (!factionsMap || playerId == null) {
            return null;
        }
        return factionsMap[playerId] ?? factionsMap[String(playerId)] ?? null;
    }

    invalidateBuildingInfoMap() {
        this._buildingInfoByType = null;
    }
    
    async connectToServer() {
        // Get game ID from URL or create new game
        const urlParams = new URLSearchParams(window.location.search);
        let gameId = urlParams.get('gameId');
        
        // Get faction config from URL if present (will be passed to WebSocket)
        const factionConfigParam = urlParams.get('factionConfig');
        let factionConfig = null;
        if (factionConfigParam) {
            try {
                // Decode from base64 (UTF-8 safe)
                const binaryString = atob(factionConfigParam);
                const bytes = Uint8Array.from(binaryString, char => char.charCodeAt(0));
                const configJson = new TextDecoder().decode(bytes);
                factionConfig = JSON.parse(configJson);
            } catch (e) {
                console.error('Failed to parse faction config from URL:', e);
            }
        }
        
        if (!gameId) {
            // Create new game
            const response = await fetch('/api/rts/games', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({}) // Send empty config for defaults
            });
            
            if (!response.ok) {
                throw new Error(`Failed to create game: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            gameId = data.gameId;
            
            // Update URL with game ID
            const newUrl = `${window.location.pathname}?gameId=${gameId}`;
            window.history.pushState({}, '', newUrl);
        }
        
        if (!gameId) {
            throw new Error('Failed to get game ID');
        }
        
        // Store game ID for later use
        this.gameId = gameId;
        
        // Get session token from URL or sessionStorage
        let sessionToken = urlParams.get('sessionToken');
        if (!sessionToken) {
            sessionToken = sessionStorage.getItem('rts_session_token');
        }
        
        // Connect via WebSocket with session token and faction config
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        let wsUrl = `${protocol}//${window.location.host}/rts/${gameId}`;
        
        const params = new URLSearchParams();
        if (sessionToken) {
            params.append('sessionToken', sessionToken);
            console.log('Connecting with session token:', sessionToken);
        }
        if (factionConfig) {
            const configJson = JSON.stringify(factionConfig);
            const utf8Bytes = new TextEncoder().encode(configJson);
            const binaryString = Array.from(utf8Bytes, byte => String.fromCharCode(byte)).join('');
            const configBase64 = btoa(binaryString);
            params.append('factionConfig', configBase64);
            console.log('Passing faction config via WebSocket (base64):', factionConfig.displayName);
        }
        
        if (params.toString()) {
            wsUrl += `?${params.toString()}`;
        }
        
        this.websocket = new WebSocket(wsUrl);
        this.websocket.binaryType = 'arraybuffer';
        
        this.websocket.onopen = () => {
        };
        
        this.websocket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
        
        this.websocket.onclose = (event) => {
        };
        
        this.websocket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleServerMessage(data);
        };
    }
    
    handleServerMessage(data) {
        switch (data.type) {
            case 'gameInitialization':
                this.handleGameInitialization(data);
                break;
            case 'gameState':
                this.updateGameState(data);
                break;
            case 'playerId':
                this.myPlayerId = data.playerId;
                break;
            case 'gameOver':
                this.handleGameOver(data);
                break;
            case 'gameEvent':
                this.handleGameEvent(data);
                break;
            case 'pong':
                // Handle ping response
                break;
            default:
                console.warn('Unknown message type:', data.type);
        }
    }
    
    handleGameInitialization(data) {
        // Store static type data
        this.unitTypes = data.unitTypes || {};
        this.buildingTypes = data.buildingTypes || {};
        this.commandAbilityTypes = data.commandAbilityTypes || null;
        
        // Store biome info
        if (data.biome) {
            this.biome = data.biome.name;
            this.groundColor = data.biome.groundColor;
            this.obstacleColor = data.biome.obstacleColor;
            this.drawWorldBounds();
        }
        
        // Store world dimensions
        if (data.worldWidth && data.worldHeight) {
            this.worldBounds.width = data.worldWidth;
            this.worldBounds.height = data.worldHeight;
            this.drawWorldBounds();
        }
        
        if (data.armyUpkeepIntervalMs) {
            this.armyUpkeepIntervalMs = data.armyUpkeepIntervalMs;
        }
        
        // Create obstacles from static data
        if (data.obstacles) {
            this.obstaclesStatic = new Map();
            data.obstacles.forEach(obstacleData => {
                this.obstaclesStatic.set(obstacleData.id, obstacleData);
                this.createObstacleFromStatic(obstacleData);
            });
        }
        
        // Store faction static data
        if (data.myFactionStatic) {
            const factionStatic = data.myFactionStatic;
            this.myTeam = factionStatic.team;
            
            // Build myFactionData from static info
            this.myFactionData = {
                factionType: 'CUSTOM',
                description: 'Player-designed faction',
                availableBuildings: factionStatic.buildingInfo || [],
                availableUnits: factionStatic.unitInfo || []
            };
            this.invalidateBuildingInfoMap();
            this._lastBuildMenuStateKey = null;
            this.refreshUnitInfoDomRefs();

            // Generate build menu now that we have faction data
            if (!this.buildMenuGenerated) {
                this.generateBuildMenu();
                this.buildMenuGenerated = true;
            }
        }
        
        this.initialized = true;
    }
    
    createObstacleFromStatic(obstacleData) {
        const container = new PIXI.Container();
        container.zIndex = -1; // Obstacles are behind most entities
        
        const graphics = new PIXI.Graphics();
        
        // Different colors for harvestable, destructible, and indestructible obstacles
        let fillColor, strokeColor;
        if (obstacleData.harvestable) {
            // Harvestable obstacles: greenish/gold color (contains resources)
            fillColor = 0x9ACD32; // Yellow-green (resource-rich)
            strokeColor = this.darkenColor(fillColor, 0.6); // Darken for outline
        } else if (obstacleData.destructible) {
            // Destructible obstacles: brownish/tan color (like rocks that can be broken)
            fillColor = 0x8B7355; // Medium brown
            strokeColor = this.darkenColor(fillColor, 0.6); // Darken for outline
        } else {
            // Indestructible obstacles: use biome color (darker, more solid looking)
            fillColor = this.obstacleColor;
            strokeColor = this.darkenColor(fillColor, 0.6); // Darken for outline
        }
        
        // Draw obstacle using vertices from physics body
        if (obstacleData.vertices && obstacleData.vertices.length > 0) {
            // Check if it's multi-fixture format (array of fixtures)
            if (Array.isArray(obstacleData.vertices[0]) && Array.isArray(obstacleData.vertices[0][0])) {
                // Multi-fixture: draw each fixture
                for (const fixtureVertices of obstacleData.vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(graphics, fixtureVertices, fillColor, 0);
                    }
                }
                graphics.stroke({ width: 2, color: strokeColor });
            } else if (Array.isArray(obstacleData.vertices[0]) && typeof obstacleData.vertices[0][0] === 'number') {
                // Single-fixture: [[x1, y1], [x2, y2], ...]
                this.drawPhysicsPolygon(graphics, obstacleData.vertices, fillColor, 0);
                graphics.stroke({ width: 2, color: strokeColor });
            } else {
                // Old format: [{x, y}, {x, y}, ...]
                graphics.moveTo(obstacleData.vertices[0].x, obstacleData.vertices[0].y);
                for (let i = 1; i < obstacleData.vertices.length; i++) {
                    graphics.lineTo(obstacleData.vertices[i].x, obstacleData.vertices[i].y);
                }
                graphics.closePath();
                graphics.fill(fillColor);
                graphics.stroke({ width: 2, color: strokeColor });
            }
        } else {
            // Fallback: draw a circle if no vertices provided (shouldn't happen)
            console.warn('Obstacle missing vertices, using fallback circle:', obstacleData.id);
            graphics.circle(0, 0, obstacleData.size || 20);
            graphics.fill(fillColor);
            graphics.stroke({ width: 2, color: strokeColor });
        }
        
        // Add health bar for destructible obstacles
        if (obstacleData.destructible) {
            const healthBar = new PIXI.Graphics();
            const barWidth = obstacleData.size * 1.5;
            const offset = obstacleData.size + 10;
            healthBar.rect(-barWidth / 2, -offset, barWidth, 5);
            healthBar.fill(0x00FF00); // Green for full health
            healthBar.visible = false; // Hide until damaged
            container.addChild(healthBar);
            container.healthBar = healthBar;
        }
        
        container.addChild(graphics);
        container.position.set(obstacleData.x, obstacleData.y);
        
        // Store graphics for later updates
        container.obstacleGraphics = graphics;
        container.obstacleData = obstacleData;
        
        this.gameContainer.addChild(container);
        this.obstacles.set(obstacleData.id, container);
    }
    
    handleGameEvent(event) {
        const eventFeed = document.getElementById('event-feed');
        if (!eventFeed) return;
        
        const eventElement = document.createElement('div');
        eventElement.className = 'game-event';
        
        // Add category class for styling
        if (event.category) {
            eventElement.classList.add(event.category.toLowerCase());
        }
        
        // Set custom color if provided
        if (event.color) {
            eventElement.style.borderLeftColor = event.color;
            eventElement.style.color = event.color;
        }
        
        // Set message
        eventElement.textContent = event.message;
        
        // Add to feed
        eventFeed.appendChild(eventElement);
        
        // Remove after animation completes (default 5 seconds)
        const duration = event.displayDuration || 5000;
        setTimeout(() => {
            eventElement.remove();
        }, duration);
        
        // Limit to 5 events max
        while (eventFeed.children.length > 5) {
            eventFeed.removeChild(eventFeed.firstChild);
        }
    }
    
    /**
     * Show a client-side game event (not from server)
     * @param {string} message - Event message to display
     * @param {string} category - Event category ('info', 'warning', 'system')
     */
    showGameEvent(message, category = 'info') {
        this.handleGameEvent({
            message: message,
            category: category
        });
    }
    
    handleGameOver(data) {
        const screen = document.getElementById('game-over-screen');
        const title = document.getElementById('game-over-title');
        const winner = document.getElementById('game-over-winner');
        const reason = document.getElementById('game-over-reason');
        
        // Check if player won
        const playerWon = this.myTeam === data.winningTeam;
        const isDraw = data.winningTeam === -1;
        
        // Get team color for winner display
        const getTeamColor = (teamNum) => {
            switch (teamNum) {
                case 1: return '#4CAF50'; // Green
                case 2: return '#F44336'; // Red
                case 3: return '#2196F3'; // Blue
                case 4: return '#FF9800'; // Orange
                default: return '#FFFFFF';
            }
        };
        
        if (isDraw) {
            title.textContent = 'DRAW';
            title.style.color = '#888888';
            winner.textContent = '';
            reason.textContent = data.reason || 'All headquarters destroyed';
        } else if (playerWon) {
            title.textContent = 'VICTORY';
            title.style.color = '#00FF00';
            winner.textContent = `Team ${data.winningTeam} Wins!`;
            winner.style.color = getTeamColor(data.winningTeam);
            reason.textContent = data.reason || 'You destroyed all enemy headquarters!';
        } else {
            title.textContent = 'DEFEAT';
            title.style.color = '#FF0000';
            winner.textContent = `Team ${data.winningTeam} Wins!`;
            winner.style.color = getTeamColor(data.winningTeam);
            reason.textContent = data.reason || 'Your headquarters was destroyed!';
        }
        
        // Set up Play Again button
        const playAgainBtn = document.getElementById('play-again-btn');
        if (playAgainBtn) {
            // Remove any existing listeners
            const newBtn = playAgainBtn.cloneNode(true);
            playAgainBtn.parentNode.replaceChild(newBtn, playAgainBtn);
            
            // Add new listener
            newBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                window.location.href = '/rts-lobby.html';
            });
        }
        
        screen.style.display = 'flex';
    }
    
    updateGameState(state) {
        this.lastGameState = state;
        
        // Update units
        if (state.units) {
            const currentUnitIds = new Set(state.units.map(u => u.id));
            
            // Remove units that no longer exist
            this.units.forEach((unitContainer, id) => {
                if (!currentUnitIds.has(id)) {
                    this.gameContainer.removeChild(unitContainer);
                    this.units.delete(id);
                }
            });
            
            // Update or create units
            state.units.forEach(unitData => {
                this.updateUnit(unitData);
            });
        }
        
        // Update buildings
        if (state.buildings) {
            const currentBuildingIds = new Set(state.buildings.map(b => b.id));
            
            // Remove buildings that no longer exist
            this.buildings.forEach((buildingContainer, id) => {
                if (!currentBuildingIds.has(id)) {
                    // Clean up rally point graphics if they exist
                    if (buildingContainer.rallyPointGraphics) {
                        this.gameContainer.removeChild(buildingContainer.rallyPointGraphics);
                        buildingContainer.rallyPointGraphics.destroy();
                    }
                    if (buildingContainer.rallyPointLine) {
                        this.gameContainer.removeChild(buildingContainer.rallyPointLine);
                        buildingContainer.rallyPointLine.destroy();
                    }
                    
                    // Remove building container
                    this.gameContainer.removeChild(buildingContainer);
                    buildingContainer.destroy();
                    this.buildings.delete(id);
                }
            });
            
            // Update or create buildings
            state.buildings.forEach(buildingData => {
                this.updateBuilding(buildingData);
            });
        }
        
        // Resource deposits removed - obstacles now contain harvestable resources
        
        // Update obstacles - now only dynamic updates (health/resources)
        // Full obstacle data was sent in gameInitialization
        
        // Remove obstacles that no longer exist (depleted)
        if (state.activeObstacleIds) {
            const activeObstacleIds = new Set(state.activeObstacleIds);
            
            // Remove obstacles that are no longer active
            this.obstacles.forEach((obstacleContainer, id) => {
                if (!activeObstacleIds.has(id)) {
                    this.gameContainer.removeChild(obstacleContainer);
                    obstacleContainer.destroy();
                    this.obstacles.delete(id);
                    this.obstaclesStatic.delete(id);
                }
            });
        }
        
        if (state.obstacleUpdates) {
            state.obstacleUpdates.forEach(update => {
                this.updateObstacleDynamic(update);
            });
        }
        
        // Update projectiles
        if (state.projectiles) {
            const currentProjectileIds = new Set(state.projectiles.map(p => p.id));
            
            // Remove projectiles that no longer exist
            this.projectiles.forEach((projectileContainer, id) => {
                if (!currentProjectileIds.has(id)) {
                    this.gameContainer.removeChild(projectileContainer);
                    this.projectiles.delete(id);
                }
            });
            
            // Update or create projectiles
            state.projectiles.forEach(projectileData => {
                this.updateProjectile(projectileData);
            });
        }
        
        // Update beams (instant-hit weapons)
        if (state.beams) {
            const currentBeamIds = new Set(state.beams.map(b => b.id));
            
            // Remove beams that no longer exist
            this.beams.forEach((beamGraphics, id) => {
                if (!currentBeamIds.has(id)) {
                    this.gameContainer.removeChild(beamGraphics);
                    this.beams.delete(id);
                }
            });
            
            // Update or create beams
            state.beams.forEach(beamData => {
                this.updateBeam(beamData);
            });
        }
        
        // Update field effects (explosions, etc.)
        if (state.fieldEffects) {
            const currentEffectIds = new Set(state.fieldEffects.map(e => e.id));
            
            // Remove effects that no longer exist
            this.fieldEffects.forEach((effectContainer, id) => {
                if (!currentEffectIds.has(id)) {
                    this.gameContainer.removeChild(effectContainer);
                    this.fieldEffects.delete(id);
                }
            });
            
            // Update or create effects
            state.fieldEffects.forEach(effectData => {
                this.updateFieldEffect(effectData);
            });
        }
        
        // Update faction info
        if (state.factions && this.myPlayerId != null) {
            this.myFaction = this.getFactionStateForPlayer(state.factions, this.myPlayerId);
            if (this.myFaction) {
                this.myTeam = this.myFaction.team;
                this.updateResourceDisplay();

                // Center camera on HQ on first update
                if (!this.hasCenteredCamera && state.buildings) {
                    this.centerCameraOnHQ(state.buildings);
                }
            }
        }
        
        // Update vision range
        if (state.visionRange) {
            this.visionRange = state.visionRange;
        }
        
        // NOTE: Biome and world dimensions are now sent once in gameInitialization
        
        // Update build menu based on available tech
        if (state.buildings) {
            this.updateBuildMenuAvailability(state.buildings);
        }
        
        // Update unit info panel
        this.updateUnitInfoPanel();
        
        // Update fog of war visualization (throttled to reduce performance impact)
        const now = Date.now();
        if (now - this.lastFogUpdate >= this.fogUpdateInterval) {
            this.updateFogOfWar();
            this.lastFogUpdate = now;
        }
    }
    
    updateUnit(unitData) {
        let unitContainer = this.units.get(unitData.id);
        
        if (!unitContainer) {
            // Create new unit
            unitContainer = this.createUnitGraphics(unitData);
            this.units.set(unitData.id, unitContainer);
            this.gameContainer.addChild(unitContainer);
            
            // Set z-index based on elevation (air units should render above buildings and projectiles)
            // Buildings have default z-index of 0
            // Ground units: z-index 1
            // Projectiles: z-index 4
            // Low altitude units: z-index 5
            // High altitude units: z-index 6
            if (unitData.elevation === 'HIGH') {
                unitContainer.zIndex = 6;
            } else if (unitData.elevation === 'LOW') {
                unitContainer.zIndex = 5;
            } else {
                unitContainer.zIndex = 1; // GROUND units
            }
        }
        
        // Update position
        unitContainer.position.set(unitData.x, unitData.y);
        
        // Update rotation (only rotate the shape and direction indicator, not health bar)
        // Direction indicator points right by default, matching atan2's angle=0
        if (unitContainer.rotatingContainer) {
            unitContainer.rotatingContainer.rotation = unitData.rotation;
        }
        
        // Update health bar (stays fixed at top)
        if (unitContainer.healthBar) {
            const healthPercent = unitData.health / unitData.maxHealth;
            
            // Hide health bar if at full health
            if (healthPercent >= 1.0) {
                unitContainer.healthBar.visible = false;
            } else {
                unitContainer.healthBar.visible = true;
                const offset = unitContainer.healthBarOffset || 25;
                unitContainer.healthBar.clear();
                unitContainer.healthBar.rect(-15, -offset, 30 * healthPercent, 3);
                unitContainer.healthBar.fill(this.getHealthColor(healthPercent));
            }
        }
        
        this.updateSupportWorkVisual(unitContainer, unitData);
        
        // Update selection indicator and track selected units
        if (unitContainer.selectionCircle) {
            unitContainer.selectionCircle.visible = unitData.selected;
        }
        
        // Update selectedUnits Set based on server state
        if (unitData.selected && unitData.ownerId === this.myPlayerId) {
            this.selectedUnits.add(unitData.id);
        } else {
            this.selectedUnits.delete(unitData.id);
        }
        
        // Update garrison label (for APCs)
        if (unitData.type === 'APC') {
            if (!unitContainer.garrisonLabel) {
                const label = new PIXI.Text('', {
                    fontFamily: 'Arial',
                    fontSize: 12,
                    fill: 0xFFFFFF,
                    stroke: 0x000000,
                    strokeThickness: 2
                });
                label.anchor.set(0.5, 1);
                label.position.set(0, -30); // Above the unit
                label.scale.y = -1; // Flip text vertically to account for inverted Y-axis
                unitContainer.addChild(label);
                unitContainer.garrisonLabel = label;
            }
            
            if (unitData.garrisonCount > 0) {
                unitContainer.garrisonLabel.text = `[${unitData.garrisonCount}/${unitData.maxGarrisonCapacity || 3}]`;
                unitContainer.garrisonLabel.visible = true;
            } else {
                unitContainer.garrisonLabel.visible = false;
            }
        }
        
        // Update cloak visual effect (for Cloak Tank and Spy)
        const typeInfo = this.unitTypes?.[unitData.type]; // Look up unit type info
        const specialAbility = typeInfo?.specialAbility; // Get special ability from static unit types
        // Spy has permanent cloak (no special ability), Cloak Tank has toggle cloak (special ability)
        const isCloakUnit = specialAbility === 'CLOAK' || unitData.type === 'SPY';
        if (isCloakUnit && unitData.cloaked !== undefined) {
            // Apply transparency and shimmer effect when cloaked
            if (unitData.cloaked) {
                // Fully cloaked - semi-transparent with shimmer (increased from 0.15 to 0.4 for better visibility)
                unitContainer.alpha = 0.4;
                
                // Add shimmer indicator if not already present
                if (!unitContainer.cloakShimmer) {
                    const shimmer = new PIXI.Graphics();
                    shimmer.circle(0, 0, (typeInfo?.size || 15) + 8);
                    shimmer.stroke({ width: 2, color: 0x00FFFF, alpha: 0.5 }); // Increased shimmer alpha from 0.3 to 0.5
                    unitContainer.addChild(shimmer);
                    unitContainer.cloakShimmer = shimmer;
                    
                    // Animate shimmer (pulsing effect)
                    shimmer.pulseDirection = 1;
                    shimmer.pulseAlpha = 0.5; // Increased from 0.3 to 0.5
                }
                unitContainer.cloakShimmer.visible = true;
                
                // Pulse the shimmer (more visible range)
                if (unitContainer.cloakShimmer.pulseAlpha !== undefined) {
                    unitContainer.cloakShimmer.pulseAlpha += 0.01 * unitContainer.cloakShimmer.pulseDirection;
                    if (unitContainer.cloakShimmer.pulseAlpha >= 0.7) { // Increased from 0.5 to 0.7
                        unitContainer.cloakShimmer.pulseDirection = -1;
                    } else if (unitContainer.cloakShimmer.pulseAlpha <= 0.3) { // Increased from 0.1 to 0.3
                        unitContainer.cloakShimmer.pulseDirection = 1;
                    }
                    unitContainer.cloakShimmer.alpha = unitContainer.cloakShimmer.pulseAlpha;
                }
            } else {
                // Cloak active but recently fired/detected - partial transparency
                unitContainer.alpha = 0.6; // Increased from 0.5 to 0.6
                if (unitContainer.cloakShimmer) {
                    unitContainer.cloakShimmer.visible = false;
                }
            }
        } else {
            // Not cloaked - full visibility
            unitContainer.alpha = 1.0;
            if (unitContainer.cloakShimmer) {
                unitContainer.cloakShimmer.visible = false;
            }
        }
        
        // Update shield visualization for SHIELD_TANK
        if (unitData.type === 'SHIELD_TANK') {
            if (!unitContainer.shieldGraphics) {
                const shieldGraphics = new PIXI.Graphics();
                unitContainer.addChild(shieldGraphics);
                unitContainer.shieldGraphics = shieldGraphics;
                // Shield should render below the unit itself
                shieldGraphics.zIndex = -1;
            }
            
            const shield = unitContainer.shieldGraphics;
            shield.clear();
            
            // Only show shield if active (Shield Tank's shield is always active when alive)
            // Shield radius from server data, or default to 120.0 if not provided
            const shieldRadius = unitData.shieldRadius || 120.0;
            shield.circle(0, 0, shieldRadius);
            shield.stroke({ width: 3, color: this.getTeamColor(unitData.team), alpha: 0.4 });
            shield.fill({ color: this.getTeamColor(unitData.team), alpha: 0.1 });
            
            // Optional: Add pulsing effect to make shield more visible
            if (shield.pulsePhase === undefined) {
                shield.pulsePhase = 0;
            }
            shield.pulsePhase += 0.02;
            const pulseAlpha = 0.3 + Math.sin(shield.pulsePhase) * 0.15;
            shield.alpha = pulseAlpha;
        }
        
        // ANIMATE AIR UNITS (bobbing, rotor spinning, etc.)
        if (unitContainer.isAirUnit) {
            this.updateAirUnitAnimation(unitContainer);
        }
        
        // Store data
        unitContainer.unitData = unitData;
    }
    
    /**
     * Update air unit animations (bobbing, rotor spinning, LED pulsing)
     */
    updateAirUnitAnimation(container) {
        // Increment animation time
        container.animationTime += 0.05;
        const time = container.animationTime;
        
        // Different animations for bomber vs. scout drone
        if (container.isBomber) {
            // BOMBER: Smooth flight, no bobbing, pulsing exhaust
            
            // 1. PULSING EXHAUST (jet engines)
            if (container.exhaustTrails) {
                const exhaustPulse = 0.5 + Math.sin(time * 3) * 0.3; // Faster pulse
                container.exhaustTrails.forEach(exhaust => {
                    exhaust.alpha = exhaustPulse;
                    // Slight scale variation for flame effect
                    const scale = 0.8 + Math.sin(time * 5 + exhaust.position.y) * 0.2;
                    exhaust.scale.set(scale, scale);
                });
            }
            
            // 2. BLINKING WING LIGHTS (navigation lights)
            if (container.wingLights) {
                const blink = Math.floor(time * 2) % 2 === 0; // Blink every 0.5 seconds
                container.wingLights.forEach(light => {
                    light.alpha = blink ? 1.0 : 0.3;
                });
            }
            
            // 3. SHADOW STAYS CONSTANT (bomber flies at constant altitude)
            // No animation needed for bomber shadow
            
        } else if (container.isHelicopter) {
            // HELICOPTER: Bobbing motion (like scout drone), spinning main rotor, spinning tail rotor
            
            // 1. BOBBING MOTION (helicopter body moves up/down, shadow stays fixed but changes alpha)
            const bob = Math.sin(time) * 4; // 4 pixel bob (slightly more than scout drone for larger craft)
            
            if (container.rotatingContainer) {
                // Move the helicopter body up and down
                container.rotatingContainer.y = bob;
            }
            
            if (container.shadowGraphics) {
                // Shadow stays at fixed position but changes alpha based on altitude
                const shadowAlpha = 0.35 - (bob * 0.02);
                container.shadowGraphics.alpha = Math.max(0.15, shadowAlpha);
            }
            
            // 2. SPIN MAIN ROTOR (top rotor blades)
            if (container.rotorContainer) {
                container.rotorContainer.rotation += 0.4; // Fast spinning
            }
            
            // 3. SPIN TAIL ROTOR (smaller rotor at rear)
            if (container.tailRotor) {
                container.tailRotor.rotation += 0.6; // Even faster than main rotor
            }
            
        } else if (container.isGunship) {
            // GUNSHIP: Smooth flight with gentle bobbing, pulsing jet exhaust (like bomber but with slight bob)
            
            // 1. GENTLE BOBBING MOTION (subtle for fixed-wing jet at low altitude)
            const bob = Math.sin(time * 0.8) * 3; // 3 pixel bob, slow frequency
            
            if (container.rotatingContainer) {
                // Move the gunship body up and down gently
                container.rotatingContainer.y = bob;
            }
            
            if (container.shadowGraphics) {
                // Shadow stays at fixed position but changes alpha based on altitude
                const shadowAlpha = 0.4 - (bob * 0.015);
                container.shadowGraphics.alpha = Math.max(0.2, shadowAlpha);
            }
        } else {
            // SCOUT DRONE: Bobbing motion, spinning rotors, pulsing LEDs
            
            // 1. BOBBING MOTION (drone body moves up/down, shadow stays fixed but changes alpha)
            const bob = Math.sin(time) * 3; // 3 pixel bob
            
            if (container.rotatingContainer) {
                // Move the drone body up and down
                container.rotatingContainer.y = bob;
            }
            
            if (container.shadowGraphics) {
                // Shadow stays at fixed position but changes alpha based on altitude
                const shadowAlpha = 0.3 - (bob * 0.02);
                container.shadowGraphics.alpha = Math.max(0.1, shadowAlpha);
            }
            
            // 2. SPIN ROTORS (4 independent rotors)
            if (container.rotors) {
                container.rotors.forEach((rotor, index) => {
                    // Each rotor spins at slightly different speed for variety
                    const speedVariation = 1 + (index * 0.1);
                    rotor.rotation += 0.3 * speedVariation;
                });
            }
            
            // 3. PULSE LED LIGHTS
            if (container.leds) {
                const ledPulse = 0.5 + Math.sin(time * 2) * 0.3; // Pulse between 0.2 and 0.8
                container.leds.forEach(led => {
                    led.alpha = ledPulse;
                });
            }
        }
    }
    
    getUnitTypeInfo(unitType) {
        const staticData = this.unitTypes[unitType];
        // Add derived properties based on backend data
        return {
            ...staticData,
            isAir: staticData.elevation === 'LOW' || staticData.elevation === 'HIGH'
        };
    }

    createUnitGraphics(unitData) {
        const typeInfo = this.getUnitTypeInfo(unitData.type);
        
        // Special rendering for air units
        if (typeInfo.isAir) {
            return this.createAirUnitGraphics(unitData, typeInfo);
        }
        
        const container = new PIXI.Container();
        // Create a rotating container for the unit shape and direction indicator
        const rotatingContainer = new PIXI.Container();
        container.addChild(rotatingContainer);
        container.rotatingContainer = rotatingContainer;
        
        // Create polygon shape (rotates with unit)
        const shape = new PIXI.Graphics();
        // Use physics body vertices if available, otherwise fall back to manual drawing
        if (unitData.vertices && unitData.vertices.length > 0) {
            // Check if this is multi-fixture format (array of fixtures) or single-fixture format
            const isMultiFixture = Array.isArray(unitData.vertices[0]) && Array.isArray(unitData.vertices[0][0]);
            
            if (isMultiFixture) {
                // Multi-fixture: vertices is [fixture1, fixture2, ...]
                // where each fixture is [[x1, y1], [x2, y2], ...]
                for (const fixtureVertices of unitData.vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(shape, fixtureVertices, typeInfo.color, unitData.team);
                        // Apply team-colored stroke to each fixture individually
                        shape.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
                    }
                }
            } else {
                // Single-fixture (backward compatibility): vertices is [[x1, y1], [x2, y2], ...]
                this.drawPhysicsPolygon(shape, unitData.vertices, typeInfo.color, unitData.team);
                // Apply team-colored stroke
                shape.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
            }
        } else {
            // Fallback for circles or if vertices not provided
            this.drawPolygon(shape, typeInfo.sides, typeInfo.size, typeInfo.color, unitData.team);
        }
        rotatingContainer.addChild(shape);
        
        // Create direction indicator OR turret barrel (rotates with unit)
        const hasTurret = unitData.type === 'TANK' || unitData.type === 'ARTILLERY';
        
        if (hasTurret) {
            // Draw turret barrel for tanks and artillery
            const barrel = new PIXI.Graphics();
            const barrelLength = typeInfo.size * 0.8;
            const barrelWidth = typeInfo.size * 0.15;
            
            // Draw barrel as a rectangle pointing right
            barrel.rect(0, -barrelWidth / 2, barrelLength, barrelWidth);
            barrel.fill({ color: 0x404040 });
            barrel.stroke({ width: 1, color: 0x000000 });
            
            // Add muzzle tip
            barrel.circle(barrelLength, 0, barrelWidth * 0.6);
            barrel.fill({ color: 0x202020 });
            barrel.stroke({ width: 1, color: 0x000000 });
            
            rotatingContainer.addChild(barrel);
            container.turretBarrel = barrel;
        } else {
            // Draw a small triangle direction indicator for non-turret units
            const directionIndicator = new PIXI.Graphics();
            directionIndicator.moveTo(typeInfo.size * 0.7, 0);
            directionIndicator.lineTo(typeInfo.size * 0.4, -typeInfo.size * 0.2);
            directionIndicator.lineTo(typeInfo.size * 0.4, typeInfo.size * 0.2);
            directionIndicator.lineTo(typeInfo.size * 0.7, 0);
            directionIndicator.fill({ color: 0xFFFFFF, alpha: 0.8 });
            directionIndicator.stroke({ width: 1, color: 0x000000 });
            rotatingContainer.addChild(directionIndicator);
        }
        
        // Create health bar (does NOT rotate)
        const healthBar = new PIXI.Graphics();
        healthBar.rect(-15, -typeInfo.size - 8, 30, 3);
        healthBar.fill(0x00FF00);
        container.addChild(healthBar);
        container.healthBar = healthBar;
        container.healthBarOffset = typeInfo.size + 8; // Store offset for updates
        
        // Create selection circle (does NOT rotate)
        const selectionCircle = new PIXI.Graphics();
        selectionCircle.circle(0, 0, typeInfo.size + 5);
        selectionCircle.stroke({ width: 2, color: 0xFFFF00 });
        selectionCircle.visible = false;
        container.addChild(selectionCircle);
        container.selectionCircle = selectionCircle;
        
        return container;
    }
    
    /**
     * Create special graphics for air units (Scout Drone, Bomber, Helicopter, Interceptor)
     * Features: shadow, rotor/engine animation, glow effects, bobbing motion
     */
    createAirUnitGraphics(unitData, typeInfo) {
        // Route to specific air unit renderer
        switch (unitData.type) {
            case 'BOMBER':
                return this.createBomberGraphics(unitData, typeInfo);
            case 'HELICOPTER':
                return this.createHelicopterGraphics(unitData, typeInfo);
            case 'INTERCEPTOR':
                return this.createInterceptorGraphics(unitData, typeInfo);
            case 'GUNSHIP':
                return this.createGunshipGraphics(unitData, typeInfo);
            case 'SCOUT_DRONE':
            default:
                return this.createScoutDroneGraphics(unitData, typeInfo);
        }
    }
    
    /**
     * Create graphics for Scout Drone (VTOL quadcopter)
     * Features: spinning rotors, bobbing motion, LEDs
     */
    createScoutDroneGraphics(unitData, typeInfo) {
        const container = new PIXI.Container();
        container.isAirUnit = true;
        
        // 1. SHADOW (drawn first, appears below unit)
        const shadow = new PIXI.Graphics();
        const shadowOffset = -24; // Negative Y for shadow below unit
        shadow.ellipse(0, shadowOffset, typeInfo.size * 0.7, typeInfo.size * 0.4);
        shadow.fill({ color: 0x000000, alpha: 0.3 });
        container.addChild(shadow);
        container.shadowGraphics = shadow;
        
        // 2. ROTATING CONTAINER for main body + rotors
        const rotatingContainer = new PIXI.Container();
        container.addChild(rotatingContainer);
        container.rotatingContainer = rotatingContainer;
        
        // 3. MAIN BODY (quadcopter center hub + arms)
        const body = new PIXI.Graphics();
        
        // Draw X-shaped drone body using physics vertices if available
        if (unitData.vertices && unitData.vertices.length > 0) {
            const isMultiFixture = Array.isArray(unitData.vertices[0]) && Array.isArray(unitData.vertices[0][0]);
            
            if (isMultiFixture) {
                for (const fixtureVertices of unitData.vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(body, fixtureVertices, typeInfo.color, unitData.team);
                        body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
                    }
                }
            } else {
                this.drawPhysicsPolygon(body, unitData.vertices, typeInfo.color, unitData.team);
                body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
            }
        } else {
            // Fallback: simple diamond shape
            this.drawPolygon(body, 4, typeInfo.size, typeInfo.color, unitData.team);
        }
        
        // Make body semi-transparent for "floating" effect
        body.alpha = 0.9;
        rotatingContainer.addChild(body);
        
        // 4. ROTOR BLADES (4 rotors at corners, spinning independently)
        const rotorPositions = [
            { x: typeInfo.size * 0.7, y: typeInfo.size * 0.7 },   // Front-right
            { x: -typeInfo.size * 0.7, y: typeInfo.size * 0.7 },  // Front-left
            { x: -typeInfo.size * 0.7, y: -typeInfo.size * 0.7 }, // Rear-left
            { x: typeInfo.size * 0.7, y: -typeInfo.size * 0.7 }   // Rear-right
        ];
        
        container.rotors = [];
        for (const pos of rotorPositions) {
            const rotor = new PIXI.Graphics();
            
            // Draw rotor blades (two perpendicular lines)
            rotor.moveTo(-typeInfo.size * 0.25, 0);
            rotor.lineTo(typeInfo.size * 0.25, 0);
            rotor.stroke({ width: 1.5, color: 0x888888, alpha: 0.7 });
            
            rotor.moveTo(0, -typeInfo.size * 0.25);
            rotor.lineTo(0, typeInfo.size * 0.25);
            rotor.stroke({ width: 1.5, color: 0x888888, alpha: 0.7 });
            
            rotor.position.set(pos.x, pos.y);
            rotatingContainer.addChild(rotor);
            container.rotors.push(rotor);
        }
        
        // 5. LED LIGHTS at rotor positions (pulsing effect)
        container.leds = [];
        for (const pos of rotorPositions) {
            const led = new PIXI.Graphics();
            led.circle(0, 0, 2);
            led.fill({ color: 0x00FFFF, alpha: 0.8 });
            led.position.set(pos.x, pos.y);
            rotatingContainer.addChild(led);
            container.leds.push(led);
        }
        
        // 6. GLOW FILTER for ethereal look
        if (PIXI.GlowFilter) {
            const glow = new PIXI.GlowFilter({
                distance: 10,
                outerStrength: 1.5,
                color: typeInfo.color,
                quality: 0.3
            });
            body.filters = [glow];
        }
        
        // 7. HEALTH BAR (same as ground units)
        const healthBar = new PIXI.Graphics();
        healthBar.rect(-typeInfo.size * 0.6, -typeInfo.size - 8, typeInfo.size * 1.2, 4);
        healthBar.fill({ color: 0x00FF00 });
        container.addChild(healthBar);
        container.healthBar = healthBar;
        
        // 8. SELECTION CIRCLE (drawn when selected)
        const selectionCircle = new PIXI.Graphics();
        selectionCircle.visible = false;
        selectionCircle.circle(0, 0, typeInfo.size * 1.3);
        selectionCircle.stroke({ width: 2, color: 0x00FF00 });
        container.addChild(selectionCircle);
        container.selectionCircle = selectionCircle;
        
        // Store animation state
        container.animationTime = Math.random() * Math.PI * 2; // Random start phase
        
        // Store data
        container.unitData = unitData;
        container.typeInfo = typeInfo;
        
        return container;
    }
    
    /**
     * Create graphics for Bomber (fixed-wing aircraft)
     * Features: delta wing shape, jet exhaust, larger shadow, no bobbing (smooth flight)
     */
    createBomberGraphics(unitData, typeInfo) {
        const container = new PIXI.Container();
        container.isAirUnit = true;
        container.isBomber = true;
        
        // 1. SHADOW (larger and more diffuse for higher altitude)
        const shadow = new PIXI.Graphics();
        const shadowOffset = -35; // Larger offset = higher altitude
        shadow.ellipse(0, shadowOffset, typeInfo.size * 1.2, typeInfo.size * 0.5);
        shadow.fill({ color: 0x000000, alpha: 0.4 });
        // Add blur for altitude effect
        if (PIXI.BlurFilter) {
            shadow.filters = [new PIXI.BlurFilter({ strength: 2 })];
        }
        container.addChild(shadow);
        container.shadowGraphics = shadow;
        
        // 2. ROTATING CONTAINER for aircraft body
        const rotatingContainer = new PIXI.Container();
        container.addChild(rotatingContainer);
        container.rotatingContainer = rotatingContainer;
        
        // 3. MAIN BODY (delta wing aircraft)
        const body = new PIXI.Graphics();
        
        // Draw delta wing shape using physics vertices if available
        if (unitData.vertices && unitData.vertices.length > 0) {
            const isMultiFixture = Array.isArray(unitData.vertices[0]) && Array.isArray(unitData.vertices[0][0]);
            
            if (isMultiFixture) {
                for (const fixtureVertices of unitData.vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(body, fixtureVertices, typeInfo.color, unitData.team);
                        body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
                    }
                }
            } else {
                this.drawPhysicsPolygon(body, unitData.vertices, typeInfo.color, unitData.team);
                body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
            }
        } else {
            // Fallback: triangle (delta wing)
            this.drawPolygon(body, 3, typeInfo.size, typeInfo.color, unitData.team);
        }
        
        // More opaque than drone (solid metal)
        body.alpha = 1.0;
        rotatingContainer.addChild(body);
        
        // 4. ENGINE EXHAUST (at rear of aircraft - tail)
        const exhaustContainer = new PIXI.Container();
        exhaustContainer.position.set(-typeInfo.size * 0.7, 0); // Rear of aircraft
        
        // Two exhaust trails
        container.exhaustTrails = [];
        const exhaustPositions = [
            { x: 0, y: -typeInfo.size * 0.15 },
            { x: 0, y: typeInfo.size * 0.15 }
        ];
        
        for (const pos of exhaustPositions) {
            const exhaust = new PIXI.Graphics();
            exhaust.circle(0, 0, 3);
            exhaust.fill({ color: 0xFF6600, alpha: 0.6 }); // Orange glow
            exhaust.position.set(pos.x, pos.y);
            exhaustContainer.addChild(exhaust);
            container.exhaustTrails.push(exhaust);
        }
        
        rotatingContainer.addChild(exhaustContainer);
        
        // 5. COCKPIT (small lighter triangle at front)
        const cockpit = new PIXI.Graphics();
        cockpit.moveTo(typeInfo.size * 0.6, 0);
        cockpit.lineTo(typeInfo.size * 0.4, -typeInfo.size * 0.1);
        cockpit.lineTo(typeInfo.size * 0.4, typeInfo.size * 0.1);
        cockpit.closePath();
        cockpit.fill({ color: 0x87CEEB, alpha: 0.7 }); // Light blue tinted glass
        rotatingContainer.addChild(cockpit);
        
        // 7. SUBTLE GLOW for jet engines
        if (PIXI.GlowFilter) {
            const glow = new PIXI.GlowFilter({
                distance: 8,
                outerStrength: 1.0,
                color: 0xFF6600, // Orange glow from engines
                quality: 0.3
            });
            exhaustContainer.filters = [glow];
        }
        
        // 8. HEALTH BAR
        const healthBar = new PIXI.Graphics();
        healthBar.rect(-typeInfo.size * 0.6, -typeInfo.size - 8, typeInfo.size * 1.2, 4);
        healthBar.fill({ color: 0x00FF00 });
        container.addChild(healthBar);
        container.healthBar = healthBar;
        
        // 9. SELECTION CIRCLE
        const selectionCircle = new PIXI.Graphics();
        selectionCircle.visible = false;
        selectionCircle.circle(0, 0, typeInfo.size * 1.3);
        selectionCircle.stroke({ width: 2, color: 0x00FF00 });
        container.addChild(selectionCircle);
        container.selectionCircle = selectionCircle;
        
        // Store animation state
        container.animationTime = Math.random() * Math.PI * 2;
        
        // Store data
        container.unitData = unitData;
        container.typeInfo = typeInfo;
        
        return container;
    }
    
    /**
     * Create graphics for Helicopter (LOW altitude attack helicopter)
     * Features: spinning rotor, bobbing motion, dual rocket pods, landing skids
     */
    createHelicopterGraphics(unitData, typeInfo) {
        const container = new PIXI.Container();
        container.isAirUnit = true;
        container.isHelicopter = true;
        
        // 1. SHADOW (drawn first, appears below unit)
        const shadow = new PIXI.Graphics();
        const shadowOffset = -32; // Negative Y for shadow below unit (lower than scout drone due to larger size)
        shadow.ellipse(0, shadowOffset, typeInfo.size * 1.0, typeInfo.size * 0.5);
        shadow.fill({ color: 0x000000, alpha: 0.35 });
        container.addChild(shadow);
        container.shadowGraphics = shadow;
        
        // 2. ROTATING CONTAINER for main body + rotors
        const rotatingContainer = new PIXI.Container();
        container.addChild(rotatingContainer);
        container.rotatingContainer = rotatingContainer;
        
        // 3. MAIN BODY (helicopter fuselage - pentagon shape)
        const body = new PIXI.Graphics();
        
        // Draw helicopter body using physics vertices if available
        if (unitData.vertices && unitData.vertices.length > 0) {
            const isMultiFixture = Array.isArray(unitData.vertices[0]) && Array.isArray(unitData.vertices[0][0]);
            
            if (isMultiFixture) {
                for (const fixtureVertices of unitData.vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(body, fixtureVertices, typeInfo.color, unitData.team);
                        body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
                    }
                }
            } else {
                this.drawPhysicsPolygon(body, unitData.vertices, typeInfo.color, unitData.team);
                body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
            }
        } else {
            // Fallback: pentagon (helicopter shape)
            this.drawPolygon(body, 5, typeInfo.size, typeInfo.color, unitData.team);
        }
        
        body.alpha = 0.95; // Slightly transparent
        rotatingContainer.addChild(body);
        
        // 4. MAIN ROTOR (top rotor blades)
        const rotorContainer = new PIXI.Container();
        rotorContainer.position.set(0, -typeInfo.size * 0.1); // Slightly above center
        
        // Two rotor blades crossing
        const rotor1 = new PIXI.Graphics();
        rotor1.rect(-typeInfo.size * 1.2, -2, typeInfo.size * 2.4, 4);
        rotor1.fill({ color: 0x808080, alpha: 0.4 }); // Semi-transparent gray
        rotorContainer.addChild(rotor1);
        
        const rotor2 = new PIXI.Graphics();
        rotor2.rect(-2, -typeInfo.size * 1.2, 4, typeInfo.size * 2.4);
        rotor2.fill({ color: 0x808080, alpha: 0.4 });
        rotorContainer.addChild(rotor2);
        
        rotatingContainer.addChild(rotorContainer);
        container.rotorContainer = rotorContainer;
        
        // 5. TAIL ROTOR (small rotor at rear)
        const tailRotor = new PIXI.Graphics();
        tailRotor.rect(-6, -1, 12, 2);
        tailRotor.fill({ color: 0x808080, alpha: 0.3 });
        tailRotor.position.set(-typeInfo.size * 0.8, 0); // At tail
        rotatingContainer.addChild(tailRotor);
        container.tailRotor = tailRotor;
        
        // 6. COCKPIT (small lighter section at front)
        const cockpit = new PIXI.Graphics();
        cockpit.circle(typeInfo.size * 0.3, 0, typeInfo.size * 0.2);
        cockpit.fill({ color: 0x87CEEB, alpha: 0.6 }); // Light blue tinted glass
        rotatingContainer.addChild(cockpit);

        // 8. LANDING SKIDS
        const leftSkid = new PIXI.Graphics();
        leftSkid.rect(-typeInfo.size * 0.6, -2, typeInfo.size * 1.2, 3);
        leftSkid.fill({ color: 0x696969, alpha: 0.8 }); // Dark gray
        leftSkid.position.set(0, -typeInfo.size * 0.4); // Below left
        rotatingContainer.addChild(leftSkid);
        
        const rightSkid = new PIXI.Graphics();
        rightSkid.rect(-typeInfo.size * 0.6, -2, typeInfo.size * 1.2, 3);
        rightSkid.fill({ color: 0x696969, alpha: 0.8 });
        rightSkid.position.set(0, typeInfo.size * 0.4); // Below right
        rotatingContainer.addChild(rightSkid);

        // 10. HEALTH BAR
        const healthBar = new PIXI.Graphics();
        healthBar.rect(-typeInfo.size * 0.6, -typeInfo.size - 8, typeInfo.size * 1.2, 4);
        healthBar.fill({ color: 0x00FF00 });
        container.addChild(healthBar);
        container.healthBar = healthBar;
        
        // 11. SELECTION CIRCLE
        const selectionCircle = new PIXI.Graphics();
        selectionCircle.visible = false;
        selectionCircle.circle(0, 0, typeInfo.size * 1.2);
        selectionCircle.stroke({ width: 2, color: 0x00FF00 });
        container.addChild(selectionCircle);
        container.selectionCircle = selectionCircle;
        
        // Store animation state
        container.animationTime = Math.random() * Math.PI * 2;
        container.rotorAngle = 0;
        
        // Store data
        container.unitData = unitData;
        container.typeInfo = typeInfo;
        
        return container;
    }
    
    /**
     * Create graphics for Gunship (LOW altitude heavy attack aircraft)
     * Features: fixed-wing design, dual jet engines, weapon pods, armored appearance
     * heavy attack jet, larger and more imposing than interceptor
     */
    createGunshipGraphics(unitData, typeInfo) {
        const container = new PIXI.Container();
        container.isAirUnit = true;
        container.isGunship = true;
        
        // 1. SHADOW (drawn first, appears below unit)
        const shadow = new PIXI.Graphics();
        const shadowOffset = -36; // Larger offset than helicopter (bigger aircraft)
        shadow.ellipse(0, shadowOffset, typeInfo.size * 1.1, typeInfo.size * 0.55);
        shadow.fill({ color: 0x000000, alpha: 0.4 });
        container.addChild(shadow);
        container.shadowGraphics = shadow;
        
        // 2. ROTATING CONTAINER for main body (rotates to face direction, but no spinning parts)
        const rotatingContainer = new PIXI.Container();
        container.addChild(rotatingContainer);
        container.rotatingContainer = rotatingContainer;
        
        // 3. MAIN BODY (gunship fuselage - pentagon shape for fixed-wing jet)
        const body = new PIXI.Graphics();
        
        // Draw gunship body using physics vertices if available
        if (unitData.vertices && unitData.vertices.length > 0) {
            const isMultiFixture = Array.isArray(unitData.vertices[0]) && Array.isArray(unitData.vertices[0][0]);
            
            if (isMultiFixture) {
                for (const fixtureVertices of unitData.vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(body, fixtureVertices, typeInfo.color, unitData.team);
                        body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
                    }
                }
            } else {
                this.drawPhysicsPolygon(body, unitData.vertices, typeInfo.color, unitData.team);
                body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
            }
        } else {
            // Fallback: pentagon (gunship shape)
            this.drawPolygon(body, 5, typeInfo.size, typeInfo.color, unitData.team);
        }
        
        body.alpha = 0.95; // Slightly transparent
        rotatingContainer.addChild(body);
        
        // 4. WINGS (swept-back delta wings for heavy attack jet)
        const leftWing = new PIXI.Graphics();
        leftWing.moveTo(0, 0);
        leftWing.lineTo(-typeInfo.size * 0.6, -typeInfo.size * 0.8);
        leftWing.lineTo(-typeInfo.size * 0.3, -typeInfo.size * 0.5);
        leftWing.fill({ color: typeInfo.color, alpha: 0.8 });
        rotatingContainer.addChild(leftWing);
        
        const rightWing = new PIXI.Graphics();
        rightWing.moveTo(0, 0);
        rightWing.lineTo(-typeInfo.size * 0.6, typeInfo.size * 0.8);
        rightWing.lineTo(-typeInfo.size * 0.3, typeInfo.size * 0.5);
        rightWing.fill({ color: typeInfo.color, alpha: 0.8 });
        rotatingContainer.addChild(rightWing);
        
        // 5. COCKPIT (armored cockpit at front)
        const cockpit = new PIXI.Graphics();
        cockpit.circle(typeInfo.size * 0.4, 0, typeInfo.size * 0.25);
        cockpit.fill({ color: 0x4682B4, alpha: 0.7 }); // Steel blue tinted glass
        rotatingContainer.addChild(cockpit);
        
        // 6. WEAPON PODS (dual weapon systems under wings)
        const leftWeapon = new PIXI.Graphics();
        leftWeapon.rect(-typeInfo.size * 0.4, -3, typeInfo.size * 0.6, 6);
        leftWeapon.fill({ color: 0x2F4F4F, alpha: 0.95 }); // Dark slate gray
        leftWeapon.position.set(-typeInfo.size * 0.1, -typeInfo.size * 0.6); // Under left wing
        rotatingContainer.addChild(leftWeapon);
        
        const rightWeapon = new PIXI.Graphics();
        rightWeapon.rect(-typeInfo.size * 0.4, -3, typeInfo.size * 0.6, 6);
        rightWeapon.fill({ color: 0x2F4F4F, alpha: 0.95 });
        rightWeapon.position.set(-typeInfo.size * 0.1, typeInfo.size * 0.6); // Under right wing
        rotatingContainer.addChild(rightWeapon);
        
        // 9. ARMOR PLATING HIGHLIGHTS
        const armorHighlight = new PIXI.Graphics();
        armorHighlight.rect(-typeInfo.size * 0.4, -typeInfo.size * 0.15, typeInfo.size * 0.8, typeInfo.size * 0.3);
        armorHighlight.fill({ color: 0x708090, alpha: 0.3 }); // Slate gray highlight
        rotatingContainer.addChild(armorHighlight);
        
        // 10. HEALTH BAR
        const healthBar = new PIXI.Graphics();
        healthBar.rect(-typeInfo.size * 0.7, -typeInfo.size - 10, typeInfo.size * 1.4, 5);
        healthBar.fill({ color: 0x00FF00 });
        container.addChild(healthBar);
        container.healthBar = healthBar;
        
        // 11. SELECTION CIRCLE
        const selectionCircle = new PIXI.Graphics();
        selectionCircle.visible = false;
        selectionCircle.circle(0, 0, typeInfo.size * 1.3);
        selectionCircle.stroke({ width: 3, color: 0xFFD700 });
        container.addChild(selectionCircle);
        container.selectionCircle = selectionCircle;
        
        // Store animation state
        container.animationTime = Math.random() * Math.PI * 2;
        
        // Store data
        container.unitData = unitData;
        container.typeInfo = typeInfo;
        
        return container;
    }
    
    /**
     * Create graphics for Interceptor (HIGH altitude fighter jet)
     * Features: delta wing design, afterburner, sleek profile
     */
    createInterceptorGraphics(unitData, typeInfo) {
        const container = new PIXI.Container();
        container.isAirUnit = true;
        container.isInterceptor = true;
        
        // 1. SHADOW (largest offset for highest altitude)
        const shadow = new PIXI.Graphics();
        const shadowOffset = -40; // Highest altitude = largest offset
        shadow.ellipse(0, shadowOffset, typeInfo.size * 1.3, typeInfo.size * 0.5);
        shadow.fill({ color: 0x000000, alpha: 0.45 });
        // Add blur for high altitude effect
        if (PIXI.BlurFilter) {
            shadow.filters = [new PIXI.BlurFilter({ strength: 3 })];
        }
        container.addChild(shadow);
        container.shadowGraphics = shadow;
        
        // 2. ROTATING CONTAINER for aircraft body
        const rotatingContainer = new PIXI.Container();
        container.addChild(rotatingContainer);
        container.rotatingContainer = rotatingContainer;
        
        // 3. MAIN BODY (sleek delta wing fighter - triangle)
        const body = new PIXI.Graphics();
        
        // Draw delta wing shape using physics vertices if available
        if (unitData.vertices && unitData.vertices.length > 0) {
            const isMultiFixture = Array.isArray(unitData.vertices[0]) && Array.isArray(unitData.vertices[0][0]);
            
            if (isMultiFixture) {
                for (const fixtureVertices of unitData.vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(body, fixtureVertices, typeInfo.color, unitData.team);
                        body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
                    }
                }
            } else {
                this.drawPhysicsPolygon(body, unitData.vertices, typeInfo.color, unitData.team);
                body.stroke({ width: 1, color: this.getTeamColor(unitData.team) });
            }
        } else {
            // Fallback: triangle (delta wing)
            this.drawPolygon(body, 3, typeInfo.size, typeInfo.color, unitData.team);
        }
        
        body.alpha = 1.0; // Solid metal
        rotatingContainer.addChild(body);
        
        // 4. AFTERBURNER (bright engine exhaust at rear)
        const afterburnerContainer = new PIXI.Container();
        afterburnerContainer.position.set(-typeInfo.size * 0.75, 0); // Rear of aircraft
        
        // Single powerful engine exhaust
        const afterburner = new PIXI.Graphics();
        afterburner.circle(0, 0, 4);
        afterburner.fill({ color: 0xFF4500, alpha: 0.9 }); // Bright orange-red
        afterburnerContainer.addChild(afterburner);
        
        // Add intense glow for afterburner
        if (PIXI.GlowFilter) {
            const glow = new PIXI.GlowFilter({
                distance: 12,
                outerStrength: 2.0,
                color: 0xFF4500,
                quality: 0.4
            });
            afterburnerContainer.filters = [glow];
        }
        
        rotatingContainer.addChild(afterburnerContainer);
        container.afterburner = afterburner;
        
        // 5. COCKPIT (small canopy at front)
        const cockpit = new PIXI.Graphics();
        cockpit.moveTo(typeInfo.size * 0.7, 0);
        cockpit.lineTo(typeInfo.size * 0.5, -typeInfo.size * 0.08);
        cockpit.lineTo(typeInfo.size * 0.5, typeInfo.size * 0.08);
        cockpit.closePath();
        cockpit.fill({ color: 0x87CEEB, alpha: 0.8 }); // Light blue tinted
        rotatingContainer.addChild(cockpit);

        // 8. HEALTH BAR
        const healthBar = new PIXI.Graphics();
        healthBar.rect(-typeInfo.size * 0.6, -typeInfo.size - 8, typeInfo.size * 1.2, 4);
        healthBar.fill({ color: 0x00FF00 });
        container.addChild(healthBar);
        container.healthBar = healthBar;
        
        // 9. FUEL/AMMO INDICATOR (for interceptors)
        const statusBar = new PIXI.Graphics();
        statusBar.rect(-typeInfo.size * 0.6, -typeInfo.size - 14, typeInfo.size * 1.2, 3);
        statusBar.fill({ color: 0x00BFFF }); // Blue for fuel
        container.addChild(statusBar);
        container.statusBar = statusBar;
        
        // 10. SELECTION CIRCLE
        const selectionCircle = new PIXI.Graphics();
        selectionCircle.visible = false;
        selectionCircle.circle(0, 0, typeInfo.size * 1.4);
        selectionCircle.stroke({ width: 2, color: 0x00FF00 });
        container.addChild(selectionCircle);
        container.selectionCircle = selectionCircle;
        
        // Store animation state
        container.animationTime = Math.random() * Math.PI * 2;
        
        // Store data
        container.unitData = unitData;
        container.typeInfo = typeInfo;
        
        return container;
    }
    
    updateBuilding(buildingData) {
        // Merge with static building type data if available
        if (this.buildingTypes && this.buildingTypes[buildingData.type]) {
            buildingData = {
                ...this.buildingTypes[buildingData.type],
                ...buildingData
            };
        }
        
        let buildingContainer = this.buildings.get(buildingData.id);
        
        if (!buildingContainer) {
            // Create new building
            buildingContainer = this.createBuildingGraphics(buildingData);
            this.buildings.set(buildingData.id, buildingContainer);
            this.gameContainer.addChild(buildingContainer);
            
            // Buildings should render below all units
            buildingContainer.zIndex = 0;
        }
        
        // Update position
        buildingContainer.position.set(buildingData.x, buildingData.y);
        
        // Update building appearance based on construction status
        if (buildingContainer.shapeGraphics && buildingContainer.typeInfo) {
            const shape = buildingContainer.shapeGraphics;
            shape.clear();
            
            // Use physics body vertices if available for accurate shape rendering
            const hasVertices = buildingData.vertices && buildingData.vertices.length > 0;
            
            if (buildingData.underConstruction) {
                // Dotted outline for buildings under construction
                if (hasVertices) {
                    // Check if this is multi-fixture format
                    const isMultiFixture = Array.isArray(buildingData.vertices[0]) && Array.isArray(buildingData.vertices[0][0]);
                    
                    if (isMultiFixture) {
                        // Multi-fixture: draw each fixture separately
                        for (const fixtureVertices of buildingData.vertices) {
                            if (fixtureVertices.length > 0) {
                                this.drawPhysicsPolygonOutline(shape, fixtureVertices, 
                                                               buildingContainer.typeInfo.color, 
                                                               buildingData.team);
                            }
                        }
                    } else {
                        // Single-fixture (backward compatibility)
                        this.drawPhysicsPolygonOutline(shape, buildingData.vertices, 
                                                       buildingContainer.typeInfo.color, 
                                                       buildingData.team);
                    }
                } else {
                    this.drawPolygonOutline(shape, buildingContainer.typeInfo.sides, 
                                           buildingContainer.typeInfo.size, 
                                           buildingContainer.typeInfo.color, 
                                           buildingData.team);
                }
            } else {
                // Solid fill for completed buildings
                if (hasVertices) {
                    // Check if this is multi-fixture format
                    const isMultiFixture = Array.isArray(buildingData.vertices[0]) && Array.isArray(buildingData.vertices[0][0]);
                    
                    if (isMultiFixture) {
                        // Multi-fixture: draw each fixture separately
                        for (const fixtureVertices of buildingData.vertices) {
                            if (fixtureVertices.length > 0) {
                                this.drawPhysicsPolygon(shape, fixtureVertices, 
                                                       buildingContainer.typeInfo.color, 
                                                       buildingData.team);
                                shape.stroke({ width: 1, color: this.getTeamColor(buildingData.team) });
                            }
                        }
                    } else {
                        // Single-fixture (backward compatibility)
                        this.drawPhysicsPolygon(shape, buildingData.vertices, 
                                               buildingContainer.typeInfo.color, 
                                               buildingData.team);
                        shape.stroke({ width: 1, color: this.getTeamColor(buildingData.team) });
                    }
                } else {
                    this.drawPolygon(shape, buildingContainer.typeInfo.sides, 
                                   buildingContainer.typeInfo.size, 
                                   buildingContainer.typeInfo.color, 
                                   buildingData.team);
                }
            }
        }
        
        // Update turret rotation for TURRET buildings (hide barrel during construction)
        if (buildingContainer.rotatingContainer && buildingContainer.turretBarrel) {
            if (buildingData.underConstruction) {
                // Hide only the turret barrel while under construction (keep base visible)
                buildingContainer.turretBarrel.visible = false;
            } else {
                // Show and rotate turret barrel when construction is complete
                buildingContainer.turretBarrel.visible = true;
                if (buildingData.rotation !== undefined) {
                    buildingContainer.rotatingContainer.rotation = buildingData.rotation;
                }
            }
        }
        
        // Update health bar (fixed at top of building)
        if (buildingContainer.healthBar) {
            const healthPercent = buildingData.health / buildingData.maxHealth;
            
            // Hide health bar if at full health
            if (healthPercent >= 1.0) {
                buildingContainer.healthBar.visible = false;
            } else {
                buildingContainer.healthBar.visible = true;
                const offset = buildingContainer.healthBarOffset || 60;
                buildingContainer.healthBar.clear();
                buildingContainer.healthBar.rect(-40, -offset, 80 * healthPercent, 5);
                buildingContainer.healthBar.fill(this.getHealthColor(healthPercent));
            }
        }
        
        // Update construction progress (just below health bar)
        if (buildingData.underConstruction && buildingContainer.constructionBar) {
            buildingContainer.constructionBar.visible = true;
            const constructionOffset = buildingContainer.constructionBarOffset || 50;
            buildingContainer.constructionBar.clear();
            buildingContainer.constructionBar.rect(-40, -constructionOffset, 80 * buildingData.constructionPercent, 5);
            buildingContainer.constructionBar.fill(0xFFFF00);
        } else if (buildingContainer.constructionBar) {
            buildingContainer.constructionBar.visible = false;
        }
        
        // Update production progress (below construction bar)
        if (buildingData.productionPercent > 0 && buildingContainer.productionBar) {
            buildingContainer.productionBar.visible = true;
            const productionOffset = buildingContainer.productionBarOffset || 40;
            buildingContainer.productionBar.clear();
            buildingContainer.productionBar.rect(-40, -productionOffset, 80 * buildingData.productionPercent, 5);
            buildingContainer.productionBar.fill(0x00BFFF); // Deep sky blue for production
        } else if (buildingContainer.productionBar) {
            buildingContainer.productionBar.visible = false;
        }
        
        // Update production queue count (next to progress bar)
        if (buildingData.productionQueueSize > 0 || buildingData.productionPercent > 0) {
            if (!buildingContainer.queueText) {
                // Create queue count text
                buildingContainer.queueText = new PIXI.Text('', {
                    fontFamily: 'Arial',
                    fontSize: 12,
                    fill: 0xFFFFFF,
                    stroke: 0x000000,
                    strokeThickness: 3
                });
                buildingContainer.queueText.anchor.set(0.5, 0.5);
                buildingContainer.queueText.scale.y = -1; // Flip vertically to match game coordinate system
                buildingContainer.addChild(buildingContainer.queueText);
            }
            
            // Show queue size (current production + queued)
            const totalInQueue = (buildingData.productionPercent > 0 ? 1 : 0) + buildingData.productionQueueSize;
            if (totalInQueue > 0) {
                buildingContainer.queueText.text = `(${totalInQueue})`;
                buildingContainer.queueText.visible = true;
                const productionOffset = buildingContainer.productionBarOffset || 40;
                buildingContainer.queueText.position.set(45, -productionOffset - 2); // Right of progress bar
            } else {
                buildingContainer.queueText.visible = false;
            }
        } else if (buildingContainer.queueText) {
            buildingContainer.queueText.visible = false;
        }
        
        // Update selection indicator
        if (buildingContainer.selectionCircle) {
            buildingContainer.selectionCircle.visible = buildingData.selected || false;
        }
        
        // Update rally point indicator (only for production buildings)
        if (buildingData.rallyPoint && buildingData.canProduceUnits) {
            if (!buildingContainer.rallyPointGraphics) {
                // Create rally point graphics
                const rallyContainer = new PIXI.Container();
                
                // Flag at rally point (flipped for game world coordinates)
                const flagPole = new PIXI.Graphics();
                flagPole.moveTo(0, 0);
                flagPole.lineTo(0, 30); // Pole goes UP (positive Y in game world)
                flagPole.stroke({ width: 2, color: 0xFFFFFF });
                
                const flag = new PIXI.Graphics();
                flag.moveTo(0, 30); // Start at top of pole
                flag.lineTo(15, 25); // Flag waves to the right
                flag.lineTo(0, 20); // Back to pole
                flag.lineTo(0, 30); // Close the triangle
                flag.fill(this.getTeamColor(buildingData.team));
                
                // Line from building to rally point
                const line = new PIXI.Graphics();
                line.moveTo(buildingData.x, buildingData.y);
                line.lineTo(buildingData.rallyPoint.x, buildingData.rallyPoint.y);
                line.stroke({ width: 2, color: this.getTeamColor(buildingData.team), alpha: 0.5 });
                
                rallyContainer.addChild(flagPole);
                rallyContainer.addChild(flag);
                
                buildingContainer.rallyPointGraphics = rallyContainer;
                buildingContainer.rallyPointLine = line;
                this.gameContainer.addChild(line); // Add line to game container (not building container)
                this.gameContainer.addChild(rallyContainer);
            }
            
            // Update rally point position
            buildingContainer.rallyPointGraphics.position.set(buildingData.rallyPoint.x, buildingData.rallyPoint.y);
            buildingContainer.rallyPointGraphics.visible = (buildingData.ownerId === this.myPlayerId);
            
            // Update line
            if (buildingContainer.rallyPointLine) {
                buildingContainer.rallyPointLine.clear();
                buildingContainer.rallyPointLine.moveTo(buildingData.x, buildingData.y);
                buildingContainer.rallyPointLine.lineTo(buildingData.rallyPoint.x, buildingData.rallyPoint.y);
                buildingContainer.rallyPointLine.stroke({ width: 2, color: this.getTeamColor(buildingData.team), alpha: 0.5 });
                buildingContainer.rallyPointLine.visible = (buildingData.ownerId === this.myPlayerId);
            }
        } else {
            // Hide rally point if it doesn't exist or building can't produce units
            if (buildingContainer.rallyPointGraphics) {
                buildingContainer.rallyPointGraphics.visible = false;
            }
            if (buildingContainer.rallyPointLine) {
                buildingContainer.rallyPointLine.visible = false;
            }
        }
        
        // Update shield visualization for SHIELD_GENERATOR
        if (buildingData.type === 'SHIELD_GENERATOR') {
            if (!buildingContainer.shieldGraphics) {
                const shieldGraphics = new PIXI.Graphics();
                buildingContainer.addChild(shieldGraphics);
                buildingContainer.shieldGraphics = shieldGraphics;
            }
            
            const shield = buildingContainer.shieldGraphics;
            shield.clear();
            
            // Only show shield if active and not under construction
            if (buildingData.shieldActive && !buildingData.underConstruction) {
                shield.circle(0, 0, buildingData.shieldRadius);
                shield.stroke({ width: 3, color: this.getTeamColor(buildingData.team), alpha: 0.4 });
                shield.fill({ color: this.getTeamColor(buildingData.team), alpha: 0.1 });
            }
        }
        
        // Update garrison label (for bunkers)
        if (buildingContainer.garrisonLabel) {
            if (buildingData.garrisonCount > 0) {
                buildingContainer.garrisonLabel.text = `[${buildingData.garrisonCount}/${buildingData.maxGarrisonCapacity}]`;
                buildingContainer.garrisonLabel.visible = true;
            } else {
                buildingContainer.garrisonLabel.visible = false;
            }
        }
        
        if (buildingContainer.airfieldBerthLabel) {
            const cap = buildingData.aircraftBerthCapacity || 4;
            const used = (buildingData.housedUnits && buildingData.housedUnits.length) || 0;
            buildingContainer.airfieldBerthLabel.text = `[${used}/${cap}]`;
            buildingContainer.airfieldBerthLabel.visible = buildingData.type === 'AIRFIELD';
        }
        
        this.updateBuildingLowPowerOverlay(buildingContainer, buildingData);
        
        // Store data
        buildingContainer.buildingData = buildingData;
    }
    
    /**
     * Spark / icon above worker, medic, engineer when supportActivity is set by server.
     */
    updateSupportWorkVisual(container, unitData) {
        const act = unitData.supportActivity;
        if (!act) {
            if (container.supportWorkGfx) {
                container.supportWorkGfx.visible = false;
            }
            return;
        }
        if (!container.supportWorkGfx) {
            const g = new PIXI.Graphics();
            g.zIndex = 10;
            container.sortableChildren = true;
            container.addChild(g);
            container.supportWorkGfx = g;
        }
        const g = container.supportWorkGfx;
        g.visible = true;
        g.clear();
        const off = container.healthBarOffset || 28;
        const t = performance.now() / 1000;
        const colors = {
            BUILD: 0xffaa44,
            MINE: 0xffdd44,
            CARRY: 0xcc8844,
            HEAL: 0x44ff88,
            REPAIR: 0x44ccff
        };
        const col = colors[act] || 0xffffff;
        const pulse = 0.55 + 0.45 * Math.sin(t * 8);
        for (let i = 0; i < 5; i++) {
            const ang = t * 4 + (i / 5) * Math.PI * 2;
            const r = 6 + pulse * 2;
            const px = Math.cos(ang) * r;
            const py = Math.sin(ang) * r - off - 4;
            g.circle(px, py, 2 + pulse * 0.5);
            g.fill({ color: col, alpha: 0.75 });
        }
        g.circle(0, -off - 4, 3.5);
        g.stroke({ width: 1.5, color: col, alpha: 0.9 });
    }
    
    /**
     * Flashing power warning on the player's own buildings when faction has low power.
     */
    updateBuildingLowPowerOverlay(buildingContainer, buildingData) {
        const low = this.myFaction && this.myFaction.hasLowPower
            && buildingData.ownerId === this.myPlayerId
            && buildingData.active !== false;
        if (!low) {
            if (buildingContainer.lowPowerOverlay) {
                buildingContainer.lowPowerOverlay.visible = false;
            }
            return;
        }
        if (!buildingContainer.lowPowerOverlay) {
            const g = new PIXI.Graphics();
            g.zIndex = 20;
            buildingContainer.sortableChildren = true;
            buildingContainer.addChild(g);
            buildingContainer.lowPowerOverlay = g;
        }
        const g = buildingContainer.lowPowerOverlay;
        g.visible = true;
        g.clear();
        const top = -(buildingContainer.typeInfo?.size || 40) - 18;
        const flash = 0.4 + 0.6 * Math.abs(Math.sin(performance.now() / 180));
        g.moveTo(0, top);
        g.lineTo(-5, top - 10);
        g.lineTo(3, top - 8);
        g.lineTo(-4, top - 22);
        g.stroke({ width: 3, color: 0xffee44, alpha: flash });
    }
    
    createBuildingGraphics(buildingData) {
        const container = new PIXI.Container();
        
        // Get building type info with unique shapes, sizes, and orientations
        const buildingTypes = {
            'HEADQUARTERS': { sides: 8, size: 80, color: 0xFFD700, rotation: 0 },
            'REFINERY': { sides: 6, size: 50, color: 0x808080, rotation: Math.PI / 6 },
            'BARRACKS': { sides: 4, size: 45, color: 0x8B4513, rotation: Math.PI / 4 },
            'POWER_PLANT': { sides: 6, size: 40, color: 0xFFFF00, rotation: 0 },
            'FACTORY': { sides: 4, size: 55, color: 0x696969, rotation: 0 },
            'RESEARCH_LAB': { sides: 6, size: 50, color: 0x00CED1, rotation: Math.PI / 6 },
            'TECH_CENTER': { sides: 8, size: 60, color: 0x4169E1, rotation: Math.PI / 8 },
            'TURRET': { sides: 5, size: 25, color: 0xFF4500, rotation: 0 },
            'ROCKET_TURRET': { sides: 6, size: 25, color: 0xFF6347, rotation: 0 },
            'FLAK_TURRET': { sides: 6, size: 25, color: 0xA0A0A0, rotation: 0 },
            'LASER_TURRET': { sides: 8, size: 25, color: 0x00FFFF, rotation: Math.PI / 8 },
            'SHIELD_GENERATOR': { sides: 6, size: 30, color: 0x00BFFF, rotation: 0 },
            'BANK': { sides: 8, size: 35, color: 0xFFD700, rotation: Math.PI / 8 },
            'BUNKER': { sides: 4, size: 40, color: 0x556B2F, rotation: Math.PI / 4 },
            'SANDSTORM_GENERATOR': { sides: 6, size: 45, color: 0xDEB887, rotation: 0 },
            'QUANTUM_NEXUS': { sides: 8, size: 50, color: 0x9370DB, rotation: Math.PI / 8 },
            'PHOTON_SPIRE': { sides: 6, size: 48, color: 0x00FF00, rotation: Math.PI / 6 },
            'ANDROID_FACTORY': { sides: 8, size: 55, color: 0x4B0082, rotation: Math.PI / 8 },
            'COMMAND_CITADEL': { sides: 8, size: 55, color: 0x4169E1, rotation: 0 },
            'TEMPEST_SPIRE': { sides: 8, size: 45, color: 0x4682B4, rotation: 0 },
            // Air unit production
            'AIRFIELD': { sides: 8, size: 60, color: 0x708090, rotation: 0 },
            // World radius for placement / preview comes from gameInitialization (buildingInfo + buildingTypes).size;
            // Values here are ~visual scale for first paint / selection chrome when vertices are not used yet.
            'STRIKE_RELAY': { sides: 6, size: 42, color: 0xCD853F, rotation: 0 },
            'SATCOM_ARRAY': { sides: 8, size: 40, color: 0x6495ED, rotation: Math.PI / 8 },
            'CARPET_PAD': { sides: 4, size: 43, color: 0x556B2F, rotation: 0 },
            'NUKE_SILO': { sides: 8, size: 48, color: 0x8B0000, rotation: 0 },
            'JUMP_PAD': { sides: 4, size: 44, color: 0x4A708B, rotation: 0 },
        };
        
        const typeInfo = buildingTypes[buildingData.type] || { sides: 4, size: 50, color: 0xFFFFFF, rotation: 0 };
        
        // Create a rotating container for turret buildings
        const hasTurret = buildingData.type === 'TURRET' || buildingData.type === 'ROCKET_TURRET'
            || buildingData.type === 'FLAK_TURRET' || buildingData.type === 'LASER_TURRET';
        let rotatingContainer;
        
        if (hasTurret) {
            rotatingContainer = new PIXI.Container();
            container.addChild(rotatingContainer);
            container.rotatingContainer = rotatingContainer;
        }
        
        // Create polygon shape with rotation
        const shape = new PIXI.Graphics();
        this.drawPolygon(shape, typeInfo.sides, typeInfo.size, typeInfo.color, buildingData.team);
        shape.rotation = typeInfo.rotation; // Apply unique rotation per building type
        if (hasTurret) {
            rotatingContainer.addChild(shape);
        } else {
            container.addChild(shape);
        }
        
        // Store references for dynamic updates
        container.shapeGraphics = shape;
        container.typeInfo = typeInfo;
        
        // Add decorative elements for certain buildings
        this.addBuildingDecorations(container, buildingData.type, typeInfo);
        
        // Add turret barrel for TURRET buildings
        if (hasTurret) {
            const barrel = new PIXI.Graphics();
            const barrelLength = typeInfo.size * 1.2;
            const barrelWidth = typeInfo.size * 0.2;
            
            // Draw barrel as a rectangle pointing right
            barrel.rect(0, -barrelWidth / 2, barrelLength, barrelWidth);
            barrel.fill({ color: 0x606060 });
            barrel.stroke({ width: 2, color: 0x000000 });
            
            // Add muzzle tip
            barrel.circle(barrelLength, 0, barrelWidth * 0.7);
            barrel.fill({ color: 0x303030 });
            barrel.stroke({ width: 2, color: 0x000000 });
            
            rotatingContainer.addChild(barrel);
            container.turretBarrel = barrel;
        }
        
        // Building type letter / short label (server-authoritative via gameInitialization.buildingTypes)
        const labelText = (this.buildingTypes && this.buildingTypes[buildingData.type] && this.buildingTypes[buildingData.type].label)
            ? this.buildingTypes[buildingData.type].label
            : '?';
        const label = new PIXI.Text(labelText, {
            fontFamily: 'Arial',
            fontSize: typeInfo.size * 0.6,
            fontWeight: 'bold',
            fill: 0xFFFFFF,
            stroke: 0x000000,
            strokeThickness: 3
        });
        label.anchor.set(0.5, 0.5);
        label.scale.y = -1; // Flip vertically to match game coordinate system
        container.addChild(label);
        
        // Create health bar (fixed at top of building)
        const healthBarOffset = typeInfo.size + 10;
        const healthBar = new PIXI.Graphics();
        healthBar.rect(-40, -healthBarOffset, 80, 5);
        healthBar.fill(0x00FF00);
        container.addChild(healthBar);
        container.healthBar = healthBar;
        container.healthBarOffset = healthBarOffset;
        
        // Create construction bar (just below health bar)
        const constructionBar = new PIXI.Graphics();
        constructionBar.visible = false;
        container.addChild(constructionBar);
        container.constructionBar = constructionBar;
        container.constructionBarOffset = healthBarOffset - 10;
        
        // Create production bar (below construction bar)
        const productionBar = new PIXI.Graphics();
        productionBar.visible = false;
        container.addChild(productionBar);
        container.productionBar = productionBar;
        container.productionBarOffset = healthBarOffset - 20;
        
        // Create selection circle
        const selectionCircle = new PIXI.Graphics();
        selectionCircle.circle(0, 0, typeInfo.size + 8);
        selectionCircle.stroke({ width: 3, color: 0xFFFF00 });
        selectionCircle.visible = false;
        container.addChild(selectionCircle);
        container.selectionCircle = selectionCircle;
        
        // Create garrison indicator (for bunkers)
        if (buildingData.type === 'BUNKER') {
            const garrisonLabel = new PIXI.Text('', {
                fontFamily: 'Arial',
                fontSize: 14,
                fill: 0xFFFFFF,
                stroke: { color: 0x000000, width: 2 }
            });
            garrisonLabel.anchor.set(0.5, 0.5);
            garrisonLabel.scale.y = -1; // Flip vertically
            garrisonLabel.y = typeInfo.size + 25; // Below building
            garrisonLabel.visible = false;
            container.addChild(garrisonLabel);
            container.garrisonLabel = garrisonLabel;
        }
        
        if (buildingData.type === 'AIRFIELD') {
            const berthLabel = new PIXI.Text('', {
                fontFamily: 'Arial',
                fontSize: 14,
                fill: 0xFFFFFF,
                stroke: { color: 0x000000, width: 2 }
            });
            berthLabel.anchor.set(0.5, 0.5);
            berthLabel.scale.y = -1;
            berthLabel.y = typeInfo.size + 25;
            berthLabel.visible = false;
            container.addChild(berthLabel);
            container.airfieldBerthLabel = berthLabel;
        }
        
        return container;
    }
    
    // Resource deposits removed - obstacles now contain harvestable resources
    
    updateObstacle(obstacleData) {
        let obstacleContainer = this.obstacles.get(obstacleData.id);
        
        if (!obstacleContainer) {
            // Create new obstacle
            obstacleContainer = this.createObstacleGraphics(obstacleData);
            this.obstacles.set(obstacleData.id, obstacleContainer);
            this.gameContainer.addChild(obstacleContainer);
            
            // Obstacles render below buildings
            obstacleContainer.zIndex = -1;
        }
        
        // Update position
        obstacleContainer.position.set(obstacleData.x, obstacleData.y);
        
        // Update health bar for destructible obstacles
        if (obstacleData.destructible && obstacleContainer.healthBar) {
            const healthPercent = obstacleData.health / obstacleData.maxHealth;
            
            // Hide health bar if at full health
            if (healthPercent >= 1.0) {
                obstacleContainer.healthBar.visible = false;
            } else {
                obstacleContainer.healthBar.visible = true;
                const barWidth = obstacleData.size * 1.5;
                const offset = obstacleData.size + 10;
                obstacleContainer.healthBar.clear();
                obstacleContainer.healthBar.rect(-barWidth / 2, -offset, barWidth * healthPercent, 5);
                obstacleContainer.healthBar.fill(this.getHealthColor(healthPercent));
            }
        }
        
        // Store data
        obstacleContainer.obstacleData = obstacleData;
    }
    
    /**
     * Update only dynamic obstacle properties (health, resources)
     * Static data comes from initialization
     */
    updateObstacleDynamic(updateData) {
        const obstacleContainer = this.obstacles.get(updateData.id);
        if (!obstacleContainer) {
            console.warn('Received update for unknown obstacle:', updateData.id);
            return;
        }
        
        const staticData = this.obstaclesStatic.get(updateData.id);
        if (!staticData) {
            console.warn('No static data for obstacle:', updateData.id);
            return;
        }
        
        // Merge static and dynamic data
        const fullData = { ...staticData, ...updateData };
        
        // Update health bar for destructible obstacles
        if (fullData.destructible && obstacleContainer.healthBar) {
            const healthPercent = fullData.health / fullData.maxHealth;
            
            // Hide health bar if at full health
            if (healthPercent >= 1.0) {
                obstacleContainer.healthBar.visible = false;
            } else {
                obstacleContainer.healthBar.visible = true;
                const barWidth = fullData.size * 1.5;
                const offset = fullData.size + 10;
                obstacleContainer.healthBar.clear();
                obstacleContainer.healthBar.rect(-barWidth / 2, -offset, barWidth * healthPercent, 5);
                obstacleContainer.healthBar.fill(this.getHealthColor(healthPercent));
            }
        }
        
        // Update resource indicator for harvestable obstacles
        if (fullData.harvestable && obstacleContainer.resourceIndicator) {
            const resourcePercent = fullData.resourcePercent || 1.0;
            // Could update visual indicator here if needed
        }
        
        // Store merged data
        obstacleContainer.obstacleData = fullData;
    }
    
    createObstacleGraphics(obstacleData) {
        const container = new PIXI.Container();
        const shape = new PIXI.Graphics();
        
        // Different colors for harvestable, destructible, and indestructible obstacles
        let fillColor, strokeColor;
        if (obstacleData.harvestable) {
            // Harvestable obstacles: greenish/gold color (contains resources)
            fillColor = 0x9ACD32; // Yellow-green (resource-rich)
            strokeColor = this.darkenColor(fillColor, 0.6); // Darken for outline
        } else if (obstacleData.destructible) {
            // Destructible obstacles: brownish/tan color (like rocks that can be broken)
            fillColor = 0x8B7355; // Medium brown
            strokeColor = this.darkenColor(fillColor, 0.6); // Darken for outline
        } else {
            // Indestructible obstacles: use biome color (darker, more solid looking)
            fillColor = this.obstacleColor;
            strokeColor = this.darkenColor(fillColor, 0.6); // Darken for outline
        }
        
        // Draw obstacle using vertices from physics body (or static data)
        const staticData = this.obstaclesStatic?.get(obstacleData.id);
        const vertices = obstacleData.vertices || staticData?.vertices;
        
        if (vertices && vertices.length > 0) {
            // Check if it's multi-fixture format (array of fixtures)
            if (Array.isArray(vertices[0]) && Array.isArray(vertices[0][0])) {
                // Multi-fixture: draw each fixture
                for (const fixtureVertices of vertices) {
                    if (fixtureVertices.length > 0) {
                        this.drawPhysicsPolygon(shape, fixtureVertices, fillColor, 0);
                    }
                }
                shape.stroke({ width: 2, color: strokeColor });
            } else if (Array.isArray(vertices[0]) && typeof vertices[0][0] === 'number') {
                // Single-fixture: [[x1, y1], [x2, y2], ...]
                this.drawPhysicsPolygon(shape, vertices, fillColor, 0);
                shape.stroke({ width: 2, color: strokeColor });
            } else {
                // Old format: [{x, y}, {x, y}, ...]
                shape.moveTo(vertices[0].x, vertices[0].y);
                for (let i = 1; i < vertices.length; i++) {
                    shape.lineTo(vertices[i].x, vertices[i].y);
                }
                shape.closePath();
                shape.fill(fillColor);
                shape.stroke({ width: 2, color: strokeColor });
            }
        } else {
            // Fallback: draw a circle
            console.warn('Obstacle missing vertices, using fallback circle:', obstacleData.id);
            shape.circle(0, 0, obstacleData.size || 20);
            shape.fill(fillColor);
            shape.stroke({ width: 2, color: strokeColor });
        }
        
        container.addChild(shape);
        
        // Add health bar for destructible obstacles
        if (obstacleData.destructible) {
            const healthBar = new PIXI.Graphics();
            const healthPercent = obstacleData.health / obstacleData.maxHealth;
            const barWidth = obstacleData.size * 1.5;
            const offset = obstacleData.size + 10;
            healthBar.rect(-barWidth / 2, -offset, barWidth * healthPercent, 5);
            healthBar.fill(this.getHealthColor(healthPercent));
            container.addChild(healthBar);
            container.healthBar = healthBar;
        }
        
        return container;
    }
    
    darkenColor(color, factor) {
        const r = (color >> 16) & 0xFF;
        const g = (color >> 8) & 0xFF;
        const b = color & 0xFF;
        
        return ((r * factor) << 16) | ((g * factor) << 8) | (b * factor);
    }
    
    updateProjectile(projectileData) {
        let projectileContainer = this.projectiles.get(projectileData.id);
        
        if (!projectileContainer) {
            // Create new projectile
            projectileContainer = this.createProjectileGraphics(projectileData);
            this.projectiles.set(projectileData.id, projectileContainer);
            this.gameContainer.addChild(projectileContainer);
            
            // Projectiles render above units (z-index 4)
            projectileContainer.zIndex = 4;
        }
        
        // Update position and rotation
        projectileContainer.position.set(projectileData.x, projectileData.y);
        projectileContainer.rotation = projectileData.rotation;
        
        // Animate trails
        if (projectileContainer.fireTrail) {
            // Animate rocket fire trail (flickering effect)
            const time = Date.now() / 100;
            projectileContainer.fireTrail.alpha = 0.8 + Math.sin(time) * 0.2;
        }
        
        if (projectileContainer.smokeTrail) {
            // Animate smoke trail (pulsing effect)
            const time = Date.now() / 200;
            projectileContainer.smokeTrail.alpha = 0.6 + Math.sin(time) * 0.2;
        }
        
        // Animate seeking missile effects
        if (projectileContainer.glow) {
            // Pulsing red glow for tracking missiles
            const time = Date.now() / 150;
            projectileContainer.glow.alpha = 0.2 + Math.sin(time) * 0.15;
        }
        
        if (projectileContainer.contrail) {
            // Flowing white contrails
            const time = Date.now() / 100;
            projectileContainer.contrail.alpha = 0.7 + Math.sin(time) * 0.2;
        }
        
        // Store data
        projectileContainer.projectileData = projectileData;
    }
    
    createProjectileGraphics(projectileData) {
        const container = new PIXI.Container();
        const size = projectileData.size;
        
        // Create projectile shape based on ordinance type
        const shape = new PIXI.Graphics();
        
        switch (projectileData.ordinance) {
            case 'ROCKET':
                // Check if this is a seeking missile
                const isSeeking = projectileData.bulletEffects && projectileData.bulletEffects.includes('SEEKING');
                
                // Seeking missiles have pulsing red glow (tracking indicator)
                if (isSeeking) {
                    const glow = new PIXI.Graphics();
                    glow.circle(0, 0, size * 2.5);
                    glow.fill({ color: 0xFF0000, alpha: 0.25 });
                    container.addChild(glow);
                    container.glow = glow;  // Store for animation
                }
                
                // Rocket: Cone-shaped with fire trail
                shape.moveTo(size * 2, 0);  // Nose (pointing right)
                shape.lineTo(-size, -size * 0.6);  // Top fin
                shape.lineTo(-size, size * 0.6);   // Bottom fin
                shape.closePath();
                shape.fill(0xFF4500);  // Orange-red body
                
                // Add metallic tip
                shape.moveTo(size * 2, 0);
                shape.lineTo(size * 0.5, -size * 0.3);
                shape.lineTo(size * 0.5, size * 0.3);
                shape.closePath();
                shape.fill(0xC0C0C0);  // Silver tip
                
                // Fire trail (animated particles will be added in update)
                const fireTrail = new PIXI.Graphics();
                for (let i = 0; i < 5; i++) {
                    const offset = -size - (i * size * 0.8);
                    const trailSize = size * (1 - i * 0.15);
                    const alpha = 1 - (i * 0.2);
                    fireTrail.circle(offset, 0, trailSize);
                    fireTrail.fill({ color: i % 2 === 0 ? 0xFF6600 : 0xFFAA00, alpha: alpha });
                }
                container.addChild(fireTrail);
                container.addChild(shape);
                container.fireTrail = fireTrail;  // Store reference for animation
                
                // Seeking missiles have white contrails (heat-seeking indicator)
                if (isSeeking) {
                    const contrail = new PIXI.Graphics();
                    for (let i = 0; i < 8; i++) {
                        const offset = -size * 1.8 - (i * size * 0.6);
                        const trailSize = size * 0.5 * (1 - i * 0.1);
                        const alpha = 0.8 - (i * 0.1);
                        contrail.circle(offset, 0, trailSize);
                        contrail.fill({ color: 0xFFFFFF, alpha: alpha });
                    }
                    container.addChild(contrail);
                    container.contrail = contrail;  // Store for animation
                }
                
                break;
                
            case 'GRENADE':
            case 'SHELL':
                // Grenade/Shell: Oval shape with smoke trail
                shape.ellipse(0, 0, size * 1.2, size * 0.8);
                shape.fill(projectileData.ordinance === 'GRENADE' ? 0x4A4A4A : 0x8B7355);
                
                // Add metallic band
                shape.rect(-size * 0.3, -size * 0.8, size * 0.6, size * 1.6);
                shape.fill(0x696969);
                
                // Smoke trail
                const smokeTrail = new PIXI.Graphics();
                for (let i = 0; i < 4; i++) {
                    const offset = -size - (i * size * 1.2);
                    const smokeSize = size * (0.6 + i * 0.2);
                    const alpha = 0.4 - (i * 0.1);
                    smokeTrail.circle(offset, 0, smokeSize);
                    smokeTrail.fill({ color: 0x808080, alpha: alpha });
                }
                container.addChild(smokeTrail);
                container.addChild(shape);
                container.smokeTrail = smokeTrail;
                break;
                
            case 'FLAK':
                // Flak Shell: Angular shell with red/orange tracer
                // Main body (darker gray, angular)
                shape.rect(-size * 1.2, -size * 0.9, size * 2, size * 1.8);
                shape.fill(0x505050);
                
                // Nose cone (pointed tip)
                shape.moveTo(size * 0.8, 0);
                shape.lineTo(size * 0.2, -size * 0.9);
                shape.lineTo(size * 0.2, size * 0.9);
                shape.closePath();
                shape.fill(0x606060);
                
                // Tracer glow (orange-red)
                shape.circle(0, 0, size * 1.3);
                shape.fill({ color: 0xFF4500, alpha: 0.4 });
                
                // Smoke trail (darker than regular shells)
                const flakSmoke = new PIXI.Graphics();
                for (let i = 0; i < 5; i++) {
                    const offset = -size * 1.5 - (i * size * 1.1);
                    const smokeSize = size * (0.5 + i * 0.15);
                    const alpha = 0.35 - (i * 0.07);
                    flakSmoke.circle(offset, 0, smokeSize);
                    flakSmoke.fill({ color: 0x404040, alpha: alpha });
                }
                container.addChild(flakSmoke);
                container.addChild(shape);
                container.smokeTrail = flakSmoke;
                break;
                
            case 'BULLET':
                // Bullet: Elongated bullet shape
                const bulletLength = size * 3;
                const bulletWidth = size * 0.8;
                
                // Bullet casing (brass)
                shape.rect(-bulletLength * 0.3, -bulletWidth, bulletLength * 0.6, bulletWidth * 2);
                shape.fill(0xB8860B);  // Dark golden rod
                
                // Bullet tip (lead/copper)
                shape.moveTo(bulletLength * 0.3, 0);
                shape.lineTo(bulletLength * 0.8, -bulletWidth * 0.6);
                shape.lineTo(bulletLength * 0.8, bulletWidth * 0.6);
                shape.closePath();
                shape.fill(0xCD7F32);  // Copper
                
                // Add slight glow
                shape.circle(0, 0, size);
                shape.fill({ color: 0xFFFF00, alpha: 0.3 });
                
                container.addChild(shape);
                break;
                
            case 'PLASMA':
                // Plasma: Glowing energy ball with corona
                // Outer glow
                shape.circle(0, 0, size * 1.5);
                shape.fill({ color: 0x00FFFF, alpha: 0.3 });
                
                // Middle layer
                shape.circle(0, 0, size * 1.1);
                shape.fill({ color: 0x00FFFF, alpha: 0.6 });
                
                // Core
                shape.circle(0, 0, size * 0.7);
                shape.fill(0xFFFFFF);
                
                container.addChild(shape);
                break;
                
            case 'DART':
                // Dart: Thin, sharp projectile
                const dartLength = size * 4;
                const dartWidth = size * 0.4;
                
                // Shaft
                shape.rect(-dartLength * 0.4, -dartWidth, dartLength * 0.8, dartWidth * 2);
                shape.fill(0xC0C0C0);
                
                // Tip
                shape.moveTo(dartLength * 0.4, 0);
                shape.lineTo(dartLength * 0.8, -dartWidth * 0.5);
                shape.lineTo(dartLength * 0.8, dartWidth * 0.5);
                shape.closePath();
                shape.fill(0x808080);
                
                // Fletching
                shape.moveTo(-dartLength * 0.4, 0);
                shape.lineTo(-dartLength * 0.6, -dartWidth * 2);
                shape.lineTo(-dartLength * 0.5, 0);
                shape.lineTo(-dartLength * 0.6, dartWidth * 2);
                shape.closePath();
                shape.fill(0xFF0000);
                
                container.addChild(shape);
                break;
                
            case 'FLAMETHROWER':
                // Flamethrower: Fire particle cluster
                for (let i = 0; i < 3; i++) {
                    const offset = (i - 1) * size * 0.5;
                    const fireSize = size * (1 + Math.random() * 0.5);
                    const fireColor = i === 1 ? 0xFFFF00 : (i === 0 ? 0xFF6600 : 0xFF0000);
                    shape.circle(offset, 0, fireSize);
                    shape.fill({ color: fireColor, alpha: 0.8 });
                }
                container.addChild(shape);
                break;
                
            default:
                // Default: Simple circle
                shape.circle(0, 0, size);
                shape.fill(0xFFFFFF);
                container.addChild(shape);
        }
        
        return container;
    }
    
    updateBeam(beamData) {
        let beamGraphics = this.beams.get(beamData.id);
        
        if (!beamGraphics) {
            // Create new beam
            beamGraphics = this.createBeamGraphics(beamData);
            this.beams.set(beamData.id, beamGraphics);
            this.gameContainer.addChild(beamGraphics);
            
            // Beams render above projectiles (z-index 5)
            beamGraphics.zIndex = 5;
        } else {
            // Update existing beam (fade out over time)
            const fadeProgress = beamData.elapsed / beamData.duration;
            beamGraphics.alpha = 1.0 - fadeProgress;
        }
        
        // Store data
        beamGraphics.beamData = beamData;
    }
    
    createBeamGraphics(beamData) {
        const container = new PIXI.Container();
        
        // Get beam color based on type with brighter, more saturated colors
        const colors = {
            'LASER': 0xFF3333,      // Bright red laser
            'PLASMA': 0x00FFFF,     // Cyan plasma
            'ION': 0xAA00FF,        // Bright purple ion
            'PARTICLE': 0xFFFF00    // Yellow particle
        };
        
        const color = colors[beamData.beamType] || 0xFF3333;
        
        // Create outer glow layer (widest, most transparent)
        const outerGlow = new PIXI.Graphics();
        outerGlow.moveTo(beamData.startX, beamData.startY);
        outerGlow.lineTo(beamData.endX, beamData.endY);
        outerGlow.stroke({ width: beamData.width * 4, color: color, alpha: 0.15 });
        outerGlow.filters = [new PIXI.BlurFilter(8)];
        container.addChild(outerGlow);
        
        // Create middle glow layer
        const middleGlow = new PIXI.Graphics();
        middleGlow.moveTo(beamData.startX, beamData.startY);
        middleGlow.lineTo(beamData.endX, beamData.endY);
        middleGlow.stroke({ width: beamData.width * 2, color: color, alpha: 0.4 });
        middleGlow.filters = [new PIXI.BlurFilter(4)];
        container.addChild(middleGlow);
        
        // Create core beam (bright, solid)
        const core = new PIXI.Graphics();
        core.moveTo(beamData.startX, beamData.startY);
        core.lineTo(beamData.endX, beamData.endY);
        core.stroke({ width: beamData.width, color: 0xFFFFFF, alpha: 0.9 }); // White core
        container.addChild(core);
        
        // Create inner colored beam (slightly wider than core)
        const innerBeam = new PIXI.Graphics();
        innerBeam.moveTo(beamData.startX, beamData.startY);
        innerBeam.lineTo(beamData.endX, beamData.endY);
        innerBeam.stroke({ width: beamData.width * 1.5, color: color, alpha: 0.7 });
        innerBeam.filters = [new PIXI.BlurFilter(1)];
        container.addChild(innerBeam);
        
        return container;
    }
    
    addBuildingDecorations(container, buildingType, typeInfo) {
        const decorations = new PIXI.Graphics();
        
        switch(buildingType) {
            case 'POWER_PLANT':
                // Add lightning bolt symbol
                decorations.moveTo(0, -typeInfo.size * 0.4);
                decorations.lineTo(-typeInfo.size * 0.15, 0);
                decorations.lineTo(typeInfo.size * 0.05, 0);
                decorations.lineTo(-typeInfo.size * 0.1, typeInfo.size * 0.4);
                decorations.lineTo(typeInfo.size * 0.2, -typeInfo.size * 0.1);
                decorations.lineTo(0, -typeInfo.size * 0.1);
                decorations.closePath();
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                break;
                
            case 'RESEARCH_LAB':
                // Add atom symbol (circles)
                decorations.circle(0, 0, typeInfo.size * 0.15);
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                decorations.circle(-typeInfo.size * 0.25, 0, typeInfo.size * 0.1);
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                decorations.circle(typeInfo.size * 0.25, 0, typeInfo.size * 0.1);
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                decorations.circle(0, -typeInfo.size * 0.25, typeInfo.size * 0.1);
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                break;

            case 'TECH_CENTER':
                // Add star pattern
                for (let i = 0; i < 4; i++) {
                    const angle = (i * Math.PI / 2);
                    const x = Math.cos(angle) * typeInfo.size * 0.3;
                    const y = Math.sin(angle) * typeInfo.size * 0.3;
                    decorations.circle(x, y, typeInfo.size * 0.08);
                    decorations.fill({ color: 0x000000, alpha: 0.3 });
                }
                decorations.circle(0, 0, typeInfo.size * 0.12);
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                break;
                
            case 'FACTORY':
                // Add gear teeth pattern on edges
                const teethCount = 6;
                const teethSize = typeInfo.size * 0.15;
                for (let i = 0; i < teethCount; i++) {
                    const angle = (i * 2 * Math.PI / teethCount);
                    const x = Math.cos(angle) * typeInfo.size * 0.7;
                    const y = Math.sin(angle) * typeInfo.size * 0.7;
                    decorations.rect(x - teethSize/2, y - teethSize/2, teethSize, teethSize);
                    decorations.fill({ color: 0x000000, alpha: 0.3 });
                }
                break;
                
            case 'BARRACKS':
                // Add door rectangle
                decorations.rect(-typeInfo.size * 0.2, typeInfo.size * 0.3, typeInfo.size * 0.4, typeInfo.size * 0.3);
                decorations.fill({ color: 0x000000, alpha: 0.4 });
                break;
                
            case 'REFINERY':
                // Add pipes (horizontal lines)
                for (let i = -1; i <= 1; i++) {
                    decorations.rect(-typeInfo.size * 0.3, i * typeInfo.size * 0.15, typeInfo.size * 0.6, typeInfo.size * 0.08);
                    decorations.fill({ color: 0x000000, alpha: 0.2 });
                }
                break;
                
            case 'TEMPEST_SPIRE':
                // Add weather sensor array (top antenna)
                decorations.rect(-typeInfo.size * 0.4, -typeInfo.size * 0.7, typeInfo.size * 0.8, typeInfo.size * 0.15);
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                
                // Add left antenna (diagonal line)
                decorations.moveTo(-typeInfo.size * 0.6, -typeInfo.size * 0.3);
                decorations.lineTo(-typeInfo.size * 0.45, -typeInfo.size * 0.5);
                decorations.stroke({ width: 3, color: 0x000000, alpha: 0.3 });
                
                // Add right antenna (diagonal line)
                decorations.moveTo(typeInfo.size * 0.6, -typeInfo.size * 0.3);
                decorations.lineTo(typeInfo.size * 0.45, -typeInfo.size * 0.5);
                decorations.stroke({ width: 3, color: 0x000000, alpha: 0.3 });
                
                // Add radar dishes (small circles at antenna ends)
                decorations.circle(-typeInfo.size * 0.6, -typeInfo.size * 0.3, typeInfo.size * 0.08);
                decorations.fill({ color: 0x000000, alpha: 0.4 });
                decorations.circle(typeInfo.size * 0.6, -typeInfo.size * 0.3, typeInfo.size * 0.08);
                decorations.fill({ color: 0x000000, alpha: 0.4 });
                break;
        }
        
        container.addChild(decorations);
    }
    
    drawPolygon(graphics, sides, radius, fillColor, team) {
        // Team colors
        const teamColors = [
            0xFFFFFF, // No team (white)
            0xFF0000, // Team 1 (red)
            0x0000FF, // Team 2 (blue)
            0x00FF00, // Team 3 (green)
            0xFFFF00  // Team 4 (yellow)
        ];
        
        const strokeColor = teamColors[team] || 0xFFFFFF;
        
        // Draw polygon
        graphics.poly(this.getPolygonPoints(sides, radius));
        graphics.fill(fillColor);
        graphics.stroke({ width: 2, color: strokeColor });
    }
    
    drawPhysicsPolygon(graphics, vertices, fillColor, team) {
        // Convert vertices array [[x1, y1], [x2, y2], ...] to flat array [x1, y1, x2, y2, ...]
        const points = [];
        for (const vertex of vertices) {
            points.push(vertex[0], vertex[1]);
        }
        
        // Draw polygon from physics body vertices
        graphics.poly(points);
        graphics.fill(fillColor);
        // Note: Stroke is applied by caller for more flexibility
    }
    
    drawPhysicsPolygonOutline(graphics, vertices, fillColor, team) {
        // Team colors
        const teamColors = [
            0xFFFFFF, // No team (white)
            0xFF0000, // Team 1 (red)
            0x0000FF, // Team 2 (blue)
            0x00FF00, // Team 3 (green)
            0xFFFF00  // Team 4 (yellow)
        ];
        
        const strokeColor = teamColors[team] || 0xFFFFFF;
        
        // Convert vertices array [[x1, y1], [x2, y2], ...] to flat array [x1, y1, x2, y2, ...]
        const points = [];
        for (const vertex of vertices) {
            points.push(vertex[0], vertex[1]);
        }
        
        // Draw polygon with dotted outline (no fill) using physics vertices
        graphics.poly(points);
        graphics.stroke({ 
            width: 3, 
            color: strokeColor, 
            alpha: 0.8,
            cap: 'round',
            join: 'round',
            // Create dashed line effect
            dashArray: [10, 5]
        });
        
        // Add semi-transparent fill to show it's under construction
        graphics.poly(points);
        graphics.fill({ color: fillColor, alpha: 0.2 });
    }
    
    drawPolygonOutline(graphics, sides, radius, fillColor, team) {
        // Team colors
        const teamColors = [
            0xFFFFFF, // No team (white)
            0xFF0000, // Team 1 (red)
            0x0000FF, // Team 2 (blue)
            0x00FF00, // Team 3 (green)
            0xFFFF00  // Team 4 (yellow)
        ];
        
        const strokeColor = teamColors[team] || 0xFFFFFF;
        
        // Draw polygon with dotted outline (no fill)
        graphics.poly(this.getPolygonPoints(sides, radius));
        graphics.stroke({ 
            width: 3, 
            color: strokeColor, 
            alpha: 0.8,
            cap: 'round',
            join: 'round',
            // Create dashed line effect
            dashArray: [10, 5]
        });
        
        // Add semi-transparent fill to show it's under construction
        graphics.poly(this.getPolygonPoints(sides, radius));
        graphics.fill({ color: fillColor, alpha: 0.2 });
    }
    
    getPolygonPoints(sides, radius) {
        const points = [];
        for (let i = 0; i < sides; i++) {
            const angle = (Math.PI * 2 * i) / sides;
            points.push({
                x: Math.cos(angle) * radius,
                y: Math.sin(angle) * radius
            });
        }
        return points;
    }
    
    getHealthColor(healthPercent) {
        if (healthPercent > 0.6) return 0x00FF00; // Green
        if (healthPercent > 0.3) return 0xFFFF00; // Yellow
        return 0xFF0000; // Red
    }
    
    getTeamColor(teamNumber) {
        const teamColors = {
            1: 0x0000FF, // Blue
            2: 0xFF0000, // Red
            3: 0x00FF00, // Green
            4: 0xFFFF00  // Yellow
        };
        return teamColors[teamNumber] || 0xFFFFFF; // Default to white
    }
    
    updateResourceDisplay() {
        const d = this.dom;
        if (this.myFaction) {
            // Store credits for easy access
            this.myMoney = this.myFaction.credits || 0;

            if (d.playerInfo) {
                d.playerInfo.textContent = `Team: ${this.myTeam}`;
            }
            if (d.creditsValue) {
                d.creditsValue.textContent = this.myFaction.credits;
            }
            const rentMs = this.myFaction.armyUpkeepIntervalMs ?? this.armyUpkeepIntervalMs ?? 30000;
            const rentSec = rentMs / 1000;
            if (d.upkeepValue) {
                d.upkeepValue.textContent =
                    `${this.myFaction.currentUpkeep} / ${rentSec}s`;
            }

            // Update power display
            if (d.powerValue) {
                const powerText = `${this.myFaction.powerConsumed}/${this.myFaction.powerGenerated}`;
                d.powerValue.textContent = powerText;

                if (this.myFaction.hasLowPower) {
                    d.powerValue.style.color = '#ff4444';
                } else if (this.myFaction.powerGenerated - this.myFaction.powerConsumed < 20) {
                    d.powerValue.style.color = '#ffaa00';
                } else {
                    d.powerValue.style.color = '#00ff00';
                }
            }

            if (d.lowPowerBanner) {
                d.lowPowerBanner.style.display = this.myFaction.hasLowPower ? 'flex' : 'none';
            }
            const econ = d.hudEconomy || d.resourcePanel;
            if (econ) {
                if (this.myFaction.hasLowPower) {
                    econ.classList.add('low-power-critical');
                } else {
                    econ.classList.remove('low-power-critical');
                }
            }
        } else {
            if (d.lowPowerBanner) {
                d.lowPowerBanner.style.display = 'none';
            }
            const econ = d.hudEconomy || d.resourcePanel;
            if (econ) {
                econ.classList.remove('low-power-critical');
            }
        }
        this.updateCommandAbilitiesPanel();
    }
    
    updateUnitInfoPanel() {
        // If a building is selected, building detail panel owns the stack — do not overwrite unit DOM.
        if (this.selectedBuilding) {
            return;
        }

        const bip = this.dom?.buildingInfoPanel || document.getElementById('building-info-panel');
        if (bip) {
            bip.style.display = 'none';
        }

        const u = this._unitInfoEls;
        if (!u?.panel || !u.panel.isConnected
                || (u.singleInfo && !u.panel.contains(u.singleInfo))) {
            this.refreshUnitInfoDomRefs();
        }

        const panel = this._unitInfoEls?.panel;
        const singleInfo = this._unitInfoEls?.singleInfo;
        const multiInfo = this._unitInfoEls?.multiInfo;

        // Safety check - if elements don't exist, bail out
        if (!panel || !singleInfo || !multiInfo) {
            return;
        }
        
        // Get selected units
        const selectedUnits = [];
        this.units.forEach((container, id) => {
            if (container.unitData && container.unitData.selected && container.unitData.ownerId === this.myPlayerId) {
                selectedUnits.push(container.unitData);
            }
        });
        
        // Hide panel if no units selected
        if (selectedUnits.length === 0) {
            panel.style.display = 'none';
        } else if (selectedUnits.length === 1) {
            // Single unit selected
            panel.style.display = 'flex';
            singleInfo.style.display = 'block';
            multiInfo.style.display = 'none';
            
            const unit = selectedUnits[0];
            const els = this._unitInfoEls;
            const unitName = els.unitName;
            const unitHealth = els.unitHealth;
            const unitType = els.unitType;
            const unitHealthFill = els.unitHealthFill;
            const abilityDiv = els.abilityDiv;
            const abilityName = els.abilityName;

            // Update with null checks
            if (unitName) unitName.textContent = unit.type;
            if (unitHealth) unitHealth.textContent = `${Math.round(unit.health)}/${Math.round(unit.maxHealth)}`;
            if (unitType) unitType.textContent = unit.type;
            
            if (unitHealthFill) {
                const healthPercent = (unit.health / unit.maxHealth) * 100;
                unitHealthFill.style.width = healthPercent + '%';
            }
            
            // Show special ability if unit has one
            if (abilityDiv) {
                if (unit.specialAbility && unit.specialAbility !== 'NONE') {
                    abilityDiv.style.display = 'block';
                    if (abilityName) abilityName.textContent = unit.specialAbility;
                } else {
                    abilityDiv.style.display = 'none';
                }
            }
            
            // Show garrison info for APCs
            let garrisonDiv = document.getElementById('unit-garrison-info');
            if (!garrisonDiv) {
                garrisonDiv = document.createElement('div');
                garrisonDiv.id = 'unit-garrison-info';
                garrisonDiv.style.marginTop = '10px';
                singleInfo.appendChild(garrisonDiv);
            }
            
            if (unit.type === 'APC' && unit.garrisonCount !== undefined) {
                garrisonDiv.style.display = 'block';
                garrisonDiv.innerHTML = `
                    <div class="unit-stat">
                        <span>Garrison:</span><span>${unit.garrisonCount}/${unit.maxGarrisonCapacity || 3}</span>
                    </div>
                `;
                
                // Add ungarrison buttons if units are garrisoned
                if (unit.garrisonCount > 0) {
                    const ungarrisonButtons = document.createElement('div');
                    ungarrisonButtons.style.marginTop = '5px';
                    
                    const ungarrisonOne = document.createElement('button');
                    ungarrisonOne.className = 'build-button';
                    ungarrisonOne.textContent = 'Ungarrison One';
                    ungarrisonOne.onclick = () => this.ungarrisonUnit(unit.id, false);
                    ungarrisonButtons.appendChild(ungarrisonOne);
                    
                    const ungarrisonAll = document.createElement('button');
                    ungarrisonAll.className = 'build-button';
                    ungarrisonAll.textContent = 'Ungarrison All';
                    ungarrisonAll.style.marginLeft = '5px';
                    ungarrisonAll.onclick = () => this.ungarrisonUnit(unit.id, true);
                    ungarrisonButtons.appendChild(ungarrisonAll);
                    
                    garrisonDiv.appendChild(ungarrisonButtons);
                }
            } else {
                garrisonDiv.style.display = 'none';
            }
        } else if (selectedUnits.length > 1) {
            // Multiple units selected
            panel.style.display = 'flex';
            singleInfo.style.display = 'none';
            multiInfo.style.display = 'block';
            
            // Count units by type
            const unitCounts = {};
            selectedUnits.forEach(unit => {
                unitCounts[unit.type] = (unitCounts[unit.type] || 0) + 1;
            });
            
            // Display counts
            const countList = this._unitInfoEls.unitCountList;
            if (countList) {
                countList.innerHTML = '';
                for (const [type, count] of Object.entries(unitCounts)) {
                    const item = document.createElement('div');
                    item.className = 'unit-stat';
                    item.innerHTML = `<span>${type}:</span><span>${count}</span>`;
                    countList.appendChild(item);
                }
            }
        }
    }
    
    /**
     * Update fog of war visualization.
     * Shows darkened areas where player has no vision.
     * Uses a simple approach: just don't render the fog overlay at all for now.
     * Server-side fog of war already prevents seeing enemy units/buildings.
     */
    updateFogOfWar() {
        // Clear previous fog
        this.fogContainer.removeChildren();
        
        // NOTE: Fog of war is handled server-side by filtering out non-visible entities.
        // Client-side visual fog overlay is disabled for performance.
        // If you want to re-enable it, uncomment the code below.
        
        /*
        if (!this.myTeam) {
            return; // No team assigned yet
        }
        
        // Create fog overlay using a mask approach
        const fogGraphics = new PIXI.Graphics();
        
        // Draw full fog over entire map
        fogGraphics.rect(
            -this.worldBounds.width / 2,
            -this.worldBounds.height / 2,
            this.worldBounds.width,
            this.worldBounds.height
        );
        fogGraphics.fill({ color: 0x000000, alpha: 0.7 });
        
        // Cut out vision circles for friendly units and buildings
        const visionSources = [];
        
        // Add friendly units
        this.units.forEach(container => {
            const unitData = container.unitData;
            if (unitData && unitData.team === this.myTeam) {
                visionSources.push({ x: unitData.x, y: unitData.y });
            }
        });
        
        // Add friendly buildings
        this.buildings.forEach(container => {
            const buildingData = container.buildingData;
            if (buildingData && buildingData.team === this.myTeam) {
                visionSources.push({ x: buildingData.x, y: buildingData.y });
            }
        });
        
        // Create vision circles (cut out from fog)
        for (const source of visionSources) {
            fogGraphics.circle(source.x, source.y, this.visionRange);
            fogGraphics.cut();
        }
        
        this.fogContainer.addChild(fogGraphics);
        */
    }
    
    clampCameraToWorld() {
        const padding = 500 / this.camera.zoom;
        const halfWidth = this.worldBounds.width / 2;
        const halfHeight = this.worldBounds.height / 2;
        this.camera.x = Math.max(-halfWidth - padding, Math.min(halfWidth + padding, this.camera.x));
        this.camera.y = Math.max(-halfHeight - padding, Math.min(halfHeight + padding, this.camera.y));
    }
    
    update() {
        // Update camera with WASD (fixed inverted controls)
        const cameraSpeed = 10 / this.camera.zoom;
        if (this.keys['w'] || this.keys['W'] || this.keys['ArrowUp']) this.camera.y += cameraSpeed;
        if (this.keys['s'] || this.keys['S'] || this.keys['ArrowDown']) this.camera.y -= cameraSpeed;
        if (this.keys['a'] || this.keys['A'] || this.keys['ArrowLeft']) this.camera.x -= cameraSpeed;
        if (this.keys['d'] || this.keys['D'] || this.keys['ArrowRight']) this.camera.x += cameraSpeed;
        
        this.clampCameraToWorld();
        this.updateCameraTransform();
        
        // Update selection box visualization
        if (this.isSelecting && this.selectionStart) {
            this.selectionBoxGraphics.clear();
            const minX = Math.min(this.selectionStart.x, this.mouseWorldPos.x);
            const maxX = Math.max(this.selectionStart.x, this.mouseWorldPos.x);
            const minY = Math.min(this.selectionStart.y, this.mouseWorldPos.y);
            const maxY = Math.max(this.selectionStart.y, this.mouseWorldPos.y);
            
            this.selectionBoxGraphics.rect(minX, minY, maxX - minX, maxY - minY);
            this.selectionBoxGraphics.stroke({ width: 2, color: 0x00FF00 });
            this.selectionBoxGraphics.fill({ color: 0x00FF00, alpha: 0.2 });
        } else {
            this.selectionBoxGraphics.clear();
        }
        
        // Update build preview (throttled validity + redraw only when geometry/colors change).
        // Placement ring radius = getBuildingInfo(type).size from server faction buildingInfo (same as RTSGameManager buildingInfo.size / BuildingType.getSize()).
        if (this.buildMode && this.buildPreview) {
            const mx = this.mouseWorldPos.x;
            const my = this.mouseWorldPos.y;
            this.buildPreview.position.set(mx, my);

            const btype = this.buildingType;
            const pm = this._buildPreviewMouse;
            const dx = mx - pm.x;
            const dy = my - pm.y;
            const movedSq = dx * dx + dy * dy;
            const movedMuch = !Number.isFinite(pm.x) || movedSq > 9; // > 3 world units
            const typeChanged = this._buildPreviewCacheType !== btype;
            this._buildPreviewFrame = (this._buildPreviewFrame || 0) + 1;

            let isValid;
            if (typeChanged || movedMuch || (this._buildPreviewFrame % 3 === 0)) {
                isValid = this.isValidBuildLocation(this.mouseWorldPos, btype);
                this._buildPreviewValidity = isValid;
                this._buildPreviewCacheType = btype;
            } else {
                isValid = this._buildPreviewValidity;
            }
            pm.x = mx;
            pm.y = my;

            const buildingInfo = this.getBuildingInfo(btype);
            const sz = buildingInfo.size;
            const dr = this._buildPreviewDrawn;
            if (this.buildPreview.shapeGraphics
                    && (dr.valid !== isValid || dr.size !== sz || dr.type !== btype)) {
                this.buildPreview.shapeGraphics.clear();
                this.buildPreview.shapeGraphics.circle(0, 0, sz);
                this.buildPreview.shapeGraphics.fill({
                    color: isValid ? 0x00FF00 : 0xFF0000,
                    alpha: 0.3
                });
                this.buildPreview.shapeGraphics.stroke({
                    width: 2,
                    color: isValid ? 0xFFFFFF : 0xFF0000
                });
                dr.valid = isValid;
                dr.size = sz;
                dr.type = btype;
            }
        }

        // Command ability strike radius preview (world coords; Y flipped via gameContainer)
        if (this.commandAbilityTargetGraphics) {
            if (this.commandAbilityTargetingMode && this.pendingCommandAbilityType) {
                const cat = this.commandAbilityTypes && this.commandAbilityTypes[this.pendingCommandAbilityType];
                const rawR = cat && typeof cat.effectRadius === 'number' ? cat.effectRadius : 220;
                const r = rawR > 0 ? rawR : 220;
                const mx = this.mouseWorldPos.x;
                const my = this.mouseWorldPos.y;
                const g = this.commandAbilityTargetGraphics;
                g.clear();
                g.circle(mx, my, r);
                g.stroke({ width: 3, color: 0xff9933, alpha: 0.95 });
                g.circle(mx, my, 6);
                g.fill({ color: 0xffcc88, alpha: 0.9 });
            } else {
                this.commandAbilityTargetGraphics.clear();
            }
        }
    }
    
    onMouseDown(e) {
        const screenPos = { x: e.clientX, y: e.clientY };
        this.mouseWorldPos = this.screenToWorld(screenPos);
        
        
        if (e.button === 0) { // Left click
            if (this.buildMode) {
                this.placeBuilding(this.buildingType, this.mouseWorldPos);
                this.exitBuildMode();
            } else if (this.attackMoveMode) {
                // Handle attack-move
                this.sendInput({ 
                    attackMoveOrder: { x: this.mouseWorldPos.x, y: this.mouseWorldPos.y }
                });
                this.exitAttackMoveMode();
            } else if (this.sortieTargetingMode) {
                // Handle sortie targeting (bomber aircraft)
                this.issueSortieOrder(this.mouseWorldPos.x, this.mouseWorldPos.y);
            } else if (this.commandAbilityTargetingMode) {
                this.issueCommandAbilityAt(this.mouseWorldPos.x, this.mouseWorldPos.y);
            } else if (this.specialAbilityTargetingMode) {
                // Handle special ability targeting
                if (this.specialAbilityTargetType === 'unit') {
                    const clickedUnit = this.getUnitAtPosition(this.mouseWorldPos);
                    if (clickedUnit && clickedUnit.ownerId === this.myPlayerId) {
                        // Target selected - send heal command
                        this.sendInput({ 
                            activateSpecialAbility: true,
                            specialAbilityTargetUnit: clickedUnit.id
                        });
                    }
                } else if (this.specialAbilityTargetType === 'building') {
                    const clickedBuilding = this.getBuildingAtPosition(this.mouseWorldPos);
                    if (clickedBuilding && clickedBuilding.ownerId === this.myPlayerId) {
                        // Target selected - send repair command
                        this.sendInput({ 
                            activateSpecialAbility: true,
                            specialAbilityTargetBuilding: clickedBuilding.id
                        });
                    }
                }
                // Exit targeting mode
                this.exitSpecialAbilityTargetingMode();
            } else {
                // Check if clicking on a building first
                const clickedBuilding = this.getBuildingAtPosition(this.mouseWorldPos);
                if (clickedBuilding && clickedBuilding.ownerId === this.myPlayerId) {
                    // Select building
                    this.selectBuilding(clickedBuilding);
                } else {
                    // Start unit selection box (marquee never selects buildings)
                    this.clearBuildingSelectionClient();
                    this.isSelecting = true;
                    this.selectionStart = this.mouseWorldPos;
                }
            }
        } else if (e.button === 2) { // Right click — defer orders vs camera pan until mouseup
            if (this.buildMode) {
                this.exitBuildMode();
                return;
            }
            if (this.sortieTargetingMode) {
                this.exitSortieTargetingMode();
                this.showGameEvent('Sortie order cancelled', 'warning');
                return;
            }
            if (this.commandAbilityTargetingMode) {
                this.exitCommandAbilityTargetingMode();
                this.showGameEvent('Command ability cancelled', 'warning');
                return;
            }
            if (this.attackMoveMode) {
                this.exitAttackMoveMode();
                return;
            }
            if (this.specialAbilityTargetingMode) {
                this.exitSpecialAbilityTargetingMode();
                return;
            }
            this.rightButtonDown = true;
            this.rightDragPanActive = false;
            this.rightDownScreen = { x: e.clientX, y: e.clientY };
            this.lastRightPanScreen = { x: e.clientX, y: e.clientY };
            this.pendingRightClickWorld = { x: this.mouseWorldPos.x, y: this.mouseWorldPos.y };
            this.pendingRightForceAttack = e.metaKey || e.ctrlKey;
        }
    }
    
    onMouseMove(e) {
        const screenPos = { x: e.clientX, y: e.clientY };
        this.mouseWorldPos = this.screenToWorld(screenPos);
        
        if (this.rightButtonDown && this.rightDownScreen) {
            const dxs = e.clientX - this.rightDownScreen.x;
            const dys = e.clientY - this.rightDownScreen.y;
            if (!this.rightDragPanActive && (dxs * dxs + dys * dys) > 36) {
                this.rightDragPanActive = true;
                this.lastRightPanScreen = { x: e.clientX, y: e.clientY };
            }
            if (this.rightDragPanActive && this.lastRightPanScreen) {
                const dx = e.clientX - this.lastRightPanScreen.x;
                const dy = e.clientY - this.lastRightPanScreen.y;
                this.lastRightPanScreen = { x: e.clientX, y: e.clientY };
                const invZ = 1 / this.camera.zoom;
                this.camera.x += dx * invZ;
                this.camera.y -= dy * invZ;
                this.clampCameraToWorld();
                this.updateCameraTransform();
            }
        }
        
        // Update cursor based on modifier keys
        const forceAttackMode = e.metaKey || e.ctrlKey;
        if (this.rightDragPanActive) {
            this.app.canvas.style.cursor = 'grabbing';
        } else if (forceAttackMode && this.selectedUnits.size > 0) {
            this.app.canvas.style.cursor = 'crosshair'; // Attack cursor
        } else if (this.commandAbilityTargetingMode || this.sortieTargetingMode) {
            this.app.canvas.style.cursor = 'crosshair';
        } else if (this.buildMode) {
            this.app.canvas.style.cursor = 'cell'; // Build cursor
        } else {
            this.app.canvas.style.cursor = 'default';
        }
    }
    
    onMouseUp(e) {
        if (e.button === 0 && this.isSelecting) {
            this.finishSelection();
            this.isSelecting = false;
            this.selectionStart = null;
        }
        if (e.button === 2) {
            this.finishRightMouseInteraction();
        }
    }
    
    onWindowMouseUp(e) {
        if (e.button === 2) {
            this.finishRightMouseInteraction();
        }
    }
    
    finishRightMouseInteraction() {
        if (!this.rightButtonDown) {
            return;
        }
        const wasPan = this.rightDragPanActive;
        const world = this.pendingRightClickWorld;
        const forceAttack = this.pendingRightForceAttack;
        this.rightButtonDown = false;
        this.rightDragPanActive = false;
        this.rightDownScreen = null;
        this.lastRightPanScreen = null;
        this.pendingRightClickWorld = null;
        this.pendingRightForceAttack = false;
        if (wasPan || !world) {
            return;
        }
        if (this.selectedBuilding && this.selectedBuilding.canProduceUnits && this.selectedBuilding.ownerId === this.myPlayerId) {
            this.setRallyPoint(this.selectedBuilding.id, world);
        } else {
            this.issueOrder(world, forceAttack);
        }
    }
    
    onMouseWheel(e) {
        e.preventDefault();
        const zoomSpeed = 0.02;
        const delta = e.deltaY > 0 ? -zoomSpeed : zoomSpeed;
        this.camera.zoom = Math.max(0.2, Math.min(2.0, this.camera.zoom + delta));
    }
    
    onKeyDown(e) {
        this.keys[e.key] = true;
        
        // Hotkeys
        if (e.key === 'b' || e.key === 'B') {
            this.toggleBuildMenu();
        } else if (e.key === 'Escape') {
            if (this.sortieTargetingMode) {
                this.exitSortieTargetingMode();
                this.showGameEvent('Sortie order cancelled', 'warning');
            } else if (this.commandAbilityTargetingMode) {
                this.exitCommandAbilityTargetingMode();
                this.showGameEvent('Command ability cancelled', 'warning');
            } else if (this.specialAbilityTargetingMode) {
                this.exitSpecialAbilityTargetingMode();
            } else if (this.attackMoveMode) {
                this.exitAttackMoveMode();
            } else {
                this.exitBuildMode();
            }
        } else if (e.key === 't' || e.key === 'T') {
            // Special ability hotkey
            this.activateSpecialAbility();
        } else if (e.key === 'q' || e.key === 'Q') {
            // Attack-move hotkey
            this.enterAttackMoveMode();
        } else if (e.key === 'x' || e.key === 'X') {
            // Scatter hotkey - scatter selected units away from their center
            this.scatterSelectedUnits();
        } else if (e.key === 'u' || e.key === 'U') {
            // Ungarrison all from selected bunker or APC
            this.ungarrisonAllFromSelected();
        }
    }
    
    onKeyUp(e) {
        this.keys[e.key] = false;
    }
    
    screenToWorld(screenPos) {
        const localPos = this.gameContainer.toLocal(screenPos);
        return { x: localPos.x, y: localPos.y }; // toLocal already handles the transform
    }
    
    /**
     * Immediately clear unit selection (local state + visual indicators)
     * Used for instant UI feedback before server confirmation
     */
    clearUnitSelectionImmediate() {
        // Clear the local set
        this.selectedUnits.clear();
        this._lastUnitSelectClickTime = 0;
        this._lastUnitSelectClickId = null;

        // Hide all selection circles immediately
        this.units.forEach((container) => {
            if (container.selectionCircle) {
                container.selectionCircle.visible = false;
            }
        });
    }
    
    finishSelection() {
        // Calculate selection box size
        const minX = Math.min(this.selectionStart.x, this.mouseWorldPos.x);
        const maxX = Math.max(this.selectionStart.x, this.mouseWorldPos.x);
        const minY = Math.min(this.selectionStart.y, this.mouseWorldPos.y);
        const maxY = Math.max(this.selectionStart.y, this.mouseWorldPos.y);
        
        const boxWidth = maxX - minX;
        const boxHeight = maxY - minY;
        const clickThreshold = 5; // If box is smaller than this, treat as a click
        
        // Check if this was a click (small box) or a drag (large box)
        const isClick = boxWidth < clickThreshold && boxHeight < clickThreshold;
        
        if (isClick) {
            // Single click - check if we clicked on a unit
            let clickedUnit = null;
            let minDist = 30; // Click tolerance
            
            this.units.forEach((container, id) => {
                const unitData = container.unitData;
                if (unitData && unitData.ownerId === this.myPlayerId) {
                    const dist = Math.sqrt(
                        Math.pow(unitData.x - this.mouseWorldPos.x, 2) + 
                        Math.pow(unitData.y - this.mouseWorldPos.y, 2)
                    );
                    if (dist < minDist) {
                        minDist = dist;
                        clickedUnit = id;
                    }
                }
            });
            
            if (clickedUnit) {
                this.clearBuildingSelectionClient();
                const now = (typeof performance !== 'undefined' && performance.now)
                    ? performance.now()
                    : Date.now();
                const clickedContainer = this.units.get(clickedUnit);
                const clickedType = clickedContainer?.unitData?.type;
                let idsToSelect = [clickedUnit];

                if (clickedType &&
                    this._lastUnitSelectClickId === clickedUnit &&
                    (now - this._lastUnitSelectClickTime) <= this._doubleClickSelectSameTypeMs) {
                    idsToSelect = [];
                    this.units.forEach((container, id) => {
                        const ud = container.unitData;
                        if (ud && ud.ownerId === this.myPlayerId && ud.type === clickedType) {
                            idsToSelect.push(id);
                        }
                    });
                    this._lastUnitSelectClickTime = 0;
                    this._lastUnitSelectClickId = null;
                } else {
                    this._lastUnitSelectClickTime = now;
                    this._lastUnitSelectClickId = clickedUnit;
                }

                this.sendInput({ selectUnits: idsToSelect });
                this.checkAndShowBuildMenu(idsToSelect);
            } else {
                this.clearBuildingSelectionClient();
                // Clicked on empty space - deselect all units
                this.sendInput({ selectUnits: [] });
                this.clearUnitSelectionImmediate(); // Clear local state and hide visual indicators
                this.hideBuildMenu();
            }
        } else {
            // Drag selection — units only (never buildings; marquee is exclusive to units).
            this._lastUnitSelectClickTime = 0;
            this._lastUnitSelectClickId = null;
            this.clearBuildingSelectionClient();
            const selectedIds = [];
            this.units.forEach((container, id) => {
                const unitData = container.unitData;
                if (unitData && unitData.ownerId === this.myPlayerId) {
                    if (unitData.x >= minX && unitData.x <= maxX &&
                        unitData.y >= minY && unitData.y <= maxY) {
                        selectedIds.push(id);
                    }
                }
            });

            this.sendInput({ selectUnits: selectedIds });

            this.checkAndShowBuildMenu(selectedIds);
        }
    }

    checkAndShowBuildMenu(selectedIds) {
        let hasWorker = false;
        for (const id of selectedIds) {
            const container = this.units.get(id);
            if (container && container.unitData && container.unitData.type === 'WORKER') {
                hasWorker = true;
                break;
            }
        }
        if (hasWorker) {
            this.showBuildMenu();
        } else {
            this.hideBuildMenu();
        }
    }
    
    issueOrder(worldPos, forceAttack = false) {
        // If force attack mode (CMD/CTRL held), skip target detection and attack ground
        if (forceAttack) {
            this.sendInput({ forceAttackOrder: { x: worldPos.x, y: worldPos.y } });
            return;
        }
        
        // Check if clicking on a unit
        let targetUnit = null;
        let minDist = 50; // Click tolerance
        
        this.units.forEach((container, id) => {
            const unitData = container.unitData;
            if (unitData) {
                const dist = Math.sqrt(
                    Math.pow(unitData.x - worldPos.x, 2) + 
                    Math.pow(unitData.y - worldPos.y, 2)
                );
                
                // Get unit size from unit type info (size is not sent in every update)
                const typeInfo = this.unitTypes?.[unitData.type];
                const unitSize = typeInfo?.size || 15; // Default to 15 if not found

                if (dist < minDist && dist < unitSize + 10) {
                    minDist = dist;
                    targetUnit = unitData;
                }
            }
        });
        
        // Check if clicking on a building
        let targetBuilding = null;
        minDist = 100;
        
        this.buildings.forEach((container, id) => {
            const buildingData = container.buildingData;
            if (buildingData) {
                const dist = Math.sqrt(
                    Math.pow(buildingData.x - worldPos.x, 2) + 
                    Math.pow(buildingData.y - worldPos.y, 2)
                );
                if (dist < minDist && dist < buildingData.size + 10) {
                    minDist = dist;
                    targetBuilding = buildingData;
                }
            }
        });
        
        // Check if clicking on an obstacle (some are harvestable)
        let targetObstacle = null;
        let targetHarvestableObstacle = null;
        minDist = 100;
        
        this.obstacles.forEach((container, id) => {
            const obstacleData = container.obstacleData;
            if (obstacleData) {
                const dist = Math.sqrt(
                    Math.pow(obstacleData.x - worldPos.x, 2) + 
                    Math.pow(obstacleData.y - worldPos.y, 2)
                );
                if (dist < minDist && dist < obstacleData.size + 10) {
                    minDist = dist;
                    targetObstacle = obstacleData;
                    // Track if this obstacle is harvestable
                    if (obstacleData.harvestable) {
                        targetHarvestableObstacle = obstacleData;
                    }
                }
            }
        });
        
        // Issue appropriate command based on target
        if (targetUnit) {
            if (targetUnit.team !== this.myTeam) {
                // Attack enemy unit
                this.sendInput({ attackUnitOrder: targetUnit.id });
            } else if (targetUnit.type === 'APC' && this.hasInfantrySelected()) {
                // Garrison infantry into friendly APC
                this.sendInput({ garrisonOrder: targetUnit.id });
            } else {
                // Can't command other player's units, just move
                this.sendInput({ moveOrder: { x: worldPos.x, y: worldPos.y } });
            }
        } else if (targetBuilding) {
            if (targetBuilding.team !== this.myTeam) {
                // Attack enemy building
                this.sendInput({ attackBuildingOrder: targetBuilding.id });
            } else if (targetBuilding.underConstruction) {
                // Help construct friendly building (for workers)
                this.sendInput({ constructOrder: targetBuilding.id });
            } else if (targetBuilding.type === 'BUNKER' && this.hasInfantrySelected()) {
                // Garrison infantry into bunker
                this.sendInput({ garrisonOrder: targetBuilding.id });
            } else {
                // Move near friendly building
                this.sendInput({ moveOrder: { x: worldPos.x, y: worldPos.y } });
            }
        } else if (targetHarvestableObstacle) {
            // Harvest resources from harvestable obstacle (for workers)
            this.sendInput({ harvestOrder: targetHarvestableObstacle.id });
        } else if (targetObstacle) {
            // Can't harvest or mine this obstacle, just move near it
            this.sendInput({ moveOrder: { x: worldPos.x, y: worldPos.y } });
        } else {
            // Just move to location
            this.sendInput({ moveOrder: { x: worldPos.x, y: worldPos.y } });
        }
    }
    
    /**
     * Check if any selected units are infantry (can garrison)
     */
    hasInfantrySelected() {
        for (const unitId of this.selectedUnits) {
            const unitContainer = this.units.get(unitId);
            if (unitContainer && unitContainer.unitData) {
                const unitType = unitContainer.unitData.type;
                const typeInfo = this.unitTypes?.[unitType];
                if (typeInfo && typeInfo.category === 'INFANTRY') {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Ungarrison units from a bunker
     */
    ungarrisonUnit(buildingId, ungarrisonAll, unitId = null) {
        const payload = {
            ungarrisonBuildingId: buildingId,
            ungarrisonAll: !!ungarrisonAll
        };
        if (unitId != null && !ungarrisonAll) {
            payload.ungarrisonUnitId = unitId;
        }
        this.sendInput(payload);
    }
    
    enterBuildMode(buildingType) {
        this.buildMode = true;
        this.buildingType = buildingType;
        this._buildPreviewMouse = { x: NaN, y: NaN };
        this._buildPreviewCacheType = null;
        this._buildPreviewFrame = 0;
        this._buildPreviewDrawn = { valid: null, size: null, type: null };

        // Get building info
        const buildingInfo = this.getBuildingInfo(buildingType);
        
        // Create build preview
        this.buildPreview = new PIXI.Container();
        
        // Building shape
        const shape = new PIXI.Graphics();
        shape.circle(0, 0, buildingInfo.size);
        shape.fill({ color: 0x00FF00, alpha: 0.3 });
        shape.stroke({ width: 2, color: 0xFFFFFF });
        this.buildPreview.addChild(shape);
        this.buildPreview.shapeGraphics = shape;
        
        // Range indicator for defensive buildings (weapon range)
        const buildingTypeData = this.buildingTypes[buildingType];
        if (buildingTypeData?.weaponRange) {
            const rangeCircle = new PIXI.Graphics();
            rangeCircle.circle(0, 0, buildingTypeData.weaponRange);
            rangeCircle.stroke({ width: 1, color: 0xFF0000, alpha: 0.3 });
            this.buildPreview.addChild(rangeCircle);
        }
        
        // Range indicator for buildings with area effects (aura radius)
        if (buildingTypeData?.auraRadius) {
            const auraCircle = new PIXI.Graphics();
            auraCircle.circle(0, 0, buildingTypeData.auraRadius);
            auraCircle.stroke({ width: 1, color: 0x00FFFF, alpha: 0.4 }); // Cyan for auras
            this.buildPreview.addChild(auraCircle);
        }
        
        this.gameContainer.addChild(this.buildPreview);
        
        document.getElementById('build-menu').style.display = 'none';
    }
    
    exitBuildMode() {
        this.buildMode = false;
        this.buildingType = null;
        this._buildPreviewDrawn = { valid: null, size: null, type: null };

        if (this.buildPreview) {
            this.gameContainer.removeChild(this.buildPreview);
            this.buildPreview = null;
        }
    }
    
    exitSpecialAbilityTargetingMode() {
        this.specialAbilityTargetingMode = false;
        this.specialAbilityTargetType = null;
        document.body.style.cursor = 'default';
    }
    
    enterAttackMoveMode() {
        
        // Check if any selected units can attack
        let hasAttackUnit = false;
        for (const id of this.selectedUnits) {
            const unitContainer = this.units.get(id);
            if (unitContainer && unitContainer.unitData) {
                const unitType = unitContainer.unitData.type;
                // Workers, Medics, Engineers cannot attack
                if (unitType !== 'WORKER' && unitType !== 'MEDIC' && unitType !== 'ENGINEER') {
                    hasAttackUnit = true;
                    break;
                }
            }
        }

        if (hasAttackUnit) {
            this.attackMoveMode = true;
            document.body.style.cursor = 'crosshair';
        }
    }
    
    exitAttackMoveMode() {
        this.attackMoveMode = false;
        document.body.style.cursor = 'default';
    }
    
    scatterSelectedUnits() {
        if (this.selectedUnits.size > 0) {
            this.sendInput({ scatterCommand: true });
        }
    }
    
    ungarrisonAllFromSelected() {
        // Check if a bunker is selected
        if (this.selectedBuilding && this.selectedBuilding.type === 'BUNKER') {
            if (this.selectedBuilding.garrisonCount > 0) {
                this.ungarrisonUnit(this.selectedBuilding.id, true);
                this.showGameEvent('Ungarrisoning all units', 'info');
            }
            return;
        }
        
        // Check if an APC is selected
        for (const unitId of this.selectedUnits) {
            const unitContainer = this.units.get(unitId);
            if (unitContainer && unitContainer.unitData) {
                const unitData = unitContainer.unitData;
                if (unitData.type === 'APC' && unitData.garrisonCount > 0) {
                    this.ungarrisonUnit(unitData.id, true);
                    this.showGameEvent('Ungarrisoning all units from APC', 'info');
                    return; // Only ungarrison from first APC
                }
            }
        }
    }
    
    getBuildingInfo(buildingType) {
        const fallback = { size: 40, cost: 100, name: 'Building' };
        if (!this.myFactionData?.availableBuildings) {
            return fallback;
        }
        if (!this._buildingInfoByType) {
            this._buildingInfoByType = new Map();
            for (const b of this.myFactionData.availableBuildings) {
                this._buildingInfoByType.set(b.buildingType, b);
            }
        }
        return this._buildingInfoByType.get(buildingType) || fallback;
    }

    /** Squared distance between two points (avoids sqrt in hot paths). */
    _distSq(ax, ay, bx, by) {
        const dx = ax - bx;
        const dy = ay - by;
        return dx * dx + dy * dy;
    }
    
    isValidBuildLocation(worldPos, buildingType) {
        const buildingInfo = this.getBuildingInfo(buildingType);
        // World-space footprint radius from server (faction buildingInfo); must match RTSGameManager / BuildingType.
        const size = buildingInfo.size;

        // Check if too close to other buildings
        for (const [id, container] of this.buildings) {
            const building = container.buildingData;
            if (building) {
                const distSq = this._distSq(building.x, building.y, worldPos.x, worldPos.y);
                const minDist = size + building.size + 20; // 20 unit buffer
                const minDistSq = minDist * minDist;
                if (distSq < minDistSq) {
                    return false;
                }
            }
        }
        
        // Check if too close to obstacles (excluding world boundaries)
        const halfWidth = this.worldBounds?.width ? this.worldBounds.width / 2 : 2000;
        const halfHeight = this.worldBounds?.height ? this.worldBounds.height / 2 : 2000;

        for (const [id, container] of this.obstacles) {
            const obstacle = container.obstacleData;
            if (obstacle) {
                // Skip world boundary obstacles (they're at the edges and have huge size values)
                const isWorldBoundary = 
                    Math.abs(Math.abs(obstacle.x) - halfWidth) < 100 || // Near left/right edge
                    Math.abs(Math.abs(obstacle.y) - halfHeight) < 100;  // Near top/bottom edge
                
                if (isWorldBoundary) {
                    continue; // Skip boundary obstacles
                }
                
                const distSq = this._distSq(obstacle.x, obstacle.y, worldPos.x, worldPos.y);
                const minDist = size + obstacle.size + 10; // 10 unit buffer
                const minDistSq = minDist * minDist;
                
                if (distSq < minDistSq) {
                    return false;
                }
            }
        }
        if (Math.abs(worldPos.x) > halfWidth - size ||
            Math.abs(worldPos.y) > halfHeight - size) {
            return false;
        }
        return true;
    }
    
    placeBuilding(buildingType, worldPos) {
        // Validate placement
        if (!this.isValidBuildLocation(worldPos, buildingType)) {
            return;
        }
        
        // Send build order to server
        this.sendInput({
            buildOrder: buildingType,
            buildLocation: { x: worldPos.x, y: worldPos.y }
        });
    }
    
    toggleBuildMenu() {
        if (!this.selectionIncludesWorker()) {
            return;
        }
        const menu = document.getElementById('build-menu');
        if (!menu) {
            return;
        }
        const hidden = menu.style.display === 'none' || menu.style.display === '';
        menu.style.display = hidden ? 'flex' : 'none';
    }

    showBuildMenu() {
        const menu = document.getElementById('build-menu');
        if (menu) {
            menu.style.display = 'flex';
        }
    }
    
    hideBuildMenu() {
        const menu = document.getElementById('build-menu');
        if (menu) {
            menu.style.display = 'none';
        }
    }
    
    updateBuildMenuAvailability(buildings) {
        // If faction data not loaded yet, can't update availability
        if (!this.myFactionData || !this.myFactionData.availableBuildings) {
            return;
        }

        // Find player's buildings
        const myBuildings = buildings.filter(b => b.ownerId === this.myPlayerId && b.active && !b.underConstruction);
        const myBuildingTypes = new Set(myBuildings.map(b => b.type));

        // Get player's current credits (with safety checks)
        const myCredits = this.myFaction && this.myFaction.credits
            ? this.myFaction.credits
            : 0;

        const sortedTypes = [...myBuildingTypes].sort().join(',');
        const menuStateKey = `${myCredits}|${sortedTypes}`;
        if (menuStateKey === this._lastBuildMenuStateKey) {
            return;
        }
        this._lastBuildMenuStateKey = menuStateKey;

        // Only building-placement buttons in the worker build menu (not train / housed UI).
        document.querySelectorAll('#build-menu .build-button').forEach(button => {
            const buildingType = button.getAttribute('data-building');
            
            // Find building in faction data
            const buildingInfo = this.myFactionData.availableBuildings.find(b => b.buildingType === buildingType);
            
            if (!buildingInfo) {
                // Building not available for this faction - should not happen if menu is generated correctly
                button.style.display = 'none';
                return;
            }
            
            // Get tech requirements from building metadata (from API)
            const requiredBuildings = buildingInfo.techRequirements || [];
            const cost = buildingInfo.cost;
            
            // Check if all tech requirements are met
            let hasTech = true;
            let missingRequirements = [];
            
            for (const required of requiredBuildings) {
                if (!myBuildingTypes.has(required)) {
                    hasTech = false;
                    missingRequirements.push(this.buildingTypes[required]?.displayName || required);
                }
            }
            
            // Check if player has enough credits
            const hasCredits = myCredits >= cost;
            
            // Update button state
            if (!hasTech) {
                // Missing tech requirements - fully disabled with lock icon
                button.disabled = true;
                button.style.opacity = '0.5';
                button.style.filter = 'grayscale(100%)';
                button.title = `🔒 Requires: ${missingRequirements.join(', ')}`;
                
                // Add lock icon if not already present
                const icon = this.getBuildingIcon(buildingType);
                const name = this.buildingTypes[buildingType]?.displayName || buildingType;
                const cost = buildingInfo.cost;
                button.innerHTML = `🔒 ${icon} ${name} <span class="build-cost">(${cost})</span>`;
            } else if (!hasCredits) {
                // Has tech but not enough credits - greyed out but different style
                button.disabled = true;
                button.style.opacity = '0.6';
                button.style.filter = 'none'; // Remove grayscale for "can't afford" state
                button.title = `Insufficient credits (need ${cost}, have ${myCredits})`;
                
                // Reset to normal icon (no lock)
                const icon = this.getBuildingIcon(buildingType);
                const name = this.buildingTypes[buildingType]?.displayName || buildingType;
                button.innerHTML = `${icon} ${name} <span class="build-cost">(${cost})</span>`;
            } else {
                // Can build
                button.disabled = false;
                button.style.opacity = '1';
                button.style.filter = 'none';
                button.title = '';
                
                // Reset to normal icon (no lock)
                const icon = this.getBuildingIcon(buildingType);
                const name = this.buildingTypes[buildingType]?.displayName || buildingType;
                const cost = buildingInfo.cost;
                button.innerHTML = `${icon} ${name} <span class="build-cost">(${cost})</span>`;
            }
        });
    }
    
    activateSpecialAbility() {
        // Check if any selected units have special abilities that require targets
        let needsTarget = false;
        let targetType = null;
        
        for (const id of this.selectedUnits) {
            const unitContainer = this.units.get(id);
            if (unitContainer && unitContainer.unitData) {
                const unitType = unitContainer.unitData.type;
                const specialAbility = this.unitTypes?.[unitType]?.specialAbility; // Look up from static unit types
                if (specialAbility === 'HEAL') {
                    needsTarget = true;
                    targetType = 'unit';
                    break;
                } else if (specialAbility === 'REPAIR') {
                    needsTarget = true;
                    targetType = 'building';
                    break;
                }
            }
        }
        
        if (needsTarget) {
            // Enter targeting mode
            this.specialAbilityTargetingMode = true;
            this.specialAbilityTargetType = targetType;
            
            // Visual feedback - change cursor or show message
            document.body.style.cursor = 'crosshair';
        } else {
            // Non-targeted ability (like deploy)
            this.sendInput({ activateSpecialAbility: true });
        }
    }
    
    sendInput(input) {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            input.type = 'rtsInput';
            this.websocket.send(JSON.stringify(input));
        }
    }
    
    handleResize() {
        this.app.renderer.resize(window.innerWidth, window.innerHeight);
        this.updateCameraTransform();
    }
    
    updateLoadingProgress(percent, status) {
        document.getElementById('loading-progress').style.width = percent + '%';
        document.getElementById('loading-status').textContent = status;
    }
    
    hideLoadingScreen() {
        document.getElementById('loading-screen').style.display = 'none';
        document.getElementById('rts-ui').style.display = 'block';
        this.cacheHudDomRefs();
        this.installHotkeyLegend();
    }
    
    getBuildingAtPosition(worldPos) {
        let closestBuilding = null;
        let minDist = Infinity;
        
        this.buildings.forEach((container, id) => {
            const buildingData = container.buildingData;
            if (buildingData) {
                const dist = Math.sqrt(
                    Math.pow(buildingData.x - worldPos.x, 2) + 
                    Math.pow(buildingData.y - worldPos.y, 2)
                );
                if (dist < buildingData.size && dist < minDist) {
                    minDist = dist;
                    closestBuilding = buildingData;
                }
            }
        });
        
        return closestBuilding;
    }
    
    getUnitAtPosition(worldPos) {
        let closestUnit = null;
        let minDist = Infinity;
        
        this.units.forEach((container, id) => {
            const unitData = container.unitData;
            if (unitData) {
                const typeInfo = this.getUnitTypeInfo(unitData.type);
                const dist = Math.sqrt(
                    Math.pow(unitData.x - worldPos.x, 2) + 
                    Math.pow(unitData.y - worldPos.y, 2)
                );
                if (dist < typeInfo.size && dist < minDist) {
                    minDist = dist;
                    closestUnit = unitData;
                }
            }
        });
        
        return closestUnit;
    }
    
    selectBuilding(buildingData) {
        this.selectedBuilding = buildingData;

        this.sendInput({ selectUnits: [] });
        this.clearUnitSelectionImmediate();
        this.hideBuildMenu();

        const uip = document.getElementById('unit-info-panel');
        if (uip) {
            uip.style.display = 'none';
        }

        this.showProductionUI(buildingData);
    }

    showProductionUI(buildingData) {
        const panel = this.dom?.buildingInfoPanel || document.getElementById('building-info-panel');
        if (!panel) {
            return;
        }
        panel.style.display = 'flex';

        panel.innerHTML = '';

        const hasProduction = Boolean(buildingData.canProduceUnits && !buildingData.underConstruction);
        const hasHoused = Boolean(buildingData.housedUnits && buildingData.housedUnits.length > 0);
        const useRightColumn = hasProduction || hasHoused;

        const layout = document.createElement('div');
        layout.className = useRightColumn
                ? 'building-info-layout'
                : 'building-info-layout building-info-layout--single';

        const meta = document.createElement('div');
        meta.className = 'building-info-meta';

        const title = document.createElement('div');
        title.className = 'unit-name';
        const bIcon = this.getBuildingIcon(buildingData.type);
        const displayName = this.buildingTypes?.[buildingData.type]?.displayName || buildingData.type;
        title.textContent = `${bIcon} ${displayName}`;
        meta.appendChild(title);

        const healthBarContainer = document.createElement('div');
        healthBarContainer.className = 'health-bar';
        const healthFill = document.createElement('div');
        healthFill.className = 'health-fill';
        const healthPercent = (buildingData.health / buildingData.maxHealth) * 100;
        healthFill.style.width = healthPercent + '%';
        healthBarContainer.appendChild(healthFill);
        meta.appendChild(healthBarContainer);

        const healthText = document.createElement('div');
        healthText.className = 'unit-stat';
        healthText.innerHTML = `<span>Health:</span><span>${Math.floor(buildingData.health)}/${buildingData.maxHealth}</span>`;
        meta.appendChild(healthText);

        if (buildingData.underConstruction) {
            const st = document.createElement('div');
            st.className = 'unit-stat';
            st.innerHTML = '<span>Status:</span><span>Under construction</span>';
            meta.appendChild(st);
        }

        if (buildingData.type === 'BUNKER') {
            const garrisonInfo = document.createElement('div');
            garrisonInfo.className = 'unit-stat';
            garrisonInfo.innerHTML = `<span>Garrison:</span><span>${buildingData.garrisonCount || 0}/${buildingData.maxGarrisonCapacity || 0}</span>`;
            meta.appendChild(garrisonInfo);
        }

        layout.appendChild(meta);

        if (useRightColumn) {
            const actionsCol = document.createElement('div');
            actionsCol.className = 'building-info-actions';
            const scroll = document.createElement('div');
            scroll.className = 'building-info-scroll';

            if (hasHoused) {
                this.renderHousedUnitsPanel(scroll, buildingData);
            }

            if (hasProduction) {
                const productionTitle = document.createElement('div');
                productionTitle.className = 'building-info-product-title';
                productionTitle.textContent = 'Train units';
                scroll.appendChild(productionTitle);

                const trainGrid = document.createElement('div');
                trainGrid.className = 'building-info-train-grid';

                const allUnits = this.getAllUnitsForBuilding(buildingData.type);
                allUnits.forEach((unitInfo) => {
                    const button = document.createElement('button');
                    button.type = 'button';
                    button.className = 'build-button';

                    const isUnlocked = unitInfo.unlocked === true;
                    const canAfford = this.myMoney >= unitInfo.cost;
                    const tickRent = unitInfo.periodicArmyRent ?? unitInfo.upkeep ?? 0;
                    let buttonHTML = `${unitInfo.name} <span class="build-cost">(💰${unitInfo.cost} ⏱${tickRent})</span>`;

                    if (!isUnlocked) {
                        buttonHTML = `🔒 ${buttonHTML}`;
                        button.disabled = true;
                        button.style.opacity = '0.5';
                        button.style.filter = 'grayscale(100%)';
                        button.title = unitInfo.lockReason || 'Tech requirements not met';
                    } else if (!canAfford) {
                        button.disabled = true;
                        button.style.opacity = '0.6';
                        button.title = 'Insufficient credits';
                    } else {
                        button.onclick = () => this.queueUnitProduction(buildingData.id, unitInfo.type);
                    }

                    button.innerHTML = buttonHTML;
                    trainGrid.appendChild(button);
                });
                scroll.appendChild(trainGrid);
            }

            actionsCol.appendChild(scroll);
            layout.appendChild(actionsCol);
        }

        panel.appendChild(layout);
    }
    
    /**
     * Check if player has a specific building type (completed, not under construction)
     */
    playerHasBuilding(buildingType) {
        if (!this.lastGameState || !this.lastGameState.buildings) {
            return false;
        }
        
        // Convert buildings array to object if needed
        let buildingsObj = this.lastGameState.buildings;
        if (Array.isArray(buildingsObj)) {
            const temp = {};
            buildingsObj.forEach(b => temp[b.id] = b);
            buildingsObj = temp;
        }
        
        for (const buildingId in buildingsObj) {
            const building = buildingsObj[buildingId];
            if (building.type === buildingType &&
                building.ownerId === this.myPlayerId &&
                building.active &&
                !building.underConstruction) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetch faction data from API
     */
    async fetchFactionData(factionType) {
        try {
            const response = await fetch(`/api/rts/factions/${factionType}`);
            if (!response.ok) {
                throw new Error(`Failed to fetch faction data for ${factionType}`);
            }
            this.myFactionData = await response.json();
            this.invalidateBuildingInfoMap();
            this._lastBuildMenuStateKey = null;

            // Generate build menu dynamically based on faction data
            this.generateBuildMenu();
            this.buildMenuGenerated = true;
        } catch (error) {
            console.error('Error fetching faction data:', error);
        }
    }
    
    /**
     * Generate build menu dynamically based on faction's available buildings
     */
    generateBuildMenu() {
        if (!this.myFactionData || !this.myFactionData.availableBuildings) {
            console.warn('Cannot generate build menu: faction data not loaded');
            return;
        }
        this.invalidateBuildingInfoMap();

        const buildMenu = document.getElementById('build-menu');
        if (!buildMenu) return;
        
        // Clear existing content except title
        const title = buildMenu.querySelector('.build-menu-title');
        buildMenu.innerHTML = '';
        if (title) {
            buildMenu.appendChild(title);
        } else {
            buildMenu.innerHTML = '<div class="build-menu-title">Build Menu</div>';
        }
        
        // Group buildings by tier
        const buildingsByTier = {
            1: [],
            2: [],
            3: []
        };
        
        this.myFactionData.availableBuildings.forEach(building => {
            if (building.buildingType !== 'HEADQUARTERS') {
                const tier = building.requiredTechTier || 1;
                buildingsByTier[tier].push(building);
            }
        });
        
        const tiersWithContent = [1, 2, 3].filter((t) => buildingsByTier[t].length > 0);
        if (tiersWithContent.length === 0) {
            if (this.lastGameState && this.lastGameState.buildings) {
                this.updateBuildMenuAvailability(this.lastGameState.buildings);
            }
            return;
        }

        const panelsWrap = document.createElement('div');
        panelsWrap.className = 'build-menu-panels';

        const appendBuildingButton = (building, panel) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'build-button';
            button.setAttribute('data-building', building.buildingType);

            const icon = this.getBuildingIcon(building.buildingType);
            const name = this.buildingTypes[building.buildingType]?.displayName || building.buildingType;
            const cost = building.cost;
            button.innerHTML = `${icon} ${name} <span class="build-cost">(${cost})</span>`;

            button.addEventListener('click', () => {
                if (!button.disabled) {
                    this.enterBuildMode(building.buildingType);
                }
            });
            panel.appendChild(button);
        };

        let tabsRow = null;
        const defaultTier = tiersWithContent[0];

        const activateTier = (tier) => {
            panelsWrap.querySelectorAll('.build-menu-tier-panel').forEach((p) => {
                p.classList.toggle('is-active', Number(p.dataset.tier) === tier);
            });
            if (tabsRow) {
                tabsRow.querySelectorAll('.build-menu-tab').forEach((b) => {
                    b.classList.toggle('is-active', Number(b.dataset.tier) === tier);
                });
            }
        };

        if (tiersWithContent.length > 1) {
            tabsRow = document.createElement('div');
            tabsRow.className = 'build-menu-tabs';
            tiersWithContent.forEach((tier) => {
                const tab = document.createElement('button');
                tab.type = 'button';
                tab.className = 'build-menu-tab';
                tab.dataset.tier = String(tier);
                tab.textContent = `Tier ${tier}`;
                if (tier === defaultTier) {
                    tab.classList.add('is-active');
                }
                tab.addEventListener('click', () => activateTier(tier));
                tabsRow.appendChild(tab);
            });
            buildMenu.appendChild(tabsRow);
        }

        tiersWithContent.forEach((tier) => {
            const panel = document.createElement('div');
            panel.className = 'build-menu-tier-panel';
            panel.dataset.tier = String(tier);
            if (tier === defaultTier) {
                panel.classList.add('is-active');
            }
            buildingsByTier[tier].forEach((building) => appendBuildingButton(building, panel));
            panelsWrap.appendChild(panel);
        });

        buildMenu.appendChild(panelsWrap);

        // Initial update of availability
        if (this.lastGameState && this.lastGameState.buildings) {
            this.updateBuildMenuAvailability(this.lastGameState.buildings);
        }
    }
    
    /**
     * Get display icon for a building type
     */
    getBuildingIcon(buildingType) {
        if (this.buildingTypes && this.buildingTypes[buildingType] && this.buildingTypes[buildingType].menuIcon) {
            return this.buildingTypes[buildingType].menuIcon;
        }
        return '🏢';
    }
    
    /**
     * Get available units for a building, filtered by faction AND tech tree
     */
    getAvailableUnits(buildingType) {
        // Get player's available units from game state (includes tech tree unlocks)
        const myFactionState = this.lastGameState?.factions
            ? this.getFactionStateForPlayer(this.lastGameState.factions, this.myPlayerId)
            : null;
        const unlockedUnits = myFactionState?.availableUnits || [];
        
        if (!this.myFactionData) {
            // No faction data loaded yet - shouldn't happen, but fallback gracefully
            return [];
        }
        
        // Convert unlocked units to Set for O(1) lookup
        const unlockedUnitsSet = new Set(unlockedUnits);
        
        // Filter faction's unit list by:
        // 1. Unit is unlocked (in game state's availableUnits)
        // 2. Unit is produced by this building
        const units = this.myFactionData.availableUnits
            .filter(unitInfo => {
                // Check if unlocked via tech tree
                if (!unlockedUnitsSet.has(unitInfo.unitType)) {
                    return false;
                }
                
                // Check if this building produces this unit
                return unitInfo.producedBy === buildingType;
            })
            .map(unitInfo => ({
                type: unitInfo.unitType,
                name: this.unitTypes[unitInfo.unitType]?.displayName || unitInfo.unitType,
                cost: unitInfo.cost, // Faction-modified cost
                baseCost: unitInfo.baseCost,
                costModifier: unitInfo.costModifier,
                upkeep: unitInfo.periodicArmyRent ?? unitInfo.upkeep,
                periodicArmyRent: unitInfo.periodicArmyRent ?? unitInfo.upkeep
            }));
        
        return units;
    }
    
    /**
     * Get ALL units for a building (including locked ones) for UI display
     */
    getAllUnitsForBuilding(buildingType) {
        if (!this.myFactionData) {
            return [];
        }
        
        // Get available units from static faction data (sent in initialization)
        // availableUnits is no longer in dynamic game state after optimization
        const myFactionState = this.lastGameState?.factions
            ? this.getFactionStateForPlayer(this.lastGameState.factions, this.myPlayerId)
            : null;

        // Return ALL units produced by this building, with lock status
        return this.myFactionData.availableUnits
            .filter(unitInfo => unitInfo.producedBy === buildingType)
            .map(unitInfo => {
                const isUnlocked = unitInfo.techRequirements.every(b => this.playerHasBuilding(b));
                
                return {
                    type: unitInfo.unitType,
                    name: this.unitTypes[unitInfo.unitType]?.displayName || unitInfo.unitType,
                    cost: unitInfo.cost,
                    baseCost: unitInfo.baseCost,
                    costModifier: unitInfo.costModifier,
                    upkeep: unitInfo.periodicArmyRent ?? unitInfo.upkeep,
                    periodicArmyRent: unitInfo.periodicArmyRent ?? unitInfo.upkeep,
                    unlocked: isUnlocked,
                    lockReason: this.getUnitLockReason(unitInfo)
                };
            });
    }
    
    /**
     * Get human-readable reason why a unit is locked
     */
    getUnitLockReason(unitInfo) {
        const required = unitInfo.techRequirements;
        const missing = required.filter(building => !this.playerHasBuilding(building))
            .map(building => this.buildingTypes[building].displayName)
            .join(', ');
        return `Requires: ${missing}`;
    }
    
    queueUnitProduction(buildingId, unitType) {
        this.sendInput({
            produceUnitOrder: unitType,
            produceBuildingId: buildingId
        });
    }
    
    setRallyPoint(buildingId, worldPos) {
        this.sendInput({
            setRallyBuildingId: buildingId,
            rallyPoint: { x: worldPos.x, y: worldPos.y }
        });
    }
    
    updateFieldEffect(effectData) {
        let effectContainer = this.fieldEffects.get(effectData.id);
        
        if (!effectContainer) {
            // Create new effect
            effectContainer = new PIXI.Container();
            effectContainer.position.set(effectData.x, effectData.y);
            
            const graphics = new PIXI.Graphics();
            effectContainer.graphics = graphics;
            effectContainer.addChild(graphics);
            
            this.fieldEffects.set(effectData.id, effectContainer);
            this.gameContainer.addChild(effectContainer);
            
            // Field effects render above ground units but below aircraft (z-index 1.5)
            effectContainer.zIndex = 1.5;
        }
        
        // Update effect visuals based on type and progress
        const graphics = effectContainer.graphics;
        graphics.clear();
        
        if (effectData.type === 'EXPLOSION') {
            // Expanding circle with fade
            const alpha = 1.0 - effectData.progress;
            const innerRadius = effectData.radius * effectData.progress;
            
            // Outer ring (orange)
            graphics.circle(0, 0, effectData.radius);
            graphics.fill({ color: 0xFF6600, alpha: alpha * 0.6 });
            
            // Inner core (yellow-white)
            graphics.circle(0, 0, innerRadius);
            graphics.fill({ color: 0xFFFF00, alpha: alpha * 0.8 });
            
            // Flash effect at start
            if (effectData.progress < 0.2) {
                graphics.circle(0, 0, effectData.radius * 1.2);
                graphics.fill({ color: 0xFFFFFF, alpha: (1.0 - effectData.progress / 0.2) * 0.5 });
            }
        } else if (effectData.type === 'ELECTRIC') {
            // Electric field - pulsing blue/cyan area with arcs
            const time = Date.now() / 1000; // Current time in seconds
            const pulseSpeed = 2.0; // Pulses per second
            const pulse = Math.sin(time * pulseSpeed * Math.PI * 2) * 0.5 + 0.5; // 0 to 1
            
            // Base alpha fades out over lifetime
            const baseAlpha = Math.max(0.3, 1.0 - effectData.progress * 0.7);
            
            // Outer glow (cyan)
            graphics.circle(0, 0, effectData.radius);
            graphics.fill({ color: 0x00FFFF, alpha: baseAlpha * 0.2 * (0.5 + pulse * 0.5) });
            
            // Inner core (bright blue)
            graphics.circle(0, 0, effectData.radius * 0.7);
            graphics.fill({ color: 0x0088FF, alpha: baseAlpha * 0.3 * (0.5 + pulse * 0.5) });
            
            // Pulsing ring
            const ringRadius = effectData.radius * (0.6 + pulse * 0.3);
            graphics.circle(0, 0, ringRadius);
            graphics.stroke({ width: 2, color: 0x00FFFF, alpha: baseAlpha * (0.6 + pulse * 0.4) });
            
            // Electric arcs (draw some lightning-like lines)
            const numArcs = 6;
            const arcAlpha = baseAlpha * (0.4 + pulse * 0.6);
            
            for (let i = 0; i < numArcs; i++) {
                const angle = (time * 0.5 + i / numArcs) * Math.PI * 2;
                const arcLength = effectData.radius * (0.7 + Math.sin(time * 3 + i) * 0.2);
                
                // Start point (near center)
                const startX = Math.cos(angle) * effectData.radius * 0.2;
                const startY = Math.sin(angle) * effectData.radius * 0.2;
                
                // End point (at edge)
                const endX = Math.cos(angle) * arcLength;
                const endY = Math.sin(angle) * arcLength;
                
                // Draw jagged line (lightning effect)
                graphics.moveTo(startX, startY);
                
                // Add some zigzag points
                const segments = 3;
                for (let j = 1; j <= segments; j++) {
                    const t = j / segments;
                    const midX = startX + (endX - startX) * t;
                    const midY = startY + (endY - startY) * t;
                    
                    // Add random offset perpendicular to the line
                    const perpAngle = angle + Math.PI / 2;
                    const offset = (Math.sin(time * 5 + i * 3 + j) * 0.5) * effectData.radius * 0.15;
                    const offsetX = midX + Math.cos(perpAngle) * offset;
                    const offsetY = midY + Math.sin(perpAngle) * offset;
                    
                    graphics.lineTo(offsetX, offsetY);
                }
                
                graphics.stroke({ width: 1.5, color: 0xFFFFFF, alpha: arcAlpha });
            }
            
            // Center spark
            if (pulse > 0.7) {
                graphics.circle(0, 0, 3);
                graphics.fill({ color: 0xFFFFFF, alpha: baseAlpha * (pulse - 0.7) * 3 });
            }
        } else if (effectData.type === 'SANDSTORM') {
            // Sandstorm - swirling sandy/brown particles with rotating vortex
            const time = Date.now() / 1000; // Current time in seconds
            const swirl1 = time * 0.3; // Slow rotation
            const swirl2 = time * 0.5; // Medium rotation
            const swirl3 = time * 0.7; // Fast rotation
            
            // Sandstorm is persistent, so alpha stays constant
            const baseAlpha = 0.4;
            
            // Base sandy cloud (tan/brown)
            graphics.circle(0, 0, effectData.radius);
            graphics.fill({ color: 0xDEB887, alpha: baseAlpha * 0.15 });
            
            // Middle layer (darker brown)
            graphics.circle(0, 0, effectData.radius * 0.75);
            graphics.fill({ color: 0xD2691E, alpha: baseAlpha * 0.2 });
            
            // Inner vortex (light sandy)
            graphics.circle(0, 0, effectData.radius * 0.4);
            graphics.fill({ color: 0xF4A460, alpha: baseAlpha * 0.25 });
            
            // Draw swirling particle streams (3 layers at different speeds)
            const drawSwirlLayer = (numStreams, rotationOffset, radiusMultiplier, color, alpha) => {
                for (let i = 0; i < numStreams; i++) {
                    const baseAngle = (i / numStreams) * Math.PI * 2 + rotationOffset;
                    
                    // Draw spiral from center to edge
                    const numPoints = 8;
                    for (let j = 0; j < numPoints - 1; j++) {
                        const t1 = j / numPoints;
                        const t2 = (j + 1) / numPoints;
                        
                        // Spiral outward with clockwise rotation (negative angle for inward vortex look)
                        const r1 = effectData.radius * t1 * radiusMultiplier;
                        const r2 = effectData.radius * t2 * radiusMultiplier;
                        const a1 = baseAngle - t1 * Math.PI * 1.5; // Negative for clockwise spiral
                        const a2 = baseAngle - t2 * Math.PI * 1.5;
                        
                        const x1 = Math.cos(a1) * r1;
                        const y1 = Math.sin(a1) * r1;
                        const x2 = Math.cos(a2) * r2;
                        const y2 = Math.sin(a2) * r2;
                        
                        // Fade out towards edge
                        const fadeAlpha = alpha * (1.0 - t1 * 0.5);
                        
                        graphics.moveTo(x1, y1);
                        graphics.lineTo(x2, y2);
                        graphics.stroke({ width: 2, color: color, alpha: fadeAlpha });
                    }
                }
            };
            
            // Three layers of swirls at different speeds and colors
            drawSwirlLayer(8, swirl1, 0.9, 0xF4A460, baseAlpha * 0.4); // Sandy outer
            drawSwirlLayer(6, swirl2, 0.7, 0xD2691E, baseAlpha * 0.5); // Brown middle
            drawSwirlLayer(4, swirl3, 0.5, 0xDEB887, baseAlpha * 0.6); // Tan inner
            
            // Pulsing danger ring (subtle red tint to indicate damage)
            const dangerPulse = Math.sin(time * 1.5) * 0.5 + 0.5;
            graphics.circle(0, 0, effectData.radius);
            graphics.stroke({ width: 2, color: 0xFF6347, alpha: baseAlpha * 0.3 * (0.3 + dangerPulse * 0.2) });
            
            // Center eye of the storm (darker, calmer)
            const eyeRadius = effectData.radius * 0.15;
            graphics.circle(0, 0, eyeRadius);
            graphics.fill({ color: 0x8B4513, alpha: baseAlpha * 0.4 });
            graphics.circle(0, 0, eyeRadius);
            graphics.stroke({ width: 1, color: 0xD2691E, alpha: baseAlpha * 0.6 });
        } else if (effectData.type === 'FLAK_EXPLOSION') {
            // Flak explosion - angular burst pattern with shrapnel (anti-air)
            const alpha = 1.0 - effectData.progress;
            const time = Date.now() / 1000;
            
            // Main burst (gray-black smoke cloud)
            graphics.circle(0, 0, effectData.radius);
            graphics.fill({ color: 0x404040, alpha: alpha * 0.5 });
            
            // Inner blast (orange-red)
            const innerRadius = effectData.radius * (0.6 + effectData.progress * 0.4);
            graphics.circle(0, 0, innerRadius);
            graphics.fill({ color: 0xFF4500, alpha: alpha * 0.7 });
            
            // Bright flash at center (white-yellow)
            if (effectData.progress < 0.3) {
                const flashAlpha = (1.0 - effectData.progress / 0.3) * alpha;
                graphics.circle(0, 0, effectData.radius * 0.4);
                graphics.fill({ color: 0xFFFF88, alpha: flashAlpha });
            }
            
            // Shrapnel burst pattern (angular, directional)
            const numShards = 12;
            const shardAlpha = alpha * 0.8;
            
            for (let i = 0; i < numShards; i++) {
                const angle = (i / numShards) * Math.PI * 2 + time * 0.5;
                const shardLength = effectData.radius * (0.7 + effectData.progress * 0.5);
                const shardWidth = 2;
                
                // Start point (near center)
                const startDist = effectData.radius * 0.2;
                const startX = Math.cos(angle) * startDist;
                const startY = Math.sin(angle) * startDist;
                
                // End point (expanding outward)
                const endX = Math.cos(angle) * shardLength;
                const endY = Math.sin(angle) * shardLength;
                
                // Draw angular shard line
                graphics.moveTo(startX, startY);
                graphics.lineTo(endX, endY);
                graphics.stroke({ width: shardWidth, color: 0xFFAA00, alpha: shardAlpha });
                
                // Add spark at tip
                if (effectData.progress < 0.5) {
                    graphics.circle(endX, endY, 2);
                    graphics.fill({ color: 0xFFFFFF, alpha: shardAlpha * 0.7 });
                }
            }
            
            // Secondary smoke rings (darker gray expanding outward)
            const numRings = 3;
            for (let i = 0; i < numRings; i++) {
                const ringProgress = (effectData.progress * 1.5 - i * 0.2);
                if (ringProgress > 0 && ringProgress < 1) {
                    const ringRadius = effectData.radius * (0.5 + ringProgress * 0.7);
                    const ringAlpha = alpha * (1.0 - ringProgress) * 0.4;
                    graphics.circle(0, 0, ringRadius);
                    graphics.stroke({ width: 3, color: 0x303030, alpha: ringAlpha });
                }
            }
        }
    }
    
    renderHousedUnitsPanel(panel, buildingData) {
        const prev = panel.querySelector('.housed-units-section');
        if (prev) prev.remove();
        const wrap = document.createElement('div');
        wrap.className = 'housed-units-section';
        wrap.style.marginTop = '12px';
        wrap.style.padding = '8px';
        wrap.style.background = 'rgba(0,0,0,0.45)';
        wrap.style.borderRadius = '6px';
        const title = document.createElement('div');
        title.style.fontWeight = 'bold';
        title.style.color = '#FFD700';
        title.style.marginBottom = '6px';
        title.textContent = 'Housed units';
        wrap.appendChild(title);

        const mine = buildingData.ownerId === this.myPlayerId;

        const actionBtnStyle = {
            padding: '1px 5px',
            fontSize: '11px',
            lineHeight: '1.15',
            minWidth: '22px',
            height: '22px',
            borderRadius: '3px',
            cursor: 'pointer',
            border: '1px solid rgba(255,255,255,0.35)',
            background: 'rgba(40,40,55,0.95)',
            color: '#eee',
            flexShrink: '0'
        };

        for (const row of buildingData.housedUnits) {
            const rowEl = document.createElement('div');
            rowEl.className = 'housed-unit-row';
            rowEl.style.display = 'flex';
            rowEl.style.alignItems = 'center';
            rowEl.style.justifyContent = 'space-between';
            rowEl.style.gap = '10px';
            rowEl.style.marginTop = '6px';

            let label = '';
            if (row.producingType) {
                const nm = this.unitTypes[row.producingType]?.displayName || row.producingType;
                const pct = Math.floor((row.productionProgress || 0) * 100);
                label = `Producing: ${nm} (${pct}%)`;
            } else {
                const nm = this.unitTypes[row.unitType]?.displayName || row.unitType;
                label = `${nm}  HP ${Math.floor(row.health)}/${Math.floor(row.maxHealth)}`;
                if (row.deployed) label += '  [Deployed]';
            }
            const lab = document.createElement('span');
            lab.textContent = label;
            lab.style.flex = '1 1 auto';
            lab.style.minWidth = '0';
            lab.style.overflow = 'hidden';
            lab.style.textOverflow = 'ellipsis';
            lab.style.whiteSpace = 'nowrap';
            lab.style.fontSize = '13px';
            rowEl.appendChild(lab);

            const actions = row.actions || [];
            if (mine && actions.length > 0) {
                const actionBar = document.createElement('div');
                actionBar.className = 'housed-unit-actions';
                actionBar.style.display = 'flex';
                actionBar.style.alignItems = 'center';
                actionBar.style.gap = '4px';
                actionBar.style.flexShrink = '0';
                actionBar.style.marginLeft = 'auto';

                for (const act of actions) {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    Object.assign(btn.style, actionBtnStyle);
                    if (act === 'LAUNCH') {
                        btn.textContent = row.unitType === 'BOMBER' ? '💣' : '📍';
                        btn.title = row.unitType === 'BOMBER' ? 'Sortie — click target on map' : 'Deploy — click station on map';
                        btn.onclick = () => this.enterSortieTargetingMode(buildingData.id, row.unitId, row.unitType);
                    } else if (act === 'RTB') {
                        btn.textContent = '🏠';
                        btn.title = 'Return to base';
                        btn.onclick = () => this.issueRTBOrder(buildingData.id, row.unitId);
                    } else if (act === 'SCRAP') {
                        btn.textContent = '✖';
                        btn.title = 'Scrap (no refund)';
                        btn.onclick = () => this.issueScrapHousedUnit(buildingData.id, row.unitId);
                    } else if (act === 'EXIT') {
                        btn.textContent = '🚪';
                        btn.title = 'Exit bunker';
                        btn.onclick = () => this.ungarrisonUnit(buildingData.id, false, row.unitId);
                    } else if (act === 'CANCEL_PRODUCTION') {
                        btn.textContent = '⏹';
                        btn.title = 'Cancel current production (refunds cost)';
                        btn.onclick = () => this.issueCancelAirfieldProduction(buildingData.id);
                    } else {
                        continue;
                    }
                    actionBar.appendChild(btn);
                }
                rowEl.appendChild(actionBar);
            }
            wrap.appendChild(rowEl);
        }
        panel.appendChild(wrap);
    }

    enterSortieTargetingMode(buildingId, housedUnitId, aircraftType) {
        this.sortieTargetingMode = true;
        this.sortieBuildingId = buildingId;
        this.sortieHousedUnitId = housedUnitId;
        this.sortieAircraftType = aircraftType;

        const message = (aircraftType === 'INTERCEPTOR' || aircraftType === 'GUNSHIP')
            ? 'Click map location for patrol station'
            : 'Click map location for bombing run';
        this.showGameEvent(message, 'info');
        document.body.style.cursor = 'crosshair';
    }

    exitSortieTargetingMode() {
        this.sortieTargetingMode = false;
        this.sortieBuildingId = null;
        this.sortieHousedUnitId = null;
        this.sortieAircraftType = null;
        document.body.style.cursor = 'default';
    }

    issueSortieOrder(targetX, targetY) {
        if (!this.sortieTargetingMode || this.sortieBuildingId == null || this.sortieHousedUnitId == null) {
            return;
        }
        this.sendInput({
            sortieBuildingId: this.sortieBuildingId,
            sortieHousedUnitId: this.sortieHousedUnitId,
            sortieTargetLocation: { x: targetX, y: targetY }
        });
        this.exitSortieTargetingMode();
        this.showGameEvent('Order issued', 'info');
    }

    issueRTBOrder(buildingId, housedUnitId) {
        this.sendInput({
            rtbBuildingId: buildingId,
            rtbHousedUnitId: housedUnitId
        });
        this.showGameEvent('Aircraft returning to base', 'info');
    }

    issueScrapHousedUnit(buildingId, unitId) {
        this.sendInput({
            scrapFromBuildingId: buildingId,
            scrapHousedUnitId: unitId
        });
    }

    issueCancelAirfieldProduction(buildingId) {
        this.sendInput({
            cancelAirfieldProductionBuildingId: buildingId
        });
    }

    updateCommandAbilitiesPanel() {
        const panel = this.dom.commandAbilitiesPanel;
        const wrap = this.dom.commandAbilitiesButtons;
        if (!panel || !wrap) {
            return;
        }
        const raw = (this.myFaction && Array.isArray(this.myFaction.commandAbilities))
            ? this.myFaction.commandAbilities
            : [];
        // Only show abilities once the server reports the unlock building is complete (no greyed-out teaser).
        const rows = raw.filter((r) => r.unlocked === true);
        if (rows.length === 0) {
            panel.style.display = 'none';
            this._commandAbilityPanelKey = null;
            return;
        }
        panel.style.display = 'flex';
        const key = rows.map((r) => {
            const cdSec = r.onCooldown ? Math.ceil((r.cooldownRemainingMs || 0) / 1000) : 0;
            const armSec = r.armingRemainingMs ? Math.ceil((r.armingRemainingMs || 0) / 1000) : 0;
            const can = r.canActivate === true ? '1' : '0';
            return `${r.id}:${r.onCooldown}:${cdSec}:${can}:${armSec}`;
        }).join('|');
        if (this._commandAbilityPanelKey === key) {
            return;
        }
        this._commandAbilityPanelKey = key;
        wrap.replaceChildren();
        for (const row of rows) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'command-ability-btn';
            const cat = this.commandAbilityTypes && this.commandAbilityTypes[row.id];
            const label = row.displayName || (cat && cat.displayName) || row.id;
            const desc = (cat && cat.description) || '';
            const reqTarget = cat && cat.requiresGroundTarget !== false;
            let title = desc || '';
            if (row.onCooldown) {
                title = `${desc ? `${desc} ` : ''}On cooldown.`.trim();
            } else if (row.canActivate === false) {
                title = (desc ? `${desc} ` : '') + (row.armingRemainingMs > 0
                    ? `Arming: ${Math.ceil(row.armingRemainingMs / 1000)}s remaining.`
                    : 'Not available right now.');
            }
            btn.title = title.trim();
            const cdLine = row.onCooldown
                ? `<span class="ca-cooldown">Cooldown: ${Math.ceil((row.cooldownRemainingMs || 0) / 1000)}s</span>`
                : '';
            const armLine = !row.onCooldown && row.armingRemainingMs > 0
                ? `<span class="ca-cooldown">Arming: ${Math.ceil(row.armingRemainingMs / 1000)}s</span>`
                : '';
            btn.innerHTML = `<span>${label}</span>${cdLine}${armLine}`;
            const enabled = row.canActivate === true;
            btn.disabled = !enabled;
            btn.addEventListener('click', () => {
                if (this.commandAbilityTargetingMode || !enabled) {
                    return;
                }
                if (!reqTarget) {
                    this.issueCommandAbilityImmediate(row.id);
                } else {
                    this.enterCommandAbilityTargeting(row.id);
                }
            });
            wrap.appendChild(btn);
        }
    }

    issueCommandAbilityImmediate(abilityTypeId) {
        this.sendInput({
            commandAbilityOrder: abilityTypeId
        });
        this.showGameEvent('Command order sent', 'info');
    }

    enterCommandAbilityTargeting(abilityTypeId) {
        if (this.buildMode) {
            this.exitBuildMode();
        }
        if (this.attackMoveMode) {
            this.exitAttackMoveMode();
        }
        if (this.specialAbilityTargetingMode) {
            this.exitSpecialAbilityTargetingMode();
        }
        if (this.sortieTargetingMode) {
            this.exitSortieTargetingMode();
            this.showGameEvent('Sortie order cancelled', 'warning');
        }
        this.commandAbilityTargetingMode = true;
        this.pendingCommandAbilityType = abilityTypeId;
        const cat = this.commandAbilityTypes && this.commandAbilityTypes[abilityTypeId];
        const name = (cat && cat.displayName) || abilityTypeId;
        this.showGameEvent(`${name}: click the map to target (Esc = cancel)`, 'info');
        document.body.style.cursor = 'crosshair';
    }

    exitCommandAbilityTargetingMode() {
        this.commandAbilityTargetingMode = false;
        this.pendingCommandAbilityType = null;
        if (this.commandAbilityTargetGraphics) {
            this.commandAbilityTargetGraphics.clear();
        }
        document.body.style.cursor = 'default';
    }

    issueCommandAbilityAt(targetX, targetY) {
        if (!this.commandAbilityTargetingMode || !this.pendingCommandAbilityType) {
            return;
        }
        this.sendInput({
            commandAbilityOrder: this.pendingCommandAbilityType,
            commandAbilityTargetLocation: { x: targetX, y: targetY }
        });
        this.exitCommandAbilityTargetingMode();
        this.showGameEvent('Command order sent', 'info');
    }
}

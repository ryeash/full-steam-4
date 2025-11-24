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
        this.resourceDeposits = new Map();
        this.obstacles = new Map();
        this.projectiles = new Map();
        this.beams = new Map();
        this.fieldEffects = new Map();
        this.wallSegments = new Map();
        
        // Player state
        this.myPlayerId = null;
        this.myFaction = null;
        this.myFactionData = null; // Faction data from API
        this.myTeam = null;
        this.hasCenteredCamera = false;
        this.visionRange = 400; // Default, updated from server
        this.lastFogUpdate = 0; // Timestamp of last fog update
        this.fogUpdateInterval = 200; // Update fog every 200ms (5 times per second)
        
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
        this.gameContainer.addChild(this.fogContainer);
        this.gameContainer.addChild(this.selectionBoxGraphics);
        
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
    }
    
    setupUI() {
        // Setup build menu buttons
        document.querySelectorAll('.build-button').forEach(button => {
            button.addEventListener('click', () => {
                const buildingType = button.getAttribute('data-building');
                this.enterBuildMode(buildingType);
            });
        });
    }
    
    async connectToServer() {
        // Get game ID from URL or create new game
        const urlParams = new URLSearchParams(window.location.search);
        let gameId = urlParams.get('gameId');
        
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
        
        // Connect via WebSocket
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/rts/${gameId}`;
        
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
        
        this.websocket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
        
        this.websocket.onclose = () => {
        };
    }
    
    handleServerMessage(data) {
        switch (data.type) {
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
    
    handleGameOver(data) {
        console.log('Game Over!', data);
        
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
        
        // Update resource deposits
        if (state.resourceDeposits) {
            const currentDepositIds = new Set(state.resourceDeposits.map(d => d.id));
            
            // Remove deposits that no longer exist (depleted)
            this.resourceDeposits.forEach((depositContainer, id) => {
                if (!currentDepositIds.has(id)) {
                    this.gameContainer.removeChild(depositContainer);
                    this.resourceDeposits.delete(id);
                }
            });
            
            // Update or create deposits
            state.resourceDeposits.forEach(depositData => {
                this.updateResourceDeposit(depositData);
            });
        }
        
        // Update obstacles
        if (state.obstacles) {
            const currentObstacleIds = new Set(state.obstacles.map(o => o.id));
            
            // Remove obstacles that no longer exist
            this.obstacles.forEach((obstacleContainer, id) => {
                if (!currentObstacleIds.has(id)) {
                    this.gameContainer.removeChild(obstacleContainer);
                    this.obstacles.delete(id);
                }
            });
            
            // Update or create obstacles
            state.obstacles.forEach(obstacleData => {
                this.updateObstacle(obstacleData);
            });
        }
        
        // Update wall segments
        if (state.wallSegments) {
            const currentSegmentIds = new Set(state.wallSegments.map(s => s.id));
            
            // Remove wall segments that no longer exist
            this.wallSegments.forEach((segmentContainer, id) => {
                if (!currentSegmentIds.has(id)) {
                    this.gameContainer.removeChild(segmentContainer);
                    segmentContainer.destroy();
                    this.wallSegments.delete(id);
                }
            });
            
            // Update or create wall segments
            state.wallSegments.forEach(segmentData => {
                this.updateWallSegment(segmentData);
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
        if (state.factions && this.myPlayerId) {
            this.myFaction = state.factions[this.myPlayerId];
            if (this.myFaction) {
                this.myTeam = this.myFaction.team;
                this.updateResourceDisplay();
                
                // Fetch faction data from API if we don't have it yet
                if (this.myFaction.factionType && !this.myFactionData) {
                    this.fetchFactionData(this.myFaction.factionType);
                }
                
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
        
        // Update biome info
        if (state.biome && !this.biome) {
            this.biome = state.biome.name;
            this.groundColor = state.biome.groundColor;
            this.obstacleColor = state.biome.obstacleColor;
            
            // Draw the world bounds rectangle with the biome color
            // Everything outside this will be black
            this.drawWorldBounds();
            
        }
        
        // Update world dimensions (for camera bounds)
        if (state.worldWidth && state.worldHeight) {
            const boundsChanged = this.worldBounds.width !== state.worldWidth || 
                                  this.worldBounds.height !== state.worldHeight;
            this.worldBounds.width = state.worldWidth;
            this.worldBounds.height = state.worldHeight;
            
            // Redraw world bounds if dimensions changed and we have a biome color
            if (boundsChanged && this.biome) {
                this.drawWorldBounds();
            }
        }
        
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
        
        // Update pickaxe durability bar (for miners)
        if (unitData.type === 'MINER' && unitData.pickaxeDurability !== undefined) {
            if (!unitContainer.pickaxeBar) {
                unitContainer.pickaxeBar = new PIXI.Graphics();
                unitContainer.addChild(unitContainer.pickaxeBar);
            }
            const durabilityPercent = unitData.pickaxeDurability / 100.0;
            
            // Hide pickaxe bar if at full durability
            if (durabilityPercent >= 1.0) {
                unitContainer.pickaxeBar.visible = false;
            } else {
                unitContainer.pickaxeBar.visible = true;
                const offset = (unitContainer.healthBarOffset || 25) - 5; // Below health bar
                unitContainer.pickaxeBar.clear();
                unitContainer.pickaxeBar.rect(-15, -offset, 30 * durabilityPercent, 2);
                // Color: green when high, yellow when medium, red when low
                const pickaxeColor = durabilityPercent > 0.5 ? 0x00FF00 : (durabilityPercent > 0.2 ? 0xFFFF00 : 0xFF0000);
                unitContainer.pickaxeBar.fill(pickaxeColor);
            }
        } else if (unitContainer.pickaxeBar) {
            // Hide pickaxe bar for non-miners
            unitContainer.pickaxeBar.visible = false;
        }
        
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
        
        // Update special ability indicator (for deployed Crawlers, etc.)
        const typeInfo = this.getUnitTypeInfo(unitData.type);
        if (unitData.specialAbilityActive && unitData.specialAbility === 'DEPLOY') {
            // Add or update deploy indicator
            if (!unitContainer.deployIndicator) {
                const indicator = new PIXI.Graphics();
                indicator.circle(0, 0, typeInfo.size + 10);
                indicator.stroke({ width: 3, color: 0xFFFF00, alpha: 0.8 });
                // Draw anchor symbols at cardinal directions
                for (let i = 0; i < 4; i++) {
                    const angle = i * Math.PI / 2;
                    const x = Math.cos(angle) * (typeInfo.size + 15);
                    const y = Math.sin(angle) * (typeInfo.size + 15);
                    indicator.moveTo(x, y - 5);
                    indicator.lineTo(x, y + 5);
                    indicator.stroke({ width: 2, color: 0xFFFF00 });
                }
                unitContainer.addChild(indicator);
                unitContainer.deployIndicator = indicator;
            }
            unitContainer.deployIndicator.visible = true;
        } else if (unitContainer.deployIndicator) {
            unitContainer.deployIndicator.visible = false;
        }
        
        // Update turrets (for deployed Crawler)
        if (unitData.turrets && unitData.turrets.length > 0) {
            if (!unitContainer.turretGraphics) {
                unitContainer.turretGraphics = [];
            }
            
            // Create or update turret graphics
            unitData.turrets.forEach((turretData, index) => {
                if (!unitContainer.turretGraphics[index]) {
                    // Create new turret graphic
                    const turretContainer = new PIXI.Container();
                    
                    // Turret base (small circle)
                    const base = new PIXI.Graphics();
                    base.circle(0, 0, 4);
                    base.fill(0x808080);
                    turretContainer.addChild(base);
                    
                    // Turret barrel (pointing right by default)
                    const barrel = new PIXI.Graphics();
                    const barrelLength = typeInfo.size * 0.5;
                    const barrelWidth = 3;
                    barrel.rect(0, -barrelWidth / 2, barrelLength, barrelWidth);
                    barrel.fill(0x404040);
                    turretContainer.addChild(barrel);
                    
                    unitContainer.addChild(turretContainer);
                    unitContainer.turretGraphics[index] = turretContainer;
                }
                
                // Update turret position (offset from unit center, rotated by unit rotation)
                const turret = unitContainer.turretGraphics[index];
                const cos = Math.cos(unitData.rotation);
                const sin = Math.sin(unitData.rotation);
                const rotatedX = turretData.offsetX * cos - turretData.offsetY * sin;
                const rotatedY = turretData.offsetX * sin + turretData.offsetY * cos;
                turret.position.set(rotatedX, rotatedY);
                
                // Set turret rotation (independent of unit rotation)
                turret.rotation = turretData.rotation;
            });
            
            // Remove excess turrets if unit has fewer than before
            while (unitContainer.turretGraphics.length > unitData.turrets.length) {
                const removed = unitContainer.turretGraphics.pop();
                unitContainer.removeChild(removed);
                removed.destroy();
            }
        } else {
            // Remove all turrets if unit no longer has them
            if (unitContainer.turretGraphics) {
                unitContainer.turretGraphics.forEach(turret => {
                    unitContainer.removeChild(turret);
                    turret.destroy();
                });
                unitContainer.turretGraphics = [];
            }
        }
        
        // Store data
        unitContainer.unitData = unitData;
    }
    
    getUnitTypeInfo(unitType) {
        const unitTypes = {
            'WORKER': { sides: 16, size: 15, color: 0xFFFF00 },
            'INFANTRY': { sides: 3, size: 12, color: 0x00FF00 },
            'LASER_INFANTRY': { sides: 3, size: 12, color: 0x00FFFF }, // Cyan for laser infantry
            'MEDIC': { sides: 6, size: 12, color: 0xFFFFFF },
            'ROCKET_SOLDIER': { sides: 3, size: 12, color: 0xFF8800 },
            'SNIPER': { sides: 3, size: 12, color: 0x8B4513 },
            'ENGINEER': { sides: 6, size: 13, color: 0x00CED1 }, // Dark turquoise (distinct from yellow worker)
            'MINER': { sides: 8, size: 14, color: 0x8B4513 },
            'JEEP': { sides: 4, size: 20, color: 0x00FFFF },
            'TANK': { sides: 5, size: 30, color: 0x8888FF },
            'ARTILLERY': { sides: 6, size: 25, color: 0xFF00FF },
            'GIGANTONAUT': { sides: 8, size: 35, color: 0x8B0000 }, // Super heavy artillery!
            'CRAWLER': { sides: 8, size: 50, color: 0x4A4A4A },
            'STEALTH_TANK': { sides: 5, size: 28, color: 0x2F4F4F },
            'MAMMOTH_TANK': { sides: 6, size: 40, color: 0x556B2F },
            // Hero units
            'PALADIN': { sides: 8, size: 32, color: 0xC0C0C0 }, // Silver (Terran hero)
            'RAIDER': { sides: 3, size: 22, color: 0xDC143C }, // Crimson (Nomads hero)
            'COLOSSUS': { sides: 6, size: 50, color: 0x4B0082 }, // Indigo (Synthesis hero)
            // Tech Alliance beam weapon units
            'PLASMA_TROOPER': { sides: 3, size: 12, color: 0x00FF7F }, // Spring green
            'ION_RANGER': { sides: 3, size: 12, color: 0x9370DB }, // Medium purple
            'PHOTON_SCOUT': { sides: 4, size: 18, color: 0x7FFF00 }, // Chartreuse
            'BEAM_TANK': { sides: 6, size: 30, color: 0x00FA9A }, // Medium spring green
            'PULSE_ARTILLERY': { sides: 6, size: 26, color: 0xFFD700 }, // Gold
            'PHOTON_TITAN': { sides: 8, size: 38, color: 0x00FF00 } // Bright green (hero unit)
        };
        return unitTypes[unitType] || { sides: 4, size: 15, color: 0xFFFFFF };
    }
    
    createUnitGraphics(unitData) {
        const container = new PIXI.Container();
        
        // Get unit type info
        const typeInfo = this.getUnitTypeInfo(unitData.type);
        
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
                    }
                }
            } else {
                // Single-fixture (backward compatibility): vertices is [[x1, y1], [x2, y2], ...]
                this.drawPhysicsPolygon(shape, unitData.vertices, typeInfo.color, unitData.team);
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
    
    updateBuilding(buildingData) {
        let buildingContainer = this.buildings.get(buildingData.id);
        
        if (!buildingContainer) {
            // Create new building
            buildingContainer = this.createBuildingGraphics(buildingData);
            this.buildings.set(buildingData.id, buildingContainer);
            this.gameContainer.addChild(buildingContainer);
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
                            }
                        }
                    } else {
                        // Single-fixture (backward compatibility)
                        this.drawPhysicsPolygon(shape, buildingData.vertices, 
                                               buildingContainer.typeInfo.color, 
                                               buildingData.team);
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
                const shieldRadius = 200; // Match server-side SHIELD_RADIUS
                shield.circle(0, 0, shieldRadius);
                shield.stroke({ width: 3, color: this.getTeamColor(buildingData.team), alpha: 0.4 });
                shield.fill({ color: this.getTeamColor(buildingData.team), alpha: 0.1 });
            }
        }
        
        // Update aura visualization for monuments
        const monumentTypes = {
            'PHOTON_SPIRE': { radius: 250, color: 0x00FF00 },
            'QUANTUM_NEXUS': { radius: 280, color: 0x9370DB },
            'SANDSTORM_GENERATOR': { radius: 300, color: 0xDEB887 }
        };
        
        if (monumentTypes[buildingData.type]) {
            if (!buildingContainer.auraGraphics) {
                const auraGraphics = new PIXI.Graphics();
                buildingContainer.addChild(auraGraphics);
                buildingContainer.auraGraphics = auraGraphics;
            }
            
            const aura = buildingContainer.auraGraphics;
            aura.clear();
            
            // Only show aura if active and not under construction
            if (buildingData.auraActive && !buildingData.underConstruction) {
                const monumentInfo = monumentTypes[buildingData.type];
                
                // Special swirling sandy effect for Sandstorm Generator
                if (buildingData.type === 'SANDSTORM_GENERATOR') {
                    const time = Date.now() / 1000;
                    const radius = monumentInfo.radius;
                    
                    // Draw multiple rotating spiral layers for sandy swirl effect
                    const numSpirals = 3;
                    for (let spiral = 0; spiral < numSpirals; spiral++) {
                        const spiralOffset = (spiral / numSpirals) * Math.PI * 2;
                        const rotationSpeed = 0.5 + (spiral * 0.2);
                        const rotation = time * rotationSpeed + spiralOffset;
                        
                        // Draw swirling arcs
                        const numArcs = 8;
                        for (let i = 0; i < numArcs; i++) {
                            const angle = (i / numArcs) * Math.PI * 2 + rotation;
                            const arcRadius = radius * (0.3 + (i / numArcs) * 0.7);
                            const arcLength = Math.PI * 0.4;
                            
                            // Pulsing opacity
                            const pulse = Math.sin(time * 2 + spiral + i) * 0.3 + 0.7;
                            const opacity = 0.15 * pulse;
                            
                            // Draw curved arc for sandy particle effect
                            aura.moveTo(
                                Math.cos(angle) * arcRadius,
                                Math.sin(angle) * arcRadius
                            );
                            aura.arc(0, 0, arcRadius, angle, angle + arcLength);
                            aura.stroke({ width: 3 + spiral, color: monumentInfo.color, alpha: opacity });
                        }
                    }
                    
                    // Add outer ring that pulses
                    const outerPulse = Math.sin(time * 1.5) * 0.2 + 0.8;
                    aura.circle(0, 0, radius * outerPulse);
                    aura.stroke({ width: 2, color: monumentInfo.color, alpha: 0.25 });
                    
                    // Add sandy particles (small circles scattered around)
                    const numParticles = 20;
                    for (let i = 0; i < numParticles; i++) {
                        const particleAngle = (i / numParticles) * Math.PI * 2 + time * 0.8;
                        const particleDistance = radius * (0.5 + Math.sin(time + i) * 0.3);
                        const particleX = Math.cos(particleAngle) * particleDistance;
                        const particleY = Math.sin(particleAngle) * particleDistance;
                        const particleSize = 2 + Math.sin(time * 3 + i) * 1;
                        
                        aura.circle(particleX, particleY, particleSize);
                        aura.fill({ color: monumentInfo.color, alpha: 0.4 });
                    }
                    
                    // Add subtle fill to show danger zone
                    aura.circle(0, 0, radius);
                    aura.fill({ color: monumentInfo.color, alpha: 0.04 });
                    
                } else {
                    // Standard pulsing effect for other monuments
                    const pulseSpeed = 2.0;
                    const pulseAmount = 0.15;
                    const pulse = Math.sin(Date.now() / 1000 * pulseSpeed) * pulseAmount + 1.0;
                    
                    aura.circle(0, 0, monumentInfo.radius);
                    aura.stroke({ width: 2, color: monumentInfo.color, alpha: 0.2 * pulse });
                    aura.fill({ color: monumentInfo.color, alpha: 0.03 * pulse });
                    
                    // Add glow to the building itself
                    if (buildingContainer.shapeGraphics) {
                        buildingContainer.shapeGraphics.filters = buildingContainer.shapeGraphics.filters || [];
                        // Simple glow by adding a slight alpha overlay
                        buildingContainer.alpha = 0.9 + (0.1 * pulse);
                    }
                }
            } else {
                // Reset alpha when inactive
                if (buildingContainer.shapeGraphics) {
                    buildingContainer.alpha = 1.0;
                }
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
        
        // Store data
        buildingContainer.buildingData = buildingData;
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
            'WEAPONS_DEPOT': { sides: 5, size: 48, color: 0x8B0000, rotation: Math.PI / 5 },
            'TECH_CENTER': { sides: 8, size: 60, color: 0x4169E1, rotation: Math.PI / 8 },
            'ADVANCED_FACTORY': { sides: 6, size: 65, color: 0x2F4F4F, rotation: 0 },
            'WALL': { sides: 4, size: 15, color: 0x708090, rotation: 0 },
            'TURRET': { sides: 5, size: 25, color: 0xFF4500, rotation: 0 },
            'SHIELD_GENERATOR': { sides: 6, size: 30, color: 0x00BFFF, rotation: 0 },
            'BANK': { sides: 8, size: 35, color: 0xFFD700, rotation: Math.PI / 8 },
            'BUNKER': { sides: 4, size: 40, color: 0x556B2F, rotation: Math.PI / 4 },
            // Monument buildings
            'SANDSTORM_GENERATOR': { sides: 6, size: 45, color: 0xDEB887, rotation: 0 },
            'QUANTUM_NEXUS': { sides: 8, size: 50, color: 0x9370DB, rotation: Math.PI / 8 },
            'PHOTON_SPIRE': { sides: 6, size: 48, color: 0x00FF00, rotation: Math.PI / 6 }
        };
        
        const typeInfo = buildingTypes[buildingData.type] || { sides: 4, size: 50, color: 0xFFFFFF, rotation: 0 };
        
        // Create a rotating container for turret buildings
        const hasTurret = buildingData.type === 'TURRET';
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
        
        // Add building type letter label
        const labelMap = {
            'HEADQUARTERS': 'H',
            'REFINERY': 'R',
            'BARRACKS': 'B',
            'FACTORY': 'F',
            'WALL': 'W',
            'TURRET': 'T',
            'POWER_PLANT': 'P',
            'RESEARCH_LAB': 'RL',
            'WEAPONS_DEPOT': 'WD',
            'TECH_CENTER': 'TC',
            'ADVANCED_FACTORY': 'AF',
            'SHIELD_GENERATOR': 'SG',
            'BANK': '$',
            'BUNKER': '⚔',
            'PHOTON_SPIRE': '⚡',
            'QUANTUM_NEXUS': '◈',
            'SANDSTORM_GENERATOR': '☁'
        };
        const label = new PIXI.Text(labelMap[buildingData.type] || '?', {
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
        
        return container;
    }
    
    updateResourceDeposit(depositData) {
        let depositContainer = this.resourceDeposits.get(depositData.id);
        
        if (!depositContainer) {
            // Create new deposit
            depositContainer = this.createResourceDepositGraphics(depositData);
            this.resourceDeposits.set(depositData.id, depositContainer);
            this.gameContainer.addChild(depositContainer);
        }
        
        // Update position
        depositContainer.position.set(depositData.x, depositData.y);
        
        // Store data
        depositContainer.resourceData = depositData; // Store as resourceData for click detection
    }
    
    createResourceDepositGraphics(depositData) {
        const container = new PIXI.Container();
        
        // Create circle for deposit
        const shape = new PIXI.Graphics();
        shape.circle(0, 0, 40);
        shape.fill(0x00FF00); // Green for resources
        shape.stroke({ width: 2, color: 0xFFFFFF });
        container.addChild(shape);
        
        return container;
    }
    
    updateObstacle(obstacleData) {
        let obstacleContainer = this.obstacles.get(obstacleData.id);
        
        if (!obstacleContainer) {
            // Create new obstacle
            obstacleContainer = this.createObstacleGraphics(obstacleData);
            this.obstacles.set(obstacleData.id, obstacleContainer);
            this.gameContainer.addChild(obstacleContainer);
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
    
    updateWallSegment(segmentData) {
        let segmentContainer = this.wallSegments.get(segmentData.id);
        
        if (!segmentContainer) {
            // Create new wall segment
            segmentContainer = this.createWallSegmentGraphics(segmentData);
            this.wallSegments.set(segmentData.id, segmentContainer);
            this.gameContainer.addChild(segmentContainer);
        }
        
        // Update position and rotation
        segmentContainer.position.set(segmentData.x, segmentData.y);
        segmentContainer.rotation = segmentData.rotation;
        
        // Update health bar
        if (segmentContainer.healthBar) {
            const healthPercent = segmentData.health / segmentData.maxHealth;
            
            // Hide health bar if at full health
            if (healthPercent >= 1.0) {
                segmentContainer.healthBar.visible = false;
            } else {
                segmentContainer.healthBar.visible = true;
                segmentContainer.healthBar.clear();
                segmentContainer.healthBar.rect(-segmentData.length / 2, -15, segmentData.length * healthPercent, 5);
                segmentContainer.healthBar.fill(this.getHealthColor(healthPercent));
            }
        }
        
        // Store data
        segmentContainer.segmentData = segmentData;
    }
    
    createWallSegmentGraphics(segmentData) {
        const container = new PIXI.Container();
        
        // Team colors
        const teamColors = {
            1: 0x0000FF, // Blue
            2: 0xFF0000, // Red
            3: 0x00FF00, // Green
            4: 0xFFFF00  // Yellow
        };
        const teamColor = teamColors[segmentData.team] || 0x808080;
        
        // Wall segment rectangle
        const wall = new PIXI.Graphics();
        const thickness = 8;
        wall.rect(-segmentData.length / 2, -thickness / 2, segmentData.length, thickness);
        wall.fill(teamColor);
        wall.stroke({ width: 2, color: this.darkenColor(teamColor, 0.5) });
        container.addChild(wall);
        
        // Health bar (above wall)
        const healthBar = new PIXI.Graphics();
        const healthPercent = segmentData.health / segmentData.maxHealth;
        healthBar.rect(-segmentData.length / 2, -15, segmentData.length * healthPercent, 5);
        healthBar.fill(this.getHealthColor(healthPercent));
        container.addChild(healthBar);
        container.healthBar = healthBar;
        
        return container;
    }
    
    createObstacleGraphics(obstacleData) {
        const container = new PIXI.Container();
        const shape = new PIXI.Graphics();
        
        // Different colors for destructible vs indestructible obstacles
        let fillColor, strokeColor;
        if (obstacleData.destructible) {
            // Destructible obstacles: brownish/tan color (like rocks that can be broken)
            fillColor = 0x8B7355; // Medium brown
            strokeColor = 0x654321; // Darker brown
        } else {
            // Indestructible obstacles: use biome color (darker, more solid looking)
            fillColor = this.obstacleColor;
            strokeColor = this.darkenColor(fillColor, 0.5);
        }
        
        // Render based on shape type
        if (obstacleData.shape === 'RECTANGLE') {
            // Rectangle obstacle
            const halfWidth = obstacleData.width / 2;
            const halfHeight = obstacleData.height / 2;
            shape.rect(-halfWidth, -halfHeight, obstacleData.width, obstacleData.height);
            shape.fill(fillColor);
            shape.stroke({ width: 2, color: strokeColor });
        } else if (obstacleData.shape === 'IRREGULAR_POLYGON' && obstacleData.vertices) {
            // Irregular polygon with custom vertices
            const vertices = obstacleData.vertices;
            if (vertices.length >= 3) {
                shape.moveTo(vertices[0].x, vertices[0].y);
                for (let i = 1; i < vertices.length; i++) {
                    shape.lineTo(vertices[i].x, vertices[i].y);
                }
                shape.closePath();
                shape.fill(fillColor);
                shape.stroke({ width: 2, color: strokeColor });
            }
        } else if (obstacleData.shape === 'POLYGON') {
            // Regular polygon
            this.drawPolygon(shape, obstacleData.sides, obstacleData.size, fillColor, 0);
            shape.stroke({ width: 2, color: strokeColor });
        } else {
            // Default to circle
            shape.circle(0, 0, obstacleData.size);
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
        } else {
            // Update existing beam (fade out over time)
            const fadeProgress = beamData.elapsed / beamData.duration;
            beamGraphics.alpha = 1.0 - fadeProgress;
        }
        
        // Store data
        beamGraphics.beamData = beamData;
    }
    
    createBeamGraphics(beamData) {
        const graphics = new PIXI.Graphics();
        
        // Get beam color based on type
        const colors = {
            'LASER': 0xFF0000,      // Red laser
            'PLASMA': 0x00FFFF,     // Cyan plasma
            'ION': 0x8800FF,        // Purple ion
            'PARTICLE': 0xFFFF00    // Yellow particle
        };
        
        const color = colors[beamData.beamType] || 0xFF0000;
        
        // Draw beam as a line
        graphics.moveTo(beamData.startX, beamData.startY);
        graphics.lineTo(beamData.endX, beamData.endY);
        graphics.stroke({ width: beamData.width, color: color });
        
        // Add glow effect
        graphics.filters = [new PIXI.BlurFilter(2)];
        
        return graphics;
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
                
            case 'WEAPONS_DEPOT':
                // Add crosshair
                decorations.rect(-typeInfo.size * 0.3, -typeInfo.size * 0.05, typeInfo.size * 0.6, typeInfo.size * 0.1);
                decorations.fill({ color: 0x000000, alpha: 0.3 });
                decorations.rect(-typeInfo.size * 0.05, -typeInfo.size * 0.3, typeInfo.size * 0.1, typeInfo.size * 0.6);
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
            case 'ADVANCED_FACTORY':
                // Add gear teeth pattern on edges
                const isAdvanced = buildingType === 'ADVANCED_FACTORY';
                const teethCount = isAdvanced ? 6 : 4;
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
        
        // Draw polygon from physics body vertices
        graphics.poly(points);
        graphics.fill(fillColor);
        graphics.stroke({ width: 2, color: strokeColor });
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
        if (this.myFaction) {
            document.getElementById('player-info').textContent = 
                `ID: ${this.myPlayerId} | Team: ${this.myTeam}`;
            document.getElementById('credits-value').textContent = this.myFaction.credits;
            document.getElementById('upkeep-value').textContent = 
                `${this.myFaction.currentUpkeep}/${this.myFaction.maxUpkeep}`;
            
            // Update power display
            const powerValue = document.getElementById('power-value');
            if (powerValue) {
                const powerText = `${this.myFaction.powerGenerated}/${this.myFaction.powerConsumed}`;
                powerValue.textContent = powerText;
                
                // Color code based on power status
                if (this.myFaction.hasLowPower) {
                    powerValue.style.color = '#ff4444'; // Red for low power
                } else if (this.myFaction.powerGenerated - this.myFaction.powerConsumed < 20) {
                    powerValue.style.color = '#ffaa00'; // Orange for close to low power
                } else {
                    powerValue.style.color = '#00ff00'; // Green for good power
                }
            }
        }
    }
    
    updateUnitInfoPanel() {
        // If a building is selected, don't touch the panel at all - let building UI handle it
        if (this.selectedBuilding) {
            return;
        }
        
        const panel = document.getElementById('unit-info-panel');
        const singleInfo = document.getElementById('single-unit-info');
        const multiInfo = document.getElementById('multi-unit-info');
        
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
            panel.style.display = 'block';
            singleInfo.style.display = 'block';
            multiInfo.style.display = 'none';
            
            const unit = selectedUnits[0];
            const unitName = document.getElementById('unit-name');
            const unitHealth = document.getElementById('unit-health');
            const unitType = document.getElementById('unit-type');
            const unitHealthFill = document.getElementById('unit-health-fill');
            const abilityDiv = document.getElementById('unit-special-ability');
            const abilityName = document.getElementById('unit-ability-name');
            
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
        } else if (selectedUnits.length > 1) {
            // Multiple units selected
            panel.style.display = 'block';
            singleInfo.style.display = 'none';
            multiInfo.style.display = 'block';
            
            // Count units by type
            const unitCounts = {};
            selectedUnits.forEach(unit => {
                unitCounts[unit.type] = (unitCounts[unit.type] || 0) + 1;
            });
            
            // Display counts
            const countList = document.getElementById('unit-count-list');
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
    
    update() {
        // Update camera with WASD (fixed inverted controls)
        const cameraSpeed = 10 / this.camera.zoom;
        if (this.keys['w'] || this.keys['W'] || this.keys['ArrowUp']) this.camera.y += cameraSpeed;
        if (this.keys['s'] || this.keys['S'] || this.keys['ArrowDown']) this.camera.y -= cameraSpeed;
        if (this.keys['a'] || this.keys['A'] || this.keys['ArrowLeft']) this.camera.x -= cameraSpeed;
        if (this.keys['d'] || this.keys['D'] || this.keys['ArrowRight']) this.camera.x += cameraSpeed;
        
        // Clamp camera to world bounds (with some padding based on zoom)
        const padding = 500 / this.camera.zoom; // More padding when zoomed in
        const halfWidth = this.worldBounds.width / 2;
        const halfHeight = this.worldBounds.height / 2;
        this.camera.x = Math.max(-halfWidth - padding, Math.min(halfWidth + padding, this.camera.x));
        this.camera.y = Math.max(-halfHeight - padding, Math.min(halfHeight + padding, this.camera.y));
        
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
        
        // Update build preview
        if (this.buildMode && this.buildPreview) {
            this.buildPreview.position.set(this.mouseWorldPos.x, this.mouseWorldPos.y);
            
            // Update color based on validity
            const isValid = this.isValidBuildLocation(this.mouseWorldPos, this.buildingType);
            if (this.buildPreview.shapeGraphics) {
                this.buildPreview.shapeGraphics.clear();
                const buildingInfo = this.getBuildingInfo(this.buildingType);
                this.buildPreview.shapeGraphics.circle(0, 0, buildingInfo.size);
                this.buildPreview.shapeGraphics.fill({ 
                    color: isValid ? 0x00FF00 : 0xFF0000, 
                    alpha: 0.3 
                });
                this.buildPreview.shapeGraphics.stroke({ 
                    width: 2, 
                    color: isValid ? 0xFFFFFF : 0xFF0000 
                });
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
                    // Start unit selection box
                    this.selectedBuilding = null;
                    this.hideProductionUI();
                    this.isSelecting = true;
                    this.selectionStart = this.mouseWorldPos;
                }
            }
        } else if (e.button === 2) { // Right click
            if (this.buildMode) {
                this.exitBuildMode();
            } else if (this.selectedBuilding && this.selectedBuilding.canProduceUnits && this.selectedBuilding.ownerId === this.myPlayerId) {
                // Set rally point for selected production building
                this.setRallyPoint(this.selectedBuilding.id, this.mouseWorldPos);
            } else {
                // Check for force attack modifier (CMD on Mac, CTRL on Windows/Linux)
                const forceAttack = e.metaKey || e.ctrlKey;
                
                // Issue move/attack order for units
                this.issueOrder(this.mouseWorldPos, forceAttack);
            }
        }
    }
    
    onMouseMove(e) {
        const screenPos = { x: e.clientX, y: e.clientY };
        this.mouseWorldPos = this.screenToWorld(screenPos);
        
        // Update cursor based on modifier keys
        const forceAttackMode = e.metaKey || e.ctrlKey;
        if (forceAttackMode && this.selectedUnits.size > 0) {
            this.app.canvas.style.cursor = 'crosshair'; // Attack cursor
        } else if (this.buildMode) {
            this.app.canvas.style.cursor = 'cell'; // Build cursor
        } else {
            this.app.canvas.style.cursor = 'default';
        }
        
        // Update selection box
        if (this.isSelecting && this.selectionStart) {
            // TODO: Draw selection box
        }
    }
    
    onMouseUp(e) {
        if (e.button === 0 && this.isSelecting) {
            this.finishSelection();
            this.isSelecting = false;
            this.selectionStart = null;
        }
    }
    
    onMouseWheel(e) {
        e.preventDefault();
        // Reduced zoom speed for better trackpad control
        const zoomSpeed = 0.02; // Changed from 0.1 to 0.02 (5x slower)
        const delta = e.deltaY > 0 ? -zoomSpeed : zoomSpeed;
        this.camera.zoom = Math.max(0.2, Math.min(2.0, this.camera.zoom + delta));
    }
    
    onKeyDown(e) {
        this.keys[e.key] = true;
        
        // Hotkeys
        if (e.key === 'b' || e.key === 'B') {
            this.toggleBuildMenu();
        } else if (e.key === 'Escape') {
            if (this.specialAbilityTargetingMode) {
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
        }
    }
    
    onKeyUp(e) {
        this.keys[e.key] = false;
    }
    
    screenToWorld(screenPos) {
        const localPos = this.gameContainer.toLocal(screenPos);
        return { x: localPos.x, y: localPos.y }; // toLocal already handles the transform
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
                // Clicked on a unit - select just that unit
                this.sendInput({ selectUnits: [clickedUnit] });
                
                // Auto-show build menu if worker is selected
                this.checkAndShowBuildMenu([clickedUnit]);
            } else {
                // Clicked on empty space - deselect all
                this.sendInput({ selectUnits: [] });
                this.hideBuildMenu();
            }
        } else {
            // Drag selection - select all units in box
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
            
            // Auto-show build menu if workers are selected
            this.checkAndShowBuildMenu(selectedIds);
        }
    }
    
    checkAndShowBuildMenu(selectedIds) {
        // Check if any selected units are workers
        let hasWorker = false;
        for (const id of selectedIds) {
            const container = this.units.get(id);
            if (container && container.unitData && container.unitData.type === 'WORKER') {
                hasWorker = true;
                break;
            }
        }
        
        // Show build menu if workers are selected
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
                if (dist < minDist && dist < unitData.size + 10) {
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
        
        // Check if clicking on a wall segment
        let targetWallSegment = null;
        minDist = 100;
        
        this.wallSegments.forEach((container, id) => {
            const segmentData = container.segmentData;
            if (segmentData) {
                const dist = Math.sqrt(
                    Math.pow(segmentData.x - worldPos.x, 2) + 
                    Math.pow(segmentData.y - worldPos.y, 2)
                );
                // Wall segments are rectangles, use approximate collision
                if (dist < minDist && dist < segmentData.length / 2 + 20) {
                    minDist = dist;
                    targetWallSegment = segmentData;
                }
            }
        });
        
        // Check if clicking on a resource deposit
        let targetResource = null;
        minDist = 100;
        
        this.resourceDeposits.forEach((container, id) => {
            const resourceData = container.resourceData;
            if (resourceData) {
                const dist = Math.sqrt(
                    Math.pow(resourceData.x - worldPos.x, 2) + 
                    Math.pow(resourceData.y - worldPos.y, 2)
                );
                if (dist < minDist && dist < resourceData.size + 10) {
                    minDist = dist;
                    targetResource = resourceData;
                }
            }
        });
        
        // Check if clicking on an obstacle
        let targetObstacle = null;
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
                }
            }
        });
        
        // Issue appropriate command based on target
        if (targetUnit) {
            if (targetUnit.team !== this.myTeam) {
                // Attack enemy unit
                this.sendInput({ attackUnitOrder: targetUnit.id });
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
        } else if (targetWallSegment) {
            if (targetWallSegment.team !== this.myTeam) {
                // Attack enemy wall segment
                this.sendInput({ attackWallSegmentOrder: targetWallSegment.id });
            } else {
                // Move near friendly wall
                this.sendInput({ moveOrder: { x: worldPos.x, y: worldPos.y } });
            }
        } else if (targetResource) {
            // Harvest resource (for workers)
            this.sendInput({ harvestOrder: targetResource.id });
        } else if (targetObstacle) {
            // Mine obstacle (for miners) - only if destructible
            if (targetObstacle.destructible) {
                this.sendInput({ mineOrder: targetObstacle.id });
            } else {
                // Can't mine indestructible obstacles, just move
                this.sendInput({ moveOrder: { x: worldPos.x, y: worldPos.y } });
            }
        } else {
            // Just move to location
            this.sendInput({ moveOrder: { x: worldPos.x, y: worldPos.y } });
        }
    }
    
    /**
     * Check if any selected units are infantry (can garrison)
     */
    hasInfantrySelected() {
        const infantryTypes = ['INFANTRY', 'LASER_INFANTRY', 'PLASMA_TROOPER', 'ROCKET_SOLDIER', 
                               'SNIPER', 'ION_RANGER', 'MEDIC', 'ENGINEER'];
        for (const unitId of this.selectedUnits) {
            const unitContainer = this.units.get(unitId);
            if (unitContainer && unitContainer.unitData && infantryTypes.includes(unitContainer.unitData.type)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Ungarrison units from a bunker
     */
    ungarrisonUnit(buildingId, ungarrisonAll) {
        this.sendInput({
            ungarrisonBuildingId: buildingId,
            ungarrisonAll: ungarrisonAll
        });
    }
    
    enterBuildMode(buildingType) {
        this.buildMode = true;
        this.buildingType = buildingType;
        
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
        
        // Range indicator (for turrets)
        if (buildingType === 'TURRET') {
            const rangeCircle = new PIXI.Graphics();
            rangeCircle.circle(0, 0, 300); // Turret range
            rangeCircle.stroke({ width: 1, color: 0xFF0000, alpha: 0.3 });
            this.buildPreview.addChild(rangeCircle);
        }
        
        this.gameContainer.addChild(this.buildPreview);
        
        document.getElementById('build-menu').style.display = 'none';
    }
    
    exitBuildMode() {
        this.buildMode = false;
        this.buildingType = null;
        
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
        } else {
        }
    }
    
    exitAttackMoveMode() {
        this.attackMoveMode = false;
        document.body.style.cursor = 'default';
    }
    
    getBuildingInfo(buildingType) {
        const buildings = {
            'HEADQUARTERS': { size: 80, cost: 0, name: 'Headquarters' },
            'REFINERY': { size: 60, cost: 300, name: 'Refinery' },
            'BARRACKS': { size: 50, cost: 200, name: 'Barracks' },
            'FACTORY': { size: 70, cost: 400, name: 'Factory' },
            'TURRET': { size: 30, cost: 250, name: 'Turret' },
            'SHIELD_GENERATOR': { size: 30, cost: 400, name: 'Shield Generator' },
            'WALL': { size: 20, cost: 50, name: 'Wall' },
            'POWER_PLANT': { size: 40, cost: 250, name: 'Power Plant' },
            'RESEARCH_LAB': { size: 50, cost: 500, name: 'Research Lab' },
            'WEAPONS_DEPOT': { size: 48, cost: 400, name: 'Weapons Depot' },
            'TECH_CENTER': { size: 60, cost: 800, name: 'Tech Center' },
            'ADVANCED_FACTORY': { size: 65, cost: 1000, name: 'Advanced Factory' },
            'BANK': { size: 35, cost: 600, name: 'Bank' }
        };
        return buildings[buildingType] || { size: 40, cost: 100, name: 'Building' };
    }
    
    isValidBuildLocation(worldPos, buildingType) {
        const buildingInfo = this.getBuildingInfo(buildingType);
        const size = buildingInfo.size;
        
        
        // Check if too close to other buildings
        for (const [id, container] of this.buildings) {
            const building = container.buildingData;
            if (building) {
                const dist = Math.sqrt(
                    Math.pow(building.x - worldPos.x, 2) + 
                    Math.pow(building.y - worldPos.y, 2)
                );
                const minDist = size + building.size + 20; // 20 unit buffer
                if (dist < minDist) {
                    return false;
                }
            }
        }
        
        // Check if too close to obstacles
        for (const [id, container] of this.obstacles) {
            const obstacle = container.obstacleData;
            if (obstacle) {
                // For rectangular obstacles, use AABB collision
                if (obstacle.shape === 'RECTANGLE') {
                    const halfWidth = obstacle.width / 2;
                    const halfHeight = obstacle.height / 2;
                    const buffer = size + 10;
                    
                    // Check if building overlaps with obstacle (with buffer)
                    if (Math.abs(worldPos.x - obstacle.x) < halfWidth + buffer &&
                        Math.abs(worldPos.y - obstacle.y) < halfHeight + buffer) {
                        return false;
                    }
                } else {
                    // For circular/polygon obstacles, use distance check
                    const dist = Math.sqrt(
                        Math.pow(obstacle.x - worldPos.x, 2) + 
                        Math.pow(obstacle.y - worldPos.y, 2)
                    );
                    const minDist = size + obstacle.size + 10;
                    if (dist < minDist) {
                        return false;
                    }
                }
            }
        }
        
        // Check if too close to resource deposits (except for refineries)
        if (buildingType !== 'REFINERY') {
            for (const [id, container] of this.resourceDeposits) {
                const resource = container.resourceData;
                if (resource) {
                    const dist = Math.sqrt(
                        Math.pow(resource.x - worldPos.x, 2) + 
                        Math.pow(resource.y - worldPos.y, 2)
                    );
                    const minDist = size + resource.size + 50; // Increased buffer to prevent overlap
                    if (dist < minDist) {
                        return false;
                    }
                }
            }
        }
        
        // Check world bounds
        const halfWidth = this.worldBounds.width / 2;
        const halfHeight = this.worldBounds.height / 2;
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
        const menu = document.getElementById('build-menu');
        menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
    }
    
    showBuildMenu() {
        const menu = document.getElementById('build-menu');
        if (menu) {
            menu.style.display = 'block';
        }
    }
    
    hideBuildMenu() {
        const menu = document.getElementById('build-menu');
        if (menu) {
            menu.style.display = 'none';
        }
    }
    
    updateBuildMenuAvailability(buildings) {
        // Find player's buildings
        const myBuildings = buildings.filter(b => b.ownerId === this.myPlayerId && b.active && !b.underConstruction);
        const myBuildingTypes = new Set(myBuildings.map(b => b.type));
        
        // Get player's current credits (with safety checks)
        const myCredits = this.myFaction && this.myFaction.credits 
            ? this.myFaction.credits 
            : 0;
        
        // If faction data not loaded yet, can't update availability
        if (!this.myFactionData || !this.myFactionData.availableBuildings) {
            return;
        }
        
        // Update each button
        document.querySelectorAll('.build-button').forEach(button => {
            const buildingType = button.getAttribute('data-building');
            
            // Find building in faction data
            const buildingInfo = this.myFactionData.availableBuildings.find(b => b.buildingType === buildingType);
            
            if (!buildingInfo) {
                // Building not available for this faction - should not happen if menu is generated correctly
                button.style.display = 'none';
                return;
            }
            
            // Get tech requirements from faction data
            const requiredBuildings = buildingInfo.techRequirements || [];
            const cost = buildingInfo.cost;
            
            // Check if all tech requirements are met
            let hasTech = true;
            let missingRequirements = [];
            
            for (const required of requiredBuildings) {
                if (!myBuildingTypes.has(required)) {
                    hasTech = false;
                    missingRequirements.push(this.getBuildingDisplayName(required));
                }
            }
            
            // Check if player has enough credits
            const hasCredits = myCredits >= cost;
            
            // Update button state
            if (!hasTech) {
                // Missing tech requirements - fully disabled
                button.disabled = true;
                button.style.opacity = '0.4';
                button.title = `Requires: ${missingRequirements.join(', ')}`;
            } else if (!hasCredits) {
                // Has tech but not enough credits - greyed out but different style
                button.disabled = true;
                button.style.opacity = '0.6';
                button.title = `Insufficient credits (need ${cost}, have ${myCredits})`;
            } else {
                // Can build
                button.disabled = false;
                button.style.opacity = '1';
                button.title = '';
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
                const specialAbility = unitContainer.unitData.specialAbility;
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
        
        // Clear unit selection
        this.sendInput({ selectUnits: [] });
        
        // Hide build menu when selecting a building
        this.hideBuildMenu();
        
        // Show production UI if:
        // 1. Building can produce units and is not under construction, OR
        // 2. Building is a bunker (has garrison UI), OR
        // 3. Building is a monument (has aura info)
        const isBunker = buildingData.type === 'BUNKER';
        const isMonument = ['PHOTON_SPIRE', 'QUANTUM_NEXUS', 'SANDSTORM_GENERATOR'].includes(buildingData.type);
        const shouldShowUI = (buildingData.canProduceUnits && !buildingData.underConstruction) || isBunker || isMonument;
        
        if (shouldShowUI) {
            this.showProductionUI(buildingData);
        } else {
            this.hideProductionUI();
        }
    }
    
    showProductionUI(buildingData) {
        const panel = document.getElementById('unit-info-panel');
        panel.style.display = 'block';
        
        // Clear previous content
        panel.innerHTML = '';
        
        // Building name
        const title = document.createElement('div');
        title.className = 'unit-name';
        title.textContent = buildingData.type;
        panel.appendChild(title);
        
        // Health bar
        const healthBarContainer = document.createElement('div');
        healthBarContainer.className = 'health-bar';
        const healthFill = document.createElement('div');
        healthFill.className = 'health-fill';
        const healthPercent = (buildingData.health / buildingData.maxHealth) * 100;
        healthFill.style.width = healthPercent + '%';
        healthBarContainer.appendChild(healthFill);
        panel.appendChild(healthBarContainer);
        
        // Health text
        const healthText = document.createElement('div');
        healthText.className = 'unit-stat';
        healthText.innerHTML = `<span>Health:</span><span>${Math.floor(buildingData.health)}/${buildingData.maxHealth}</span>`;
        panel.appendChild(healthText);
        
        // Garrison info (for bunkers)
        if (buildingData.type === 'BUNKER') {
            const garrisonInfo = document.createElement('div');
            garrisonInfo.className = 'unit-stat';
            garrisonInfo.innerHTML = `<span>Garrison:</span><span>${buildingData.garrisonCount || 0}/${buildingData.maxGarrisonCapacity || 0}</span>`;
            panel.appendChild(garrisonInfo);
            
            // Ungarrison button (if units are garrisoned)
            if (buildingData.garrisonCount > 0) {
                const ungarrisonTitle = document.createElement('div');
                ungarrisonTitle.style.marginTop = '15px';
                ungarrisonTitle.style.fontWeight = 'bold';
                ungarrisonTitle.style.color = '#FFD700';
                ungarrisonTitle.textContent = 'Garrison:';
                panel.appendChild(ungarrisonTitle);
                
                const ungarrisonButton = document.createElement('button');
                ungarrisonButton.className = 'build-button';
                ungarrisonButton.textContent = 'Ungarrison One';
                ungarrisonButton.onclick = () => this.ungarrisonUnit(buildingData.id, false);
                panel.appendChild(ungarrisonButton);
                
                const ungarrisonAllButton = document.createElement('button');
                ungarrisonAllButton.className = 'build-button';
                ungarrisonAllButton.textContent = 'Ungarrison All';
                ungarrisonAllButton.onclick = () => this.ungarrisonUnit(buildingData.id, true);
                panel.appendChild(ungarrisonAllButton);
            }
        }
        
        // Monument aura info
        const monumentInfo = {
            'PHOTON_SPIRE': { name: 'Beam Amplifier', effect: '+35% beam damage', radius: 250 },
            'QUANTUM_NEXUS': { name: 'Quantum Shield', effect: '+25% max health', radius: 280 },
            'SANDSTORM_GENERATOR': { name: 'Sandstorm', effect: '5 damage/sec to enemies', radius: 300 }
        };
        
        if (monumentInfo[buildingData.type]) {
            const info = monumentInfo[buildingData.type];
            
            const monumentTitle = document.createElement('div');
            monumentTitle.style.marginTop = '15px';
            monumentTitle.style.fontWeight = 'bold';
            monumentTitle.style.color = '#FFD700';
            monumentTitle.textContent = info.name + ':';
            panel.appendChild(monumentTitle);
            
            const effectInfo = document.createElement('div');
            effectInfo.className = 'unit-stat';
            effectInfo.innerHTML = `<span>Effect:</span><span>${info.effect}</span>`;
            panel.appendChild(effectInfo);
            
            const radiusInfo = document.createElement('div');
            radiusInfo.className = 'unit-stat';
            radiusInfo.innerHTML = `<span>Radius:</span><span>${info.radius}</span>`;
            panel.appendChild(radiusInfo);
            
            const statusInfo = document.createElement('div');
            statusInfo.className = 'unit-stat';
            const status = buildingData.auraActive ? '✓ Active' : '✗ Inactive';
            const statusColor = buildingData.auraActive ? '#00FF00' : '#FF0000';
            statusInfo.innerHTML = `<span>Status:</span><span style="color: ${statusColor}">${status}</span>`;
            panel.appendChild(statusInfo);
        }
        
        // Production buttons
        if (buildingData.canProduceUnits && !buildingData.underConstruction) {
            const productionTitle = document.createElement('div');
            productionTitle.style.marginTop = '15px';
            productionTitle.style.fontWeight = 'bold';
            productionTitle.style.color = '#FFD700';
            productionTitle.textContent = 'Train Units:';
            panel.appendChild(productionTitle);
            
            // Get available units for this building type
            const availableUnits = this.getAvailableUnits(buildingData.type);
            
            availableUnits.forEach(unitType => {
                const button = document.createElement('button');
                button.className = 'build-button';
                button.innerHTML = `${unitType.name} <span class="build-cost">(💰${unitType.cost} ⚙️${unitType.upkeep})</span>`;
                button.onclick = () => this.queueUnitProduction(buildingData.id, unitType.type);
                
                // Disable if can't afford
                if (this.myMoney < unitType.cost) {
                    button.disabled = true;
                    button.style.opacity = '0.5';
                }
                
                panel.appendChild(button);
            });
        }
    }
    
    hideProductionUI() {
        document.getElementById('unit-info-panel').style.display = 'none';
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
            
            // Generate build menu dynamically based on faction data
            this.generateBuildMenu();
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
            const tier = building.requiredTechTier || 1;
            buildingsByTier[tier].push(building);
        });
        
        // Define tier categories
        const tierCategories = {
            1: 'Basic',
            2: 'Advanced (T2)',
            3: 'Elite (T3)'
        };
        
        // Create buttons for each tier
        [1, 2, 3].forEach(tier => {
            if (buildingsByTier[tier].length === 0) return;
            
            // Add category header
            const category = document.createElement('div');
            category.className = 'build-category';
            category.textContent = tierCategories[tier];
            buildMenu.appendChild(category);
            
            // Add building buttons
            buildingsByTier[tier].forEach(building => {
                const button = document.createElement('button');
                button.className = 'build-button';
                button.setAttribute('data-building', building.buildingType);
                
                // Get building icon
                const icon = this.getBuildingIcon(building.buildingType);
                const name = this.getBuildingDisplayName(building.buildingType);
                const cost = building.cost;
                
                button.innerHTML = `${icon} ${name} <span class="build-cost">(${cost})</span>`;
                
                button.addEventListener('click', () => {
                    this.enterBuildMode(building.buildingType);
                });
                
                buildMenu.appendChild(button);
            });
        });
        
    }
    
    /**
     * Get display icon for a building type
     */
    getBuildingIcon(buildingType) {
        const icons = {
            'HEADQUARTERS': '🏛️',
            'POWER_PLANT': '⚡',
            'BARRACKS': '🏰',
            'REFINERY': '🏭',
            'WALL': '🧱',
            'RESEARCH_LAB': '🔬',
            'FACTORY': '🚗',
            'WEAPONS_DEPOT': '🔫',
            'TURRET': '🎯',
            'SHIELD_GENERATOR': '🛡️',
            'TECH_CENTER': '🧪',
            'ADVANCED_FACTORY': '🏭',
            'BANK': '💰',
            'BUNKER': '🏰',
            'SANDSTORM_GENERATOR': '🌪️',
            'QUANTUM_NEXUS': '⚛️',
            'PHOTON_SPIRE': '💎'
        };
        return icons[buildingType] || '🏢';
    }
    
    /**
     * Get display name for a building type
     */
    getBuildingDisplayName(buildingType) {
        const names = {
            'HEADQUARTERS': 'Headquarters',
            'POWER_PLANT': 'Power Plant',
            'BARRACKS': 'Barracks',
            'REFINERY': 'Refinery',
            'WALL': 'Wall',
            'RESEARCH_LAB': 'Research Lab',
            'FACTORY': 'Factory',
            'WEAPONS_DEPOT': 'Weapons Depot',
            'TURRET': 'Turret',
            'SHIELD_GENERATOR': 'Shield Generator',
            'TECH_CENTER': 'Tech Center',
            'ADVANCED_FACTORY': 'Advanced Factory',
            'BANK': 'Bank',
            'BUNKER': 'Bunker',
            'SANDSTORM_GENERATOR': 'Sandstorm Generator',
            'QUANTUM_NEXUS': 'Quantum Nexus',
            'PHOTON_SPIRE': 'Photon Spire'
        };
        return names[buildingType] || buildingType.replace(/_/g, ' ');
    }
    
    /**
     * Get available units for a building, filtered by faction
     */
    getAvailableUnits(buildingType) {
        // If we have faction data, use it
        if (this.myFactionData) {
            // Get the building info from faction data
            const building = this.myFactionData.availableBuildings.find(b => b.buildingType === buildingType);
            
            if (!building || !building.producedUnits || building.producedUnits.length === 0) {
                return [];
            }
            
            // Map produced units to the format expected by UI
            const units = building.producedUnits.map(unitType => {
                const unitInfo = this.myFactionData.availableUnits.find(u => u.unitType === unitType);
                if (!unitInfo) return null;
                
                return {
                    type: unitInfo.unitType,
                    name: this.getUnitDisplayName(unitInfo.unitType),
                    cost: unitInfo.cost, // Faction-modified cost
                    baseCost: unitInfo.baseCost,
                    costModifier: unitInfo.costModifier,
                    upkeep: unitInfo.upkeep // Upkeep cost
                };
            }).filter(u => u !== null);
            
            return units;
        }
        
        // Fallback to hardcoded data if faction data not loaded
        const unitsByBuilding = {
            'HEADQUARTERS': [
                { type: 'WORKER', name: '👷 Worker', cost: 50 },
                { type: 'MINER', name: '⛏️ Miner', cost: 100 }
            ],
            'BARRACKS': [
                { type: 'INFANTRY', name: '🪖 Infantry', cost: 75 },
                { type: 'MEDIC', name: '⚕️ Medic', cost: 100 }
            ],
            'WEAPONS_DEPOT': [
                { type: 'ROCKET_SOLDIER', name: '🚀 Rocket Soldier', cost: 150 },
                { type: 'SNIPER', name: '🎯 Sniper', cost: 200 },
                { type: 'ENGINEER', name: '🔧 Engineer', cost: 150 }
            ],
            'FACTORY': [
                { type: 'JEEP', name: '🚙 Jeep', cost: 200 },
                { type: 'TANK', name: '🛡️ Tank', cost: 400 }
            ],
            'ADVANCED_FACTORY': [
                { type: 'ARTILLERY', name: '💣 Artillery', cost: 500 },
                { type: 'GIGANTONAUT', name: '🏔️ Gigantonaut', cost: 1200 },
                { type: 'CRAWLER', name: '🦂 Crawler', cost: 1500 },
                { type: 'STEALTH_TANK', name: '👻 Stealth Tank', cost: 800 },
                { type: 'MAMMOTH_TANK', name: '🦣 Mammoth Tank', cost: 1200 }
            ]
        };
        
        return unitsByBuilding[buildingType] || [];
    }
    
    /**
     * Get display name for a unit type
     */
    getUnitDisplayName(unitType) {
        const names = {
            'WORKER': '👷 Worker',
            'MINER': '⛏️ Miner',
            'INFANTRY': '🪖 Infantry',
            'LASER_INFANTRY': '⚡ Laser Infantry',
            'MEDIC': '⚕️ Medic',
            'ROCKET_SOLDIER': '🚀 Rocket Soldier',
            'SNIPER': '🎯 Sniper',
            'ENGINEER': '🔧 Engineer',
            'JEEP': '🚙 Jeep',
            'TANK': '🛡️ Tank',
            'ARTILLERY': '💣 Artillery',
            'GIGANTONAUT': '🏔️ Gigantonaut',
            'CRAWLER': '🦂 Crawler',
            'STEALTH_TANK': '👻 Stealth Tank',
            'MAMMOTH_TANK': '🦣 Mammoth Tank',
            // Hero units
            'PALADIN': '⚔️ Paladin',
            'RAIDER': '🏇 Raider',
            'COLOSSUS': '🗿 Colossus',
            'PHOTON_TITAN': '💎 Photon Titan',
            // Beam units
            'PLASMA_TROOPER': '⚡ Plasma Trooper',
            'ION_RANGER': '🔫 Ion Ranger',
            'PHOTON_SCOUT': '✨ Photon Scout',
            'BEAM_TANK': '💠 Beam Tank',
            'PULSE_ARTILLERY': '🌟 Pulse Artillery'
        };
        return names[unitType] || unitType;
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
        }
    }
}


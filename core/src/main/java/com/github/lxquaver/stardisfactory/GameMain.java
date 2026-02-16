package com.github.lxquaver.stardisfactory;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameMain extends ApplicationAdapter {

    // --- Rendu Jeu ---
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private FitViewport viewport;

    // --- UI ---
    private Stage uiStage;
    private Skin skin;
    private ButtonGroup<TextButton> toolGroup;
    private ButtonGroup<TextButton> buildingGroup;

    // Popup planter
    private com.badlogic.gdx.scenes.scene2d.ui.Window planterWindow;
    private Building currentPlanterForUI = null;

    // --- Player ---
    private Texture playerTextureImg;
    private Sprite playerSprite;
    private Vector2 playerPos;
    private final float MOVE_SPEED = 5f;

    // --- Tiles textures (Terrain) ---
    // Mets tes images dans: lwjgl3/assets/
    private Texture tileDirt;
    private Texture tileGrass;
    private Texture tileTilled;
    private Texture tileRoad;

    // --- World Data ---
    private static final int MAP_SIZE = 100;
    private static final int MAP_OFFSET = 50;

    private Terrain[][] terrainGrid;
    private List<Building> buildings;
    private List<Crop> crops;

    // --- État ---
    private String selectedTool = "NONE"; // "TILL" "GRASS" "ROAD" "CLEAR" "BULLDOZE" / "NONE"
    private Building.Type selectedBuildingType = null;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(20, 15, camera);

        uiStage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(uiStage);
        createUI();

        // --- Charger textures tiles ---
        tileDirt = new Texture("tile_dirt.png");
        tileGrass = new Texture("tile_grass.png");
        tileTilled = new Texture("tile_tilled.png");
        tileRoad = new Texture("tile_road.png");

        // Pixel-art net (évite le flou)
        tileDirt.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tileGrass.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tileTilled.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tileRoad.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Joueur
        playerTextureImg = new Texture("player.png");
        playerSprite = new Sprite(playerTextureImg);
        playerSprite.setSize(1f, 1f);
        playerPos = new Vector2(0, 0);

        // Monde
        terrainGrid = new Terrain[MAP_SIZE][MAP_SIZE];
        buildings = new ArrayList<>();
        crops = new ArrayList<>();

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                terrainGrid[x][y] = new Terrain(Terrain.Type.DIRT);
            }
        }

        // Données de test
        terrainGrid[50][50] = new Terrain(Terrain.Type.GRASS);
        terrainGrid[51][50] = new Terrain(Terrain.Type.GRASS);
        terrainGrid[50][51] = new Terrain(Terrain.Type.GRASS);
        terrainGrid[51][51] = new Terrain(Terrain.Type.GRASS);
    }

    private void createUI() {
        skin = new Skin();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());
        pixmap.dispose();

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.checked = skin.newDrawable("white", Color.ROYAL);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        // ===== BARRE OUTILS EN BAS =====
        Table bottom = new Table();
        bottom.bottom();
        bottom.setFillParent(true);

        TextButton btnTill = new TextButton("Labourer", skin);
        TextButton btnGrass = new TextButton("Herbe", skin);
        TextButton btnRoad = new TextButton("Route", skin);
        TextButton btnClear = new TextButton("Nettoyer", skin);

        btnTill.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedTool = "TILL";
                selectedBuildingType = null;
                closePlanterPopup();
                if (buildingGroup != null) buildingGroup.uncheckAll();
            }
        });
        btnGrass.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedTool = "GRASS";
                selectedBuildingType = null;
                closePlanterPopup();
                if (buildingGroup != null) buildingGroup.uncheckAll();
            }
        });
        btnRoad.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedTool = "ROAD";
                selectedBuildingType = null;
                closePlanterPopup();
                if (buildingGroup != null) buildingGroup.uncheckAll();
            }
        });
        btnClear.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedTool = "CLEAR";
                selectedBuildingType = null;
                closePlanterPopup();
                if (buildingGroup != null) buildingGroup.uncheckAll();
            }
        });

        toolGroup = new ButtonGroup<>(btnTill, btnGrass, btnRoad, btnClear);
        toolGroup.setMaxCheckCount(1);
        toolGroup.setMinCheckCount(0);
        toolGroup.setUncheckLast(true);

        bottom.add(btnTill).prefWidth(110).pad(10);
        bottom.add(btnGrass).prefWidth(110).pad(10);
        bottom.add(btnRoad).prefWidth(110).pad(10);
        bottom.add(btnClear).prefWidth(110).pad(10);

        uiStage.addActor(bottom);

        // ===== BARRE BUILDINGS A GAUCHE =====
        Table left = new Table();
        left.setFillParent(true);
        left.left().top().pad(10);

        TextButton btnHQ = new TextButton("HQ", skin);
        TextButton btnCow = new TextButton("Cow Coop", skin);
        TextButton btnConv = new TextButton("Conveyor", skin);
        TextButton btnPlanter = new TextButton("Jardiniere", skin);
        TextButton btnBulldoze = new TextButton("Bulldozer", skin);

        btnHQ.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedBuildingType = Building.Type.MAIN_HQ;
                selectedTool = "NONE";
                closePlanterPopup();
                toolGroup.uncheckAll();
            }
        });
        btnCow.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedBuildingType = Building.Type.COW_COOP;
                selectedTool = "NONE";
                closePlanterPopup();
                toolGroup.uncheckAll();
            }
        });
        btnConv.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedBuildingType = Building.Type.CONVEYOR_BELT;
                selectedTool = "NONE";
                closePlanterPopup();
                toolGroup.uncheckAll();
            }
        });
        btnPlanter.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedBuildingType = Building.Type.PLANTER;
                selectedTool = "NONE";
                closePlanterPopup();
                toolGroup.uncheckAll();
            }
        });
        btnBulldoze.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedTool = "BULLDOZE";
                selectedBuildingType = null;
                closePlanterPopup();
                toolGroup.uncheckAll();
            }
        });

        buildingGroup = new ButtonGroup<>(btnHQ, btnCow, btnConv, btnPlanter, btnBulldoze);
        buildingGroup.setMaxCheckCount(1);
        buildingGroup.setMinCheckCount(0);
        buildingGroup.setUncheckLast(true);

        left.add(btnHQ).width(120).pad(5).row();
        left.add(btnCow).width(120).pad(5).row();
        left.add(btnConv).width(120).pad(5).row();
        left.add(btnPlanter).width(120).pad(5).row();
        left.add(btnBulldoze).width(120).pad(5).row();

        uiStage.addActor(left);

        // ===== BOUTONS SAVE/LOAD EN HAUT A DROITE =====
        Table topRight = new Table();
        topRight.setFillParent(true);
        topRight.top().right().pad(10);

        TextButton btnSave = new TextButton("Save (F5)", skin);
        TextButton btnLoad = new TextButton("Load (F9)", skin);

        btnSave.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { saveGame(); }});
        btnLoad.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { loadGame(); }});

        topRight.add(btnSave).width(120).pad(5).row();
        topRight.add(btnLoad).width(120).pad(5).row();

        uiStage.addActor(topRight);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        handleInput(dt);

        // Update buildings (planter growth)
        for (Building b : buildings) {
            b.update(dt);
        }

        camera.position.set(playerPos.x, playerPos.y, 0);
        camera.update();

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        // Souris monde (pour ghost / bulldoze)
        Vector3 worldMouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        int mouseWorldX = MathUtils.floor(worldMouse.x);
        int mouseWorldY = MathUtils.floor(worldMouse.y);

        // ===== 1) RENDU TERRAIN EN IMAGES (SpriteBatch) =====
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                Terrain t = terrainGrid[x][y];
                Texture tex;

                switch (t.getType()) {
                    case DIRT:   tex = tileDirt; break;
                    case GRASS:  tex = tileGrass; break;
                    case TILLED: tex = tileTilled; break;
                    case ROAD:   tex = tileRoad; break;
                    default:     tex = tileDirt; break;
                }

                float worldX = x - MAP_OFFSET;
                float worldY = y - MAP_OFFSET;

                batch.draw(tex, worldX, worldY, 1f, 1f);
            }
        }

        batch.end();

        // ===== 2) RENDU BUILDINGS + GHOST/HOVER (ShapeRenderer Filled) =====
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Buildings
        for (Building b : buildings) {
            float wx = b.getGridX() - MAP_OFFSET;
            float wy = b.getGridY() - MAP_OFFSET;

            switch (b.getType()) {
                case MAIN_HQ:
                    shapeRenderer.setColor(Color.BLUE);
                    break;
                case COW_COOP:
                    shapeRenderer.setColor(Color.BROWN);
                    break;
                case CONVEYOR_BELT:
                    shapeRenderer.setColor(Color.GRAY);
                    break;

                case PLANTER:
                    // Couleur selon contenu/état
                    if (b.isPlanterEmpty()) {
                        shapeRenderer.setColor(new Color(0.55f, 0.35f, 0.15f, 1f)); // marron vide
                    } else if (b.isPlanterReady()) {
                        if (b.getPlanterCrop() == Building.PlanterCrop.TOMATO) {
                            shapeRenderer.setColor(new Color(0.9f, 0.2f, 0.2f, 1f)); // rouge prêt
                        } else {
                            shapeRenderer.setColor(new Color(0.95f, 0.8f, 0.2f, 1f)); // jaune prêt
                        }
                    } else {
                        if (b.getPlanterCrop() == Building.PlanterCrop.TOMATO) {
                            shapeRenderer.setColor(new Color(0.6f, 0.15f, 0.15f, 1f)); // rouge sombre pousse
                        } else {
                            shapeRenderer.setColor(new Color(0.6f, 0.5f, 0.15f, 1f)); // jaune sombre pousse
                        }
                    }
                    break;

                default:
                    shapeRenderer.setColor(Color.WHITE);
                    break;
            }

            shapeRenderer.rect(wx, wy, b.getType().width, b.getType().height);
        }

        // Highlight simple (case) si outil terrain sélectionné
        if (!selectedTool.equals("NONE") && selectedBuildingType == null && !selectedTool.equals("BULLDOZE")) {
            if (isInInteractionRange(mouseWorldX, mouseWorldY)) {
                shapeRenderer.setColor(1f, 1f, 1f, 0.2f);
            } else {
                shapeRenderer.setColor(1f, 0f, 0f, 0.2f);
            }
            shapeRenderer.rect(mouseWorldX, mouseWorldY, 1, 1);
        }

        // Ghost preview (bâtiment sélectionné)
        if (selectedBuildingType != null) {
            int gridX = mouseWorldX + MAP_OFFSET;
            int gridY = mouseWorldY + MAP_OFFSET;

            boolean inRange = isInInteractionRange(mouseWorldX, mouseWorldY);
            boolean ok = inRange && canPlaceBuilding(selectedBuildingType, gridX, gridY);

            if (ok) shapeRenderer.setColor(0f, 1f, 0f, 0.25f);
            else shapeRenderer.setColor(1f, 0f, 0f, 0.25f);

            shapeRenderer.rect(mouseWorldX, mouseWorldY, selectedBuildingType.width, selectedBuildingType.height);
        }

        // Bulldoze hover
        if (selectedTool.equals("BULLDOZE")) {
            Building hovered = getBuildingAtWorldCell(mouseWorldX, mouseWorldY);
            if (hovered != null) {
                float wx = hovered.getGridX() - MAP_OFFSET;
                float wy = hovered.getGridY() - MAP_OFFSET;
                shapeRenderer.setColor(1f, 0f, 0f, 0.25f);
                shapeRenderer.rect(wx, wy, hovered.getType().width, hovered.getType().height);
            }
        }

        shapeRenderer.end();

        // ===== 3) GRILLE + FOOTPRINT (ShapeRenderer Line) =====
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Grille
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);
        for (int x = 0; x <= MAP_SIZE; x++) {
            shapeRenderer.line(x - MAP_OFFSET, -MAP_OFFSET, x - MAP_OFFSET, MAP_SIZE - MAP_OFFSET);
        }
        for (int y = 0; y <= MAP_SIZE; y++) {
            shapeRenderer.line(-MAP_OFFSET, y - MAP_OFFSET, MAP_SIZE - MAP_OFFSET, y - MAP_OFFSET);
        }

        // Footprint outline (bâtiment sélectionné)
        if (selectedBuildingType != null) {
            int gridX = mouseWorldX + MAP_OFFSET;
            int gridY = mouseWorldY + MAP_OFFSET;

            boolean inRange = isInInteractionRange(mouseWorldX, mouseWorldY);
            boolean ok = inRange && canPlaceBuilding(selectedBuildingType, gridX, gridY);

            shapeRenderer.setColor(ok ? Color.GREEN : Color.RED);

            // contour global
            shapeRenderer.rect(mouseWorldX, mouseWorldY, selectedBuildingType.width, selectedBuildingType.height);

            // sous-cases
            for (int dx = 0; dx < selectedBuildingType.width; dx++) {
                for (int dy = 0; dy < selectedBuildingType.height; dy++) {
                    shapeRenderer.rect(mouseWorldX + dx, mouseWorldY + dy, 1, 1);
                }
            }
        }

        shapeRenderer.end();

        // ===== 4) JOUEUR (SpriteBatch) =====
        batch.begin();
        playerSprite.setPosition(
                playerPos.x - playerSprite.getWidth() / 2f,
                playerPos.y - playerSprite.getHeight() / 2f
        );
        playerSprite.draw(batch);
        batch.end();

        // ===== 5) UI =====
        uiStage.act(dt);
        uiStage.draw();
    }

    private void handleInput(float dt) {
        // Save/Load clavier
        if (Gdx.input.isKeyJustPressed(Keys.F5)) saveGame();
        if (Gdx.input.isKeyJustPressed(Keys.F9)) loadGame();

        // ESC annule
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) cancelSelection();

        // Interagir (E) avec jardinière
        if (Gdx.input.isKeyJustPressed(Keys.E)) {
            tryInteractWithPlanter();
        }

        // Mouvement joueur
        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.Z) || Gdx.input.isKeyPressed(Keys.UP)) {
            playerPos.y += MOVE_SPEED * dt;
        }
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
            playerPos.y -= MOVE_SPEED * dt;
        }
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.Q)) {
            playerPos.x -= MOVE_SPEED * dt;
            playerSprite.setFlip(true, false);
        }
        if (Gdx.input.isKeyPressed(Keys.D)) {
            playerPos.x += MOVE_SPEED * dt;
            playerSprite.setFlip(false, false);
        }

        // Clic droit = annuler sélection (+ ferme popup)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            cancelSelection();
            return;
        }

        // Clic gauche
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            boolean hitUI = uiStage.hit(stageCoords.x, stageCoords.y, true) != null;
            if (hitUI) return;

            Vector3 worldPos = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            int worldGridX = MathUtils.floor(worldPos.x);
            int worldGridY = MathUtils.floor(worldPos.y);

            // Bulldozer
            if (selectedTool.equals("BULLDOZE")) {
                if (isInInteractionRange(worldGridX, worldGridY)) {
                    deleteBuildingAtWorldCell(worldGridX, worldGridY);
                }
                return;
            }

            // Placement bâtiment
            if (selectedBuildingType != null) {
                placeBuilding(worldPos.x, worldPos.y);
                return;
            }

            // Outils terrain
            if (!selectedTool.equals("NONE")) {
                applyTool(worldPos.x, worldPos.y);
            }
        }
    }

    private void tryInteractWithPlanter() {
        int playerWorldX = Math.round(playerPos.x);
        int playerWorldY = Math.round(playerPos.y);

        Building nearbyPlanter = null;

        // Cherche une jardinière dans le carré 3x3 autour du joueur
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Building b = getBuildingAtWorldCell(playerWorldX + dx, playerWorldY + dy);
                if (b != null && b.getType() == Building.Type.PLANTER) {
                    nearbyPlanter = b;
                    break;
                }
            }
            if (nearbyPlanter != null) break;
        }

        if (nearbyPlanter == null) return;

        // Si prêt -> récolter (pas de stockage pour l’instant)
        if (!nearbyPlanter.isPlanterEmpty() && nearbyPlanter.isPlanterReady()) {
            int amount = nearbyPlanter.harvest();
            System.out.println("Harvest: +" + amount + " (stockage plus tard)");
            return;
        }

        // Si vide -> popup choix
        if (nearbyPlanter.isPlanterEmpty()) {
            openPlanterPopup(nearbyPlanter);
        }
    }

    private void openPlanterPopup(Building planter) {
        closePlanterPopup();

        currentPlanterForUI = planter;

        // Style de Window (fond sombre)
        com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle ws =
                new com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(
                        skin.getFont("default"),
                        Color.WHITE,
                        skin.newDrawable("white", new Color(0f, 0f, 0f, 0.7f))
                );

        planterWindow = new com.badlogic.gdx.scenes.scene2d.ui.Window("Jardiniere", ws);
        planterWindow.pad(10);

        TextButton tomatoBtn = new TextButton("Tomate (2min -> 10)", skin);
        TextButton wheatBtn  = new TextButton("Ble (1min -> 5)", skin);
        TextButton cancelBtn = new TextButton("Annuler", skin);

        tomatoBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentPlanterForUI != null) {
                    currentPlanterForUI.plant(Building.PlanterCrop.TOMATO);
                }
                closePlanterPopup();
            }
        });

        wheatBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentPlanterForUI != null) {
                    currentPlanterForUI.plant(Building.PlanterCrop.WHEAT);
                }
                closePlanterPopup();
            }
        });

        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closePlanterPopup();
            }
        });

        planterWindow.add(tomatoBtn).width(240).pad(5).row();
        planterWindow.add(wheatBtn).width(240).pad(5).row();
        planterWindow.add(cancelBtn).width(240).pad(5).row();

        planterWindow.pack();

        // Centre en pixels écran (ScreenViewport)
        float cx = (Gdx.graphics.getWidth() - planterWindow.getWidth()) / 2f;
        float cy = (Gdx.graphics.getHeight() - planterWindow.getHeight()) / 2f;
        planterWindow.setPosition(cx, cy);

        uiStage.addActor(planterWindow);
    }

    private void closePlanterPopup() {
        if (planterWindow != null) {
            planterWindow.remove();
            planterWindow = null;
        }
        currentPlanterForUI = null;
    }

    private void cancelSelection() {
        selectedBuildingType = null;
        selectedTool = "NONE";
        closePlanterPopup();
        if (toolGroup != null) toolGroup.uncheckAll();
        if (buildingGroup != null) buildingGroup.uncheckAll();
    }

    private void placeBuilding(float worldX, float worldY) {
        int worldGridX = MathUtils.floor(worldX);
        int worldGridY = MathUtils.floor(worldY);

        if (!isInInteractionRange(worldGridX, worldGridY)) return;

        int gridX = worldGridX + MAP_OFFSET;
        int gridY = worldGridY + MAP_OFFSET;

        if (!canPlaceBuilding(selectedBuildingType, gridX, gridY)) return;

        buildings.add(new Building(selectedBuildingType, gridX, gridY));
    }

    private boolean canPlaceBuilding(Building.Type type, int gridX, int gridY) {
        if (type == null) return false;

        // bounds footprint
        if (gridX < 0 || gridY < 0) return false;
        if (gridX + type.width > MAP_SIZE) return false;
        if (gridY + type.height > MAP_SIZE) return false;

        // Terrain rules + no ROAD + planter on TILLED
        for (int x = gridX; x < gridX + type.width; x++) {
            for (int y = gridY; y < gridY + type.height; y++) {
                Terrain t = terrainGrid[x][y];

                if (t.getType() == Terrain.Type.ROAD) return false;

                // Animal buildings doivent être sur GRASS
                if (type.category == Building.BuildingCategory.ANIMAL) {
                    if (!t.canPlaceAnimalBuilding()) return false;
                }

                // Jardinière uniquement sur TILLED
                if (type == Building.Type.PLANTER) {
                    if (t.getType() != Terrain.Type.TILLED) return false;
                }
            }
        }

        // Overlap buildings
        for (Building b : buildings) {
            if (rectOverlap(gridX, gridY, type.width, type.height,
                    b.getGridX(), b.getGridY(), b.getType().width, b.getType().height)) {
                return false;
            }
        }

        return true;
    }

    private boolean rectOverlap(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private Building getBuildingAtWorldCell(int worldX, int worldY) {
        int gridX = worldX + MAP_OFFSET;
        int gridY = worldY + MAP_OFFSET;

        for (Building b : buildings) {
            int bx = b.getGridX();
            int by = b.getGridY();
            int bw = b.getType().width;
            int bh = b.getType().height;

            if (gridX >= bx && gridX < bx + bw && gridY >= by && gridY < by + bh) {
                return b;
            }
        }
        return null;
    }

    private void deleteBuildingAtWorldCell(int worldX, int worldY) {
        int gridX = worldX + MAP_OFFSET;
        int gridY = worldY + MAP_OFFSET;

        Iterator<Building> it = buildings.iterator();
        while (it.hasNext()) {
            Building b = it.next();
            int bx = b.getGridX();
            int by = b.getGridY();
            int bw = b.getType().width;
            int bh = b.getType().height;

            if (gridX >= bx && gridX < bx + bw && gridY >= by && gridY < by + bh) {
                it.remove();
                closePlanterPopup();
                return;
            }
        }
    }

    private void applyTool(float worldX, float worldY) {
        int worldGridX = Math.round(worldX);
        int worldGridY = Math.round(worldY);

        if (!isInInteractionRange(worldGridX, worldGridY)) return;

        int gridX = worldGridX + MAP_OFFSET;
        int gridY = worldGridY + MAP_OFFSET;

        if (gridX >= 0 && gridX < MAP_SIZE && gridY >= 0 && gridY < MAP_SIZE) {
            Terrain t = terrainGrid[gridX][gridY];
            switch (selectedTool) {
                case "TILL":  t.till(); break;
                case "GRASS": t.plantGrass(); break;
                case "ROAD":  t.buildRoad(); break;
                case "CLEAR": t.clear(); break;
            }
        }
    }

    private boolean isInInteractionRange(int targetWorldX, int targetWorldY) {
        int playerWorldX = Math.round(playerPos.x);
        int playerWorldY = Math.round(playerPos.y);

        int dx = Math.abs(targetWorldX - playerWorldX);
        int dy = Math.abs(targetWorldY - playerWorldY);

        return dx <= 1 && dy <= 1;
    }

    // ===== SAVE / LOAD =====
    private void saveGame() {
        GameSave save = new GameSave();
        save.mapSize = MAP_SIZE;
        save.mapOffset = MAP_OFFSET;
        save.playerX = playerPos.x;
        save.playerY = playerPos.y;

        save.terrainTypes = new int[MAP_SIZE * MAP_SIZE];
        int idx = 0;
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                save.terrainTypes[idx++] = terrainGrid[x][y].getType().ordinal();
            }
        }

        for (Building b : buildings) {
            GameSave.BuildingSave bs = new GameSave.BuildingSave();
            bs.type = b.getType().name();
            bs.x = b.getGridX();
            bs.y = b.getGridY();
            bs.rotation = b.getRotation();
            save.buildings.add(bs);
        }

        SaveSystem.save(save);
    }

    private void loadGame() {
        GameSave save = SaveSystem.load();
        if (save == null) return;

        if (save.mapSize != MAP_SIZE || save.mapOffset != MAP_OFFSET) return;
        if (save.terrainTypes == null || save.terrainTypes.length != MAP_SIZE * MAP_SIZE) return;

        int idx = 0;
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int ord = save.terrainTypes[idx++];
                Terrain.Type type = Terrain.Type.DIRT;
                if (ord >= 0 && ord < Terrain.Type.values().length) {
                    type = Terrain.Type.values()[ord];
                }
                terrainGrid[x][y] = new Terrain(type);
            }
        }

        playerPos.set(save.playerX, save.playerY);

        buildings.clear();
        if (save.buildings != null) {
            for (GameSave.BuildingSave bs : save.buildings) {
                try {
                    Building.Type t = Building.Type.valueOf(bs.type);
                    Building b = new Building(t, bs.x, bs.y, bs.rotation);
                    buildings.add(b);
                } catch (Exception ignored) {
                    // ignore building invalide
                }
            }
        }

        cancelSelection();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();

        if (playerTextureImg != null) playerTextureImg.dispose();

        if (tileDirt != null) tileDirt.dispose();
        if (tileGrass != null) tileGrass.dispose();
        if (tileTilled != null) tileTilled.dispose();
        if (tileRoad != null) tileRoad.dispose();

        uiStage.dispose();
        skin.dispose();
    }
}
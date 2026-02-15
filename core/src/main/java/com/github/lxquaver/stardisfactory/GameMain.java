package com.github.lxquaver.stardisfactory;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
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

    // --- Player ---
    private Texture playerTextureImg;
    private Sprite playerSprite;
    private Vector2 playerPos;
    private final float MOVE_SPEED = 5f;

    // --- World Data ---
    private static final int MAP_SIZE = 100;
    private static final int MAP_OFFSET = 50;
    private Terrain[][] terrainGrid;
    private List<Building> buildings;
    private List<Crop> crops;

    // --- État ---
    private String selectedTool = "NONE"; // Outil sélectionné

    @Override
    public void create() {
        // 1. Init Moteur Rendu Jeu
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(20, 15, camera); // Vue "Jeu" (Zoomée)

        // 2. Init UI (ATH)
        // ScreenViewport garde l'UI à la même taille pixel peu importe la fenêtre
        uiStage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(uiStage); // L'UI capture les clics en priorité
        createUI();

        // 3. Init Joueur
        playerTextureImg = new Texture("player.png");
        playerSprite = new Sprite(playerTextureImg);
        playerSprite.setSize(1f, 1f);
        playerPos = new Vector2(0, 0);

        // 4. Init Monde
        terrainGrid = new Terrain[MAP_SIZE][MAP_SIZE];
        buildings = new ArrayList<>();
        crops = new ArrayList<>();

        // Remplissage par défaut
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

    /**
     * Crée l'interface utilisateur programmatique (sans fichier skin externe)
     */
    private void createUI() {
        skin = new Skin();

        // Génération d'une texture blanche 1x1 pour les fonds de boutons
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont()); // Police par défaut

        // Configuration du style des boutons
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);    // Normal
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY);       // Clic
        textButtonStyle.checked = skin.newDrawable("white", Color.ROYAL);   // Sélectionné
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY); // Survol
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        // Création de la mise en page (Table)
        Table table = new Table();
        table.bottom(); // Ancrer en bas
        table.setFillParent(true);

        // Création des boutons
        TextButton btnTill = new TextButton("Labourer", skin);
        TextButton btnGrass = new TextButton("Herbe", skin);
        TextButton btnRoad = new TextButton("Route", skin);
        TextButton btnClear = new TextButton("Nettoyer", skin);

        // Ajout aux listeners
        btnTill.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { selectedTool = "TILL"; } });
        btnGrass.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { selectedTool = "GRASS"; } });
        btnRoad.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { selectedTool = "ROAD"; } });
        btnClear.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { selectedTool = "CLEAR"; } });

        // Groupe de boutons pour gérer l'état "checked" (radio buttons)
        ButtonGroup<TextButton> buttonGroup = new ButtonGroup<>(btnTill, btnGrass, btnRoad, btnClear);
        buttonGroup.setMaxCheckCount(1);
        buttonGroup.setMinCheckCount(0);
        buttonGroup.setUncheckLast(true); // Permet de tout désélectionner

        // Ajout à la table (taille relative)
        table.add(btnTill).prefWidth(100).pad(10);
        table.add(btnGrass).prefWidth(100).pad(10);
        table.add(btnRoad).prefWidth(100).pad(10);
        table.add(btnClear).prefWidth(100).pad(10);

        uiStage.addActor(table);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // 1. Logique Jeu (Mouvements + Clics Monde)
        handleInput(dt);

        // 2. Camera Update
        camera.position.set(playerPos.x, playerPos.y, 0);
        camera.update();

        // 3. Clear Screen
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        // 4. Rendu Terrain
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                Terrain t = terrainGrid[x][y];
                switch (t.getType()) {
                    case DIRT:  shapeRenderer.setColor(0.4f, 0.25f, 0.1f, 1); break;
                    case GRASS: shapeRenderer.setColor(0.1f, 0.6f, 0.1f, 1); break;
                    case TILLED: shapeRenderer.setColor(0.3f, 0.15f, 0.05f, 1); break;
                    case ROAD:  shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1); break;
                }
                shapeRenderer.rect(x - MAP_OFFSET, y - MAP_OFFSET, 1, 1);
            }
        }

        // Surbrillance souris sur le terrain (Visualisation)
        Vector3 worldMouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        int mouseWorldX = Math.round(worldMouse.x);
        int mouseWorldY = Math.round(worldMouse.y);

        // Vérification portée et coloration curseur
        if (isInInteractionRange(mouseWorldX, mouseWorldY)) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.2f); // Blanc (Valide)
        } else {
            shapeRenderer.setColor(1f, 0f, 0f, 0.2f); // Rouge (Trop loin)
        }
        shapeRenderer.rect(mouseWorldX, mouseWorldY, 1, 1);
        shapeRenderer.end();

        // 5. Rendu Grille
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND); // Active la transparence pour les lignes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);
        for (int x = 0; x <= MAP_SIZE; x++) {
            shapeRenderer.line(x - MAP_OFFSET, -MAP_OFFSET, x - MAP_OFFSET, MAP_SIZE - MAP_OFFSET);
        }
        for (int y = 0; y <= MAP_SIZE; y++) {
            shapeRenderer.line(-MAP_OFFSET, y - MAP_OFFSET, MAP_SIZE - MAP_OFFSET, y - MAP_OFFSET);
        }
        shapeRenderer.end();

        // 6. Rendu Joueur
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        playerSprite.setPosition(playerPos.x - playerSprite.getWidth() / 2f,
            playerPos.y - playerSprite.getHeight() / 2f);
        playerSprite.draw(batch);
        batch.end();

        // 7. Rendu UI (ATH) par dessus tout le reste
        uiStage.act(dt);
        uiStage.draw();
    }

    private void handleInput(float dt) {
        // --- Clavier (Mouvements) ---
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

        // --- Souris (Action Terrain) ---
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Vérifie si la souris touche l'UI. Si oui, on ne clique pas sur le terrain.
            // On convertit les coordonnées écran Y (inversées dans Gdx.input)
            Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            boolean hitUI = uiStage.hit(stageCoords.x, stageCoords.y, true) != null;

            if (!hitUI && !selectedTool.equals("NONE")) {
                // Conversion coordonnées écran -> Monde
                Vector3 worldPos = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                applyTool(worldPos.x, worldPos.y);
            }
        }
    }

    private void applyTool(float worldX, float worldY) {
        int worldGridX = Math.round(worldX);
        int worldGridY = Math.round(worldY);

        // Vérification de la portée (Carré 3x3 autour du joueur)
        if (!isInInteractionRange(worldGridX, worldGridY)) {
            return; // Trop loin, on annule l'action
        }

        int gridX = worldGridX + MAP_OFFSET;
        int gridY = worldGridY + MAP_OFFSET;

        if (gridX >= 0 && gridX < MAP_SIZE && gridY >= 0 && gridY < MAP_SIZE) {
            Terrain t = terrainGrid[gridX][gridY];
            switch (selectedTool) {
                case "TILL": t.till(); break;
                case "GRASS": t.plantGrass(); break;
                case "ROAD": t.buildRoad(); break;
                case "CLEAR": t.clear(); break;
            }
        }
    }

    /**
     * Vérifie si la coordonnée monde cible est adjacente au joueur (inclus diagonales)
     */
    private boolean isInInteractionRange(int targetWorldX, int targetWorldY) {
        int playerWorldX = Math.round(playerPos.x);
        int playerWorldY = Math.round(playerPos.y);

        int dx = Math.abs(targetWorldX - playerWorldX);
        int dy = Math.abs(targetWorldY - playerWorldY);

        return dx <= 1 && dy <= 1;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiStage.getViewport().update(width, height, true); // Update UI viewport
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (playerTextureImg != null) playerTextureImg.dispose();
        uiStage.dispose();
        skin.dispose();
    }
}

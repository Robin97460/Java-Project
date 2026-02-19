package com.github.lxquaver.stardisfactory;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Le cœur du jeu ! C'est ici que tout se passe : affichage, logique, menus...
 */
public class GameMain extends ApplicationAdapter {

    // --- Outils de Rendu (Le pinceau et la toile) ---
    private SpriteBatch batch; // Pour dessiner les images (textures)
    private ShapeRenderer shapeRenderer; // Pour dessiner des formes géométriques (carrés de couleur)
    private OrthographicCamera camera; // Notre caméra 2D
    private FitViewport viewport; // Gère le redimensionnement de la fenêtre

    // --- Interface Utilisateur (UI) ---
    private Stage uiStage; // La scène qui contient tous les boutons et fenêtres
    private Skin skin; // Le style visuel de l'UI (couleurs, polices...)
    private ButtonGroup<TextButton> toolGroup; // Groupe pour les outils (un seul actif à la fois)
    private ButtonGroup<TextButton> buildingGroup; // Groupe pour les bâtiments

    // Éléments des Menus
    private Table mainMenuTable;
    private Window pauseWindow;
    private TextField nameField;
    private Slider volumeSlider;
    private CheckBox musicCheckBox;
    private Label statsLabel;
    private Label selectionLabel;


    // Fenêtre contextuelle pour la jardinière
    private Window planterWindow;
    private Building currentPlanterForUI = null;

    // Fenêtre contextuelle pour le HQ
    private Window hqWindow;
    private Building currentHQForUI = null;

    // --- Audio ---
    private Music backgroundMusic;

    // --- Le Joueur ---
    private Texture playerTextureImg;
    private Sprite playerSprite;
    private Vector2 playerPos;
    private final float MOVE_SPEED = 5f; // Vitesse de déplacement
    private Animation<TextureRegion> walkAnimationDown;
    private Animation<TextureRegion> walkAnimationUp;
    private Animation<TextureRegion> walkAnimationSide;
    private Animation<TextureRegion> idleAnimationDown;
    private Animation<TextureRegion> idleAnimationUp;
    private Animation<TextureRegion> idleAnimationSide;
    private float stateTime;
    private boolean isMoving;
    private int playerDirection = 2; // 0=up, 1=right, 2=down, 3=left

    // --- Textures du Terrain ---
    // Ces images doivent être dans le dossier 'assets'
    private Texture tileDirt;
    private Texture tileGrass;
    private Texture tileTilled;
    private Texture tileRoad;
    private Texture hqTexture;
    private final float HQ_SPRITE_SCALE = 1.4f;

    // --- Tiled Map ---
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private static final float UNIT_SCALE = 1 / 16f;



    // --- Données du Monde ---
    private static final int MAP_WIDTH = 30;
    private static final int MAP_HEIGHT = 20;
    private static final int MAP_OFFSET = 0;

    private Terrain[][] terrainGrid; // La grille de sol
    private List<Building> buildings; // La liste de tous les bâtiments posés

    // --- État de la sélection ---
    private String selectedTool = "NONE"; // Outil actuel (ex: "TILL" pour labourer)
    private Building.Type selectedBuildingType = null; // Bâtiment à construire
    private int currentRotation = 0; // Rotation actuelle (0: Nord, 1: Est, 2: Sud, 3: Ouest)

    // --- État du Jeu (Menu, Jeu, Pause) ---
    private enum GameState { MENU, PLAYING, PAUSED }
    private GameState currentState = GameState.MENU;

    // --- Statistiques et Paramètres ---
    private String playerName = "Joueur";
    private float musicVolume = 0.5f;
    private boolean musicEnabled = true;
    private int totalTomatoes = 0; // Compteur de tomates récoltées
    private int totalWheat = 0;    // Compteur de blé récolté
    private int money = 0;         // Argent du joueur

    @Override
    public void create() {
        // Initialisation des outils graphiques
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(MAP_WIDTH, MAP_HEIGHT, camera);

        // Initialisation de l'UI
        uiStage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(uiStage); // L'UI reçoit les clics en premier
        createUI();

        // Chargement des images (Textures)
        tileDirt = new Texture("tile_dirt.png");
        tileGrass = new Texture("tile_grass.png");
        tileTilled = new Texture("tile_tilled.png");
        tileRoad = new Texture("tile_road.png");
        hqTexture = new Texture("House.png");


        // On garde le pixel-art bien net (pas de flou quand on zoome)
        tileDirt.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tileGrass.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tileTilled.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tileRoad.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        hqTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        // Chargement de la map Tiled
        tiledMap = new TmxMapLoader().load("maps/main.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, UNIT_SCALE);


        // Chargement de la musique (si le fichier existe)
        try {
            if (Gdx.files.internal("music.mp3").exists()) {
                backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
                backgroundMusic.setLooping(true); // Jouer en boucle
                backgroundMusic.setVolume(musicVolume);
            } else {
                System.out.println("Fichier music.mp3 introuvable dans assets/");
            }
        } catch (Exception e) {
            System.out.println("Erreur chargement musique: " + e.getMessage());
        }

        // Création du joueur
        playerTextureImg = new Texture("player.png");

        // Charger les spritesheets pour chaque direction
        Texture walkDownSheet = new Texture(Gdx.files.internal("Character/Walk_down.png"));
        Texture walkUpSheet = new Texture(Gdx.files.internal("Character/Walk_up.png"));
        Texture walkSideSheet = new Texture(Gdx.files.internal("Character/Walk.png"));
        Texture idleDownSheet = new Texture(Gdx.files.internal("Character/Idle.png"));
        Texture idleUpSheet = new Texture(Gdx.files.internal("Character/Idle_up.png"));
        Texture idleSideSheet = new Texture(Gdx.files.internal("Character/Idle_side.png"));

        // Découper les frames
        TextureRegion[][] tmpWalkDown = TextureRegion.split(walkDownSheet, 32, 32);
        TextureRegion[][] tmpWalkUp = TextureRegion.split(walkUpSheet, 32, 32);
        TextureRegion[][] tmpWalkSide = TextureRegion.split(walkSideSheet, 32, 32);
        TextureRegion[][] tmpIdleDown = TextureRegion.split(idleDownSheet, 32, 32);
        TextureRegion[][] tmpIdleUp = TextureRegion.split(idleUpSheet, 32, 32);
        TextureRegion[][] tmpIdleSide = TextureRegion.split(idleSideSheet, 32, 32);

        // Créer les animations
        walkAnimationDown = new Animation<>(0.1f, tmpWalkDown[0]);
        walkAnimationUp = new Animation<>(0.1f, tmpWalkUp[0]);
        walkAnimationSide = new Animation<>(0.1f, tmpWalkSide[0]);
        idleAnimationDown = new Animation<>(0.2f, tmpIdleDown[0]);
        idleAnimationUp = new Animation<>(0.2f, tmpIdleUp[0]);
        idleAnimationSide = new Animation<>(0.2f, tmpIdleSide[0]);

        // Configurer les boucles
        walkAnimationDown.setPlayMode(Animation.PlayMode.LOOP);
        walkAnimationUp.setPlayMode(Animation.PlayMode.LOOP);
        walkAnimationSide.setPlayMode(Animation.PlayMode.LOOP);
        idleAnimationDown.setPlayMode(Animation.PlayMode.LOOP);
        idleAnimationUp.setPlayMode(Animation.PlayMode.LOOP);
        idleAnimationSide.setPlayMode(Animation.PlayMode.LOOP);

        stateTime = 0f;

        // Sprite initial
        playerSprite = new Sprite(idleAnimationDown.getKeyFrame(0));
        playerSprite.setSize(1f, 1f);
        playerPos = new Vector2(15, 10);

        // Création du monde vide
        terrainGrid = new Terrain[MAP_WIDTH][MAP_HEIGHT];
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                terrainGrid[x][y] = new Terrain(Terrain.Type.DIRT);
            }
        }

        buildings = new ArrayList<>();

        // On commence sur le menu principal
        showMainMenu();
    }

    /**
     * Crée toute l'interface utilisateur (Menus, HUD, Styles).
     */
    private void createUI() {
        skin = new Skin();

        // Création d'une texture blanche de 1x1 pixel pour dessiner les boutons
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont()); // Police par défaut
        pixmap.dispose();

        // --- Définition des styles (apparence) ---

        // Style des boutons texte
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.checked = skin.newDrawable("white", Color.ROYAL); // Bleu quand sélectionné
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        // Style des étiquettes (Labels)
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Style des champs de texte
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", Color.BLUE);
        textFieldStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", textFieldStyle);

        // Style des sliders (barres de volume)
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        sliderStyle.knob = skin.newDrawable("white", Color.ROYAL);
        skin.add("default-horizontal", sliderStyle);

        // Style des cases à cocher
        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.checkboxOn = skin.newDrawable("white", Color.GREEN);
        checkBoxStyle.checkboxOff = skin.newDrawable("white", Color.RED);
        checkBoxStyle.font = skin.getFont("default");
        skin.add("default", checkBoxStyle);

        // Style des fenêtres
        Window.WindowStyle windowStyle = new Window.WindowStyle(
                skin.getFont("default"),
                Color.WHITE,
                skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.9f)) // Fond noir semi-transparent
        );
        skin.add("default", windowStyle);

        // Création des différentes parties de l'UI
        createGameHUD();
        createMainMenuUI();
        createPauseMenuUI();
    }

    /**
     * Crée l'interface visible pendant le jeu (Barres d'outils, Stats).
     */
    private void createGameHUD() {
        // --- Barre d'outils en bas (Terrain) ---
        Table bottom = new Table();
        bottom.bottom();
        bottom.setFillParent(true);
        bottom.setName("HUD_BOTTOM"); // Nom pour pouvoir la cacher/montrer facilement

        TextButton btnTill = new TextButton("Labourer", skin);
        TextButton btnGrass = new TextButton("Herbe", skin);
        TextButton btnRoad = new TextButton("Route", skin);
        TextButton btnClear = new TextButton("Nettoyer", skin);

        // Logique des boutons : quand on clique, on sélectionne l'outil
        btnTill.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectTool("TILL");
            }
        });
        btnGrass.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectTool("GRASS");
            }
        });
        btnRoad.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectTool("ROAD");
            }
        });
        btnClear.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectTool("CLEAR");
            }
        });

        // Groupe pour qu'un seul bouton soit actif à la fois
        toolGroup = new ButtonGroup<>(btnTill, btnGrass, btnRoad, btnClear);
        toolGroup.setMaxCheckCount(1);
        toolGroup.setMinCheckCount(0);
        toolGroup.setUncheckLast(true);

        bottom.add(btnTill).prefWidth(110).pad(10);
        bottom.add(btnGrass).prefWidth(110).pad(10);
        bottom.add(btnRoad).prefWidth(110).pad(10);
        bottom.add(btnClear).prefWidth(110).pad(10);

        uiStage.addActor(bottom);

        // --- Barre de bâtiments à gauche ---
        Table left = new Table();
        left.setFillParent(true);
        left.left().top().pad(10);
        left.setName("HUD_LEFT");

        TextButton btnHQ = new TextButton("HQ", skin);
        TextButton btnCow = new TextButton("Cow Coop", skin);
        TextButton btnConv = new TextButton("Conveyor", skin);
        TextButton btnPlanter = new TextButton("Jardiniere", skin);
        TextButton btnBulldoze = new TextButton("Bulldozer", skin);

        btnHQ.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectBuilding(Building.Type.MAIN_HQ);
            }
        });
        btnCow.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectBuilding(Building.Type.COW_COOP);
            }
        });
        btnConv.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectBuilding(Building.Type.CONVEYOR_BELT);
            }
        });
        btnPlanter.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectBuilding(Building.Type.PLANTER);
            }
        });
        btnBulldoze.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectTool("BULLDOZE");
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

        // --- Stats en haut à gauche ---
        Table statsTable = new Table();
        statsTable.top().left().pad(10);
        statsTable.setFillParent(true);
        statsTable.setName("HUD_STATS");

        statsLabel = new Label("Argent: 0$ | Tomates: 0 | Ble: 0", skin);
        statsTable.add(statsLabel);

        uiStage.addActor(statsTable);

        // --- Indicateur de sélection en haut au centre ---
        Table topCenter = new Table();
        topCenter.top().pad(10);
        topCenter.setFillParent(true);
        topCenter.setName("HUD_SELECTION");

        selectionLabel = new Label("", skin);
        topCenter.add(selectionLabel);

        uiStage.addActor(topCenter);
    }

    // Méthodes utilitaires pour simplifier la sélection
    private void selectTool(String toolName) {
        selectedTool = toolName;
        selectedBuildingType = null;
        updateSelectionLabel();
        closePlanterPopup();
        closeHQPopup();
        if (buildingGroup != null) buildingGroup.uncheckAll();
    }

    private void selectBuilding(Building.Type type) {
        selectedBuildingType = type;
        selectedTool = "NONE";
        updateSelectionLabel();
        closePlanterPopup();
        closeHQPopup();
        if (toolGroup != null) toolGroup.uncheckAll();
    }

    private void updateSelectionLabel() {
        if (selectionLabel == null) return;

        if (selectedBuildingType != null) {
            selectionLabel.setText("Selection: " + selectedBuildingType.name());
        } else if (!selectedTool.equals("NONE")) {
            selectionLabel.setText("Outil: " + selectedTool);
        } else {
            selectionLabel.setText("");
        }
    }

    /**
     * Crée le Menu Principal (Pseudo, Volume, Jouer).
     */
    private void createMainMenuUI() {
        mainMenuTable = new Table();
        mainMenuTable.setFillParent(true);
        mainMenuTable.center();

        Label titleLabel = new Label("STARDIS FACTORY", skin);
        titleLabel.setFontScale(2f);

        Label nameLabel = new Label("Pseudo:", skin);
        nameField = new TextField("Joueur", skin);

        Label volumeLabel = new Label("Volume Musique:", skin);
        volumeSlider = new Slider(0f, 1f, 0.1f, false, skin);
        volumeSlider.setValue(musicVolume);
        volumeSlider.addListener(new ClickListener() {
            @Override public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                updateMusicVolume(volumeSlider.getValue());
            }
        });

        musicCheckBox = new CheckBox(" Musique Active", skin);
        musicCheckBox.setChecked(musicEnabled);
        musicCheckBox.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                musicEnabled = musicCheckBox.isChecked();
                updateMusicVolume(volumeSlider.getValue());
            }
        });

        TextButton playBtn = new TextButton("JOUER", skin);
        playBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                startGame();
            }
        });

        TextButton quitBtn = new TextButton("QUITTER", skin);
        quitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Mise en page du menu
        mainMenuTable.add(titleLabel).padBottom(50).colspan(2).row();
        mainMenuTable.add(nameLabel).right().pad(10);
        mainMenuTable.add(nameField).width(200).pad(10).row();
        mainMenuTable.add(volumeLabel).right().pad(10);
        mainMenuTable.add(volumeSlider).width(200).pad(10).row();
        mainMenuTable.add(musicCheckBox).colspan(2).pad(10).row();
        mainMenuTable.add(playBtn).width(200).height(50).padTop(30).colspan(2).row();
        mainMenuTable.add(quitBtn).width(200).height(50).padTop(10).colspan(2).row();

        uiStage.addActor(mainMenuTable);
    }

    /**
     * Crée le Menu de Pause (Echap).
     */
    private void createPauseMenuUI() {
        pauseWindow = new Window("PAUSE", skin);
        pauseWindow.setModal(true); // Bloque les clics en dehors
        pauseWindow.setMovable(false);
        pauseWindow.pad(20);

        Label volumeLabel = new Label("Volume:", skin);
        final Slider pauseVolumeSlider = new Slider(0f, 1f, 0.1f, false, skin);
        pauseVolumeSlider.setValue(musicVolume);
        pauseVolumeSlider.addListener(new ClickListener() {
            @Override public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                updateMusicVolume(pauseVolumeSlider.getValue());
            }
        });

        TextButton resumeBtn = new TextButton("Reprendre", skin);
        resumeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                resumeGame();
            }
        });

        TextButton saveQuitBtn = new TextButton("Sauvegarder & Menu", skin);
        saveQuitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                saveGame();
                showMainMenu();
            }
        });

        TextButton abandonBtn = new TextButton("Abandonner (Sans Save)", skin);
        abandonBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showMainMenu();
            }
        });

        pauseWindow.add(volumeLabel).pad(5);
        pauseWindow.add(pauseVolumeSlider).width(150).pad(5).row();
        pauseWindow.add(resumeBtn).width(200).pad(5).colspan(2).row();
        pauseWindow.add(saveQuitBtn).width(200).pad(5).colspan(2).row();
        pauseWindow.add(abandonBtn).width(200).pad(5).colspan(2).row();

        pauseWindow.pack();
        // On centre la fenêtre
        pauseWindow.setPosition(
                Gdx.graphics.getWidth() / 2f - pauseWindow.getWidth() / 2f,
                Gdx.graphics.getHeight() / 2f - pauseWindow.getHeight() / 2f
        );

        uiStage.addActor(pauseWindow);
        pauseWindow.setVisible(false); // Caché par défaut
    }

    private void updateMusicVolume(float volume) {
        musicVolume = volume;
        if (backgroundMusic != null) {
            if (musicEnabled) {
                backgroundMusic.setVolume(musicVolume);
                if (!backgroundMusic.isPlaying()) backgroundMusic.play();
            } else {
                backgroundMusic.pause();
            }
        }
    }

    // --- Gestion des États ---

    private void showMainMenu() {
        currentState = GameState.MENU;
        mainMenuTable.setVisible(true);
        pauseWindow.setVisible(false);
        setGameHUDVisible(false); // On cache le HUD du jeu

        // Reset input processor pour être sûr que l'UI reçoit les clics
        Gdx.input.setInputProcessor(uiStage);

        // On joue la musique dans le menu aussi
        if (backgroundMusic != null && musicEnabled && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    private void startGame() {
        playerName = nameField.getText();
        musicVolume = volumeSlider.getValue();
        musicEnabled = musicCheckBox.isChecked();

        updateMusicVolume(musicVolume);

        currentState = GameState.PLAYING;
        mainMenuTable.setVisible(false);
        pauseWindow.setVisible(false);
        setGameHUDVisible(true); // On affiche le HUD du jeu

        // On place le HQ de départ
        buildings.clear();
        // Le HQ est déjà présent dans la map Tiled
    }

    private void resumeGame() {
        currentState = GameState.PLAYING;
        pauseWindow.setVisible(false);
    }

    private void pauseGame() {
        currentState = GameState.PAUSED;
        pauseWindow.setVisible(true);
        pauseWindow.toFront(); // Met la fenêtre au premier plan
    }

    private void setGameHUDVisible(boolean visible) {
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : uiStage.getActors()) {
            // On cherche tous les éléments qui commencent par "HUD_"
            if (actor.getName() != null && actor.getName().startsWith("HUD_")) {
                actor.setVisible(visible);
            }
        }
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        stateTime += dt;

        // --- Logique du jeu (Mise à jour) ---
        if (currentState == GameState.PLAYING) {
            handleInput(dt);

            // On met à jour tous les bâtiments (pousse des plantes, convoyeurs...)
            for (Building b : buildings) {
                b.update(dt);
            }
            updateConveyors(dt);

            // La caméra suit le joueur
            camera.position.set(playerPos.x, playerPos.y, 0);
            camera.update();
            viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else if (currentState == GameState.PAUSED) {
            // En pause, on écoute juste Echap pour reprendre
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                resumeGame();
            }
        }
        // En MENU, pas de logique de jeu

        // --- Rendu Graphique (Dessin) ---
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1); // Fond gris foncé

        // Si on joue ou qu'on est en pause, on dessine le monde en arrière-plan
        if (currentState == GameState.PLAYING || currentState == GameState.PAUSED) {

            // 1) Dessiner la map Tiled (layers seulement)
            mapRenderer.setView(camera);
            mapRenderer.render(new int[]{0, 1, 2}); // Ground, Background, Foreground layers

            // 2) Dessiner les terrains modifiés par-dessus la map
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            for (int x = 0; x < MAP_WIDTH; x++) {
                for (int y = 0; y < MAP_HEIGHT; y++) {
                    Terrain t = terrainGrid[x][y];
                    if (t.getType() != Terrain.Type.DIRT) {
                        Texture tex;
                        switch (t.getType()) {
                            case GRASS:  tex = tileGrass; break;
                            case TILLED: tex = tileTilled; break;
                            case ROAD:   tex = tileRoad; break;
                            default:     tex = null; break;
                        }
                        if (tex != null) {
                            batch.draw(tex, x - MAP_OFFSET, y - MAP_OFFSET, 1f, 1f);
                        }
                    }
                }
            }
            batch.end();

            // 3) Dessiner les objets Tiled
            renderTiledObjects();

            // 4) Dessiner les bâtiments (Carrés de couleur)
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Building b : buildings) {
                if (b.getType() == Building.Type.MAIN_HQ) continue;
                float wx = b.getGridX() - MAP_OFFSET;
                float wy = b.getGridY() - MAP_OFFSET;

                // Choix de la couleur selon le type
                switch (b.getType()) {
                    case COW_COOP: shapeRenderer.setColor(Color.BROWN); break;
                    case CONVEYOR_BELT: shapeRenderer.setColor(Color.GRAY); break;
                    case PLANTER:
                        // Couleur changeante selon l'état de la plante
                        if (b.isPlanterEmpty()) shapeRenderer.setColor(new Color(0.55f, 0.35f, 0.15f, 1f));
                        else if (b.isPlanterReady()) shapeRenderer.setColor(b.getPlanterCrop() == Building.PlanterCrop.TOMATO ? new Color(0.9f, 0.2f, 0.2f, 1f) : new Color(0.95f, 0.8f, 0.2f, 1f));
                        else shapeRenderer.setColor(b.getPlanterCrop() == Building.PlanterCrop.TOMATO ? new Color(0.6f, 0.15f, 0.15f, 1f) : new Color(0.6f, 0.5f, 0.15f, 1f));
                        break;
                    default: shapeRenderer.setColor(Color.WHITE); break;
                }
                shapeRenderer.rect(wx, wy, b.getType().width, b.getType().height);

                // Si c'est un convoyeur avec un objet, on dessine l'objet dessus
                if (b.isConveyor() && b.hasItem()) {
                    shapeRenderer.setColor(b.getHeldItem() == Building.PlanterCrop.TOMATO ? Color.RED : Color.YELLOW);
                    float progress = b.getTransportProgress();
                    float itemX = wx + 0.25f;
                    float itemY = wy + 0.25f;

                    // Animation de déplacement
                    switch (b.getRotation()) {
                        case 0: itemY += progress * 0.5f; break; // Nord
                        case 1: itemX += progress * 0.5f; break; // Est
                        case 2: itemY -= progress * 0.5f; break; // Sud
                        case 3: itemX -= progress * 0.5f; break; // Ouest
                    }
                    shapeRenderer.rect(itemX, itemY, 0.5f, 0.5f);
                }
            }
            shapeRenderer.end();

            // On dessine maintenant les bâtiments avec texture, comme le HQ
            batch.begin();
            for (Building b : buildings) {
                if (b.getType() == Building.Type.MAIN_HQ) {
                    // --- MODIFICATION DE LA TAILLE VISUELLE ---
                    // Taille visuelle souhaitée pour la texture (ex: 4x4)
                    final float visualWidth = b.getType().width * HQ_SPRITE_SCALE;
                    final float visualHeight = b.getType().height * HQ_SPRITE_SCALE;

                    // Taille logique du bâtiment (ex: 3x3, depuis Building.java)
                    final float logicalWidth = b.getType().width;
                    final float logicalHeight = b.getType().height;

                    // On calcule la position pour que la texture plus grande reste centrée sur l'empreinte logique
                    float drawX = (b.getGridX() - MAP_OFFSET + logicalWidth / 2f) - (visualWidth / 2f);
                    float drawY = (b.getGridY() - MAP_OFFSET + logicalHeight / 2f) - (visualHeight / 2.2f);

                    batch.draw(hqTexture, drawX, drawY, visualWidth, visualHeight);
                }
            }
            batch.end(); // On ferme le batch après avoir dessiné le HQ

            // 3) Dessiner les prévisualisations (Ghost) et la sélection
            Vector3 worldMouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            int mouseWorldX = MathUtils.floor(worldMouse.x);
            int mouseWorldY = MathUtils.floor(worldMouse.y);

            if (currentState == GameState.PLAYING) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                // Case sous la souris (Rouge si hors de portée, Jaune si ok)
                if (!selectedTool.equals("NONE") && selectedBuildingType == null && !selectedTool.equals("BULLDOZE")) {
                    shapeRenderer.setColor(isInInteractionRange(mouseWorldX, mouseWorldY) ? Color.YELLOW : Color.RED);
                    shapeRenderer.rect(mouseWorldX, mouseWorldY, 1, 1);
                }
                shapeRenderer.end();

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                // Fantôme du bâtiment à construire
                if (selectedBuildingType != null) {
                    boolean ok = isInInteractionRange(mouseWorldX, mouseWorldY) && canPlaceBuilding(selectedBuildingType, mouseWorldX + MAP_OFFSET, mouseWorldY + MAP_OFFSET);
                    shapeRenderer.setColor(ok ? new Color(0f, 1f, 0f, 0.25f) : new Color(1f, 0f, 0f, 0.25f));
                    shapeRenderer.rect(mouseWorldX, mouseWorldY, selectedBuildingType.width, selectedBuildingType.height);

                    // Indicateur de rotation (petit trait jaune)
                    shapeRenderer.setColor(Color.YELLOW);
                    float cx = mouseWorldX + selectedBuildingType.width / 2f;
                    float cy = mouseWorldY + selectedBuildingType.height / 2f;
                    float len = 0.4f;
                    switch (currentRotation) {
                        case 0: shapeRenderer.rect(cx - 0.05f, cy, 0.1f, len); break;
                        case 1: shapeRenderer.rect(cx, cy - 0.05f, len, 0.1f); break;
                        case 2: shapeRenderer.rect(cx - 0.05f, cy - len, 0.1f, len); break;
                        case 3: shapeRenderer.rect(cx - len, cy - 0.05f, len, 0.1f); break;
                    }
                }
                shapeRenderer.end();
            }


            // 4) Dessiner la grille
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.5f);
            for (int x = 0; x <= MAP_WIDTH; x++) shapeRenderer.line(x - MAP_OFFSET, -MAP_OFFSET, x - MAP_OFFSET, MAP_HEIGHT - MAP_OFFSET);
            for (int y = 0; y <= MAP_HEIGHT; y++) shapeRenderer.line(-MAP_OFFSET, y - MAP_OFFSET, MAP_WIDTH - MAP_OFFSET, y - MAP_OFFSET);
            shapeRenderer.end();

            // 5) Dessiner le joueur
            batch.begin();

            // Choisir la bonne animation selon la direction
            Animation<TextureRegion> currentAnim;
            if (isMoving) {
                switch (playerDirection) {
                    case 0: currentAnim = walkAnimationUp; break;
                    case 1: currentAnim = walkAnimationSide; break;
                    case 2: currentAnim = walkAnimationDown; break;
                    default: currentAnim = walkAnimationSide; break;
                }
            } else {
                switch (playerDirection) {
                    case 0: currentAnim = idleAnimationUp; break;
                    case 1: currentAnim = idleAnimationSide; break;
                    case 2: currentAnim = idleAnimationDown; break;
                    default: currentAnim = idleAnimationSide; break;
                }
            }
            TextureRegion currentFrame = currentAnim.getKeyFrame(stateTime, true);

            playerSprite.setRegion(currentFrame);

            // Flip pour la direction gauche
            if (playerDirection == 3) {
                playerSprite.setFlip(true, false);
            } else {
                playerSprite.setFlip(false, false);
            }

            playerSprite.setPosition(playerPos.x - 0.5f, playerPos.y - 0.5f);
            playerSprite.draw(batch);
            batch.end();

            // Si PAUSE, on assombrit l'écran pour faire joli
            if (currentState == GameState.PAUSED) {
                Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
                shapeRenderer.rect(-1000, -1000, 2000, 2000); // Grand rectangle noir transparent
                shapeRenderer.end();
            }
        }

        // ===== 6) DESSINER L'UI (Boutons, Menus...) =====
        uiStage.act(dt);
        uiStage.draw();
    }

    private void handleInput(float dt) {
        // Pause (Echap)
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            pauseGame();
            return;
        }

        // Save/Load clavier
        if (Gdx.input.isKeyJustPressed(Keys.F5)) saveGame();
        if (Gdx.input.isKeyJustPressed(Keys.F9)) loadGame();

        // Rotation (R)
        if (Gdx.input.isKeyJustPressed(Keys.R)) {
            currentRotation = (currentRotation + 1) % 4;
        }

        // Interagir (E) avec jardinière ou HQ
        if (Gdx.input.isKeyJustPressed(Keys.E)) {
            tryInteract();
        }

        // Mouvement joueur (ZQSD ou Flèches)
        Vector2 lastPos = new Vector2(playerPos);
        isMoving = false;

        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.Z) || Gdx.input.isKeyPressed(Keys.UP)) {
            playerPos.y += MOVE_SPEED * dt;
            playerDirection = 0; // up
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
            playerPos.y -= MOVE_SPEED * dt;
            playerDirection = 2; // down
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.Q)) {
            playerPos.x -= MOVE_SPEED * dt;
            playerDirection = 3; // left
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Keys.D)) {
            playerPos.x += MOVE_SPEED * dt;
            playerDirection = 1; // right
            isMoving = true;
        }

        // Clic droit = annuler sélection (+ ferme popup)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            cancelSelection();
            return;
        }

        // Clic gauche (Action principale)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Si on clique sur l'UI, on ne fait rien dans le monde
            Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            boolean hitUI = uiStage.hit(stageCoords.x, stageCoords.y, true) != null;
            if (hitUI) return;

            // Conversion coordonnées écran -> coordonnées monde
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

    private boolean isCollidingWithBuilding(float worldX, float worldY) {
        float playerHalfW = playerSprite.getWidth() * 0.5f;
        float playerHalfH = playerSprite.getHeight() * 0.5f;
        float playerLeft = worldX - playerHalfW;
        float playerRight = worldX + playerHalfW;
        float playerBottom = worldY - playerHalfH;
        float playerTop = worldY + playerHalfH;

        for (Building b : buildings) {
            if (b.isPlanter() || b.isConveyor()) continue;

            float bx = b.getGridX() - MAP_OFFSET;
            float by = b.getGridY() - MAP_OFFSET;
            float bw = b.getType().width;
            float bh = b.getType().height;

            if (playerLeft < bx + bw && playerRight > bx &&
                    playerBottom < by + bh && playerTop > by) {
                return true;
            }
        }
        return false;
    }

    private void tryInteract() {
        int playerWorldX = Math.round(playerPos.x);
        int playerWorldY = Math.round(playerPos.y);

        Building nearbyBuilding = null;

        // Cherche un bâtiment interactif dans le carré 3x3 autour du joueur
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Building b = getBuildingAtWorldCell(playerWorldX + dx, playerWorldY + dy);
                if (b != null) {
                    if (b.isPlanter() || b.isHQ()) {
                        nearbyBuilding = b;
                        break;
                    }
                }
            }
            if (nearbyBuilding != null) break;
        }

        if (nearbyBuilding == null) return;

        // --- Interaction avec JARDINIÈRE ---
        if (nearbyBuilding.isPlanter()) {
            // Si prêt -> récolter
            if (!nearbyBuilding.isPlanterEmpty() && nearbyBuilding.isPlanterReady()) {
                Building.PlanterCrop crop = nearbyBuilding.harvest();
                int amount = Building.getYieldFor(crop);

                // Mise à jour stats
                if (crop == Building.PlanterCrop.TOMATO) totalTomatoes += amount;
                if (crop == Building.PlanterCrop.WHEAT) totalWheat += amount;
                updateStatsLabel();

                System.out.println("Harvest: " + crop + " +" + amount);
                return;
            }
            // Si vide -> popup choix
            if (nearbyBuilding.isPlanterEmpty()) {
                openPlanterPopup(nearbyBuilding);
            }
        }

        // --- Interaction avec HQ ---
        else if (nearbyBuilding.isHQ()) {
            openHQPopup(nearbyBuilding);
        }
    }

    private void updateStatsLabel() {
        if (statsLabel != null) {
            statsLabel.setText("Argent: " + money + "$ | Tomates: " + totalTomatoes + " | Ble: " + totalWheat);
        }
    }

    private void openPlanterPopup(Building planter) {
        closePlanterPopup();
        closeHQPopup();

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

        // Centre en pixels écran
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

    private void openHQPopup(Building hq) {
        closePlanterPopup();
        closeHQPopup();

        currentHQForUI = hq;

        Window.WindowStyle ws = new Window.WindowStyle(
                skin.getFont("default"),
                Color.WHITE,
                skin.newDrawable("white", new Color(0f, 0f, 0f, 0.8f))
        );

        hqWindow = new Window("Stockage HQ", ws);
        hqWindow.pad(20);

        int t = hq.getHQStock(Building.PlanterCrop.TOMATO);
        int w = hq.getHQStock(Building.PlanterCrop.WHEAT);

        Label stockLabel = new Label("Stock:\nTomates: " + t + "\nBle: " + w, skin);
        stockLabel.setAlignment(Align.center);

        TextButton sellBtn = new TextButton("TOUT VENDRE", skin);
        sellBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentHQForUI != null) {
                    int t = currentHQForUI.getHQStock(Building.PlanterCrop.TOMATO);
                    int w = currentHQForUI.getHQStock(Building.PlanterCrop.WHEAT);

                    // Calcul du gain (Tomate=10$, Blé=5$)
                    int gain = (t * 10) + (w * 5);
                    money += gain;

                    currentHQForUI.clearHQStock();
                    updateStatsLabel();
                    closeHQPopup(); // On ferme pour rafraîchir ou juste finir
                }
            }
        });

        TextButton closeBtn = new TextButton("Fermer", skin);
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeHQPopup();
            }
        });

        hqWindow.add(stockLabel).pad(10).row();
        hqWindow.add(sellBtn).width(200).pad(10).row();
        hqWindow.add(closeBtn).width(200).pad(5).row();

        hqWindow.pack();
        hqWindow.setPosition(
                Gdx.graphics.getWidth() / 2f - hqWindow.getWidth() / 2f,
                Gdx.graphics.getHeight() / 2f - hqWindow.getHeight() / 2f
        );

        uiStage.addActor(hqWindow);
    }

    private void closeHQPopup() {
        if (hqWindow != null) {
            hqWindow.remove();
            hqWindow = null;
        }
        currentHQForUI = null;
    }

    private void cancelSelection() {
        selectedBuildingType = null;
        selectedTool = "NONE";
        updateSelectionLabel();
        closePlanterPopup();
        closeHQPopup();
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

        buildings.add(new Building(selectedBuildingType, gridX, gridY, currentRotation));
    }

    private boolean canPlaceBuilding(Building.Type type, int gridX, int gridY) {
        if (type == null) return false;

        // bounds footprint
        if (gridX < 0 || gridY < 0) return false;
        if (gridX + type.width > MAP_WIDTH) return false;
        if (gridY + type.height > MAP_HEIGHT) return false;

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

                // HQ peut être placé sur l'herbe
                if (type == Building.Type.MAIN_HQ) {
                    if (t.getType() != Terrain.Type.GRASS) return false;
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
                closeHQPopup();
                return;
            }
        }
    }

    private void applyTool(float worldX, float worldY) {
        int worldGridX = MathUtils.floor(worldX);
        int worldGridY = MathUtils.floor(worldY);

        if (!isInInteractionRange(worldGridX, worldGridY)) return;

        int gridX = worldGridX + MAP_OFFSET;
        int gridY = worldGridY + MAP_OFFSET;

        if (gridX >= 0 && gridX < MAP_WIDTH && gridY >= 0 && gridY < MAP_HEIGHT) {
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
        int playerWorldY = Math.round(playerPos.y - 0.5f);

        int dx = Math.abs(targetWorldX - playerWorldX);
        int dy = Math.abs(targetWorldY - playerWorldY);

        return dx <= 1 && dy <= 1;
    }

    // ===== SAVE / LOAD =====
    private void saveGame() {
        GameSave save = new GameSave();
        save.mapSize = MAP_WIDTH;
        save.mapOffset = MAP_OFFSET;

        save.terrainTypes = new int[MAP_WIDTH * MAP_HEIGHT];
        int idx = 0;
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
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

        if (save.mapSize != MAP_WIDTH || save.mapOffset != MAP_OFFSET) return;
        if (save.terrainTypes == null || save.terrainTypes.length != MAP_WIDTH * MAP_HEIGHT) return;

        int idx = 0;
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
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

    private void renderTiledObjects() {
        MapLayer objectLayer = null;
        for (MapLayer layer : tiledMap.getLayers()) {
            if (layer.getName().equals("Objects")) {
                objectLayer = layer;
                break;
            }
        }
        if (objectLayer == null) return;

        MapObjects objects = objectLayer.getObjects();
        if (objects == null) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (MapObject obj : objects) {
            if (obj instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tileObj = (TiledMapTileMapObject) obj;
                TiledMapTile tile = tileObj.getTile();
                if (tile != null) {
                    float x = tileObj.getX() * UNIT_SCALE;
                    float y = tileObj.getY() * UNIT_SCALE;
                    float width = tile.getTextureRegion().getRegionWidth() * UNIT_SCALE;
                    float height = tile.getTextureRegion().getRegionHeight() * UNIT_SCALE;

                    batch.draw(tile.getTextureRegion(), x, y, width, height);
                }
            }
        }

        batch.end();
    }

    private void updateConveyors(float dt) {
        for (Building b : buildings) {
            if (!b.isConveyor()) continue;

            if (!b.hasItem()) {
                tryFeedConveyor(b);
            }

            if (b.hasItem() && b.getTransportProgress() >= 1f) {
                int frontX = getFrontGridX(b);
                int frontY = getFrontGridY(b);
                Building front = getBuildingCoveringGridCell(frontX, frontY);

                if (front != null && front.isHQ() && canConveyorOutputToHQSide(b, front, frontX)) {
                    front.addToHQStock(b.takeItem(), 1);
                }
            }
        }
    }

    private void tryFeedConveyor(Building conveyor) {
        int[][] neighbors = new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        for (int[] dir : neighbors) {
            int nx = conveyor.getGridX() + dir[0];
            int ny = conveyor.getGridY() + dir[1];
            Building neighbor = getBuildingAtGridCell(nx, ny);
            if (neighbor == null || !neighbor.isConveyor()) continue;
            if (!neighbor.hasItem() || neighbor.getTransportProgress() < 1f) continue;

            if (getFrontGridX(neighbor) == conveyor.getGridX() && getFrontGridY(neighbor) == conveyor.getGridY()) {
                conveyor.receiveItem(neighbor.takeItem());
                return;
            }
        }

        for (int[] dir : neighbors) {
            int nx = conveyor.getGridX() + dir[0];
            int ny = conveyor.getGridY() + dir[1];
            Building neighbor = getBuildingAtGridCell(nx, ny);
            if (neighbor != null && neighbor.isPlanter() && !neighbor.isPlanterEmpty() && neighbor.isPlanterReady()) {
                conveyor.receiveItem(neighbor.harvest());
                return;
            }
        }
    }

    private int getFrontGridX(Building b) {
        int x = b.getGridX();
        if (b.getRotation() == 1) x += 1;
        if (b.getRotation() == 3) x -= 1;
        return x;
    }

    private int getFrontGridY(Building b) {
        int y = b.getGridY();
        if (b.getRotation() == 0) y += 1;
        if (b.getRotation() == 2) y -= 1;
        return y;
    }

    private boolean canConveyorOutputToHQSide(Building conveyor, Building hq, int hqCellX) {
        if (conveyor.getRotation() == 1) {
            return hqCellX == hq.getGridX();
        }
        if (conveyor.getRotation() == 3) {
            return hqCellX == hq.getGridX() + hq.getType().width - 1;
        }
        return false;
    }

    private Building getBuildingAtGridCell(int gridX, int gridY) {
        for (Building b : buildings) {
            if (b.getGridX() == gridX && b.getGridY() == gridY) {
                return b;
            }
        }
        return null;
    }

    private Building getBuildingCoveringGridCell(int gridX, int gridY) {
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

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiStage.getViewport().update(width, height, true);

        // Recentre la fenêtre de pause si visible
        if (pauseWindow != null) {
            pauseWindow.setPosition(
                    width / 2f - pauseWindow.getWidth() / 2f,
                    height / 2f - pauseWindow.getHeight() / 2f
            );
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();

        if (tileDirt != null) tileDirt.dispose();
        if (tileGrass != null) tileGrass.dispose();
        if (tileTilled != null) tileTilled.dispose();
        if (tileRoad != null) tileRoad.dispose();
        if (hqTexture != null) hqTexture.dispose();

        if (backgroundMusic != null) backgroundMusic.dispose();

        uiStage.dispose();
        skin.dispose();
    }
}

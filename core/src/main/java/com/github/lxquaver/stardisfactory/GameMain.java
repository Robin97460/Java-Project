package com.github.lxquaver.stardisfactory;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
    private Table optionsMenuTable;
    private Table controlsMenuTable;
    private TextButton continueBtn;
    private Label statsLabel;
    private Label selectionLabel;


    // Fenêtre contextuelle pour la jardinière
    private Window planterWindow;
    private Building currentPlanterForUI = null;

    // Fenêtre contextuelle pour le HQ
    private Window hqWindow;
    private Building currentHQForUI = null;

    // Fenêtre contextuelle pour l'hôtel des ventes
    private Window auctionWindow;
    private Building currentAuctionForUI = null;

    // --- Audio ---
    private Music backgroundMusic;
    private Sound buyItemSound;
    private Sound sellItemSound;
    private Sound plantationInSound;
    private Sound plantationOutSound;
    private Sound planterPlacementSound;
    private Sound conveyorPlacementSound;
    private Sound tillSound;
    private Sound clearSound;
    private static final float SFX_VOLUME = 0.5f;

    // --- Le Joueur ---
    private Texture playerTextureImg;
    private Sprite playerSprite;
    private Vector2 playerPos;
    private final Vector2 conveyorItemTmp = new Vector2();
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
    private Texture tileGrass;
    private Texture tileTilled;
    private Texture hqTexture;
    private Texture auctionTexture;
    private Texture potatoTexture;
    private Texture strawberryTexture;
    private Texture leekTexture;
    private Texture potatoBagTexture;
    private Texture strawberryBagTexture;
    private Texture leekBagTexture;
    private Texture planterTexture;
    private Texture planterPotatoTexture;
    private Texture planterStrawberryTexture;
    private Texture planterLeekTexture;
    private Texture conveyorTopTexture;
    private Texture conveyorRightTexture;
    private Texture conveyorBottomTexture;
    private Texture conveyorLeftTexture;
    private Texture gameLogoTexture;
    private final float HQ_SPRITE_SCALE = 1.4f;
    private static final float PLANTER_VISUAL_SCALE = 1.0f;
    private static final float READY_FILTER_SCALE = 0.7f;

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
    private enum MenuPage { MAIN, OPTIONS, CONTROLS }
    private MenuPage currentMenuPage = MenuPage.MAIN;

    // --- Statistiques et Paramètres ---
    private String playerName = "Joueur";
    private float musicVolume = 0.1f;
    private int money = 0;
    private int buildingStockPlanter = 0;
    private int buildingStockConveyor = 0;
    private int seedBagsPotato = 0;
    private int seedBagsStrawberry = 0;
    private int seedBagsLeek = 0;
    private int carriedPotato = 0;
    private int carriedStrawberry = 0;
    private int carriedLeek = 0;

    private static final int AUCTION_TAB_SEEDS = 0;
    private static final int AUCTION_TAB_BUILDINGS = 1;
    private static final float AUCTION_TAB_WIDTH = 200f;
    private static final float AUCTION_TAB_HEIGHT = 46f;
    private static final float AUCTION_CARD_WIDTH = 230f;
    private static final float AUCTION_CARD_HEIGHT = 175f;
    private int auctionActiveTab = AUCTION_TAB_SEEDS;

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

        gameLogoTexture = new Texture("LOGO.png");
        gameLogoTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        createUI();

        // Chargement des images (Textures)
        tileGrass = new Texture("tile_grass.png");
        tileTilled = new Texture("tile_tilled.png");
        hqTexture = new Texture("hq.png");
        auctionTexture = new Texture("hq2.png");
        potatoTexture = new Texture("CROPS/Patate.png");
        strawberryTexture = new Texture("CROPS/Fraise.png");
        leekTexture = new Texture("CROPS/Poireaux.png");
        potatoBagTexture = new Texture("CROPS/Patate_bag.png");
        strawberryBagTexture = new Texture("CROPS/Fraise_Bag.png");
        leekBagTexture = new Texture("CROPS/Poireaux_bag.png");
        planterTexture = new Texture("Jardinière.png");
        planterPotatoTexture = new Texture("Jardinière_patate.png");
        planterStrawberryTexture = new Texture("Jardinière_Fraise.png");
        planterLeekTexture = new Texture("Jardinière_Poireau.png");
        conveyorTopTexture = new Texture("Convoyeur_top.png");
        conveyorRightTexture = new Texture("Convoyeur_right.png");
        conveyorBottomTexture = new Texture("Convoyeur_bottom.png");
        conveyorLeftTexture = new Texture("Convoyeur_left.png");


        // On garde le pixel-art bien net (pas de flou quand on zoome)
        tileGrass.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        tileTilled.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        hqTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        auctionTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        potatoTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        strawberryTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        leekTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        potatoBagTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        strawberryBagTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        leekBagTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        planterTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        planterPotatoTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        planterStrawberryTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        planterLeekTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        conveyorTopTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        conveyorRightTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        conveyorBottomTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        conveyorLeftTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

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

        buyItemSound = loadSound("Sound_effect/Buy_item.mp3");
        sellItemSound = loadSound("Sound_effect/Sell_item.mp3");
        plantationInSound = loadSound("Sound_effect/Plantation_in.mp3");
        plantationOutSound = loadSound("Sound_effect/Plantation_out.mp3");
        planterPlacementSound = loadSound("Sound_effect/Jardinière_placement.mp3");
        conveyorPlacementSound = loadSound("Sound_effect/Convoyeur_placement.mp3");
        tillSound = loadSound("Sound_effect/Labourer.ogg");
        clearSound = loadSound("Sound_effect/Clear.ogg");

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
        TextButton btnClear = new TextButton("Nettoyer", skin);

        // Logique des boutons : quand on clique, on sélectionne l'outil
        btnTill.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectTool("TILL");
            }
        });
        btnClear.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectTool("CLEAR");
            }
        });

        // Groupe pour qu'un seul bouton soit actif à la fois
        toolGroup = new ButtonGroup<>(btnTill, btnClear);
        toolGroup.setMaxCheckCount(1);
        toolGroup.setMinCheckCount(0);
        toolGroup.setUncheckLast(true);

        bottom.add(btnTill).prefWidth(110).pad(10);
        bottom.add(btnClear).prefWidth(110).pad(10);

        uiStage.addActor(bottom);

        // --- Barre de bâtiments à gauche ---
        Table left = new Table();
        left.setFillParent(true);
        left.left().top().pad(10);
        left.setName("HUD_LEFT");

        TextButton btnConv = new TextButton("Conveyor", skin);
        TextButton btnPlanter = new TextButton("Jardiniere", skin);
        TextButton btnBulldoze = new TextButton("Bulldozer", skin);

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

        buildingGroup = new ButtonGroup<>(btnConv, btnPlanter, btnBulldoze);
        buildingGroup.setMaxCheckCount(1);
        buildingGroup.setMinCheckCount(0);
        buildingGroup.setUncheckLast(true);

        left.add(btnConv).width(120).pad(5).row();
        left.add(btnPlanter).width(120).pad(5).row();
        left.add(btnBulldoze).width(120).pad(5).row();

        uiStage.addActor(left);

        // --- Stats en haut à gauche ---
        Table statsTable = new Table();
        statsTable.top().left().pad(10);
        statsTable.setFillParent(true);
        statsTable.setName("HUD_STATS");

        statsLabel = new Label("", skin);
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
        closeAuctionPopup();
        if (buildingGroup != null) buildingGroup.uncheckAll();
    }

    private void selectBuilding(Building.Type type) {
        selectedBuildingType = type;
        selectedTool = "NONE";
        updateSelectionLabel();
        closePlanterPopup();
        closeHQPopup();
        closeAuctionPopup();
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
     * Crée le Menu Principal et les sous-pages (Options, Controles).
     */
    private void createMainMenuUI() {
        mainMenuTable = new Table();
        mainMenuTable.setFillParent(true);
        mainMenuTable.center();

        Image logoImage = new Image(gameLogoTexture);
        float logoMaxSize = 260f;
        float logoW = gameLogoTexture.getWidth();
        float logoH = gameLogoTexture.getHeight();
        float logoScale = Math.min(logoMaxSize / logoW, logoMaxSize / logoH);
        float logoDisplayW = logoW * logoScale;
        float logoDisplayH = logoH * logoScale;

        continueBtn = new TextButton("CONTINUER", skin);
        continueBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                continueGame();
            }
        });

        TextButton newGameBtn = new TextButton("NOUVELLE PARTIE", skin);
        newGameBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                SaveSystem.delete();
                startNewGame();
            }
        });

        TextButton optionsBtn = new TextButton("OPTIONS", skin);
        optionsBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showOptionsPage();
            }
        });

        TextButton controlsBtn = new TextButton("CONTROLES", skin);
        controlsBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showControlsPage();
            }
        });

        TextButton quitBtn = new TextButton("QUITTER", skin);
        quitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Mise en page du menu
        mainMenuTable.defaults().pad(8);
        mainMenuTable.add(logoImage).width(logoDisplayW).height(logoDisplayH).padBottom(12).row();
        mainMenuTable.add(continueBtn).width(260).height(48).padTop(10).row();
        mainMenuTable.add(newGameBtn).width(260).height(48).row();
        mainMenuTable.add(optionsBtn).width(260).height(48).row();
        mainMenuTable.add(controlsBtn).width(260).height(48).row();
        mainMenuTable.add(quitBtn).width(260).height(48).padTop(4).row();

        optionsMenuTable = new Table();
        optionsMenuTable.setFillParent(true);
        optionsMenuTable.center();

        Label optionsTitleLabel = new Label("OPTIONS", skin);
        optionsTitleLabel.setFontScale(1.6f);

        Label displayLabel = new Label("Affichage", skin);
        final TextButton displayModeBtn = new TextButton(getDisplayModeLabel(), skin);
        displayModeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                toggleFullscreen();
                displayModeBtn.setText(getDisplayModeLabel());
            }
        });

        Label volumeLabel = new Label("Volume Musique", skin);
        final Label currentVolumeLabel = new Label(String.format("%d%%", Math.round(musicVolume * 100f)), skin);
        Slider menuVolumeSlider = new Slider(0f, 1f, 0.1f, false, skin);
        menuVolumeSlider.setValue(musicVolume);
        menuVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                float value = ((Slider) actor).getValue();
                currentVolumeLabel.setText(String.format("%d%%", Math.round(value * 100f)));
                updateMusicVolume(value);
            }
        });

        TextButton optionsBackBtn = new TextButton("RETOUR", skin);
        optionsBackBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showMainMenuPage();
            }
        });

        optionsMenuTable.defaults().pad(8);
        optionsMenuTable.add(optionsTitleLabel).padBottom(12).row();
        optionsMenuTable.add(displayLabel).left().row();
        optionsMenuTable.add(displayModeBtn).width(340).height(48).padBottom(8).row();
        optionsMenuTable.add(volumeLabel).left().row();
        optionsMenuTable.add(menuVolumeSlider).width(340).height(30).row();
        optionsMenuTable.add(currentVolumeLabel).left().padBottom(10).row();
        optionsMenuTable.add(optionsBackBtn).width(260).height(48).padTop(8).row();

        controlsMenuTable = new Table();
        controlsMenuTable.setFillParent(true);
        controlsMenuTable.center();

        Label controlsTitleLabel = new Label("CONTROLES", skin);
        controlsTitleLabel.setFontScale(1.6f);

        Table controlsContentTable = new Table();
        controlsContentTable.defaults().pad(4).left();
        controlsContentTable.add(new Label("Deplacement: ZQSD / Fleches", skin)).row();
        controlsContentTable.add(new Label("Interaction: E", skin)).row();
        controlsContentTable.add(new Label("Rotation batiment: R", skin)).row();
        controlsContentTable.add(new Label("Action principale: Clic gauche", skin)).row();
        controlsContentTable.add(new Label("Annuler selection: Clic droit", skin)).row();
        controlsContentTable.add(new Label("Pause: Echap", skin)).row();
        controlsContentTable.add(new Label("Sauvegarder rapide: F5", skin)).row();
        controlsContentTable.add(new Label("Charger rapide: F9", skin)).row();

        TextButton controlsBackBtn = new TextButton("RETOUR", skin);
        controlsBackBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showMainMenuPage();
            }
        });

        controlsMenuTable.defaults().pad(8);
        controlsMenuTable.add(controlsTitleLabel).padBottom(12).row();
        controlsMenuTable.add(controlsContentTable).left().padBottom(8).row();
        controlsMenuTable.add(controlsBackBtn).width(260).height(48).padTop(4).row();

        uiStage.addActor(mainMenuTable);
        uiStage.addActor(optionsMenuTable);
        uiStage.addActor(controlsMenuTable);

        showMainMenuPage();
    }

    private void showMainMenuPage() {
        currentMenuPage = MenuPage.MAIN;
        mainMenuTable.setVisible(true);
        optionsMenuTable.setVisible(false);
        controlsMenuTable.setVisible(false);
    }

    private void showOptionsPage() {
        currentMenuPage = MenuPage.OPTIONS;
        mainMenuTable.setVisible(false);
        optionsMenuTable.setVisible(true);
        controlsMenuTable.setVisible(false);
    }

    private void showControlsPage() {
        currentMenuPage = MenuPage.CONTROLS;
        mainMenuTable.setVisible(false);
        optionsMenuTable.setVisible(false);
        controlsMenuTable.setVisible(true);
    }

    private String getDisplayModeLabel() {
        return Gdx.graphics.isFullscreen() ? "Plein ecran" : "Fenetre";
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1280, 720);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    private void startNewGame() {
        playerName = "Joueur";

        currentState = GameState.PLAYING;
        mainMenuTable.setVisible(false);
        pauseWindow.setVisible(false);
        optionsMenuTable.setVisible(false);
        controlsMenuTable.setVisible(false);
        setGameHUDVisible(true);

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                terrainGrid[x][y] = new Terrain(Terrain.Type.GRASS);
            }
        }

        buildings.clear();
        buildings.add(new Building(Building.Type.MAIN_HQ, 55, 55, 0));
        buildings.add(new Building(Building.Type.AUCTION_HOUSE, 55, 51, 0));

        playerPos.set(0, 0);
        currentRotation = 0;
        money = 1000;
        buildingStockPlanter = 1;
        buildingStockConveyor = 0;
        seedBagsPotato = 0;
        seedBagsStrawberry = 0;
        seedBagsLeek = 0;
        carriedPotato = 0;
        carriedStrawberry = 0;
        carriedLeek = 0;
        cancelSelection();
        updateStatsLabel();
    }

    private void continueGame() {
        playerName = "Joueur";

        if (SaveSystem.exists()) {
            if (!loadGame()) {
                startNewGame();
            }
        } else {
            startNewGame();
        }

        currentState = GameState.PLAYING;
        mainMenuTable.setVisible(false);
        pauseWindow.setVisible(false);
        optionsMenuTable.setVisible(false);
        controlsMenuTable.setVisible(false);
        setGameHUDVisible(true);
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
        pauseWindow.add(pauseVolumeSlider).width(280).height(30).pad(5).row();
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
            backgroundMusic.setVolume(musicVolume);
            if (!backgroundMusic.isPlaying()) backgroundMusic.play();
        }
    }

    // --- Gestion des États ---

    private void showMainMenu() {
        currentState = GameState.MENU;
        showMainMenuPage();
        pauseWindow.setVisible(false);
        setGameHUDVisible(false); // On cache le HUD du jeu
        if (continueBtn != null) continueBtn.setDisabled(!SaveSystem.exists());

        // Reset input processor pour être sûr que l'UI reçoit les clics
        Gdx.input.setInputProcessor(uiStage);

        // On joue la musique dans le menu aussi
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    private void resumeGame() {
        currentState = GameState.PLAYING;
        pauseWindow.setVisible(false);
        setGameHUDVisible(true); // On affiche le HUD du jeu

        // On place le HQ de départ
        buildings.clear();
        // Le HQ est déjà présent dans la map Tiled
    }

    private void ensureCoreBuildings() {
        boolean hasHQ = false;
        boolean hasAuctionHouse = false;

        for (Building b : buildings) {
            if (b.isHQ()) hasHQ = true;
            if (b.isAuctionHouse()) hasAuctionHouse = true;
        }

        if (!hasHQ) buildings.add(new Building(Building.Type.MAIN_HQ, 55, 55, 0));
        if (!hasAuctionHouse) buildings.add(new Building(Building.Type.AUCTION_HOUSE, 55, 51, 0));
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
                    Texture tex;
                    switch (t.getType()) {
                        case GRASS:  tex = tileGrass; break;
                        case TILLED: tex = tileTilled; break;
                        default:     tex = tileGrass; break;
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
                if (b.getType() == Building.Type.MAIN_HQ || b.getType() == Building.Type.AUCTION_HOUSE) continue;
                float wx = b.getGridX() - MAP_OFFSET;
                float wy = b.getGridY() - MAP_OFFSET;
                boolean drawRect = true;

                // Choix de la couleur selon le type
                switch (b.getType()) {
                    case CONVEYOR_BELT: shapeRenderer.setColor(Color.GRAY); break;
                    case PLANTER:
                        if (planterTexture != null || planterPotatoTexture != null || planterStrawberryTexture != null || planterLeekTexture != null) {
                            drawRect = false;
                            break;
                        }
                        // Couleur changeante selon l'état de la plante
                        if (b.isPlanterEmpty()) shapeRenderer.setColor(new Color(0.55f, 0.35f, 0.15f, 1f));
                        else if (b.isPlanterReady()) shapeRenderer.setColor(getCropReadyColor(b.getPlanterCrop()));
                        else shapeRenderer.setColor(getCropGrowingColor(b.getPlanterCrop()));
                        break;
                    default: shapeRenderer.setColor(Color.WHITE); break;
                }
                if (drawRect) {
                    if (b.isPlanter()) {
                        float drawWidth = b.getType().width * PLANTER_VISUAL_SCALE;
                        float drawHeight = b.getType().height * PLANTER_VISUAL_SCALE;
                        float drawX = wx + (b.getType().width - drawWidth) / 2f;
                        float drawY = wy + (b.getType().height - drawHeight) / 2f;
                        shapeRenderer.rect(drawX, drawY, drawWidth, drawHeight);
                    } else {
                        shapeRenderer.rect(wx, wy, b.getType().width, b.getType().height);
                    }
                }

                // Si c'est un convoyeur avec un objet, on dessine l'objet dessus
                if (b.isConveyor() && b.hasItem()) {
                    Texture heldItemTexture = getCropTexture(b.getHeldItem());
                    if (heldItemTexture == null) {
                        shapeRenderer.setColor(getCropReadyColor(b.getHeldItem()));
                        Vector2 itemPos = getConveyorItemWorldPosition(b, conveyorItemTmp);
                        float itemX = itemPos.x;
                        float itemY = itemPos.y;
                        shapeRenderer.rect(itemX, itemY, 0.5f, 0.5f);
                    }
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
                } else if (b.getType() == Building.Type.AUCTION_HOUSE) {
                    final float visualWidth = b.getType().width * HQ_SPRITE_SCALE;
                    final float visualHeight = b.getType().height * HQ_SPRITE_SCALE;
                    final float logicalWidth = b.getType().width;
                    final float logicalHeight = b.getType().height;

                    float drawX = (b.getGridX() - MAP_OFFSET + logicalWidth / 2f) - (visualWidth / 2f);
                    float drawY = (b.getGridY() - MAP_OFFSET + logicalHeight / 2f) - (visualHeight / 2.2f);

                    batch.draw(auctionTexture, drawX, drawY, visualWidth, visualHeight);
                } else if (b.isPlanter()) {
                    float wx = b.getGridX() - MAP_OFFSET;
                    float wy = b.getGridY() - MAP_OFFSET;
                    Texture planterStateTexture;

                    if (!b.isPlanterEmpty()) {
                        switch (b.getPlanterCrop()) {
                            case POTATO:
                                planterStateTexture = planterPotatoTexture;
                                break;
                            case STRAWBERRY:
                                planterStateTexture = planterStrawberryTexture;
                                break;
                            case LEEK:
                                planterStateTexture = planterLeekTexture;
                                break;
                            default:
                                planterStateTexture = planterTexture;
                                break;
                        }
                    } else {
                        planterStateTexture = planterTexture;
                    }

                    if (planterStateTexture == null) {
                        planterStateTexture = planterTexture;
                    }

                    if (planterStateTexture != null) {
                        float drawWidth = b.getType().width * PLANTER_VISUAL_SCALE;
                        float drawHeight = b.getType().height * PLANTER_VISUAL_SCALE;
                        float drawX = wx + (b.getType().width - drawWidth) / 2f;
                        float drawY = wy + (b.getType().height - drawHeight) / 2f;
                        batch.draw(planterStateTexture, drawX, drawY, drawWidth, drawHeight);
                    }
                } else if (b.isConveyor()) {
                    Texture conveyorTexture = getConveyorTextureForRotation(b.getRotation());
                    float wx = b.getGridX() - MAP_OFFSET;
                    float wy = b.getGridY() - MAP_OFFSET;
                    if (conveyorTexture != null) {
                        batch.draw(conveyorTexture, wx, wy, b.getType().width, b.getType().height);
                    }
                    if (b.hasItem()) {
                        Texture heldItemTexture = getCropTexture(b.getHeldItem());
                        if (heldItemTexture != null) {
                            Vector2 itemPos = getConveyorItemWorldPosition(b, conveyorItemTmp);
                            float itemX = itemPos.x;
                            float itemY = itemPos.y;
                            batch.draw(heldItemTexture, itemX, itemY, 0.5f, 0.5f);
                        }
                    }
                }
            }
            batch.end(); // On ferme le batch après avoir dessiné le HQ

            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Building b : buildings) {
                if (!b.isPlanter() || b.isPlanterEmpty() || !b.isPlanterReady()) continue;
                float wx = b.getGridX() - MAP_OFFSET;
                float wy = b.getGridY() - MAP_OFFSET;
                float overlaySize = READY_FILTER_SCALE;
                float offset = (1f - overlaySize) * 0.5f;
                Color c = getCropReadyOverlayColor(b.getPlanterCrop());
                shapeRenderer.setColor(c);
                shapeRenderer.rect(wx + offset, wy + offset, overlaySize, overlaySize);
            }
            shapeRenderer.end();

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
                    if (b.isPlanter() || b.isHQ() || b.isAuctionHouse()) {
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

                addToCarried(crop, amount);
                updateStatsLabel();
                playSfx(plantationOutSound);
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
        } else if (nearbyBuilding.isAuctionHouse()) {
            openAuctionPopup(nearbyBuilding);
        }
    }

    private void updateStatsLabel() {
        if (statsLabel != null) {
            statsLabel.setText(
                    "Argent: " + money + "$"
                            + " | Batiments J/C: " + buildingStockPlanter + "/" + buildingStockConveyor
                            + " | Sacs P/F/L: " + seedBagsPotato + "/" + seedBagsStrawberry + "/" + seedBagsLeek
                            + " | Sur moi P/F/L: " + carriedPotato + "/" + carriedStrawberry + "/" + carriedLeek
            );
        }
    }

    private void openPlanterPopup(Building planter) {
        closePlanterPopup();
        closeHQPopup();
        closeAuctionPopup();

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

        TextButton potatoBtn = new TextButton("Patate (sac: " + seedBagsPotato + ")", skin);
        TextButton strawberryBtn = new TextButton("Fraise (sac: " + seedBagsStrawberry + ")", skin);
        TextButton leekBtn = new TextButton("Poireau (sac: " + seedBagsLeek + ")", skin);
        TextButton cancelBtn = new TextButton("Annuler", skin);

        potatoBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentPlanterForUI != null && seedBagsPotato > 0) {
                    seedBagsPotato--;
                    currentPlanterForUI.plant(Building.PlanterCrop.POTATO);
                    updateStatsLabel();
                    playSfx(plantationInSound);
                }
                closePlanterPopup();
            }
        });

        strawberryBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentPlanterForUI != null && seedBagsStrawberry > 0) {
                    seedBagsStrawberry--;
                    currentPlanterForUI.plant(Building.PlanterCrop.STRAWBERRY);
                    updateStatsLabel();
                    playSfx(plantationInSound);
                }
                closePlanterPopup();
            }
        });

        leekBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentPlanterForUI != null && seedBagsLeek > 0) {
                    seedBagsLeek--;
                    currentPlanterForUI.plant(Building.PlanterCrop.LEEK);
                    updateStatsLabel();
                    playSfx(plantationInSound);
                }
                closePlanterPopup();
            }
        });

        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closePlanterPopup();
            }
        });

        planterWindow.add(potatoBtn).width(260).pad(5).row();
        planterWindow.add(strawberryBtn).width(260).pad(5).row();
        planterWindow.add(leekBtn).width(260).pad(5).row();
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
        closeAuctionPopup();

        currentHQForUI = hq;

        Window.WindowStyle ws = new Window.WindowStyle(
                skin.getFont("default"),
                Color.WHITE,
                skin.newDrawable("white", new Color(0f, 0f, 0f, 0.8f))
        );

        hqWindow = new Window("QG - Vente et Stock", ws);
        hqWindow.pad(16);

        Table root = new Table();
        root.defaults().pad(6);

        Table titlePanel = new Table();
        titlePanel.setBackground(skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f)));
        titlePanel.pad(8);
        titlePanel.add(new Label("Stock interne HQ", skin));
        root.add(titlePanel).growX().row();

        Table cards = new Table();
        cards.defaults().pad(6);
        addHQSellRow(cards, "Sac de patate", Building.PlanterCrop.POTATO);
        addHQSellRow(cards, "Sac de fraise", Building.PlanterCrop.STRAWBERRY);
        addHQSellRow(cards, "Sac de poireau", Building.PlanterCrop.LEEK);
        root.add(cards).growX().row();

        TextButton storeBtn = new TextButton("Stocker sur moi -> HQ", skin);
        storeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentHQForUI == null) return;

                if (carriedPotato > 0) {
                    currentHQForUI.addToHQStock(Building.PlanterCrop.POTATO, carriedPotato);
                    carriedPotato = 0;
                }
                if (carriedStrawberry > 0) {
                    currentHQForUI.addToHQStock(Building.PlanterCrop.STRAWBERRY, carriedStrawberry);
                    carriedStrawberry = 0;
                }
                if (carriedLeek > 0) {
                    currentHQForUI.addToHQStock(Building.PlanterCrop.LEEK, carriedLeek);
                    carriedLeek = 0;
                }

                updateStatsLabel();
                openHQPopup(currentHQForUI);
            }
        });

        TextButton closeBtn = new TextButton("Fermer", skin);
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeHQPopup();
            }
        });

        Table footerPanel = new Table();
        footerPanel.setBackground(skin.newDrawable("white", new Color(0.12f, 0.12f, 0.12f, 1f)));
        footerPanel.pad(8);
        footerPanel.defaults().pad(4);
        footerPanel.add(new Label("Sur moi: P " + carriedPotato + " | F " + carriedStrawberry + " | L " + carriedLeek, skin)).left().row();
        footerPanel.add(storeBtn).width(300).left().row();
        root.add(footerPanel).growX().row();

        hqWindow.add(root).row();
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

    private void addHQSellRow(Table table, String title, Building.PlanterCrop crop) {
        int stock = currentHQForUI == null ? 0 : currentHQForUI.getHQStock(crop);
        int unitPrice = Building.getSellPriceFor(crop);

        Table card = new Table();
        card.setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.18f, 1f)));
        card.pad(10);
        card.defaults().pad(3);

        card.add(new Label(title, skin)).row();

        Texture cropTexture = getCropTexture(crop);
        if (cropTexture != null) {
            card.add(new Image(cropTexture)).size(40).padBottom(2).row();
        } else {
            Table icon = new Table();
            icon.setBackground(skin.newDrawable("white", getCropReadyColor(crop)));
            card.add(icon).size(40, 26).padBottom(2).row();
        }

        card.add(new Label(unitPrice + "$ / unite", skin)).row();
        card.add(new Label("Stock: " + stock, skin)).row();

        TextButton sell1 = new TextButton("Vendre 1", skin);
        sell1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                sellFromHQ(crop, 1);
            }
        });

        TextButton sell10 = new TextButton("Vendre 10", skin);
        sell10.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                sellFromHQ(crop, 10);
            }
        });

        TextButton sellAll = new TextButton("Tout vendre", skin);
        sellAll.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentHQForUI != null) {
                    sellFromHQ(crop, currentHQForUI.getHQStock(crop));
                }
            }
        });

        Table buttons = new Table();
        buttons.defaults().pad(2);
        buttons.add(sell1).width(72);
        buttons.add(sell10).width(72);
        buttons.add(sellAll).width(82);

        card.add(buttons).row();
        table.add(card).width(250).top();
    }

    private void addSeedCard(Table table, String title, String detail, Building.PlanterCrop crop, int price) {
        Table card = new Table();
        card.top();
        card.setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.18f, 1f)));
        card.pad(10);
        card.defaults().pad(4);

        card.add(new Label(title, skin)).row();

        Texture cropBagTexture = getCropBagTexture(crop);
        if (cropBagTexture != null) {
            card.add(new Image(cropBagTexture)).size(40).row();
        } else {
            Table icon = new Table();
            icon.setBackground(skin.newDrawable("white", getCropReadyColor(crop)));
            card.add(icon).size(40, 26).row();
        }

        card.add(new Label(price + "$", skin)).row();
        card.add(new Label(detail, skin)).row();

        TextButton buy = new TextButton("BUY", skin);
        buy.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                buySeedBag(crop, price);
            }
        });
        card.add(buy).width(140).padTop(4).row();

        table.add(card).size(AUCTION_CARD_WIDTH, AUCTION_CARD_HEIGHT).top().pad(4);
    }

    private void addBuildingCard(Table table, String title, int price, boolean planter) {
        Table card = new Table();
        card.top();
        card.setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.18f, 1f)));
        card.pad(10);
        card.defaults().pad(4);

        card.add(new Label(title, skin)).row();

        Texture buildingTexture = planter ? planterTexture : conveyorBottomTexture;
        if (buildingTexture != null) {
            card.add(new Image(buildingTexture)).size(56, 32).row();
        } else {
            Table icon = new Table();
            icon.setBackground(skin.newDrawable("white", planter ? new Color(0.55f, 0.35f, 0.15f, 1f) : Color.GRAY));
            card.add(icon).size(56, 32).row();
        }

        card.add(new Label(price + "$", skin)).row();

        TextButton buy = new TextButton("BUY", skin);
        buy.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                buyBuildingStock(planter, price);
            }
        });
        card.add(buy).width(140).padTop(4).row();

        table.add(card).size(AUCTION_CARD_WIDTH, AUCTION_CARD_HEIGHT).top().pad(4);
    }

    private void sellFromHQ(Building.PlanterCrop crop, int amount) {
        if (currentHQForUI == null) return;
        int sold = currentHQForUI.removeFromHQStock(crop, amount);
        if (sold <= 0) return;

        money += sold * Building.getSellPriceFor(crop);
        updateStatsLabel();
        playSfx(sellItemSound);
        openHQPopup(currentHQForUI);
    }

    private void openAuctionPopup(Building auctionHouse) {
        closePlanterPopup();
        closeHQPopup();
        closeAuctionPopup();

        currentAuctionForUI = auctionHouse;

        Window.WindowStyle ws = new Window.WindowStyle(
                skin.getFont("default"),
                Color.WHITE,
                skin.newDrawable("white", new Color(0f, 0f, 0f, 0.82f))
        );

        auctionWindow = new Window("Hotel des ventes", ws);
        auctionWindow.pad(16);

        Table root = new Table();
        root.defaults().pad(6);

        Table tabs = new Table();
        TextButton seedsTab = new TextButton("Graines", skin);
        seedsTab.setChecked(auctionActiveTab == AUCTION_TAB_SEEDS);
        seedsTab.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (auctionActiveTab == AUCTION_TAB_SEEDS) return;
                auctionActiveTab = AUCTION_TAB_SEEDS;
                if (currentAuctionForUI != null) openAuctionPopup(currentAuctionForUI);
            }
        });

        TextButton buildingsTab = new TextButton("Batiments", skin);
        buildingsTab.setChecked(auctionActiveTab == AUCTION_TAB_BUILDINGS);
        buildingsTab.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (auctionActiveTab == AUCTION_TAB_BUILDINGS) return;
                auctionActiveTab = AUCTION_TAB_BUILDINGS;
                if (currentAuctionForUI != null) openAuctionPopup(currentAuctionForUI);
            }
        });

        tabs.setBackground(skin.newDrawable("white", new Color(0.12f, 0.12f, 0.12f, 1f)));
        tabs.pad(8);
        tabs.add(seedsTab).size(AUCTION_TAB_WIDTH, AUCTION_TAB_HEIGHT).padRight(8);
        tabs.add(buildingsTab).size(AUCTION_TAB_WIDTH, AUCTION_TAB_HEIGHT);
        root.add(tabs).growX().row();

        Table content = new Table();
        content.setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)));
        content.pad(8);
        content.defaults().pad(4);

        if (auctionActiveTab == AUCTION_TAB_SEEDS) {
            addSeedCard(content, "Sac de patate", "Rendement 10 | 30s", Building.PlanterCrop.POTATO, 10);
            addSeedCard(content, "Sac de fraise", "Rendement 7 | 1min", Building.PlanterCrop.STRAWBERRY, 15);
            addSeedCard(content, "Sac de poireau", "Rendement 5 | 5min", Building.PlanterCrop.LEEK, 100);
        } else {
            addBuildingCard(content, "Jardiniere", 20, true);
            addBuildingCard(content, "Convoyeur", 50, false);
        }

        root.add(content).growX().row();

        Table stockPanel = new Table();
        stockPanel.setBackground(skin.newDrawable("white", new Color(0.12f, 0.12f, 0.12f, 1f)));
        stockPanel.pad(8);
        stockPanel.defaults().pad(2);
        stockPanel.add(new Label("Stocks sacs P/F/L: " + seedBagsPotato + "/" + seedBagsStrawberry + "/" + seedBagsLeek, skin)).left().row();
        stockPanel.add(new Label("Stocks batiments J/C: " + buildingStockPlanter + "/" + buildingStockConveyor, skin)).left().row();
        root.add(stockPanel).growX().row();

        TextButton closeBtn = new TextButton("Fermer", skin);
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeAuctionPopup();
            }
        });

        auctionWindow.add(root).row();
        auctionWindow.add(closeBtn).width(200).padTop(10).row();

        auctionWindow.pack();
        auctionWindow.setPosition(
                Gdx.graphics.getWidth() / 2f - auctionWindow.getWidth() / 2f,
                Gdx.graphics.getHeight() / 2f - auctionWindow.getHeight() / 2f
        );

        uiStage.addActor(auctionWindow);
    }

    private void closeAuctionPopup() {
        if (auctionWindow != null) {
            auctionWindow.remove();
            auctionWindow = null;
        }
        currentAuctionForUI = null;
    }

    private void buySeedBag(Building.PlanterCrop crop, int price) {
        if (money < price) return;
        money -= price;

        if (crop == Building.PlanterCrop.POTATO) seedBagsPotato++;
        if (crop == Building.PlanterCrop.STRAWBERRY) seedBagsStrawberry++;
        if (crop == Building.PlanterCrop.LEEK) seedBagsLeek++;

        updateStatsLabel();
        playSfx(buyItemSound);
        if (currentAuctionForUI != null) openAuctionPopup(currentAuctionForUI);
    }

    private void buyBuildingStock(boolean planter, int price) {
        if (money < price) return;
        money -= price;
        if (planter) buildingStockPlanter++;
        else buildingStockConveyor++;
        updateStatsLabel();
        playSfx(buyItemSound);
        if (currentAuctionForUI != null) openAuctionPopup(currentAuctionForUI);
    }

    private void addToCarried(Building.PlanterCrop crop, int amount) {
        if (amount <= 0) return;
        if (crop == Building.PlanterCrop.POTATO) carriedPotato += amount;
        if (crop == Building.PlanterCrop.STRAWBERRY) carriedStrawberry += amount;
        if (crop == Building.PlanterCrop.LEEK) carriedLeek += amount;
    }

    private void cancelSelection() {
        selectedBuildingType = null;
        selectedTool = "NONE";
        updateSelectionLabel();
        closePlanterPopup();
        closeHQPopup();
        closeAuctionPopup();
        if (toolGroup != null) toolGroup.uncheckAll();
        if (buildingGroup != null) buildingGroup.uncheckAll();
    }

    private void placeBuilding(float worldX, float worldY) {
        int worldGridX = MathUtils.floor(worldX);
        int worldGridY = MathUtils.floor(worldY);

        if (!isInInteractionRange(worldGridX, worldGridY)) return;

        int gridX = worldGridX + MAP_OFFSET;
        int gridY = worldGridY + MAP_OFFSET;

        if (selectedBuildingType == Building.Type.PLANTER && buildingStockPlanter <= 0) return;
        if (selectedBuildingType == Building.Type.CONVEYOR_BELT && buildingStockConveyor <= 0) return;

        if (!canPlaceBuilding(selectedBuildingType, gridX, gridY)) return;

        buildings.add(new Building(selectedBuildingType, gridX, gridY, currentRotation));
        if (selectedBuildingType == Building.Type.PLANTER) buildingStockPlanter--;
        if (selectedBuildingType == Building.Type.CONVEYOR_BELT) buildingStockConveyor--;
        updateStatsLabel();
        if (selectedBuildingType == Building.Type.PLANTER) playSfx(planterPlacementSound);
        if (selectedBuildingType == Building.Type.CONVEYOR_BELT) playSfx(conveyorPlacementSound);
    }

    private boolean canPlaceBuilding(Building.Type type, int gridX, int gridY) {
        if (type == null) return false;

        // bounds footprint
        if (gridX < 0 || gridY < 0) return false;
        if (gridX + type.width > MAP_WIDTH) return false;
        if (gridY + type.height > MAP_HEIGHT) return false;

        // Terrain rules + planter on TILLED
        for (int x = gridX; x < gridX + type.width; x++) {
            for (int y = gridY; y < gridY + type.height; y++) {
                Terrain t = terrainGrid[x][y];

                // Jardinière uniquement sur TILLED
                if (type == Building.Type.PLANTER) {
                    if (t.getType() != Terrain.Type.TILLED) return false;
                }

                // HQ peut être placé sur l'herbe
                if (type == Building.Type.MAIN_HQ || type == Building.Type.AUCTION_HOUSE) {
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
                if (b.isHQ() || b.isAuctionHouse()) {
                    return;
                }
                it.remove();
                closePlanterPopup();
                closeHQPopup();
                closeAuctionPopup();
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
            Terrain.Type beforeType = t.getType();
            switch (selectedTool) {
                case "TILL":
                    t.till();
                    if (beforeType != t.getType()) playSfx(tillSound);
                    break;
                case "CLEAR":
                    t.clear();
                    if (beforeType != t.getType()) playSfx(clearSound);
                    break;
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
save.playerX = playerPos.x;
save.playerY = playerPos.y;
save.money = money;

save.buildingStockPlanter = buildingStockPlanter;
save.buildingStockConveyor = buildingStockConveyor;

save.seedBagsPotato = seedBagsPotato;
save.seedBagsStrawberry = seedBagsStrawberry;
save.seedBagsLeek = seedBagsLeek;

save.carriedPotato = carriedPotato;
save.carriedStrawberry = carriedStrawberry;
save.carriedLeek = carriedLeek;

save.terrainTypeNames = new String[MAP_WIDTH * MAP_HEIGHT];
int idx = 0;

for (int y = 0; y < MAP_HEIGHT; y++) {
    for (int x = 0; x < MAP_WIDTH; x++) {
        save.terrainTypeNames[idx++] =
            terrainGrid[x][y].getType().name();
    }
}

            }
        }

        for (Building b : buildings) {
            GameSave.BuildingSave bs = new GameSave.BuildingSave();
            bs.type = b.getType().name();
            bs.x = b.getGridX();
            bs.y = b.getGridY();
            bs.rotation = b.getRotation();

            bs.planterCrop = b.getPlanterCrop().name();
            bs.growTimerSeconds = b.getGrowTimerSeconds();
            bs.planterReady = b.isPlanterReady();

            bs.heldItem = b.getHeldItem().name();
            bs.heldAmount = b.getHeldAmount();
            bs.transportTimer = b.getTransportTimerSeconds();

            bs.hqPotatoes = b.getHQStock(Building.PlanterCrop.POTATO);
            bs.hqStrawberries = b.getHQStock(Building.PlanterCrop.STRAWBERRY);
            bs.hqLeeks = b.getHQStock(Building.PlanterCrop.LEEK);
            save.buildings.add(bs);
        }

        SaveSystem.save(save);
    }

    private boolean loadGame() {
        GameSave save = SaveSystem.load();
        if (save == null) return false;

        if (save.mapSize != MAP_SIZE || save.mapOffset != MAP_OFFSET) return false;
        boolean hasNewTerrainFormat = save.terrainTypeNames != null && save.terrainTypeNames.length == MAP_SIZE * MAP_SIZE;
        boolean hasLegacyTerrainFormat = save.terrainTypes != null && save.terrainTypes.length == MAP_SIZE * MAP_SIZE;
        if (!hasNewTerrainFormat && !hasLegacyTerrainFormat) return false;

        int idx = 0;
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                Terrain.Type type = Terrain.Type.GRASS;
                if (hasNewTerrainFormat) {
                    String typeName = save.terrainTypeNames[idx];
                    if (typeName != null) {
                        try {
                            type = Terrain.Type.valueOf(typeName);
                        } catch (Exception ignored) {
                            type = Terrain.Type.GRASS;
                        }
                    }
                } else {
                    int ord = save.terrainTypes[idx];
                    type = mapLegacyTerrainOrdinal(ord);
                }
                idx++;
                terrainGrid[x][y] = new Terrain(type);
            }
        }

        playerPos.set(save.playerX, save.playerY);

        money = save.money;
        buildingStockPlanter = save.buildingStockPlanter;
        buildingStockConveyor = save.buildingStockConveyor;
        seedBagsPotato = save.seedBagsPotato;
        seedBagsStrawberry = save.seedBagsStrawberry;
        seedBagsLeek = save.seedBagsLeek;
        carriedPotato = save.carriedPotato;
        carriedStrawberry = save.carriedStrawberry;
        carriedLeek = save.carriedLeek;

        buildings.clear();
        if (save.buildings != null) {
            for (GameSave.BuildingSave bs : save.buildings) {
                try {
                    Building.Type t = Building.Type.valueOf(bs.type);
                    Building b = new Building(t, bs.x, bs.y, bs.rotation);

                    Building.PlanterCrop planterCrop = Building.PlanterCrop.NONE;
                    if (bs.planterCrop != null) {
                        try {
                            planterCrop = Building.PlanterCrop.valueOf(bs.planterCrop);
                        } catch (Exception ignored) {
                            planterCrop = Building.PlanterCrop.NONE;
                        }
                    }
                    b.setPlanterState(planterCrop, bs.growTimerSeconds, bs.planterReady);

                    Building.PlanterCrop heldItem = Building.PlanterCrop.NONE;
                    if (bs.heldItem != null) {
                        try {
                            heldItem = Building.PlanterCrop.valueOf(bs.heldItem);
                        } catch (Exception ignored) {
                            heldItem = Building.PlanterCrop.NONE;
                        }
                    }
                    b.setConveyorState(heldItem, bs.heldAmount, bs.transportTimer);

                    b.setHQStock(Building.PlanterCrop.POTATO, bs.hqPotatoes);
                    b.setHQStock(Building.PlanterCrop.STRAWBERRY, bs.hqStrawberries);
                    b.setHQStock(Building.PlanterCrop.LEEK, bs.hqLeeks);

                    buildings.add(b);
                } catch (Exception ignored) {
                    // ignore building invalide
                }
            }
        }

        ensureCoreBuildings();
        updateStatsLabel();
        cancelSelection();
        return true;
    }

    private Terrain.Type mapLegacyTerrainOrdinal(int legacyOrdinal) {
        if (legacyOrdinal == 2) return Terrain.Type.TILLED;
        // Legacy DIRT/GRASS/ROAD deviennent GRASS dans la nouvelle logique.
        return Terrain.Type.GRASS;
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
                    Building.ConveyedItem conveyedItem = b.takeConveyedItem();
                    if (conveyedItem.crop != Building.PlanterCrop.NONE && conveyedItem.amount > 0) {
                        front.addToHQStock(conveyedItem.crop, conveyedItem.amount);
                    }
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
                Building.ConveyedItem conveyedItem = neighbor.takeConveyedItem();
                int entryDirection = getEntryDirectionFromSource(neighbor.getGridX(), neighbor.getGridY(), conveyor);
                conveyor.receiveItem(conveyedItem.crop, conveyedItem.amount, entryDirection);
                return;
            }
        }

        for (int[] dir : neighbors) {
            int nx = conveyor.getGridX() + dir[0];
            int ny = conveyor.getGridY() + dir[1];
            Building neighbor = getBuildingAtGridCell(nx, ny);
            if (neighbor != null && neighbor.isPlanter() && !neighbor.isPlanterEmpty() && neighbor.isPlanterReady()) {
                Building.PlanterCrop crop = neighbor.harvest();
                int entryDirection = getEntryDirectionFromSource(neighbor.getGridX(), neighbor.getGridY(), conveyor);
                conveyor.receiveItem(crop, Building.getYieldFor(crop), entryDirection);
                playSfx(plantationOutSound);
                return;
            }
        }
    }

    private Sound loadSound(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                return Gdx.audio.newSound(Gdx.files.internal(path));
            }
            System.out.println("Fichier son introuvable: " + path);
        } catch (Exception e) {
            System.out.println("Erreur chargement son " + path + ": " + e.getMessage());
        }
        return null;
    }

    private void playSfx(Sound sound) {
        if (sound == null) return;
        sound.play(SFX_VOLUME);
    }

    private int getEntryDirectionFromSource(int sourceGridX, int sourceGridY, Building targetConveyor) {
        int dx = sourceGridX - targetConveyor.getGridX();
        int dy = sourceGridY - targetConveyor.getGridY();

        if (dx == -1 && dy == 0) return 3;
        if (dx == 1 && dy == 0) return 1;
        if (dx == 0 && dy == -1) return 2;
        if (dx == 0 && dy == 1) return 0;
        return getOppositeDirection(targetConveyor.getRotation());
    }

    private Vector2 getConveyorItemWorldPosition(Building conveyor, Vector2 outPos) {
        float wx = conveyor.getGridX() - MAP_OFFSET;
        float wy = conveyor.getGridY() - MAP_OFFSET;

        int exitDirection = normalizeDirection(conveyor.getRotation());
        int entryDirection = conveyor.getEntryDirection();
        if (entryDirection < 0) entryDirection = getOppositeDirection(exitDirection);

        float progress = MathUtils.clamp(conveyor.getTransportProgress(), 0f, 1f);
        float localX;
        float localY;

        if (progress <= 0.5f) {
            float t = progress * 2f;
            localX = MathUtils.lerp(getConveyorEdgeOffsetX(entryDirection), 0.25f, t);
            localY = MathUtils.lerp(getConveyorEdgeOffsetY(entryDirection), 0.25f, t);
        } else {
            float t = (progress - 0.5f) * 2f;
            localX = MathUtils.lerp(0.25f, getConveyorEdgeOffsetX(exitDirection), t);
            localY = MathUtils.lerp(0.25f, getConveyorEdgeOffsetY(exitDirection), t);
        }

        return outPos.set(wx + localX, wy + localY);
    }

    private float getConveyorEdgeOffsetX(int direction) {
        if (direction == 1) return 0.75f;
        if (direction == 3) return -0.25f;
        return 0.25f;
    }

    private float getConveyorEdgeOffsetY(int direction) {
        if (direction == 0) return 0.75f;
        if (direction == 2) return -0.25f;
        return 0.25f;
    }

    private int normalizeDirection(int direction) {
        int normalized = direction % 4;
        if (normalized < 0) normalized += 4;
        return normalized;
    }

    private int getOppositeDirection(int direction) {
        return normalizeDirection(direction + 2);
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

    private Color getCropReadyColor(Building.PlanterCrop crop) {
        if (crop == Building.PlanterCrop.POTATO) return new Color(0.8f, 0.65f, 0.35f, 1f);
        if (crop == Building.PlanterCrop.STRAWBERRY) return new Color(0.9f, 0.2f, 0.2f, 1f);
        if (crop == Building.PlanterCrop.LEEK) return new Color(0.82f, 1f, 0.9f, 1f);
        return Color.WHITE;
    }

    private Color getCropReadyOverlayColor(Building.PlanterCrop crop) {
        if (crop == Building.PlanterCrop.POTATO) return new Color(1f, 0.95f, 0.25f, 0.6f);
        if (crop == Building.PlanterCrop.STRAWBERRY) return new Color(1f, 0.15f, 0.15f, 0.6f);
        if (crop == Building.PlanterCrop.LEEK) return new Color(0.85f, 1f, 0.9f, 0.6f);
        return new Color(1f, 1f, 1f, 0.5f);
    }

    private Texture getConveyorTextureForRotation(int rotation) {
        int normalized = normalizeDirection(rotation);
        if (normalized == 0) return conveyorTopTexture;
        if (normalized == 1) return conveyorRightTexture;
        if (normalized == 2) return conveyorBottomTexture;
        return conveyorLeftTexture;
    }

    private Texture getCropTexture(Building.PlanterCrop crop) {
        if (crop == Building.PlanterCrop.POTATO) return potatoTexture;
        if (crop == Building.PlanterCrop.STRAWBERRY) return strawberryTexture;
        if (crop == Building.PlanterCrop.LEEK) return leekTexture;
        return null;
    }

    private Texture getCropBagTexture(Building.PlanterCrop crop) {
        if (crop == Building.PlanterCrop.POTATO) return potatoBagTexture;
        if (crop == Building.PlanterCrop.STRAWBERRY) return strawberryBagTexture;
        if (crop == Building.PlanterCrop.LEEK) return leekBagTexture;
        return null;
    }

    private Color getCropGrowingColor(Building.PlanterCrop crop) {
        if (crop == Building.PlanterCrop.POTATO) return new Color(0.55f, 0.45f, 0.2f, 1f);
        if (crop == Building.PlanterCrop.STRAWBERRY) return new Color(0.55f, 0.15f, 0.15f, 1f);
        if (crop == Building.PlanterCrop.LEEK) return new Color(0.2f, 0.55f, 0.2f, 1f);
        return new Color(0.4f, 0.4f, 0.4f, 1f);
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
        if (currentState == GameState.PLAYING || currentState == GameState.PAUSED) {
            saveGame();
        }

        batch.dispose();
        shapeRenderer.dispose();

        if (playerTextureImg != null) playerTextureImg.dispose();

        if (tileGrass != null) tileGrass.dispose();
        if (tileTilled != null) tileTilled.dispose();
        if (hqTexture != null) hqTexture.dispose();
        if (auctionTexture != null) auctionTexture.dispose();
        if (potatoTexture != null) potatoTexture.dispose();
        if (strawberryTexture != null) strawberryTexture.dispose();
        if (leekTexture != null) leekTexture.dispose();
        if (potatoBagTexture != null) potatoBagTexture.dispose();
        if (strawberryBagTexture != null) strawberryBagTexture.dispose();
        if (leekBagTexture != null) leekBagTexture.dispose();
        if (planterTexture != null) planterTexture.dispose();
        if (planterPotatoTexture != null) planterPotatoTexture.dispose();
        if (planterStrawberryTexture != null) planterStrawberryTexture.dispose();
        if (planterLeekTexture != null) planterLeekTexture.dispose();
        if (conveyorTopTexture != null) conveyorTopTexture.dispose();
        if (conveyorRightTexture != null) conveyorRightTexture.dispose();
        if (conveyorBottomTexture != null) conveyorBottomTexture.dispose();
        if (conveyorLeftTexture != null) conveyorLeftTexture.dispose();
        if (gameLogoTexture != null) gameLogoTexture.dispose();

        if (backgroundMusic != null) backgroundMusic.dispose();
        if (buyItemSound != null) buyItemSound.dispose();
        if (sellItemSound != null) sellItemSound.dispose();
        if (plantationInSound != null) plantationInSound.dispose();
        if (plantationOutSound != null) plantationOutSound.dispose();
        if (planterPlacementSound != null) planterPlacementSound.dispose();
        if (conveyorPlacementSound != null) conveyorPlacementSound.dispose();
        if (tillSound != null) tillSound.dispose();
        if (clearSound != null) clearSound.dispose();

        uiStage.dispose();
        skin.dispose();
    }
}

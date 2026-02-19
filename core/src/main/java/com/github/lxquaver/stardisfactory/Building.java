package com.github.lxquaver.stardisfactory;

/**
 * Représente un bâtiment posé sur la carte.
 */
public class Building {

    public enum Type {
        MAIN_HQ(4, 4, BuildingCategory.LOGISTICS),
        AUCTION_HOUSE(4, 4, BuildingCategory.LOGISTICS),
        CONVEYOR_BELT(1, 1, BuildingCategory.TRANSPORT),
        PLANTER(1, 1, BuildingCategory.FARMING);

        public final int width;
        public final int height;
        public final BuildingCategory category;

        Type(int width, int height, BuildingCategory category) {
            this.width = width;
            this.height = height;
            this.category = category;
        }
    }

    public enum BuildingCategory {
        LOGISTICS,
        TRANSPORT,
        FARMING
    }

    public enum PlanterCrop {
        NONE,
        POTATO,
        STRAWBERRY,
        LEEK
    }

    public static class ConveyedItem {
        public final PlanterCrop crop;
        public final int amount;

        public ConveyedItem(PlanterCrop crop, int amount) {
            this.crop = crop == null ? PlanterCrop.NONE : crop;
            this.amount = Math.max(0, amount);
        }
    }

    private final Type type;
    private final int gridX;
    private final int gridY;
    private int rotation;

    private PlanterCrop planterCrop = PlanterCrop.NONE;
    private float growTimerSeconds = 0f;
    private boolean ready = false;

    private PlanterCrop heldItem = PlanterCrop.NONE;
    private int heldAmount = 0;
    private float transportTimer = 0f;
    private int entryDirection = -1;
    private static final float TRANSPORT_TIME = 1.0f;

    private int hqPotatoes = 0;
    private int hqStrawberries = 0;
    private int hqLeeks = 0;

    public Building(Type type, int gridX, int gridY) {
        this(type, gridX, gridY, 0);
    }

    public Building(Type type, int gridX, int gridY, int rotation) {
        this.type = type;
        this.gridX = gridX;
        this.gridY = gridY;
        this.rotation = rotation % 4;
        if (this.rotation < 0) this.rotation += 4;
    }

    public Type getType() { return type; }
    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }

    public void rotate() { rotation = (rotation + 1) % 4; }
    public int getRotation() { return rotation; }
    public void setRotation(int rotation) {
        this.rotation = rotation % 4;
        if (this.rotation < 0) this.rotation += 4;
    }

    public boolean isPlanter() {
        return type == Type.PLANTER;
    }

    public PlanterCrop getPlanterCrop() {
        return planterCrop;
    }

    public boolean isPlanterEmpty() {
        return planterCrop == PlanterCrop.NONE;
    }

    public boolean isPlanterReady() {
        return ready;
    }

    public float getPlanterGrowProgress01() {
        float total = getGrowTimeSecondsFor(planterCrop);
        if (total <= 0f) return 0f;
        return Math.min(1f, growTimerSeconds / total);
    }

    public void plant(PlanterCrop crop) {
        if (!isPlanter()) return;
        if (crop == null || crop == PlanterCrop.NONE) return;

        planterCrop = crop;
        growTimerSeconds = 0f;
        ready = false;
    }

    public void update(float dt) {
        if (isPlanter()) {
            updatePlanter(dt);
        } else if (isConveyor()) {
            updateConveyor(dt);
        }
    }

    private void updatePlanter(float dt) {
        if (planterCrop == PlanterCrop.NONE) return;
        if (ready) return;

        growTimerSeconds += dt;

        float total = getGrowTimeSecondsFor(planterCrop);
        if (growTimerSeconds >= total) {
            growTimerSeconds = total;
            ready = true;
        }
    }

    public PlanterCrop harvest() {
        if (!isPlanter()) return PlanterCrop.NONE;
        if (planterCrop == PlanterCrop.NONE) return PlanterCrop.NONE;
        if (!ready) return PlanterCrop.NONE;

        PlanterCrop harvested = planterCrop;
        planterCrop = PlanterCrop.NONE;
        growTimerSeconds = 0f;
        ready = false;

        return harvested;
    }

    public static float getGrowTimeSecondsFor(PlanterCrop crop) {
        if (crop == null) return 0f;
        switch (crop) {
            case POTATO: return 30f;
            case STRAWBERRY: return 60f;
            case LEEK: return 300f;
            default: return 0f;
        }
    }

    public static int getYieldFor(PlanterCrop crop) {
        if (crop == null) return 0;
        switch (crop) {
            case POTATO: return 10;
            case STRAWBERRY: return 7;
            case LEEK: return 5;
            default: return 0;
        }
    }

    public static int getSellPriceFor(PlanterCrop crop) {
        if (crop == null) return 0;
        switch (crop) {
            case POTATO: return 3;
            case STRAWBERRY: return 8;
            case LEEK: return 100;
            default: return 0;
        }
    }

    public boolean isConveyor() {
        return type == Type.CONVEYOR_BELT;
    }

    public boolean hasItem() {
        return heldItem != PlanterCrop.NONE;
    }

    public PlanterCrop getHeldItem() {
        return heldItem;
    }

    public int getHeldAmount() {
        return heldAmount;
    }

    public float getTransportProgress() {
        if (!hasItem()) return 0f;
        return Math.min(1f, transportTimer / TRANSPORT_TIME);
    }

    public boolean canReceiveItem() {
        return isConveyor() && !hasItem();
    }

    public void receiveItem(PlanterCrop item) {
        receiveItem(item, 1);
    }

    public void receiveItem(PlanterCrop item, int amount) {
        receiveItem(item, amount, getOppositeDirection(rotation));
    }

    public void receiveItem(PlanterCrop item, int amount, int entryDirection) {
        if (!canReceiveItem()) return;
        if (item == null || item == PlanterCrop.NONE) return;
        if (amount <= 0) return;
        heldItem = item;
        heldAmount = amount;
        transportTimer = 0f;
        this.entryDirection = normalizeDirection(entryDirection);
    }

    public PlanterCrop takeItem() {
        if (!hasItem()) return PlanterCrop.NONE;
        PlanterCrop item = heldItem;
        heldItem = PlanterCrop.NONE;
        heldAmount = 0;
        transportTimer = 0f;
        entryDirection = -1;
        return item;
    }

    public ConveyedItem takeConveyedItem() {
        if (!hasItem()) return new ConveyedItem(PlanterCrop.NONE, 0);
        ConveyedItem item = new ConveyedItem(heldItem, heldAmount);
        heldItem = PlanterCrop.NONE;
        heldAmount = 0;
        transportTimer = 0f;
        entryDirection = -1;
        return item;
    }

    private void updateConveyor(float dt) {
        if (hasItem()) {
            transportTimer += dt;
            if (transportTimer > TRANSPORT_TIME) {
                transportTimer = TRANSPORT_TIME;
            }
        }
    }

    public boolean isHQ() {
        return type == Type.MAIN_HQ;
    }

    public boolean isAuctionHouse() {
        return type == Type.AUCTION_HOUSE;
    }

    public void addToHQStock(PlanterCrop crop, int amount) {
        if (!isHQ()) return;
        if (amount <= 0) return;
        if (crop == PlanterCrop.POTATO) hqPotatoes += amount;
        if (crop == PlanterCrop.STRAWBERRY) hqStrawberries += amount;
        if (crop == PlanterCrop.LEEK) hqLeeks += amount;
    }

    public int getHQStock(PlanterCrop crop) {
        if (!isHQ()) return 0;
        if (crop == PlanterCrop.POTATO) return hqPotatoes;
        if (crop == PlanterCrop.STRAWBERRY) return hqStrawberries;
        if (crop == PlanterCrop.LEEK) return hqLeeks;
        return 0;
    }

    public int removeFromHQStock(PlanterCrop crop, int amount) {
        if (!isHQ()) return 0;
        if (amount <= 0) return 0;

        if (crop == PlanterCrop.POTATO) {
            int sold = Math.min(amount, hqPotatoes);
            hqPotatoes -= sold;
            return sold;
        }
        if (crop == PlanterCrop.STRAWBERRY) {
            int sold = Math.min(amount, hqStrawberries);
            hqStrawberries -= sold;
            return sold;
        }
        if (crop == PlanterCrop.LEEK) {
            int sold = Math.min(amount, hqLeeks);
            hqLeeks -= sold;
            return sold;
        }
        return 0;
    }

    public void clearHQStock() {
        hqPotatoes = 0;
        hqStrawberries = 0;
        hqLeeks = 0;
    }

    public float getGrowTimerSeconds() {
        return growTimerSeconds;
    }

    public void setPlanterState(PlanterCrop crop, float growTimerSeconds, boolean ready) {
        if (!isPlanter()) return;
        this.planterCrop = crop == null ? PlanterCrop.NONE : crop;
        this.growTimerSeconds = Math.max(0f, growTimerSeconds);
        this.ready = ready;
    }

    public float getTransportTimerSeconds() {
        return transportTimer;
    }

    public void setConveyorState(PlanterCrop item, int amount, float transportTimer) {
        if (!isConveyor()) return;
        this.heldItem = item == null ? PlanterCrop.NONE : item;
        this.heldAmount = this.heldItem == PlanterCrop.NONE ? 0 : Math.max(0, amount);
        this.transportTimer = Math.max(0f, Math.min(TRANSPORT_TIME, transportTimer));
        this.entryDirection = this.heldItem == PlanterCrop.NONE ? -1 : getOppositeDirection(rotation);
    }

    public int getEntryDirection() {
        if (!hasItem()) return -1;
        return entryDirection;
    }

    private static int normalizeDirection(int direction) {
        int normalized = direction % 4;
        if (normalized < 0) normalized += 4;
        return normalized;
    }

    private static int getOppositeDirection(int direction) {
        return normalizeDirection(direction + 2);
    }

    public void setHQStock(PlanterCrop crop, int amount) {
        if (!isHQ()) return;
        int safeAmount = Math.max(0, amount);
        if (crop == PlanterCrop.POTATO) hqPotatoes = safeAmount;
        if (crop == PlanterCrop.STRAWBERRY) hqStrawberries = safeAmount;
        if (crop == PlanterCrop.LEEK) hqLeeks = safeAmount;
    }
}

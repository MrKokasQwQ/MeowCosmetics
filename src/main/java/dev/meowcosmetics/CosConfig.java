package dev.meowcosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Settings, in config/meowcosmetics.json. Speeds and amounts are percentages. */
public final class CosConfig {
    public boolean capeOn = false;
    /** 0 Starry Forest, 1 Sakura, 2 Aurora, 3 your own image. */
    public int capeStyle = 0;
    public String capeFile = "";
    public int capeSpeed = 100;
    public boolean ears = false, tail = false;
    public int earColor = 0, tailColor = 0;
    public int catSpeed = 100;
    public int earSize = 100, earSpread = 0, earForward = 0, earHeight = 0, earTilt = 10;
    public int tailLength = 9, tailCurl = 50, tailSide = 0, tailHeight = 0;
    public boolean auraOn = false;
    public int auraStyle = 0;
    public int auraSpeed = 100, auraAmount = 75;
    public boolean petOn = false;
    public int petStyle = 0;
    public int petSpeed = 100;
    public int petSize = 100, petSide = 0;
    /** Which pets the preview flips through: 0 cats, 1 slimes. */
    public int petGroup = 0;
    public boolean hideArmor = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("meowcosmetics.json");
    private static CosConfig instance;

    public static CosConfig get() {
        if (instance == null) {
            try { instance = Files.exists(FILE) ? GSON.fromJson(Files.readString(FILE), CosConfig.class) : null; } catch (Exception ignored) {}
            if (instance == null) instance = new CosConfig();
        }
        return instance;
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(get())); } catch (Exception ignored) {}
    }
}

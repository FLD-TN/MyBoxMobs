package com.dex.myboxmobs;

import com.dex.myboxmobs.api.MyBoxMobsService;
import com.dex.myboxmobs.command.MbmCommand;
import com.dex.myboxmobs.config.ConfigFiles;
import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.config.Settings;
import com.dex.myboxmobs.gui.ChatInput;
import com.dex.myboxmobs.gui.GuiListener;
import com.dex.myboxmobs.hook.BoxClansSupport;
import com.dex.myboxmobs.hook.ClanSupport;
import com.dex.myboxmobs.hook.DecentHolograms;
import com.dex.myboxmobs.hook.Economy;
import com.dex.myboxmobs.hook.EconomyHooks;
import com.dex.myboxmobs.hook.Holograms;
import com.dex.myboxmobs.hook.ItemsAdderHook;
import com.dex.myboxmobs.hook.MegModelSupport;
import com.dex.myboxmobs.hook.ModelSupport;
import com.dex.myboxmobs.hook.MythicHook;
import com.dex.myboxmobs.hook.PapiHook;
import com.dex.myboxmobs.live.Announcer;
import com.dex.myboxmobs.live.ClanBossListener;
import com.dex.myboxmobs.live.KillStats;
import com.dex.myboxmobs.live.MobListener;
import com.dex.myboxmobs.live.MobService;
import com.dex.myboxmobs.live.NameTags;
import com.dex.myboxmobs.live.RewardService;
import com.dex.myboxmobs.mob.MobRegistry;
import com.dex.myboxmobs.place.AltarDef;
import com.dex.myboxmobs.place.AltarListener;
import com.dex.myboxmobs.place.AltarService;
import com.dex.myboxmobs.place.SpawnerService;
import com.dex.myboxmobs.text.Text;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/** MyBoxMobs: custom mobs and bosses on top of MythicMobs and ModelEngine (MYBOXMOBS_SPEC.md). */
public final class MyBoxMobsPlugin extends JavaPlugin {

    private Settings settings;
    private final Messages messages = new Messages(this);
    private MythicHook mythic;
    private ModelSupport models = ModelSupport.NONE;
    private Holograms holograms = Holograms.NONE;
    private ClanSupport clans = ClanSupport.NONE;
    private Economy economy;
    private ItemsAdderHook itemsAdder;
    private MobRegistry registry;
    private KillStats kills;
    private MobService mobs;
    private Announcer announcer;
    private RewardService rewards;
    private SpawnerService spawners;
    private AltarService altars;
    private NameTags nameTags;
    private ChatInput input;

    @Override
    public void onEnable() {
        PluginManager pm = Bukkit.getPluginManager();
        if (!pm.isPluginEnabled("MythicMobs")) {
            getLogger().severe("MythicMobs is not enabled: MyBoxMobs disabled");
            pm.disablePlugin(this);
            return;
        }
        boolean firstRun = !new File(getDataFolder(), "mobs").exists();
        loadConfigFiles();
        mythic = new MythicHook();
        if (pm.isPluginEnabled("ModelEngine")) models = new MegModelSupport(getLogger());
        if (pm.isPluginEnabled("DecentHolograms")) holograms = new DecentHolograms(getLogger());
        if (pm.isPluginEnabled("BoxClans")) clans = new BoxClansSupport(getLogger());
        economy = new EconomyHooks();
        itemsAdder = ItemsAdderHook.create(this);
        if (pm.isPluginEnabled("PlaceholderAPI")) {
            Text.setPlaceholderResolver(PapiHook.resolver());
            PapiHook.register(this);
        }
        if (firstRun) saveResource("mobs/example_zombie.yml", false);
        registry = new MobRegistry(getDataFolder(), getLogger());
        registry.load(getConfig());
        kills = new KillStats(getDataFolder(), getLogger());
        kills.open();
        mobs = new MobService(this);
        nameTags = new NameTags(this);
        mobs.addEndListener(nameTags);
        announcer = new Announcer(() -> settings, mobs::displayName, () -> clans);
        rewards = new RewardService(() -> settings, messages, economy, mobs::displayName, () -> clans, getLogger());
        spawners = new SpawnerService(this);
        spawners.load();
        altars = new AltarService(this);
        altars.load();
        mobs.addEndListener(altars);
        input = new ChatInput(this);

        pm.registerEvents(new MobListener(this, mobs), this);
        pm.registerEvents(new AltarListener(altars, messages), this);
        pm.registerEvents(new ClanBossListener(this), this);
        pm.registerEvents(nameTags, this);
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(input, this);
        PluginCommand command = getCommand("myboxmobs");
        if (command != null) {
            MbmCommand handler = new MbmCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        Bukkit.getServicesManager().register(MyBoxMobsService.class, new ServiceImpl(this), this, ServicePriority.Normal);

        int adopted = 0;
        for (World w : Bukkit.getWorlds()) adopted += mobs.adopt(w.getEntities());
        if (adopted > 0) getLogger().info("Took back " + adopted + " MyBoxMobs mobs still in the world");
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            mobs.tick();
            spawners.tick();
            altars.tick();
        }, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, nameTags::tick, 1L, 1L);
        getLogger().info("ModelEngine " + (models.available() ? "found" : "not found")
                + ", DecentHolograms " + (holograms.available() ? "found" : "not found")
                + ", BoxClans " + (clans != ClanSupport.NONE ? "found" : "not found"));
    }

    @Override
    public void onDisable() {
        GuiListener.closeAll();
        if (mobs != null) mobs.shutdown();
        if (nameTags != null) nameTags.clear();
        if (spawners != null) spawners.clearRuntime();
        if (altars != null) altars.clearHolograms();
        if (kills != null) kills.close();
        Bukkit.getServicesManager().unregisterAll(this);
    }

    private void loadConfigFiles() {
        ConfigFiles.ensure(this, "config.yml");
        reloadConfig();
        settings = new Settings(getConfig());
        messages.load();
    }

    /** /mbm reload: files are read again; living mobs and altar bosses are kept. */
    public void reloadAll() {
        GuiListener.closeAll();
        loadConfigFiles();
        registry.load(getConfig());
        spawners.load();
        Map<String, UUID[]> bosses = new HashMap<>();
        for (AltarDef d : altars.all()) bosses.put(d.id, new UUID[]{d.boss, d.summoner});
        altars.load();
        for (AltarDef d : altars.all()) {
            UUID[] state = bosses.get(d.id);
            if (state != null) {
                d.boss = state[0];
                d.summoner = state[1];
            }
        }
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public MythicHook mythic() {
        return mythic;
    }

    public ModelSupport models() {
        return models;
    }

    public Holograms holograms() {
        return holograms;
    }

    /** BoxClans, or {@link ClanSupport#NONE} when it is not installed. */
    public ClanSupport clans() {
        return clans;
    }

    public Economy economy() {
        return economy;
    }

    public ItemsAdderHook itemsAdder() {
        return itemsAdder;
    }

    public MobRegistry registry() {
        return registry;
    }

    public KillStats kills() {
        return kills;
    }

    public MobService mobs() {
        return mobs;
    }

    public Announcer announcer() {
        return announcer;
    }

    public RewardService rewards() {
        return rewards;
    }

    public SpawnerService spawners() {
        return spawners;
    }

    public AltarService altars() {
        return altars;
    }

    public NameTags nameTags() {
        return nameTags;
    }

    public ChatInput input() {
        return input;
    }
}

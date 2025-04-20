package com.solanacoin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.types.TokenResultObjects.TokenAmountInfo;
import com.solanacoin.util.Base58;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.types.SplTokenAccountInfo;

import java.sql.Connection;
import java.math.RoundingMode;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MinePathCoinPlugin extends JavaPlugin implements Listener {

    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    static {
        NUMBER_FORMAT.setRoundingMode(RoundingMode.FLOOR);
        NUMBER_FORMAT.setGroupingUsed(true);
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    FileConfiguration config = getConfig();
    protected String rpcURL;
    protected RpcClient rpcClient;
    protected Account signer;
    protected PublicKey publicKey;
    protected PublicKey tokenMintAddress;
    protected PublicKey associatedTokenAddress;
    protected int requestLimitPerSecond;
    protected double minimumExport;
    protected double startingBalance = 0.0;
    protected org.p2p.solanaj.rpc.types.TokenResultObjects.TokenInfo tokenMintInfo;
    protected SQL db;
    boolean enabled;
    protected VaultIntegration vaultIntegration;
    protected String currencySymbol;
    protected int tokenDecimals;


   protected String chatPrefix = ChatColor.GRAY + "["+ChatColor.RESET+"MINEPATH"+ChatColor.GRAY + "]: " + ChatColor.RESET;


    public void loadSignerAccount() {
        this.chatPrefix = ChatColor.GRAY + "["+ChatColor.RESET+"MINEPATH"+ChatColor.GRAY + "]: " + ChatColor.RESET;
        getServer().getConsoleSender().sendMessage("Statring to collecting Signer");
        if (config.contains("signer")) {
            this.signer = new Account(Base58.decode(Objects.requireNonNull(config.getString("signer"))));
        }
    }

    public void loadPublicKey() {
        String publicKey = config.getString("publicKey");
        getServer().getConsoleSender().sendMessage("Statring to collecting publicKey");
        try {
            assert publicKey != null;
            this.publicKey = new PublicKey(publicKey);
        } catch(IllegalArgumentException e) {
            getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.RED + " config field 'publicKey' invalid.");
        }

    }
    
    private PublicKey getAssociatedTokenAddress(PublicKey wallet, PublicKey mint) {
        try {
            // Use the three seeds: wallet, TokenProgram.PROGRAM_ID, mint
            return PublicKey.findProgramAddress(
                    List.of(
                            wallet.toByteArray(),
                            org.p2p.solanaj.programs.TokenProgram.PROGRAM_ID.toByteArray(),
                            mint.toByteArray()
                    ),
                    org.p2p.solanaj.programs.AssociatedTokenProgram.PROGRAM_ID
            ).getAddress();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute associated token address", e);
        }
    }
    
    public void loadTokenMint() {
        String tokenMint = config.getString("tokenMint");
        getServer().getConsoleSender().sendMessage("Statring to collecting tokenMint");
        try {
            assert tokenMint != null;
            this.tokenMintAddress = new PublicKey(tokenMint);
            TokenAmountInfo supplyInfo = rpcClient.getApi().getTokenSupply(this.tokenMintAddress);
            this.tokenDecimals = supplyInfo.getDecimals();
            getServer().getConsoleSender().sendMessage(chatPrefix + "Detected token decimals = " + this.tokenDecimals);
            try {
            // Use the new RPC API to get the SPL token account info with "jsonParsed" encoding
            // This returns a TokenAccountInfo from which we extract the parsed token info.
            this.associatedTokenAddress = getAssociatedTokenAddress(this.publicKey, this.tokenMintAddress);
            SplTokenAccountInfo tokenAccountInfo = rpcClient.getApi().getSplTokenAccountInfo(this.associatedTokenAddress);
            if (tokenAccountInfo == null || tokenAccountInfo.getValue().getData().getParsed().getInfo() == null) {
                throw new Exception("Parsed mint info not available.");
            }
            // Store the mint info from the parsed data.
            this.tokenMintInfo = tokenAccountInfo.getValue().getData().getParsed().getInfo();
            getServer().getConsoleSender().sendMessage("Token Mint Info: " + this.tokenMintInfo);
            // Replace the old Mint call with the new AssociatedTokenProgram method.
            // Note: parameter order is now (wallet, tokenMintAddress)
            getServer().getConsoleSender().sendMessage("Wallet: " + this.publicKey + ", Token Mint Address: " + this.tokenMintAddress);
            getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.GREEN + "Token mint loaded successfully");
       
            } catch (Exception e) {
                getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.RED + "Failed to get mint info: " + e.getMessage());
                e.printStackTrace();
            }
        } catch(IllegalArgumentException e) {
            getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.RED + "Config field 'tokenMint' invalid.");
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.RED + "Failed to find associated token account, make sure you have some of the token in your account.");
            e.printStackTrace();
        }
    }

    public void loadSQL() {
        this.getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.GRAY + "Loading SQL...");
        if (this.db != null) {
            this.db.disconnect();
        }
        this.db = new SQL(
                this
        );
        try {
            if (Objects.requireNonNull(config.getString("dbType")).equalsIgnoreCase("sqlite")) {
                this.db.connectSQLite(config.getString("sqliteLocation"));
            } else {
                this.db.connectSQL(config.getString("dbType"), config.getString("dbHost"),
                        config.getString("dbPort"),
                        config.getString("dbName"),
                        config.getString("dbUsername"),
                        config.getString("dbPassword"),
                        config.getBoolean("dbUseSSL"));
            }
            this.getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.GREEN + "Database connected! Ahihi");
        } catch (SQLException e) {
            e.printStackTrace();
            this.getServer().getConsoleSender().sendMessage(chatPrefix + ChatColor.RED + "Database connection failed. Oh no!");
        }
    }

    public void setupSQL() {
        this.loadSQL();
        if (this.db.isConnected())
            this.db.setupBalanceTable();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.tryHookVault("onLoad");
    }

    @Override
    public void onEnable () {

        this.reloadConfig();
        getServer().getConsoleSender().sendMessage(chatPrefix + "Plugin enabled with config: " + config.getValues(true));
        if (this.enabled) {

            Objects.requireNonNull(this.getCommand("admin")).setExecutor(new AdminCommand(this));
            Objects.requireNonNull(this.getCommand("balance")).setExecutor(new BalanceCommand(this));
            Objects.requireNonNull(this.getCommand("serverbalance")).setExecutor(new ServerBalanceCommand(this));
            Objects.requireNonNull(this.getCommand("db")).setExecutor(new DBCommand(this));
            Objects.requireNonNull(this.getCommand("export")).setExecutor(new ExportCommand(this));
            Objects.requireNonNull(this.getCommand("lbh")).setExecutor(new LbhCommand(this));
            Objects.requireNonNull(this.getCommand("send")).setExecutor(new SendCommand(this));

            getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.GREEN + "MINEPATH Enabled");
            this.tryHookVault("onEnable");
            getServer().getPluginManager().registerEvents(this, this);
        } else {
            getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.GREEN + "MINEPATH Not Enabled");
        }

        startAutoBalanceUpdates();
    }

    @Override
    public void onDisable () {
        if (this.db != null) {
            if (this.db.isConnected()) {
                this.db.disconnect();
            }
        }
        getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.RED + "MINEPATH Disabled");

    }

    // This method checks for incoming players and sends them a message
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (this.enabled) {
           player.sendMessage(chatPrefix+"This server has MINEPATH support!");
            if(this.db.isConnected()) {
                this.db.addPlayerToBalanceTable(player);
            }
        }
    }

    public enum TELLRAWCOLOR {
        red,
        dark_red,
        yellow,
        gold,
        green,
        dark_green,
        blue,
        dark_blue,
        aqua,
        dark_aqua,
        light_purple,
        dark_purple,
        gray,
        dark_gray,
        white,
        black
    }

    public void sendURLToPlayer(Player player, String message, String url, TELLRAWCOLOR color) {
        this.getServer().dispatchCommand(
                this.getServer().getConsoleSender(),
                "tellraw " + player.getName() +
                        " {\"text\":\"" + message + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" +
                        url + "\"},\"color\":\""+color.name()+"\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\""+url+"\"]}}");
    }

    public void sendCopyableTextToPlayer(Player player, String message, String toCopy, TELLRAWCOLOR color) {
        this.getServer().dispatchCommand(
                this.getServer().getConsoleSender(),
                "tellraw " + player.getName() +
                        " {\"text\":\"" + message + "\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" +
                        toCopy + "\"},\"color\":\""+color.name()+"\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Copy to clipboard\"]}}");
    }

    public Timestamp getNow() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = formatter.format((new Date(System.currentTimeMillis())));
        return Timestamp.valueOf(now);
    }

    public boolean shouldRateLimit(Player player) {

        Timestamp lastRequest = Timestamp.valueOf(this.db.getLastRequestTimestamp(player.getUniqueId()));
        Timestamp now = this.getNow();
        long milliseconds = now.getTime() - lastRequest.getTime();
        int seconds = (int) milliseconds / 1000;
        return (seconds < this.requestLimitPerSecond);
    }

    public void tryHookVault(String where) {
        if (this.config.getBoolean("vaultEnabled")) {
            if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
                if (this.vaultIntegration == null) {
                    this.vaultIntegration = new VaultIntegration(this);
                }
            } else {
                this.getServer().getLogger().warning("[WARNING][MINEPATH] Vault not found during " + where + ".");
            }
        }
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        if (this.config.getBoolean("vaultEnabled")) {
            if (this.vaultIntegration != null) {
                if (this.vaultIntegration.getPermissions() != null) {
                    if (this.vaultIntegration.getPermissions().isEnabled()) {
                        return vaultIntegration.getPermissions().has(sender, permission);
                    }
                }
            }
        }
        return sender.hasPermission(permission);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        saveDefaultConfig();
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        this.enabled = config.getBoolean("enabled");

        if (this.enabled) {

//            this.tryHookVault("reload");


            if (config.contains("rpcURL")) {
                String rpcURL = config.getString("rpcURL");
                boolean loadClient = true;
                if (this.rpcURL == null) {
                    this.rpcURL = rpcURL;
                } else if (this.rpcURL.equalsIgnoreCase(rpcURL) && this.rpcClient == null) {
                    loadClient = false;
                }
                if (loadClient && !Objects.requireNonNull(rpcURL).equals("")) {
                    this.rpcClient = new RpcClient(rpcURL);
                } else {
                    this.rpcClient = new RpcClient(Cluster.MAINNET);
                    getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.RED + "config field 'rpcURL' is blank. Defaulting to \"https://api.mainnet-beta.solana.com\".");
                }
            }


            if (config.contains("signer")) {
                if (this.signer == null ||
                !Base58.encode(this.signer.getSecretKey()).equalsIgnoreCase(config.getString("signer"))) {
                    if(this.signer != null) {
                        getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.YELLOW + "'signer' config field changed!");
                    }
                    this.loadSignerAccount();
                }
            } else {
                getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.RED + "config field 'signer' missing! Won't be able to send transactions.");
            }

            if (config.contains("publicKey")) {
                if (this.publicKey == null
                || !this.publicKey.toBase58().equalsIgnoreCase(config.getString("publicKey"))) {
                    if (this.publicKey != null) {
                        getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.YELLOW + "'publicKey' config field changed!");
                    }
                this.loadPublicKey();
                }
            } else {
                getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.RED + "config field 'publicKey' missing. Won't be able to send transactions.");
            }

            if (config.contains("tokenMint")) {
                String cfgMint = config.getString("tokenMint");
                // always load if we haven't loaded one yet, or if it differs
                if (this.tokenMintAddress == null
                    || !this.tokenMintAddress.toBase58().equalsIgnoreCase(cfgMint)) {
                  if (this.tokenMintAddress != null) {
                    getServer().getConsoleSender()
                      .sendMessage(chatPrefix + ChatColor.YELLOW + "'tokenMint' config field changed!");
                  }
                  loadTokenMint();           // now runs on first-ever load, and whenever config changes
                }
            } else {
                getServer().getConsoleSender()
                  .sendMessage(chatPrefix + ChatColor.RED  + "config field 'tokenMint' missing.");
            }

            if (config.contains("minimumExport")) {
                this.minimumExport = config.getDouble("minimumExport");
            }

            if (config.contains("requestLimitPerSecond")) {
                this.requestLimitPerSecond = config.getInt("requestLimitPerSecond");
            }

            if(config.contains("startingBalance")) {
                this.startingBalance = config.getDouble("startingBalance");
            }

            if (config.contains("currencySymbol")) {
                boolean loadCurrencySymbol = true;
                if (this.currencySymbol != null) {
                    if (!this.currencySymbol.equals("")) {
                        if (!this.currencySymbol.equalsIgnoreCase(config.getString("currencySymbol"))) {
                            getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.YELLOW + "'currencySymbol' config field changed!");
                        } else {
                            loadCurrencySymbol = false;
                        }
                    }
                }
                if (loadCurrencySymbol) {
                    this.currencySymbol = config.getString("currencySymbol");
                }

            }
            this.setupSQL();
            getServer().getConsoleSender().sendMessage(chatPrefix
            + " rpcURL=" + rpcURL
            + ", signerPubKey=" + (signer  != null ? signer.getPublicKey().toBase58() : "null")
            + ", userPubKey="   + (publicKey != null ? publicKey.toBase58() : "null")
            + ", tokenMint="    + (tokenMintAddress != null ? tokenMintAddress.toBase58() : "null")
            + ", ata="          + (associatedTokenAddress != null ? associatedTokenAddress.toBase58() : "null")
            );

        } else {
            if (this.db != null) {
                if (this.db.isConnected()) {
                    this.db.disconnect();
                }
                getServer().getConsoleSender().sendMessage(chatPrefix+ChatColor.RED + "MINEPATH Disabled via Config Reload");
            }
        }


    }
    private void startAutoBalanceUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Fetch and display the player's wallet balance asynchronously
                    Bukkit.getScheduler().runTaskAsynchronously(MinePathCoinPlugin.this, () -> {
                        String balanceStr = "Fetching...";
                        try {
                            // Fetch the player's wallet address from the database
                            String walletAddress = getPlayerWalletAddress(player.getUniqueId().toString());
                            if (walletAddress != null && !walletAddress.isEmpty()) {
                                PublicKey wallet = new PublicKey(walletAddress);

                                // Fetch associated token address & balance
                                PublicKey UserATA = PublicKey.findProgramAddress(
                                    List.of(
                                            wallet.toByteArray(),
                                            org.p2p.solanaj.programs.TokenProgram.PROGRAM_ID.toByteArray(),
                                            tokenMintAddress.toByteArray()
                                    ),
                                    org.p2p.solanaj.programs.AssociatedTokenProgram.PROGRAM_ID
                                ).getAddress();
                                TokenAmountInfo balance = rpcClient.getApi().getTokenAccountBalance(UserATA, null);
                                balanceStr = balance.getUiAmountString();
                            } else {
                                balanceStr = "Wallet not linked";
                            }
                        } catch (Exception e) {
                            balanceStr = "No ATA dectected";
                        }

                        // Update the player's scoreboard with the fetched balance
                        String finalBalanceStr = balanceStr;
                        Bukkit.getScheduler().runTask(MinePathCoinPlugin.this, () -> {
                            updatePlayerScoreboard(player, finalBalanceStr);
                        });
                    });
                }
            }
        }.runTaskTimer(this, 0L, 50L);
    }
    private String getPlayerWalletAddress(String playerUUID) {
            String walletAddress = null;
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(
                        getConfig().getString("external-db.url"),
                        getConfig().getString("external-db.user"),
                        getConfig().getString("external-db.password")
                );

                PreparedStatement ps = conn.prepareStatement(
                        "SELECT wallet_address FROM walletlogin_wallets WHERE uuid = ?"
                );
                ps.setString(1, playerUUID);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    walletAddress = rs.getString("wallet_address");
                }

                rs.close();
                ps.close();
            } catch (SQLException e) {
                getLogger().severe("Error fetching wallet address: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            return walletAddress;
    }

    // Function to update the player's scoreboard with the latest balance
    private void updatePlayerScoreboard(Player player, String balanceStr) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("walletBal");

        // If the objective doesn't exist yet, create it
        if (objective == null) {
            scoreboard = scoreboardManager.getNewScoreboard();
            objective = scoreboard.registerNewObjective("walletBal", "dummy", ChatColor.GOLD + "Your Wallet");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(scoreboard);
        }

        // Reset existing entries (optional: track specific entries for updates)
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Add the balance to the sidebar
        Score score = objective.getScore(ChatColor.GREEN + "Balance: " + ChatColor.YELLOW + balanceStr);
        score.setScore(0);
    }


}

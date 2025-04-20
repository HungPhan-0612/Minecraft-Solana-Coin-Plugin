# 🌟 MinePathCoinPlugin 🌟

Welcome to the **MinePathCoinPlugin** — where **Minecraft meets the magic of Solana**! 💸 Whether you're rewarding players with tokens, enabling wallet exports, or checking server balances like a boss, this plugin has your back.

---

## 🚀 What This Plugin Can Do

✨ Track player balances like a banker  
✨ Export tokens straight to Solana wallets  
✨ Let players send tokens to each other  
✨ Admins can rule the token world 🏰  
✨ Integrates with SQL, Vault, and Solana RPC

---

## 🛠️ Setup (a.k.a. "How to become the crypto king of your server")

### Step 1: Install JDK 17 ☕
Because modern plugins deserve modern Java.

#### Ubuntu/Debian:
```bash
sudo apt update && sudo apt install openjdk-17-jdk
```

#### macOS (Homebrew):
```bash
brew install openjdk@17
```

#### Windows:
- Download from: https://jdk.java.net/17/
- Add it to your system's PATH if it’s being shy.

Verify:
```bash
java -version
# Should say something like "openjdk version \"17..."
```

---

### Step 2: Build the Plugin 🧱

This project uses the **Gradle Wrapper**, so no extra tools needed.

```bash
# Clone the repo
git clone https://github.com/HungPhan-0612/Minecraft-Solana-Coin-Plugin.git
cd solanacoin-plugin

# Build it with style
gradlew clean shadowJar
```

You’ll find your treasure at:
```bash
build/libs/MinePathCoinPlugin.jar
```

---

### Step 3: Deploy! 🎮
1. Toss that `.jar` file into your server’s `plugins/` folder.
2. Start (or restart) your server.
3. A wild `MinePath/config.yml` will appear — edit it!

---

## ⚙️ Configuration (aka Spellbook of Settings)

```yaml
enabled: true
vaultEnabled: true
sqliteLocation: plugins/MinePath/MinePath.db
dbType: mysql (sqlite, mysql, postgresql)
dbHost: localhost
dbPort: 3306
dbName: minepath
dbTable: balance
dbUsername: root
dbPassword: 
dbUseSSL: false
external-db:
  url: jdbc:mysql://localhost:3306/....
  user: root
  password: 
tokenMint: 8ukoz8y6bJxpjUVSE3bEDbGjwyStqXBQZiSyxjhjNx1g
signer: 
publicKey: 
rpcURL: https://api.devnet.solana.com
currencySymbol: MINECRAFT
startingBalance: 0.0
minimumExport: 0.5
requestLimitPerSecond: 1
```

---

## 🧙‍♂️ Commands & Permissions

| Command | What it does | Permission |
|--------|---------------|------------|
| `/minepath:balance` | See your coin stash | `minepath.balance` |
| `/minepath:serverbalance` | Peek at the server vault | `minepath.serverbalance` |
| `/minepath:send <player> <amount>` | Send tokens to your buddies | `minepath.send` |
| `/minepath:export <amount> confirm` | Export to your Solana wallet | `minepath.export` |
| `/minepath:admin ...` | God mode: adjust balances | `minepath.admin` |
| `/minepath:db status` | DB health check | `minepath.db` |
| `/minepath:lbh` | Latest Solana blockhash | `minepath.lbh` |

### Admin Command Menu 📜

```bash
/minepath:admin balance [player]       # View player/server balance
/minepath:admin add <player> <amt>     # Give tokens
/minepath:admin subtract <player> <amt># Take tokens
/minepath:admin set <player> <amt>     # Set exact balance
/minepath:admin delete <player>        # Yeet a player’s balance
/minepath:admin destroydb confirm      # ⚠️ Wipe the DB ⚠️
/minepath:admin reload                 # Reload config
```

---

## 🧨 Common Issues & Fixes

❌ **Plugin not loading?**  
➡️ Check your `config.yml`. Required fields: `signer`, `publicKey`, `tokenMint`

❌ **DB not connecting?**  
➡️ Double check your DB host/port/user/pass and JDBC URL

❌ **Transaction failed?**  
➡️ Could be Solana congestion. The plugin retries & refunds automatically!

❌ **Vault permissions not working?**  
➡️ Enable `vaultEnabled: true` and install the Vault plugin + permissions plugin (LuckPerms, etc.)

---

## 🧠 Inspiration
This plugin was inspired by the innovative work on Synex Coin, a Solana utility token designed for integrating crypto into Minecraft. A big shoutout to the Synex Creator (https://github.com/JIBSIL) for paving the way in merging blockchain technology with

---
https://www.spigotmc.org/resources/synex-coin-add-real-crypto-to-your-minecraft-server.101696/

---
https://github.com/JIBSIL/synex-coin

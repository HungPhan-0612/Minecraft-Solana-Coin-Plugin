### If you're looking for the compiled (.jar) version of the plugin:
You might be a little lost. Check out our [Beta Releases on GitHub](https://github.com/JIBSIL/synex-coin/releases) or stable releases on [SpigotMC](https://www.spigotmc.org/resources/synex-coin-add-real-crypto-to-your-minecraft-server.101696/).

# synex plugin
SynexCoin Spigot Plugin

Developed and tested on Spigot v1.18.2  
SQLite and JDBC (MySQL) support.  
Vault support.  
Uses [solanaj](https://github.com/lmvdz/solanaj) for Solana Mint/AssociatedTokenAccount SPL support.  

## Installation  

Download the latest `.jar` build from [build/libs](https://github.com/JIBSIL/synex-coin/releases).  
Add to your `plugins` directory.  
Start the server and edit the `plugins/synex/config.yml` file.  

To be able to use the `/synex:export` command you'll need a valid `signer` private key.  
You'll want to make sure the `publicKey` corresponds to the `signer` private key (base58 encoded).  
`tokenMint` is defaulted to [Synex Coin Token](https://solscan.io/token/FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ).  
By default, the database is set to use sqlite and has been tested with MySQL.  
  
If you need help converting your Uint8Array private key to base58 there are a few methods.  
1. Use Phantom wallet, Add/Connect wallet, import private key, and paste the Uint8Array. Open the wallet, go to the settings, scroll down and export the private key.  
3. Use NodeJS [Uint8Array to Base58 gist](https://gist.github.com/Xavier59/b0b216f003b8e54db53c39397e98cd70).  
  
## Configuration with comments  

```yml
#Should the plugin be enabled
enabled: true

#Should the plugin be loaded as an Economy provider
vaultEnabled: true

# The location of the sqlite db
sqliteLocation: plugins/synex/synex.db
# The db type (sqlite, mysql, postgresql)
dbType: sqlite
# The hostname of the database
dbHost: localhost
# The port of the database
dbPort: 3306
# The database name (if it doesn't exist the plugin will error out when using non-sqlite dbType)
dbName: synex
# The database table
dbTable: balance
# The database username
dbUsername: root
# The database password (mysql default root password is no password, you should change it)
dbPassword:
# Should the JDBC (non-sqlite) connection use SSL
dbUseSSL: false

# The PublicKey of the token which should be used
tokenMint: FTkj421DxbS1wajE74J34BJ5a1o9ccA97PkK6mYq9hNQ
# The private key used to sign the transactions (must be base58 encoded)
# To get this, use the wallet Phantom (https://phantom.app)
# In phantom, click the settings gear at the bottom of the screen --> click "Export Private Key" --> Copy and paste the text into the "signer" field.
# This key is never shared and never leaves your server.
signer:
# The PublicKey associated with the signer
publicKey: 8Ci5UbpoAFL5sAj4jeKwADceYrDxKQktXJnn1Vwgug5m
# The URL of the RPC you want to connect to
rpcURL: https://ssc-dao.genesysgo.net/
# Currency Symbol
currencySymbol: MINECRAFT


# Starting balance of a new player
startingBalance: 0.0
# The minimum amount a player is allowed to export
minimumExport: 0.5
# How often can a player try to export or view the server balance in seconds
requestLimitPerSecond: 1
```

<p align="center">
  <img src="https://raw.githubusercontent.com/che11111/GlobalMarketPlusBedrock/main/img/GlobalMarketPlusBedrock.png" alt="GlobalMarketPlusBedrock Logo" width="128">
</p>

<h1 align="center">GlobalMarketPlusBedrock</h1>
<p align="center">Bedrock Edition forms for GlobalMarketPlus</p>
<p align="center">
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Minecraft-1.16%2B-3fb950" alt="Minecraft 1.16+; see requirements below"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/GlobalMarketPlus-1.4.3_API-5865F2" alt="Built against the GlobalMarketPlus 1.4.3 API"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/floodgate-2.x-00AAFF" alt="Floodgate 2.x is required on the backend"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Java-8%2B_bytecode-orange" alt="Java 8 bytecode; runtime Java version follows the server"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Spigot%20%2F%20Paper-supported-blue" alt="Spigot and Paper supported"></a>
  <a href="#requirements-versions-and-server-software"><img src="https://img.shields.io/badge/Folia-conditional-blueviolet" alt="Conditional Folia support"></a>
</p>

<p align="center"><b>English</b> · <a href="README.zh-CN.md">简体中文</a></p>
<p align="center">
  <a href="https://github.com/che11111/GlobalMarketPlusBedrock/releases">Download</a> ·
  <a href="https://github.com/che11111/GlobalMarketPlusBedrock/issues">Report an Issue</a> ·
  <a href="https://bstats.org/plugin/bukkit/GlobalMarketPlusBedrock/34829">bStats</a>
</p>

GlobalMarketPlusBedrock is a Bukkit addon for [GlobalMarketPlus](https://www.spigotmc.org/resources/103648/). It provides native Bedrock Edition form interfaces for marketplace listings, auctions, shops, player profiles, and mailboxes to players connecting through Geyser and Floodgate.

Permissions, inventory operations, payments, taxes, and item delivery are still handled by GMP. This README also serves as the installation, configuration, and administration guide.

## Demo111

<p align="center">
  <img src="https://raw.githubusercontent.com/che11111/GlobalMarketPlusBedrock/main/img/202610294010951.gif" alt="GlobalMarketPlusBedrock demo" width="800">
</p>

## Features

- Provides Bedrock forms for selling, buying, auctions, merchandise management, mailboxes, shops, merchant profiles, and categories.
- Supports product search, filtering, pagination, sorting, and item icons. Shulker box and container contents can be previewed in read-only mode without allowing items to be moved.
- Supports listing creation and management, price and option changes, bidding and early auction purchases, and mailbox operations. Validation and transaction processing remain under GMP's control.

## Requirements, Versions, and Server Software

| Component | Requirements and Compatibility |
| --- | --- |
| Server software | **Minecraft 1.16+**; Spigot, Paper, Purpur, Folia, and others |
| GlobalMarketPlus | **Required**. Developed against **version 1.4.3.0**; other versions require separate verification |
| Floodgate | **Required**. Must be installed on the **backend server running this addon**. Installing it only on the proxy does not replace the backend API |
| Geyser | Required for Bedrock players to connect. Can be deployed on the proxy or backend |
| PlaceholderAPI | **Required** for parsing placeholders in the addon's messages |

The actual compatible version range depends on compatibility between GMP, Geyser, Floodgate, and PlaceholderAPI.

## Installation and Updates

1. Install GlobalMarketPlus, Floodgate, and PlaceholderAPI on the backend server, and configure Geyser to accept Bedrock connections.
2. Download the addon and place it in the `plugins/` directory.
3. Start the server, join using a Bedrock Edition client, and run `/gmpbe`, `/bemarket`, or the full command `/globalmarketplusbedrock`.

When upgrading, keep your existing configuration and language files. Reloading fills in missing default entries while preserving existing values. If a YAML file is invalid, the addon keeps the original file and falls back to the last working text or built-in messages.

After changing the configuration or language files, run `/gmpbe reload`.

## Commands and Permissions

| Command | Permission | Default |
| --- | --- | --- |
| `/globalmarketplusbedrock` | `globalmarketplusbedrock.use` | Everyone |
| `/gmpbe reload` | `globalmarketplusbedrock.admin` | OP |
| `/gmpbe update` | `globalmarketplusbedrock.admin` | OP |

Aliases: `/gmpbe`, `/bemarket`.

Full command: `/globalmarketplusbedrock`.

The permissions provided by this addon do **not** grant GMP permissions. Menu access follows GMP's configurable permission nodes, default permissions, permitted user groups, currency permissions, module toggles, and disabled-world settings. All write operations are still validated by GMP.

## Menu Feature Comparison

| GMP Built-in Menu | Bedrock Form Implementation |
| --- | --- |
| GlobalMarketGUI | Sell, buy, and auction listings; pagination, categories, GMP sorting, and manual refresh |
| TransactionGUI | Whole-lot or per-item purchases, integer quantity input, total quote calculation, and confirmation |
| AuctionGUI | View current bids and the highest bidder; enter bid amounts manually or adjust them by ±10/100/1000; buy out auctions early from merchandise management |
| MerchandiseGUI | My merchandise, delisting, whole-lot/per-item prices, never-expiring listings, unlimited merchandise, and toggles for hidden listings/seller names; manage other players' merchandise when authorized by UID |
| MailboxGUI | Filter pending, all, expired, and claimed mail; claim/delete individual messages; claim/delete the entire mailbox; categories and sorting |
| MerchantProfileGUI | Edit shop name and description; hide the shop, close it for business, lock the mailbox, configure sorting/categories, and refresh merchant caches |
| MerchantStoreGUI | Shop summary, description, statistics, paginated product browsing, and transactions |
| MerchantStoreListGUI | Shop list, pagination, and shop access; closed shops are hidden from visitor lists |
| CategoryGUI | Select or reset GMP categories and apply them to marketplace, shop, and mailbox lists |
| SearchGUI | Filter by material, seller, item name/description, price range, currency, category, and item type; find similar listings |
| SellInventoryGUI | Enter a price in a form, then use **GMP's native chest GUI** to complete equivalent-price or average-price bulk selling |
| BuyInventoryGUI | Enter a price in a form, then use **GMP's native chest GUI** to complete equivalent-price or average-price bulk buying |



